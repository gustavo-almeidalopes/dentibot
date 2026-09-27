/* Agenda sem conexão (ST-54). Lê o que o service worker guardou e mostra a
   agenda de hoje — ou a mais recente, dizendo de quando é. Módulo puro no
   topo, DOM só se houver documento: o node --test importa escolherAgenda. */

const AGENDA = 'dentibot-agenda';

/**
 * Das agendas salvas ({ url, salvoEm, consultas }), a que cobre `agora`; sem
 * nenhuma que cubra, a salva por último. A URL diz o intervalo (de, ate).
 */
export function escolherAgenda(salvas, agora) {
  const doDia = salvas.filter((s) => {
    const q = new URL(s.url).searchParams;
    const de = Date.parse(q.get('de'));
    const ate = Date.parse(q.get('ate'));
    return de <= agora && agora < ate;
  });
  const candidatas = doDia.length > 0 ? doDia : salvas;
  return candidatas.reduce((a, b) => (a && a.salvoEm >= b.salvoEm ? a : b), null);
}

async function lerSalvas() {
  const cache = await caches.open(AGENDA);
  const salvas = [];
  let dentistas = new Map();
  for (const req of await cache.keys()) {
    const r = await cache.match(req);
    const dados = await r.json();
    if (req.url.includes('/equipe/dentistas')) {
      dentistas = new Map(dados.map((d) => [d.idDentista, d.nomeCompleto]));
    } else {
      salvas.push({ url: req.url, salvoEm: r.headers.get('x-dentibot-salvo-em') ?? '', consultas: dados });
    }
  }
  return { salvas, dentistas };
}

const HORA = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
const DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' });
const DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const STATUS = {
  agendada: 'Agendada', confirmada: 'Confirmada', em_atendimento: 'Em atendimento',
  realizada: 'Realizada', faltou: 'Faltou', cancelada: 'Cancelada',
};

function linha(texto, classe) {
  const el = document.createElement('p');
  el.className = classe;
  el.textContent = texto;
  return el;
}

async function mostrar() {
  const alvo = document.getElementById('agenda');
  const { salvas, dentistas } = await lerSalvas();
  const agenda = escolherAgenda(salvas, Date.now());
  if (!agenda) {
    alvo.append(linha('Nenhuma agenda foi salva neste navegador. Ela é guardada quando a tela'
      + ' da agenda abre com internet.', 'nota'));
    return;
  }
  const de = new URL(agenda.url).searchParams.get('de');
  alvo.append(linha(DATA.format(new Date(de)), 'dia'));
  alvo.append(linha(`Salva em ${DATA_HORA.format(new Date(agenda.salvoEm))}. O que mudou depois disso não aparece aqui.`, 'nota'));
  const lista = document.createElement('ol');
  for (const c of [...agenda.consultas].sort((a, b) => a.inicioEm.localeCompare(b.inicioEm))) {
    const item = document.createElement('li');
    item.append(linha(HORA.format(new Date(c.inicioEm)), 'hora'));
    item.append(linha(c.nomePaciente ?? `Paciente ${c.idPaciente}`, 'nome'));
    item.append(linha([STATUS[c.status] ?? c.status, dentistas.get(c.idDentista), c.telefonePaciente]
      .filter(Boolean).join(' · '), 'nota'));
    lista.append(item);
  }
  if (agenda.consultas.length === 0) alvo.append(linha('Nenhuma consulta neste dia.', 'nota'));
  alvo.append(lista);
}

if (typeof document !== 'undefined') {
  mostrar();
  window.addEventListener('online', () => window.location.assign('/agenda'));
}
