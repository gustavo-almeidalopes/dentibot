/**
 * O orçamento de peso da landing (ODS-18).
 *
 *   cd web && npm run build && node a11y/peso.mjs
 *
 * Soma o gzip do JS e do CSS que o index.html carrega de cara — a entrada, os
 * modulepreload e a folha de estilo. Os chunks sob demanda (o sistema, o Clerk)
 * não entram: quem abre a landing não baixa nenhum deles. Fonte também não
 * entra: vem pelo CSS, e o navegador só busca o subconjunto que usa.
 */
import { readFileSync } from 'node:fs';
import { gzipSync } from 'node:zlib';

/* Medido em 27/09/2026, depois da divisão por rota, do Clerk fora da landing e
   das fontes da casa: 93,9 KB (JS 86,8 + CSS 7,1). O limite é isso mais 10%,
   arredondado para cima. Pela mesma conta, antes destas mudanças a landing
   carregava 154,9 KB (JS 148,9 + CSS 6,0) — e ainda baixava o clerk-js e o
   @clerk/ui do CDN do Clerk e o Google Fonts, que nem entram nesta soma.
   Subir este número é decisão, não ajuste: o commit que o sobe diz por quê. */
const LIMITE_KB = 104;

const DIST = new URL('../dist/', import.meta.url);
const html = readFileSync(new URL('index.html', DIST), 'utf8');
const arquivos = [...new Set(
  [...html.matchAll(/(?:src|href)="\/(assets\/[^"]+\.(?:js|css))"/g)].map((m) => m[1]),
)];

let total = 0;
for (const arquivo of arquivos) {
  const kb = gzipSync(readFileSync(new URL(arquivo, DIST))).length / 1024;
  total += kb;
  console.log(`${kb.toFixed(1).padStart(7)} KB  ${arquivo}`);
}
console.log(`${total.toFixed(1).padStart(7)} KB  total em gzip — limite ${LIMITE_KB} KB`);

if (arquivos.length === 0) {
  console.error('O index.html do dist não referencia nenhum asset — o build mudou de forma?');
  process.exitCode = 1;
} else if (total > LIMITE_KB) {
  console.error('Acima do orçamento de peso da landing.');
  process.exitCode = 1;
}
