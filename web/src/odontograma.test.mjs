import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { CONDICOES } from './odontograma.js';

const SQL = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'api', 'src', 'main',
  'resources', 'db', 'migration', 'V12__prontuario.sql');

test('as condições do odontograma da tela são as do CHECK do banco', () => {
  const sql = readFileSync(SQL, 'utf8');
  const bloco = sql.match(/condicao\s+TEXT\s+NOT NULL CHECK \(condicao IN \(([\s\S]*?)\)\)/);
  assert.ok(bloco, 'não achei o CHECK de condicao na V12 — o SQL mudou de forma');
  const doBanco = [...bloco[1].matchAll(/'([a-z_]+)'/g)].map((m) => m[1]).sort();
  assert.deepEqual(CONDICOES.map((c) => c.valor).sort(), doBanco);
});
