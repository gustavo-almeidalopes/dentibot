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
import { ApiError, EXIGEM_IDEMPOTENCIA, semApi } from './api.js';

const AQUI = dirname(fileURLToPath(import.meta.url));
const FILTRO = join(AQUI, '..', '..', 'api', 'src', 'main', 'java', 'br', 'com',
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

test('404 sem corpo é o host sem API, e 404 do back-end não é', () => {
  // O back-end responde todo 404 em ProblemDetail. O 404 de texto é o da
  // Vercel servindo só o web — que antes aparecia como "Erro 404" na /agenda.
  assert.equal(semApi(new ApiError('Erro 404', 404, null)), true);
  assert.equal(semApi(new ApiError('Recurso não encontrado.', 404,
    { status: 404, detail: 'Recurso não encontrado.' })), false);
  // Outros status seguem pelo erro comum, com o "Tentar de novo".
  assert.equal(semApi(new ApiError('Erro 500', 500, null)), false);
  assert.equal(semApi(new TypeError('Failed to fetch')), false);
  assert.equal(semApi(undefined), false);
});
