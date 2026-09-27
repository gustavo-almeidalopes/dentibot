// ST-47: o pico da manhã (8h–10h) — 500 pessoas abrindo a agenda do dia,
// conferindo paciente, voltando à agenda. Meta: p95 da agenda < 400 ms, e
// nenhum endpoint mais de 20% pior que perf/linha-de-base.json.
//
//   docker run --rm -i -v "%cd%/perf:/perf" -e TOKEN=<jwt> grafana/k6 run /perf/agenda.js
//
// TOKEN é um JWT do Clerk com vida longa (JWT template no dashboard): o de
// sessão vence em 60 s. Todos os usuários virtuais usam a mesma identidade,
// então a API precisa subir com DENTIBOT_RATELIMIT_GERAL_POR_MINUTO alto —
// senão o teste mede o rate limit, e o limiar de falhas reprova.
// GRAVAR=1 reescreve a linha de base com o p95 desta execução.
import { check, sleep } from 'k6';
import http from 'k6/http';

const API = __ENV.API_URL || 'http://host.docker.internal:8080';
const cab = { headers: { Authorization: `Bearer ${__ENV.TOKEN}` } };
const base = JSON.parse(open('./linha-de-base.json'));

// 20% sobre a linha de base, e a agenda nunca acima da meta absoluta. Lista
// vazia ainda cria a submétrica, que é o que o handleSummary lê.
const limite = (nome, teto = Infinity) => {
  const ms = Math.min(teto, base[nome] ? base[nome] * 1.2 : teto);
  return Number.isFinite(ms) ? [`p(95)<${ms}`] : [];
};

export const options = {
  scenarios: {
    manha: {
      executor: 'ramping-vus',
      stages: [
        { duration: '2m', target: 500 },
        { duration: '5m', target: 500 },
        { duration: '1m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{nome:agenda}': limite('agenda', 400),
    'http_req_duration{nome:pacientes}': limite('pacientes'),
    'http_req_duration{nome:resumo}': limite('resumo'),
  },
};

const dia = () => {
  const d = new Date();
  d.setUTCHours(3, 0, 0, 0); // meia-noite em Brasília
  return [d.toISOString(), new Date(d.getTime() + 864e5 - 1).toISOString()];
};

export default function () {
  const [de, ate] = dia();
  const agenda = http.get(`${API}/api/v1/consultas?de=${de}&ate=${ate}`,
    { ...cab, tags: { nome: 'agenda' } });
  check(agenda, { 'agenda 200': (r) => r.status === 200 });

  const pacientes = http.get(`${API}/api/v1/pacientes?limite=50`,
    { ...cab, tags: { nome: 'pacientes' } });
  check(pacientes, { 'pacientes 200': (r) => r.status === 200 });

  // A recepção abre o resumo de quem está chegando.
  const consultas = agenda.status === 200 ? agenda.json() : [];
  if (consultas.length) {
    const c = consultas[Math.floor(Math.random() * consultas.length)];
    http.get(`${API}/api/v1/pacientes/${c.idPaciente}/resumo`, { ...cab, tags: { nome: 'resumo' } });
  }
  sleep(1 + Math.random() * 2);
}

export function handleSummary(dados) {
  const p95 = (nome) => dados.metrics[`http_req_duration{nome:${nome}}`]?.values['p(95)'];
  const saida = { stdout: `\np95 (ms): agenda ${p95('agenda')}, pacientes ${p95('pacientes')}, resumo ${p95('resumo')}\n` };
  if (__ENV.GRAVAR) {
    saida['/perf/linha-de-base.json'] = JSON.stringify(
      { agenda: p95('agenda'), pacientes: p95('pacientes'), resumo: p95('resumo') }, null, 2) + '\n';
  }
  return saida;
}
