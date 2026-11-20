/**
 * O contraste do app móvel (ODS-17), lido do `theme.ts`.
 *
 *   node --test src/*.test.mjs
 *
 * Mesmo idioma do contrato.test.mjs: lê o fonte, sem montar tela e sem
 * `npm ci`. O app só tem tema escuro, e é sobre a obsidiana que as cores daqui
 * precisam passar.
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const AQUI = dirname(fileURLToPath(import.meta.url));
const ler = (nome) => readFileSync(join(AQUI, nome), 'utf8');

const TEMA = ler('theme.ts');
const COR = Object.fromEntries(
  [...(TEMA.match(/export const cor = \{([\s\S]*?)\} as const/)?.[1] ?? '').matchAll(/(\w+):\s*'([^']+)'/g)]
    .map((m) => [m[1], m[2]]),
);

/** `[r, g, b, alfa]` de um `#rrggbb` ou de um `rgba()`. */
function rgba(valor) {
  const hex = valor.match(/^#([0-9a-f]{6})$/i);
  if (hex) return [0, 2, 4].map((i) => parseInt(hex[1].slice(i, i + 2), 16)).concat(1);
  const m = valor.match(/^rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+)\s*)?\)$/);
  assert.ok(m, `cor que o teste não sabe ler: ${valor}`);
  return [Number(m[1]), Number(m[2]), Number(m[3]), m[4] === undefined ? 1 : Number(m[4])];
}

/** Razão de contraste da WCAG 2.x. A frente com alfa é composta sobre o fundo. */
function contraste(frente, fundo) {
  const composta = frente.slice(0, 3).map((c, i) => c * frente[3] + fundo[i] * (1 - frente[3]));
  const luminancia = (rgb) => {
    const [r, g, b] = rgb.map((c) => {
      const s = c / 255;
      return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
    });
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  };
  const [clara, escura] = [luminancia(composta), luminancia(fundo)].sort((a, b) => b - a);
  return (clara + 0.05) / (escura + 0.05);
}

const sobreObsidiana = (nome) => contraste(rgba(COR[nome]), rgba(COR.obsidiana));

test('o tema do app foi lido', () => {
  assert.deepEqual(Object.keys(COR).sort(), ['alarme', 'cinza', 'fio', 'obsidiana', 'osso']);
});

test('todo texto do app passa sobre a obsidiana', () => {
  for (const nome of ['osso', 'cinza', 'alarme']) {
    const r = sobreObsidiana(nome);
    assert.ok(r >= 4.5, `${nome} dá ${r.toFixed(2)}:1; o AA pede 4,5:1`);
  }
  // Botão sólido: texto obsidiana sobre fundo osso.
  assert.ok(contraste(rgba(COR.obsidiana), rgba(COR.osso)) >= 4.5);

  const status = TEMA.match(/export const corDoStatus[^=]*=\s*\{([\s\S]*?)\};/)?.[1] ?? '';
  const usadas = [...status.matchAll(/:\s*cor\.(\w+)/g)].map((m) => m[1]);
  assert.equal(usadas.length, 6, 'não achei as seis cores de status');
  for (const nome of usadas) assert.ok(sobreObsidiana(nome) >= 4.5, `status em ${nome} reprova`);
});

test('o campo de digitação se distingue da obsidiana a 3:1', () => {
  // A borda do Campo era cor.fio, branco a 26%: 2,10:1. Num TextInput de fundo
  // transparente, a borda é tudo o que diz onde digitar (critério 1.4.11).
  const campo = ler('ui.tsx').match(/export function Campo[\s\S]*?\n\}/)?.[0] ?? '';
  const borda = campo.match(/borderColor:\s*cor\.(\w+)/)?.[1];
  assert.ok(borda, 'não achei a borda do Campo');
  const r = sobreObsidiana(borda);
  assert.ok(r >= 3, `a borda do Campo (cor.${borda}) dá ${r.toFixed(2)}:1`);
});
