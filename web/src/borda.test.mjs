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

/**
 * Conteúdo dos `<script>` sem `src`. Varredura por índice e não regex: regex de
 * HTML sempre esquece um caso (`<SCRIPT>`, `</script\t>`), e um script inline
 * esquecido aqui é um script sem hash na CSP.
 */
function scriptsInline(html) {
  const baixo = html.toLowerCase();
  const achados = [];
  let i = 0;
  for (;;) {
    const abre = baixo.indexOf('<script', i);
    if (abre < 0) return achados;
    const fimAbre = baixo.indexOf('>', abre);
    const fecha = baixo.indexOf('</script', fimAbre);
    if (fimAbre < 0 || fecha < 0) return achados;
    if (!/\bsrc\s*=/.test(baixo.slice(abre, fimAbre))) achados.push(html.slice(fimAbre + 1, fecha));
    i = fecha + 1;
  }
}

test('todo script inline do index.html tem o sha256 no script-src', () => {
  const html = readFileSync(join(WEB, 'index.html'), 'utf8');
  const inlines = scriptsInline(html);
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

test('a API padrão do build da Vercel está no connect-src', () => {
  /* Lido como texto, como o api.test.mjs lê o Java: importar o vite.config.js
     puxaria o Vite inteiro para dentro do `node --test`. */
  const config = readFileSync(join(WEB, 'vite.config.js'), 'utf8');
  const url = config.match(/API_NA_VERCEL = '([^']+)'/)?.[1];
  assert.ok(url, 'não achei API_NA_VERCEL no vite.config.js');
  const { origin } = new URL(url);
  assert.ok(diretiva('connect-src').split(/\s+/).includes(origin), `connect-src sem ${origin}`);
});

test('script-src não abre mão do que a CSP existe para impedir', () => {
  assert.doesNotMatch(diretiva('script-src'), /'unsafe-(inline|eval)'/);
  assert.match(csp(), /frame-ancestors 'none'/);
  assert.match(csp(), /object-src 'none'/);
});
