/**
 * Os mapas de apresentação usam como chave os valores que o banco grava. Cada
 * um é conferido contra o CHECK da coluna, lido da migration: um valor que o
 * banco aceita e a tela não conhece aparece cru, e um que a tela manda e o
 * banco não aceita vira erro no clique.
 *
 *   cd web && npm test
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import {
  ACAO_AUDITORIA, CONDICAO_DENTE, STATUS_CONSULTA, STATUS_MEMBRO, STATUS_PACIENTE,
  STATUS_RECEBIVEL,
} from './apresentacao.js';

const migration = (nome) => readFileSync(
  new URL(`../../api/src/main/resources/db/migration/${nome}`, import.meta.url), 'utf8');

const valores = (lista) => [...lista.matchAll(/'([^']+)'/g)].map((m) => m[1]).sort();

/** Os valores de `CHECK (coluna IN (...))` dentro do CREATE TABLE da tabela. */
function check(arquivo, tabela, coluna) {
  const sql = migration(arquivo);
  const inicio = sql.indexOf(`CREATE TABLE ${tabela} (`);
  assert.ok(inicio >= 0, `não achei CREATE TABLE ${tabela} em ${arquivo}`);
  const bloco = sql.slice(inicio, sql.indexOf('\n);', inicio));
  const m = bloco.match(new RegExp(`CHECK\\s*\\(\\s*${coluna}\\s+IN\\s*\\(([^)]*)\\)`));
  assert.ok(m, `não achei o CHECK de ${tabela}.${coluna}`);
  return valores(m[1]);
}

const chaves = (mapa) => Object.keys(mapa).sort();

for (const [nome, mapa, arquivo, tabela, coluna] of [
  ['STATUS_CONSULTA', STATUS_CONSULTA, 'V10__agenda.sql', 'agenda.consultas', 'status'],
  ['STATUS_RECEBIVEL', STATUS_RECEBIVEL, 'V14__financeiro.sql', 'financeiro.contas_receber', 'status'],
  ['STATUS_MEMBRO', STATUS_MEMBRO, 'V3__identidade.sql', 'identidade.usuarios', 'status'],
  ['STATUS_PACIENTE', STATUS_PACIENTE, 'V9__pacientes.sql', 'pacientes.pacientes', 'status'],
  ['ACAO_AUDITORIA', ACAO_AUDITORIA, 'V5__auditoria.sql', 'auditoria.eventos', 'acao'],
]) {
  test(`${nome} tem os valores do CHECK de ${tabela}.${coluna}`, () => {
    assert.deepEqual(chaves(mapa), check(arquivo, tabela, coluna));
  });
}

test('CONDICAO_DENTE segue os dois lados do ck_face_coerente', () => {
  const sql = migration('V12__prontuario.sql');
  const m = sql.match(/ck_face_coerente CHECK \(\s*\(condicao IN \(([^)]*)\) AND face IS NULL\)\s*OR \(condicao IN \(([^)]*)\) AND face IS NOT NULL\)/);
  assert.ok(m, 'não achei o ck_face_coerente — a migration mudou de forma');
  const doMapa = (inteiro) => Object.entries(CONDICAO_DENTE)
    .filter(([, c]) => c.inteiro === inteiro).map(([v]) => v).sort();
  assert.deepEqual(doMapa(true), valores(m[1]));
  assert.deepEqual(doMapa(false), valores(m[2]));
});
