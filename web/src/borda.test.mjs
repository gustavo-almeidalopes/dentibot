/**
 * Os cabeçalhos de segurança do web (ST-32) vivem em DOIS vercel.json — um por
 * Root Directory possível — e a Vercel lê só um. Duplicata silenciosa apodrece:
 * este teste exige que os dois digam o mesmo, e que a CSP cubra o script inline
 * do index.html. Editar aquela linha sem atualizar o hash quebraria a landing em
 * produção sem erro nenhum na tela; aqui quebra o teste.
 *
 *   cd web && npm test
 */
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const WEB = join(dirname(fileURLToPath(import.meta.url)), '..');
const lerJson = (p) => JSON.parse(readFileSync(p, 'utf8'));
const daRaiz = lerJson(join(WEB, '..', 'vercel.json'));
const doWeb = lerJson(join(WEB, 'vercel.json'));

const cabecalhos = (config) =>
  Object.fromEntries((config.headers ?? []).flatMap((h) => h.headers).map((h) => [h.key, h.value]));
const csp = () => cabecalhos(doWeb)['Content-Security-Policy'] ?? '';
const diretiva = (nome) =>
  csp().split(';').map((d) => d.trim()).find((d) => d.startsWith(`${nome} `)) ?? '';

test('os dois vercel.json têm os mesmos headers e rewrites', () => {
  assert.deepEqual(daRaiz.headers, doWeb.headers);
  assert.deepEqual(daRaiz.rewrites, doWeb.rewrites);
});

test('os seis cabeçalhos existem', () => {
  const nomes = Object.keys(cabecalhos(doWeb));
  for (const nome of ['Content-Security-Policy', 'Strict-Transport-Security',
    'X-Content-Type-Options', 'X-Frame-Options', 'Referrer-Policy', 'Permissions-Policy']) {
    assert.ok(nomes.includes(nome), `falta ${nome}`);
  }
});

test('todo script inline do index.html tem o sha256 no script-src', () => {
  const html = readFileSync(join(WEB, 'index.html'), 'utf8');
  // Qualquer caixa e atributo: `<SCRIPT>` inline também roda e precisa do hash.
  const inlines = [...html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script\s*>/gi)]
    .filter((m) => !/\bsrc\s*=/i.test(m[1]))
    .map((m) => m[2]);
  assert.ok(inlines.length > 0, 'o index.html não tem mais script inline — revise a CSP');
  for (const js of inlines) {
    const hash = `'sha256-${createHash('sha256').update(js).digest('base64')}'`;
    assert.ok(diretiva('script-src').includes(hash), `script-src sem ${hash}`);
  }
});

test('security.txt tem contato e não está para vencer (RFC 9116, ST-36)', () => {
  const txt = readFileSync(join(WEB, 'public', '.well-known', 'security.txt'), 'utf8');
  assert.match(txt, /^Contact: https:\/\//m);
  const expira = new Date(txt.match(/^Expires: (.+)$/m)?.[1]);
  const trintaDias = 30 * 24 * 3600 * 1000;
  // Vencido, o arquivo é tratado como abandonado por quem reporta. Renove o
  // Expires por mais um ano quando este teste avisar.
  assert.ok(expira - Date.now() > trintaDias, `security.txt vence em ${expira.toISOString()}`);
});

test('script-src não abre mão do que a CSP existe para impedir', () => {
  assert.doesNotMatch(diretiva('script-src'), /'unsafe-(inline|eval)'/);
  assert.match(csp(), /frame-ancestors 'none'/);
  assert.match(csp(), /object-src 'none'/);
});
