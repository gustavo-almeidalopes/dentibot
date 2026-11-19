/**
 * O app duplica uma coisa que mora no backend: a lista de rotas que exigem
 * `Idempotency-Key`. Duplicata silenciosa é a que apodrece — o backend ganha
 * um prefixo, o app continua mandando POST sem a chave, e o usuário toma 400
 * sem entender.
 *
 * Este teste lê o Java e compara. Segue o estilo do próprio repositório, onde
 * `FronteiraDeSchemaTest` varre SQL e `ConformidadeDoSchemaTest` consulta o
 * catálogo do Postgres: verificar por máquina, não por revisão.
 *
 *   node --test src/*.test.mjs
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const AQUI = dirname(fileURLToPath(import.meta.url));
const JAVA = join(AQUI, '..', '..', 'api', 'src', 'main', 'java', 'br', 'com', 'dentibot');

const ler = (...p) => readFileSync(join(...p), 'utf8');
const citadas = (texto) => [...texto.matchAll(/"([a-z_/0-9.]+)"/g)].map((m) => m[1]);

test('a lista de rotas idempotentes do app é a do FiltroIdempotencia', () => {
  const java = ler(JAVA, 'plataforma', 'idempotencia', 'FiltroIdempotencia.java');
  const bloco = java.match(/PREFIXOS_OBRIGATORIOS\s*=\s*Set\.of\(([\s\S]*?)\);/);
  assert.ok(bloco, 'não achei PREFIXOS_OBRIGATORIOS — o Java mudou de forma');

  const doBackend = citadas(bloco[1]).sort();

  const ts = ler(AQUI, 'api.ts');
  const blocoTs = ts.match(/EXIGEM_IDEMPOTENCIA\s*=\s*\[([\s\S]*?)\]/);
  assert.ok(blocoTs, 'não achei EXIGEM_IDEMPOTENCIA em api.ts');
  const doApp = [...blocoTs[1].matchAll(/'([^']+)'/g)].map((m) => m[1]).sort();

  assert.deepEqual(
    doApp,
    doBackend,
    'app e backend discordam sobre quais POSTs exigem Idempotency-Key',
  );
});
