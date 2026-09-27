/**
 * ST-54: o que o service worker pode pôr no disco, e qual agenda a página
 * offline mostra. A primeira é regra de LGPD — prontuário em cache de
 * computador de recepção é vazamento — e por isso tem teste.
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { escolherAgenda } from '../public/agenda-offline.js';

const sw = readFileSync(new URL('../public/sw.js', import.meta.url), 'utf8');
const GUARDA = new RegExp(sw.match(/const GUARDA = \/(.+)\/;/)[1]);
const guarda = (caminho) => GUARDA.test(caminho);

test('só a agenda e a lista de dentistas vão para o disco', () => {
  assert.ok(guarda('/api/v1/consultas?de=2026-09-27T03:00:00Z&ate=2026-09-28T03:00:00Z'));
  assert.ok(guarda('/api/v1/equipe/dentistas'));
  for (const fora of [
    '/api/v1/pacientes/7/prontuario/evolucoes',
    '/api/v1/pacientes?limite=200',
    '/api/v1/financeiro/recebiveis',
    '/api/v1/consultas/12/confirmar',
    '/api/v1/comunicacao/escaladas',
    '/api/v1/eu',
  ]) assert.ok(!guarda(fora), fora);
});

test('a agenda offline é a do dia; sem ela, a salva por último', () => {
  const url = (de, ate) => `https://x.test/api/v1/consultas?de=${de}&ate=${ate}`;
  const ontem = { url: url('2026-09-26T03:00:00Z', '2026-09-27T03:00:00Z'), salvoEm: '2026-09-26T20:00:00Z', consultas: [] };
  const hoje = { url: url('2026-09-27T03:00:00Z', '2026-09-28T03:00:00Z'), salvoEm: '2026-09-27T08:00:00Z', consultas: [] };
  const amanha = { url: url('2026-09-28T03:00:00Z', '2026-09-29T03:00:00Z'), salvoEm: '2026-09-27T09:00:00Z', consultas: [] };
  const agora = Date.parse('2026-09-27T14:00:00Z');
  assert.equal(escolherAgenda([ontem, hoje, amanha], agora), hoje);
  assert.equal(escolherAgenda([ontem, amanha], agora), amanha);
  assert.equal(escolherAgenda([], agora), null);
});
