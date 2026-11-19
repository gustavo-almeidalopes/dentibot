/**
 * Invariantes de estilo que nenhum teste de unidade pegaria.
 *
 *   cd web && npm test
 *
 * Este projeto não tem jsdom nem testing-library, e não vai ganhar: layout se
 * confere olhando. O que dá para provar por máquina é o que apodrece calado —
 * token usado e nunca definido, endpoint vazando para a interface, primitivo da
 * landing usado como linha de dado.
 */
import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import test from 'node:test';

const ler = (nome) => readFileSync(new URL(`./${nome}`, import.meta.url), 'utf8');

const cssDoApp = () => ler('app.css');
const cssDaLanding = () => ler('style.css');

const telas = () => readdirSync(new URL('./paginas', import.meta.url))
  .filter((f) => f.endsWith('.jsx'))
  .map((f) => [f, ler(`paginas/${f}`)]);

test('todo token que o CSS usa está definido em algum lugar', () => {
  const fonte = cssDaLanding() + cssDoApp();

  // `var(--x)` sem fallback: se ninguém definir --x, a propriedade inteira é
  // descartada, sem erro.
  const usados = new Set(
    [...fonte.matchAll(/var\(\s*(--[\w-]+)\s*\)/g)].map((m) => m[1]),
  );
  const definidos = new Set(
    [...fonte.matchAll(/(--[\w-]+)\s*:/g)].map((m) => m[1]),
  );

  const orfaos = [...usados].filter((t) => !definidos.has(t));
  assert.deepEqual(orfaos, [], `tokens usados e nunca definidos: ${orfaos}`);
});

test('o tema claro define todo token semântico que o escuro define', () => {
  // Escuro é o :root da marca; claro é opt-in por data-tema, só nas telas do
  // sistema (tema.js).
  const fonte = cssDaLanding() + cssDoApp();
  const bloco = (re) => (fonte.match(re) ?? [''])[0];

  const semanticos = (texto) => new Set(
    [...texto.matchAll(/(--(?:fundo|superficie|tinta|tinta-fraca|hair|hair-fraca|foco))\s*:/g)]
      .map((m) => m[1]),
  );

  const escuro = semanticos(bloco(/:root\s*\{[^}]*\}/));
  const claro = semanticos(bloco(/:root\[data-tema="claro"\]:has\(\.app-main\)\s*\{[^}]*\}/));

  // Token definido só no escuro: a tela clara herda o valor do preto e o texto
  // some no fundo.
  const faltando = [...escuro].filter((t) => !claro.has(t));
  assert.deepEqual(faltando, [], `sem valor no tema claro: ${faltando}`);
  assert.ok(escuro.size >= 7, 'os sete tokens semânticos precisam existir');
});

test('nenhuma tela imprime o endpoint na interface', () => {
  for (const [nome, fonte] of telas()) {
    // assert.ok e não doesNotMatch: o doesNotMatch despeja o arquivo inteiro
    // no erro, e a falha vira 200 linhas de JSX escapado.
    assert.ok(!/GET \/api\//.test(fonte), `${nome} mostra endpoint na tela`);
  }
});

test('as telas do sistema não usam o .row da landing', () => {
  // `.row` é grid de DUAS colunas com padding-block de 42px, feito para a
  // landing: um terceiro filho cai numa linha implícita.
  // `row-title` é outra classe (o título em Antonio), e `\b` casava com ela.
  for (const [nome, fonte] of telas()) {
    assert.ok(!/className="(?:[^"]*\s)?row["\s]/.test(fonte),
      `${nome} ainda usa .row`);
  }
});
