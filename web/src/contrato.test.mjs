/**
 * ST-55: toda chamada que o web faz existe no contrato da API.
 *
 * O contrato é api/openapi.json, gerado do código e travado pelo
 * ContratoOpenApiTest (ST-20). Endpoint renomeado ou removido quebra aqui, no
 * teste, e não em produção na frente de quem clicou. Não é gerador de cliente:
 * o web é JavaScript, e um cliente tipado gerado não teria quem o checasse —
 * este teste é o compilador que faltava.
 */
import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const RAIZ = fileURLToPath(new URL('.', import.meta.url));
const contrato = JSON.parse(readFileSync(new URL('../../api/openapi.json', import.meta.url), 'utf8'));

const forma = (caminho) => caminho.split('?')[0].replace(/\$\{[^}]*\}|\{[^}]+\}/g, '{}');
const rotas = Object.entries(contrato.paths)
  .map(([caminho, ops]) => [forma(caminho), new Set(Object.keys(ops))]);

/* Do lado do cliente, `${x}` é qualquer segmento — pode ser um id ou uma ação
   (`/consultas/${id}/${acao}`). No começo do caminho é um prefixo inteiro
   (`${base}/confirmar`). */
const padrao = (caminho) => new RegExp(`^${forma(caminho)
  .replace(/[.*+?^$()|[\]\\]/g, '\\$&')
  .replace(/^\/api\/v1\{\}/, '/api/v1.+')
  .replace(/\{\}/g, '[^/]+')}$`);
const existe = ({ metodo, caminho }) => {
  const re = padrao(caminho);
  return rotas.some(([c, ops]) => re.test(c) && ops.has(metodo));
};

function fontes(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = join(dir, e.name);
    if (e.isDirectory()) return fontes(p);
    return /\.(js|jsx)$/.test(e.name) && !e.name.includes('.test.') ? [p] : [];
  });
}

const METODO = { get: 'get', post: 'post', put: 'put', del: 'delete', useRecurso: 'get', baixar: 'get' };
const CHAMADA = /\b(?:api\.(get|post|put|del)|(useRecurso|baixar))\(\s*(['`])([^'`]*)\3/g;

export function chamadasDoWeb() {
  return fontes(RAIZ).flatMap((arquivo) => [...readFileSync(arquivo, 'utf8').matchAll(CHAMADA)]
    .map((m) => ({ arquivo, metodo: METODO[m[1] ?? m[2]], caminho: `/api/v1${m[4]}` })));
}

test('o web só chama o que existe na API', () => {
  const chamadas = chamadasDoWeb();
  assert.ok(chamadas.length > 30, `achei só ${chamadas.length} chamadas — a regex quebrou?`);
  const fora = chamadas.filter((c) => !existe(c))
    .map((c) => `${c.metodo.toUpperCase()} ${c.caminho} (${c.arquivo.split(/[\\/]src[\\/]/)[1]})`);
  assert.deepEqual(fora, [], 'chamadas sem endpoint no contrato (api/openapi.json)');
});
