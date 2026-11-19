/**
 * A parte das telas que dá para provar sem navegador.
 *
 *   cd web && npm test
 *
 * Layout se confere olhando, mas "14:00–14:45", "200 primeiros" e o tom do selo
 * são decisões com resposta certa — e todas estavam espalhadas dentro do JSX,
 * onde não havia como testá-las.
 */
import assert from 'node:assert/strict';
import test from 'node:test';
import {
  STATUS_CONSULTA, agruparPorHora, casaBusca, contagem, descrever, faixaHoraria, horarios,
  jaPassou, momento, porExtenso, relativo, somarDias, telHref, whatsappHref,
} from './apresentacao.js';

test('status conhecido vira rótulo em português e tom de selo', () => {
  assert.deepEqual(descrever(STATUS_CONSULTA, 'em_atendimento'),
    { rotulo: 'Em atendimento', tom: 'alarme' });
  assert.equal(descrever(STATUS_CONSULTA, 'cancelada').tom, 'riscado');
});

test('status desconhecido aparece como veio, em vez de sumir', () => {
  // O CHECK do banco pode ganhar um valor antes desta tela. Mostrar o valor cru
  // é feio; esconder a linha é perder informação clínica.
  assert.deepEqual(descrever(STATUS_CONSULTA, 'remarcada'),
    { rotulo: 'remarcada', tom: 'fraco' });
  assert.equal(descrever(STATUS_CONSULTA, null).rotulo, '—');
});

test('a faixa mostra início e término', () => {
  assert.equal(
    faixaHoraria('2026-09-13T17:00:00Z', '2026-09-13T17:45:00Z').replace(/\s/g, ''),
    '14:00–14:45',
  );
});

test('sem término a faixa não inventa um', () => {
  // `new Date(null)` é 01/01/1970, não erro — numa agenda isso passa por hora
  // de verdade.
  assert.equal(faixaHoraria('2026-09-13T17:00:00Z', null).replace(/\s/g, ''), '14:00');
});

test('o dia é agrupado por hora cheia, em ordem', () => {
  const consultas = [
    { idConsulta: 2, inicioEm: '2026-09-13T18:30:00Z' },
    { idConsulta: 1, inicioEm: '2026-09-13T17:00:00Z' },
    { idConsulta: 3, inicioEm: '2026-09-13T17:00:00Z' },
  ];
  const calha = agruparPorHora(consultas);

  assert.deepEqual(calha.map((d) => d.hora), ['14', '15']);
  // Duas cadeiras atendendo às 14h é o caso normal de uma clínica, não anomalia.
  assert.deepEqual(calha[0].consultas.map((c) => c.idConsulta), [1, 3]);
});

test('telefone vira link discável, vazio não vira nada', () => {
  assert.equal(telHref('(11) 99999-8888'), 'tel:+5511999998888');
  assert.equal(telHref('+55 11 99999-8888'), 'tel:+5511999998888');
  assert.equal(telHref(''), null);
  assert.equal(telHref(null), null);
});

test('somar dias atravessa mês e ano sem fuso no caminho', () => {
  assert.equal(somarDias('2026-09-30', 1), '2026-10-01');
  assert.equal(somarDias('2026-01-01', -1), '2025-12-31');
  assert.equal(somarDias('2026-03-01', -1), '2026-02-28');
});

test('a contagem não mente quando bateu o limite', () => {
  // A tela pedia limite=200, recebia 200 e escrevia "200 no cadastro". Numa
  // clínica com 900 pacientes a frase é falsa.
  assert.equal(contagem(200, 200, 'paciente', 'pacientes'), '200 primeiros');
  assert.equal(contagem(7, 200, 'paciente', 'pacientes'), '7 pacientes');
  assert.equal(contagem(1, 200, 'paciente', 'pacientes'), '1 paciente');
  assert.equal(contagem(0, 200, 'paciente', 'pacientes'), 'nenhum paciente');
});

test('o zero concorda em gênero com o substantivo', () => {
  // "nenhum consulta" é o tipo de erro que só aparece em produção, no dia em
  // que a agenda amanhece vazia.
  assert.equal(contagem(0, Infinity, 'consulta', 'consultas'), 'nenhuma consulta');
  assert.equal(contagem(0, 200, 'parcela', 'parcelas'), 'nenhuma parcela');
});

test('o dia por extenso é o dia civil, não o de UTC', () => {
  assert.match(porExtenso('2026-09-13'), /domingo/i);
  assert.match(porExtenso('2026-09-13'), /13 de setembro/i);
});

test('a grade de horários respeita o [) do banco e esconde o que passou', () => {
  const ocupada = {
    status: 'confirmada',
    inicioEm: new Date(2026, 9, 13, 9, 0).toISOString(),
    terminoEm: new Date(2026, 9, 13, 10, 0).toISOString(),
  };
  const vespera = new Date(2026, 9, 12, 12, 0);
  const em = (grade, h) => grade.find((g) => g.hora === h);
  const grade = horarios('2026-10-13', [ocupada], 30, vespera);

  assert.equal(em(grade, '08:00').livre, true);
  assert.equal(em(grade, '08:30').livre, true, '08:30–09:00 encosta e não sobrepõe');
  assert.equal(em(grade, '09:00').livre, false);
  assert.equal(em(grade, '09:30').livre, false);
  assert.equal(em(grade, '10:00').livre, true, 'quem termina 10:00 não bloqueia 10:00');
  assert.equal(grade.at(-1).hora, '18:30', 'a última cabe inteira antes de fechar');

  // Com 60 min, 08:30 já invade a das 9h.
  assert.equal(em(horarios('2026-10-13', [ocupada], 60, vespera), '08:30').livre, false);

  // Cancelada libera o horário, como no WHERE da EXCLUDE.
  assert.equal(em(horarios('2026-10-13', [{ ...ocupada, status: 'cancelada' }], 30, vespera),
    '09:00').livre, true);

  // O que já passou nem aparece.
  assert.equal(horarios('2026-10-13', [], 30, new Date(2026, 9, 13, 14, 10))[0].hora, '14:30');
});

test('o momento separa quem está na cadeira de quem é o próximo', () => {
  const c = (id, h, m, dur, status = 'agendada') => ({
    idConsulta: id, status,
    inicioEm: new Date(2026, 9, 13, h, m).toISOString(),
    terminoEm: new Date(2026, 9, 13, h, m + dur).toISOString(),
  });
  const agora = new Date(2026, 9, 13, 10, 15);
  const { naCadeira, proxima } = momento([
    c(3, 11, 0, 30), c(1, 10, 0, 45, 'confirmada'), c(2, 10, 30, 30),
    c(4, 9, 0, 30, 'realizada'), c(5, 10, 20, 30, 'cancelada'),
  ], agora);

  assert.deepEqual(naCadeira.map((x) => x.idConsulta), [1]);
  assert.equal(proxima.idConsulta, 2, 'a mais cedo, não a primeira da resposta');
  assert.equal(jaPassou(c(4, 9, 0, 30, 'realizada'), agora), true);
  assert.equal(jaPassou(c(1, 10, 0, 45), agora), false);
});

test('o tempo relativo é o que a recepção fala em voz alta', () => {
  const agora = new Date(2026, 9, 13, 10, 0);
  assert.equal(relativo(new Date(2026, 9, 13, 10, 25), agora), 'em 25 min');
  assert.equal(relativo(new Date(2026, 9, 13, 9, 55), agora), 'há 5 min');
  assert.equal(relativo(new Date(2026, 9, 13, 11, 10), agora), 'em 1 h 10 min');
  assert.equal(relativo(new Date(2026, 9, 13, 12, 0), agora), 'em 2 h');
  assert.equal(relativo(agora, agora), 'agora');
});

test('a busca ignora acento e casa telefone por dígitos', () => {
  assert.equal(casaBusca('joao', 'João Pedro', null), true);
  assert.equal(casaBusca('987-65', 'Ana', '11987654321'), true);
  assert.equal(casaBusca('1', 'Ana', '11987654321'), false, 'um dígito casaria todo DDD 11');
  assert.equal(casaBusca('', 'Ana', null), true);
});

test('o link do WhatsApp sai em E.164 com a mensagem codificada', () => {
  assert.equal(whatsappHref('(11) 98765-4321', 'Olá, Ana!'),
    'https://wa.me/5511987654321?text=Ol%C3%A1%2C%20Ana!');
  assert.equal(whatsappHref('123', 'x'), null);
});
