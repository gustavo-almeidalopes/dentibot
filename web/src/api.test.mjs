/**
 * A lista de rotas idempotentes do front é a do back-end.
 *
 * <p>Mesmo teste que o `app/src/contrato.test.mjs` já faz para o mobile, agora
 * para a web — e pelo mesmo motivo: a duplicata silenciosa é a que apodrece.
 * O back-end ganha um prefixo, o front continua mandando POST sem a chave, e o
 * usuário toma 400 numa tela que funcionava ontem.
 *
 *   cd web && npm test
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { EXIGEM_IDEMPOTENCIA } from './api.js';

const AQUI = dirname(fileURLToPath(import.meta.url));
const FILTRO = join(AQUI, '..', '..', 'api-java', 'src', 'main', 'java', 'br', 'com',
  'dentibot', 'plataforma', 'idempotencia', 'FiltroIdempotencia.java');

test('a lista de rotas idempotentes do front é a do FiltroIdempotencia', () => {
  const java = readFileSync(FILTRO, 'utf8');
  const bloco = java.match(/PREFIXOS_OBRIGATORIOS\s*=\s*Set\.of\(([\s\S]*?)\);/);
  assert.ok(bloco, 'não achei PREFIXOS_OBRIGATORIOS — o Java mudou de forma');

  // O Java guarda o caminho completo (/api/v1/consultas); o front guarda o
  // sufixo, porque o prefixo já está no BASE. Comparar exige tirar a base.
  const doBackend = [...bloco[1].matchAll(/"\/api\/v1(\/[a-z0-9/_-]+)"/g)]
    .map((m) => m[1])
    .sort();

  assert.ok(doBackend.length > 0, 'o extrator não encontrou prefixo nenhum');
  assert.deepEqual([...EXIGEM_IDEMPOTENCIA].sort(), doBackend);
});
