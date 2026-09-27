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
  STATUS_CONSULTA, agruparPorHora, contagem, descrever, faixaHoraria,
  porExtenso, somarDias, telHref,
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

test('dinheiro, quantidade e data curta não quebram com ausente', async () => {
  const { reais, quantidade, dataCurta } = await import('./apresentacao.js');
  assert.match(reais(200), /R\$\s?200,00/);
  assert.match(reais(null), /R\$\s?0,00/);
  assert.equal(quantidade(0.3333333), '0,333');
  assert.equal(dataCurta(null), '—');
});
