# Acessibilidade digital como gate de entrega — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A DentiBot passa a cumprir WCAG 2.2 AA nas telas claras, no `/login` e no app móvel, com um gate que impede os defeitos de voltar, e a landing fica leve para aparelho e rede modestos (ODS-17, 18, 19 e 16).

**Architecture:** Três camadas de prova. Invariantes em `node:test` que leem o CSS e o JSX — contraste por token, literal da marca nas telas claras, estrutura, grafo de imports — rodam em todo PR no job `contratos`. Um script com Playwright e axe-core, contra o `preview()` do Vite, audita as rotas públicas, o reflow a 320px e a carga que falha, e grava o relatório como artefato no job novo `acessibilidade`, junto com o orçamento de peso. As telas autenticadas ficam com o roteiro manual de `docs/acessibilidade.md`. As correções trocam literal da marca por token semântico — no escuro o valor é o mesmo, e a landing não muda — e o vermelho vira marca no tema claro.

**Tech Stack:** React 19, Vite 8.2, react-router-dom 7, `@clerk/react` 6, `node --test`. Entram `playwright-core` 1.63 e `@axe-core/playwright` 4.13 (devDependencies) e `@fontsource/antonio`, `@fontsource/cormorant-sc` e `@fontsource/inter` 5.3.

**Spec:** `docs/superpowers/specs/2026-09-26-ods-acessibilidade-digital-design.md`

## Global Constraints

- **Brutalismo.** Só as cores que já existem: obsidiana `#000000`, osso `#ffffff`, cinza `#838383`, vermelho `#ed1c24` e, no tema claro, `#faf9f7`, `#ffffff`, `#111111`, `#5c5c5c`. Nenhuma cor nova, nenhum segundo vermelho, raio zero, fontes Antonio, Cormorant SC e Inter.
- **WCAG 2.2 AA:** texto ≥ 4,5:1; texto grande (≥ 24px, ou ≥ 18,66px em negrito) ≥ 3:1; o que identifica um controle ou um estado ≥ 3:1.
- **No tema claro o vermelho não é cor de texto.** Ele é filete, borda ou fundo.
- **O escuro não muda de valor.** Só se troca `--bone`, `--obsidian` ou `--ash` por `--tinta`, `--fundo` ou `--tinta-fraca` porque, no `:root`, o semântico vale exatamente o literal trocado. A landing fica pixel-idêntica.
- **Dependências novas, exatamente estas:** em `dependencies`, `@fontsource/antonio@^5.3.0`, `@fontsource/cormorant-sc@^5.3.0` e `@fontsource/inter@^5.3.0`; em `devDependencies`, `playwright-core@^1.63.0` e `@axe-core/playwright@^4.13.0`. Nenhuma outra.
- **Sem jsdom, testing-library ou vitest.** Teste é `node:test` lendo o fonte; navegador só no `web/a11y/auditar.mjs`.
- **Nada em `api/`.**
- **Português** em variável, classe, teste, comentário e commit. Comentário explica por quê.
- Todo commit termina com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Trabalho no worktree `.worktrees/ods-acessibilidade`, branch `feat/ods-acessibilidade-digital`. O `web/.env.local` (chave publicável do Clerk) já está lá; sem ele o build é só a tela de "Configuração ausente".
- Rodar de `web/`: `npm test` e `npm run build`. De `app/`: `npm test`.

## Review Focus

1. **Build sem a chave do Clerk:** landing e 404 renderizam normalmente; só `/login`, `/cadastro` e o sistema mostram "Configuração ausente". Preso na Task 10, Step 7.
2. **Troca de tela dentro do sistema:** a casca clara não pisca para o preto nem desmonta o `ClerkProvider`. Preso na Task 6, teste do `Suspense` em volta do `Outlet`.
3. **Chunk que não chega (3G que cai):** mensagem com "Tentar de novo", nunca tela branca. Preso na Task 10, carga que falha.
4. **Quem já tem sessão e abre a landing:** "Entrar" continua no menu e na barra e leva ao `/login`, que mostra "Abrir o sistema". Preso na Task 6, teste das duas ocorrências de `href={LOGIN}`.
5. **Tela estreita e zoom:** a 320px nenhuma rota pública rola na horizontal. Preso na Task 10 (reflow); nas telas autenticadas, no roteiro da Task 12.

---

### Task 1: Contraste por token e borda de campo

**Files:**
- Modify: `web/src/paginas/Cadastro.jsx:95` e `:191`
- Create: `web/src/acessibilidade.test.mjs`
- Modify: `web/src/app.css` (regra `.filtros input, … .campo-app textarea`)
- Modify: `web/src/style.css` (regra `.pagamento-campo input`)

**Interfaces:**
- Consumes: nada.
- Produces: em `web/src/acessibilidade.test.mjs`, os helpers que as Tasks 2, 3, 4, 6, 7 e 8 usam: `ler(nome)`, `semComentario(css)`, `regras(css)` → `[{ seletor, decls }]`, `ESCURO` e `CLARO` (`{ '--token': 'valor cru' }`), `cor(valor, tema)` → `[r, g, b, alfa]`, `contraste(frente, fundo)` → número.

- [ ] **Step 1: Deixar a linha de base verde**

O teste `nenhuma tela usa a classe de erro do formulário de pagamento` já está vermelho. Em `web/src/paginas/Cadastro.jsx`, troque as duas ocorrências:

```jsx
          <p className="pagamento-erro" role="alert">
```
por
```jsx
          <p className="erro-campo" role="alert">
```
e
```jsx
      {erro && <p className="pagamento-erro" role="alert">{erro.message}</p>}
```
por
```jsx
      {erro && <p className="erro-campo" role="alert">{erro.message}</p>}
```

Run: `cd web && npm test`
Expected: tudo PASS. É a mesma troca que o dono do repositório tem sem commit no checkout principal; as duas convergem.

- [ ] **Step 2: Escrever o teste que falha**

Crie `web/src/acessibilidade.test.mjs`:

```js
/**
 * O gate estático de acessibilidade (ODS-17): o que dá para provar lendo o
 * fonte, sem navegador.
 *
 *   cd web && npm test
 *
 * O axe, em navegador real, audita as rotas públicas (a11y/auditar.mjs); as
 * telas autenticadas têm o roteiro manual de docs/acessibilidade.md. Este
 * arquivo cobre o que apodrece calado entre os dois: um token que muda de
 * valor, um literal da marca que vaza para o papel, um vermelho que vira tinta.
 */
import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import test from 'node:test';

const ler = (nome) => readFileSync(new URL(`./${nome}`, import.meta.url), 'utf8');
const semComentario = (css) => css.replace(/\/\*[\s\S]*?\*\//g, '');

/** As regras mais internas: o `@media` some, o que está dentro dele fica. */
const regras = (css) => [...semComentario(css).matchAll(/([^{}]+)\{([^{}]*)\}/g)]
  .map((m) => ({ seletor: m[1].trim(), decls: m[2] }));

/** As custom properties de um bloco, com o valor cru: `{ '--fundo': 'var(--obsidian)' }`. */
const tokensDe = (css, bloco) => Object.fromEntries(
  [...((semComentario(css).match(bloco) ?? [''])[0]).matchAll(/(--[\w-]+)\s*:\s*([^;]+);/g)]
    .map((m) => [m[1], m[2].trim()]),
);

const ESCURO = tokensDe(ler('style.css'), /:root\s*\{[^}]*\}/);
const CLARO = { ...ESCURO, ...tokensDe(ler('app.css'), /:root:has\(\.app-main\)\s*\{[^}]*\}/) };

/** Cor resolvida em `[r, g, b, alfa]`, seguindo `var()` até o literal. */
function cor(valor, tema) {
  const v = valor.trim();
  const ref = v.match(/^var\(\s*(--[\w-]+)\s*(?:,[^)]*)?\)$/);
  if (ref) {
    assert.ok(tema[ref[1]], `token sem valor: ${ref[1]}`);
    return cor(tema[ref[1]], tema);
  }
  const hex = v.match(/^#([0-9a-f]{6})$/i);
  if (hex) return [0, 2, 4].map((i) => parseInt(hex[1].slice(i, i + 2), 16)).concat(1);
  const rgb = v.match(/^rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+)\s*)?\)$/);
  if (rgb) return [Number(rgb[1]), Number(rgb[2]), Number(rgb[3]), rgb[4] === undefined ? 1 : Number(rgb[4])];
  throw new Error(`cor que o teste não sabe ler: ${valor}`);
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

const TEMAS = [['escuro', ESCURO], ['claro', CLARO]];

test('texto e foco passam nos dois temas', () => {
  for (const [nome, tema] of TEMAS) {
    for (const fundo of ['--fundo', '--superficie']) {
      for (const texto of ['--tinta', '--tinta-fraca']) {
        const r = contraste(cor(`var(${texto})`, tema), cor(`var(${fundo})`, tema));
        assert.ok(r >= 4.5, `${nome}: ${texto} sobre ${fundo} dá ${r.toFixed(2)}:1; o AA pede 4,5:1`);
      }
    }
    const foco = contraste(cor('var(--foco)', tema), cor('var(--fundo)', tema));
    assert.ok(foco >= 3, `${nome}: --foco dá ${foco.toFixed(2)}:1; o mínimo é 3:1`);
  }
});

test('no escuro os tokens semânticos continuam valendo a marca', () => {
  // A landing passa a usar --tinta onde escrevia --bone. Isso só não muda nada
  // na tela preta enquanto estes quatro valerem exatamente o literal de antes.
  const rgb = (token) => cor(`var(${token})`, ESCURO).slice(0, 3).join(',');
  assert.equal(rgb('--fundo'), '0,0,0');
  assert.equal(rgb('--superficie'), '0,0,0');
  assert.equal(rgb('--tinta'), '255,255,255');
  assert.equal(rgb('--tinta-fraca'), '131,131,131');
});

/** A borda declarada em toda regra de input, select ou textarea. */
function bordasDeCampo(css) {
  const achadas = [];
  for (const { seletor, decls } of regras(css)) {
    if (!/(^|[\s,>+~])(input|select|textarea)\b(?!::)/.test(seletor)) continue;
    for (const m of decls.matchAll(/(?:^|;)\s*border(?:-color)?\s*:\s*([^;]+)/g)) {
      const valor = m[1].match(/var\(\s*--[\w-]+\s*\)|#[0-9a-f]{6}|rgba?\([^)]*\)/i);
      if (valor) achadas.push({ seletor: seletor.replace(/\s+/g, ' '), valor: valor[0] });
    }
  }
  return achadas;
}

test('campo de digitação se distingue do fundo a 3:1', () => {
  // Hairline decora; controle se enxerga. Com --hair, o campo de fundo
  // transparente dava 1,69:1 no papel e 2,10:1 no preto (critério 1.4.11).
  const casos = [
    ...bordasDeCampo(ler('style.css')).map((b) => ({ ...b, tema: ESCURO, nome: 'escuro' })),
    ...bordasDeCampo(ler('app.css')).map((b) => ({ ...b, tema: CLARO, nome: 'claro' })),
  ];
  assert.ok(casos.length >= 2, 'o extrator não achou borda de campo nenhuma');
  for (const { seletor, valor, tema, nome } of casos) {
    const r = contraste(cor(valor, tema), cor('var(--fundo)', tema));
    assert.ok(r >= 3, `${nome}: ${seletor} tem borda ${valor} a ${r.toFixed(2)}:1`);
  }
});
```

- [ ] **Step 3: Rodar e ver falhar**

Run: `cd web && npm test`
Expected: FAIL em `campo de digitação se distingue do fundo a 3:1`, com `escuro: .pagamento-campo input tem borda var(--hair) a 2.10:1`. Os outros dois testes novos passam — são guardas do que hoje está certo.

- [ ] **Step 4: Corrigir as duas bordas**

Em `web/src/app.css`, na regra que começa com `.filtros input, .filtros select,`, troque

```css
  border: 1px solid var(--hair);
  border-radius: 0;
  color: var(--tinta);
  font-family: inherit;
  font-size: 14px;
```
por
```css
  /* Controle, não hairline: --hair dava 1,69:1 no papel (1.4.11 pede 3:1). */
  border: 1px solid var(--tinta-fraca);
  border-radius: 0;
  color: var(--tinta);
  font-family: inherit;
  font-size: 14px;
```

Em `web/src/style.css`, na regra `.pagamento-campo input {`, troque

```css
.pagamento-campo input {
  appearance: none;
  background: transparent;
  border: 1px solid var(--hair);
```
por
```css
.pagamento-campo input {
  appearance: none;
  background: transparent;
  /* Controle, não hairline: --hair dava 2,10:1 no preto (1.4.11 pede 3:1). */
  border: 1px solid var(--tinta-fraca);
```

- [ ] **Step 5: Rodar e ver passar**

Run: `cd web && npm test`
Expected: tudo PASS.

- [ ] **Step 6: Commit**

```bash
git add web/src/paginas/Cadastro.jsx web/src/acessibilidade.test.mjs web/src/app.css web/src/style.css
git commit -m "fix(web): campo de digitação com borda que se enxerga, nos dois temas" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: No tema claro, o vermelho é marca, não tinta

**Files:**
- Modify: `web/src/acessibilidade.test.mjs` (acrescentar ao fim)
- Modify: `web/src/app.css` (`.selo-staff`, `.selo[data-tom="alarme"]`, `.aviso[data-tom="erro"]`, `.erro-campo`, `.btn-perigo`, `.indicador`)
- Modify: `web/src/paginas/Layout.jsx` (erro do `Estado`)
- Modify: `web/src/paginas/Prontuario.jsx:150`

**Interfaces:**
- Consumes: `ler`, `regras` (Task 1).
- Produces: `CLARAS` (lista de arquivos das telas claras), `regrasDasTelasClaras()` → `[{ onde, decls }]` e `pintaTexto(decls, tokens)` → boolean, usados pela Task 3.

- [ ] **Step 1: Escrever o teste que falha**

Acrescente ao fim de `web/src/acessibilidade.test.mjs`:

```js
/*
 * As telas claras e os componentes que elas montam. O 404 fica de fora: ele é
 * da landing, e é preto.
 */
const CLARAS = [
  ...readdirSync(new URL('./paginas', import.meta.url))
    .filter((f) => f.endsWith('.jsx') && f !== 'NaoEncontrada.jsx')
    .map((f) => `paginas/${f}`),
  'components/FichaPaciente.jsx',
  'components/Confirmar.jsx',
  'components/primitivos.jsx',
];

/** As regras de CSS cujas classes aparecem todas em alguma tela clara. */
function regrasDasTelasClaras() {
  const corpus = CLARAS.map(ler).join('\n');
  const usada = (classe) => new RegExp(`(?<![\\w-])${classe}(?![\\w-])`).test(corpus);
  const achadas = [];
  for (const arquivo of ['style.css', 'app.css']) {
    for (const { seletor, decls } of regras(ler(arquivo))) {
      if (seletor.startsWith('@') || seletor.startsWith(':root')) continue;
      // `.on-red .btn` não vale no papel: `.on-red` não existe nas telas claras.
      const partes = seletor.split(',').map((p) => p.trim()).filter((p) => {
        const classes = [...p.matchAll(/\.([\w-]+)/g)].map((m) => m[1]);
        return classes.length > 0 && classes.every(usada);
      });
      if (partes.length) achadas.push({ onde: `${arquivo}: ${partes.join(', ')}`, decls });
    }
  }
  return achadas;
}

/** A regra pinta texto com algum destes tokens? */
const pintaTexto = (decls, tokens) => [...decls.matchAll(/(?:^|;)\s*color\s*:\s*([^;]+)/g)]
  .some((m) => tokens.some((t) => m[1].includes(`var(${t})`)));

test('vermelho é marca, não tinta, nas telas claras', () => {
  // #ed1c24 dá 4,16:1 no papel e 4,38:1 no branco: reprova como texto. No
  // tema claro ele vira filete, e o texto fica em --tinta.
  const ruins = regrasDasTelasClaras()
    .filter(({ decls }) => pintaTexto(decls, ['--alarm']))
    .map(({ onde }) => onde);
  assert.deepEqual(ruins, []);

  for (const f of CLARAS) {
    assert.ok(!/color:\s*['"`]var\(--alarm\)/.test(ler(f)), `${f} pinta texto de vermelho em style inline`);
  }
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd web && npm test`
Expected: FAIL em `vermelho é marca, não tinta, nas telas claras`, listando `app.css: .selo-staff`, `app.css: .selo[data-tom="alarme"]`, `app.css: .aviso[data-tom="erro"]`, `app.css: .erro-campo`, `app.css: .btn-perigo` e `app.css: .indicador[data-alarme="sim"] .indicador-valor`.

- [ ] **Step 3: Corrigir o CSS**

Em `web/src/app.css`, troque cada regra:

```css
.selo-staff { font-size: 11px; letter-spacing: .1em; text-transform: uppercase; color: var(--alarm); }
```
por
```css
.selo-staff { font-size: 11px; letter-spacing: .1em; text-transform: uppercase; border-left: 3px solid var(--alarm); padding-left: 6px; }
```

```css
.selo[data-tom="alarme"]  { color: var(--alarm); border-color: var(--alarm); border-left-width: 3px; }
```
por
```css
.selo[data-tom="alarme"]  { border-color: var(--alarm); border-left-width: 3px; }
```

```css
.aviso[data-tom="erro"]    { border-left-color: var(--alarm); color: var(--alarm); }
```
por
```css
.aviso[data-tom="erro"]    { border-left-color: var(--alarm); }
```

```css
/* Erro de campo. Antes era `.pagamento-erro`, emprestado do formulário de
   cartão de crédito por quatro telas que não têm nada com pagamento. */
.erro-campo { margin: 0; font-size: 12px; color: var(--alarm); }
```
por
```css
/* Erro de campo. Antes era `.pagamento-erro`, emprestado do formulário de
   cartão de crédito por quatro telas que não têm nada com pagamento. O texto
   fica em tinta: o vermelho dá 4,16:1 no papel, reprova como letra, e vira o
   filete. */
.erro-campo { margin: 0; font-size: 12px; border-left: 3px solid var(--alarm); padding-left: 8px; }
```

```css
.btn-perigo { border-color: var(--alarm); color: var(--alarm); }
.btn-perigo::before { background: var(--alarm); }
.btn-perigo:hover, .btn-perigo:focus-visible { color: var(--superficie); }
```
por
```css
.btn-perigo { border-color: var(--alarm); }
.btn-perigo::before { background: var(--alarm); }
/* Obsidiana e não --tinta: #111 sobre o vermelho dá 4,31:1 e reprova; #000 dá 4,79:1. */
.btn-perigo:hover, .btn-perigo:focus-visible { color: var(--obsidian); }
```

```css
.indicador[data-alarme="sim"] .indicador-valor { color: var(--alarm); }
```
por
```css
/* O número em vermelho dava 4,38:1 no branco. O alarme é o filete; o número, tinta. */
.indicador[data-alarme="sim"] { border-left: 3px solid var(--alarm); padding-left: 15px; }
```

- [ ] **Step 4: Corrigir os dois vermelhos inline**

Em `web/src/paginas/Layout.jsx`, dentro de `Estado`, troque

```jsx
        <p className="body" style={{ color: 'var(--alarm)' }}>
          {erro?.message || 'Não foi possível carregar.'}
        </p>
```
por
```jsx
        <p className="aviso" data-tom="erro">
          {erro?.message || 'Não foi possível carregar.'}
        </p>
```

Em `web/src/paginas/Prontuario.jsx`, troque

```jsx
                  <p className="cap" style={{ color: 'var(--alarm)' }}>
```
por
```jsx
                  {/* A retificação já tem a marca vermelha na margem da evolução;
                      o texto dela é tinta, que se lê. */}
                  <p className="cap">
```

- [ ] **Step 5: Rodar e ver passar**

Run: `cd web && npm test && npm run build`
Expected: tudo PASS; build sem erro.

- [ ] **Step 6: Commit**

```bash
git add web/src/acessibilidade.test.mjs web/src/app.css web/src/paginas/Layout.jsx web/src/paginas/Prontuario.jsx
git commit -m "fix(web): no tema claro o vermelho é marca, não tinta" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Literais da marca nos primitivos compartilhados

**Files:**
- Modify: `web/src/acessibilidade.test.mjs` (acrescentar ao fim)
- Modify: `web/src/style.css` (`.skip-link`, `.cap-ash`, `.body-ash`, `.btn` e família, `.auth-swap`)
- Modify: `web/src/app.css` (`.btn-sm`)

**Interfaces:**
- Consumes: `CLARAS`, `regrasDasTelasClaras()`, `pintaTexto()` (Task 2).
- Produces: nada novo.

- [ ] **Step 1: Escrever o teste que falha**

Acrescente ao fim de `web/src/acessibilidade.test.mjs`:

```js
test('literal da marca pensado para o preto não pinta as telas claras', () => {
  // --ash dá 3,6:1 no papel; --bone some nele. O .cap-ash, o .btn e o
  // .skip-link usavam os dois, e aparecem em todas as telas do sistema.
  const ruins = regrasDasTelasClaras()
    .filter(({ decls }) => pintaTexto(decls, ['--ash', '--bone'])
      || /(?:^|;)\s*(?:background|border)[\w-]*\s*:[^;]*var\(--bone\)/.test(decls))
    .map(({ onde }) => onde);
  assert.deepEqual(ruins, []);

  for (const f of CLARAS) {
    assert.ok(!/color:\s*['"`]var\(--(?:ash|bone)\)/.test(ler(f)), `${f} usa literal da marca em style inline`);
  }
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd web && npm test`
Expected: FAIL listando, todas em `style.css`: `.skip-link`, `.cap-ash`, `.body-ash`, `.btn`, `.btn::before`, `.btn-fill`, `.btn-fill:hover, .btn-fill:focus-visible`, `.auth-swap a, .auth-swap button` e `.auth-swap a:hover, .auth-swap button:hover`.

- [ ] **Step 3: Corrigir `style.css`**

Na regra `.skip-link {`, troque

```css
  border: 1px solid var(--bone);
  border-radius: 0;
  background: var(--bone);
  color: var(--obsidian);
```
por
```css
  border: 1px solid var(--tinta);
  border-radius: 0;
  background: var(--tinta);
  color: var(--fundo);
```

Troque

```css
.cap-ash { color: var(--ash); }
```
por
```css
/* --tinta-fraca e não --ash: no preto são o mesmo cinza, mas no papel o --ash
   dá 3,6:1 e reprova. As telas do sistema usam estas duas classes 67 vezes. */
.cap-ash { color: var(--tinta-fraca); }
```
e
```css
.body-ash { color: var(--ash); }
```
por
```css
.body-ash { color: var(--tinta-fraca); }
```

Na regra `.btn {`, troque

```css
  border: 2px solid var(--bone);
```
por
```css
  /* Token semântico, não literal: no preto --tinta é osso e --fundo é
     obsidiana, e a landing não muda. No papel o botão vira a caixa preta de
     canto vivo, em vez de uma borda branca que sumia. */
  border: 2px solid var(--tinta);
```

Troque o bloco

```css
.btn::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  background: var(--bone);
```
por
```css
.btn::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  background: var(--tinta);
```

Troque

```css
.btn:hover, .btn:focus-visible { color: var(--obsidian); }

/* Filled block starts flipped, so its sweep runs the other way. */
.btn-fill { background: var(--bone); color: var(--obsidian); }
.btn-fill::before { background: var(--obsidian); transform-origin: right center; }
.btn-fill:hover, .btn-fill:focus-visible { color: var(--bone); }
```
por
```css
.btn:hover, .btn:focus-visible { color: var(--fundo); }

/* Filled block starts flipped, so its sweep runs the other way. */
.btn-fill { background: var(--tinta); color: var(--fundo); }
.btn-fill::before { background: var(--fundo); transform-origin: right center; }
.btn-fill:hover, .btn-fill:focus-visible { color: var(--tinta); }
```

Na regra `.auth-swap a, .auth-swap button {`, troque

```css
  color: var(--ash);
```
por
```css
  color: var(--tinta-fraca);
```
e troque
```css
.auth-swap a:hover, .auth-swap button:hover { color: var(--bone); }
```
por
```css
.auth-swap a:hover, .auth-swap button:hover { color: var(--tinta); }
```

- [ ] **Step 4: Aliviar o botão de linha no app**

Em `web/src/app.css`, troque

```css
.btn-sm { padding: 6px 12px; font-size: 12px; }
```
por
```css
/* Ação de linha com borda de 1px: uma coluna de caixas de 2px grita mais que o
   dado da linha. */
.btn-sm { padding: 6px 12px; font-size: 12px; border-width: 1px; }
```

- [ ] **Step 5: Rodar e ver passar**

Run: `cd web && npm test && npm run build`
Expected: tudo PASS; build sem erro.

- [ ] **Step 6: Conferir no navegador que a landing não mudou e o papel ganhou caixa**

Suba o preview (`ods-web-preview` no `.claude/launch.json` da raiz, ou `npm run preview` em `web/`) e, na landing (`/`), rode no console:

```js
const b = getComputedStyle(document.querySelector('.nav .btn'));
[b.borderTopColor, getComputedStyle(document.querySelector('.nav .btn-fill')).backgroundColor]
```
Expected: `['rgb(255, 255, 255)', 'rgb(255, 255, 255)']` — o mesmo de antes.

Depois, na mesma página, simule uma tela clara:

```js
const m = document.createElement('main'); m.className = 'app-main edge';
m.innerHTML = '<button class="btn">A</button><a class="btn btn-fill" href="#">B</a><p class="cap cap-ash">c</p>';
document.body.appendChild(m);
const s = [...m.children].map((e) => { const c = getComputedStyle(e); return [c.borderTopColor, c.backgroundColor, c.color]; });
m.remove(); s
```
Expected: o `.btn` com borda `rgb(17, 17, 17)`; o `.btn-fill` com fundo `rgb(17, 17, 17)` e texto `rgb(250, 249, 247)`; o `.cap-ash` com texto `rgb(92, 92, 92)`.

- [ ] **Step 7: Commit**

```bash
git add web/src/acessibilidade.test.mjs web/src/style.css web/src/app.css
git commit -m "fix(web): botões, legendas e skip link com token semântico, e o papel volta a ter caixa" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Estrutura e odontograma

**Files:**
- Modify: `web/src/acessibilidade.test.mjs` (acrescentar ao fim)
- Modify: `web/src/app.css` (regras `.odonto-dente` e `.odonto-amostra`)

**Interfaces:**
- Consumes: `ler`, `semComentario`, `regras`, `CLARO`, `cor`, `contraste` (Task 1).
- Produces: `arquivosJs()` → lista de `.js`/`.jsx` de `src/` (caminhos com `/`) e `indexHtml()` → texto do `web/index.html`, usados pelas Tasks 7 e 8.

- [ ] **Step 1: Escrever os testes**

Acrescente ao fim de `web/src/acessibilidade.test.mjs`:

```js
/** Todo .js e .jsx do front. Os testes são .mjs e ficam de fora sozinhos. */
const arquivosJs = () => readdirSync(new URL('.', import.meta.url), { recursive: true })
  .map((f) => f.replaceAll('\\', '/'))
  .filter((f) => /\.jsx?$/.test(f));

const indexHtml = () => readFileSync(new URL('../index.html', import.meta.url), 'utf8');

test('a página diz o idioma e deixa ampliar', () => {
  const html = indexHtml();
  assert.match(html, /<html[^>]*\blang="pt-BR"/);
  const viewport = html.match(/<meta[^>]*name="viewport"[^>]*content="([^"]*)"/)?.[1];
  assert.ok(viewport, 'sem meta viewport');
  // Bloquear o zoom reprova o 1.4.4: quem precisa de 200% não chega lá.
  assert.ok(!/user-scalable\s*=\s*(no|0)|maximum-scale/i.test(viewport), `viewport bloqueia zoom: ${viewport}`);
});

test('o anel de foco nunca some e a tabulação segue o documento', () => {
  for (const arquivo of ['style.css', 'app.css']) {
    assert.ok(!/outline\s*:\s*(none|0)\b/.test(semComentario(ler(arquivo))), `${arquivo} apaga o anel de foco`);
  }
  for (const f of arquivosJs()) {
    assert.ok(!/tabIndex=\{?\s*["']?[1-9]/.test(ler(f)), `${f} tem tabIndex positivo`);
  }
});

test('toda tela tem título de primeiro nível', () => {
  const paginas = readdirSync(new URL('./paginas', import.meta.url))
    .filter((f) => f.endsWith('.jsx') && f !== 'Layout.jsx');
  for (const f of paginas) {
    assert.ok(/<h1\b|<Cabecalho\b/.test(ler(`paginas/${f}`)), `${f} não tem h1`);
  }
});

test('a marca de condição do dente se lê a 3:1', () => {
  // O critério 1.4.11 vale para estado: o tracejado do ausente e o filete de
  // 3px do restaurado ficavam no --hair, a 1,69:1, e o ausente ainda apagava o
  // número com opacity: .38 (2,46:1).
  const marcas = regras(ler('app.css'))
    .filter(({ seletor }) => /\.odonto-(dente|amostra)\[data-condicao=/.test(seletor));
  assert.ok(marcas.length >= 4, 'o extrator não achou as marcas do odontograma');
  for (const { seletor, decls } of marcas) {
    const nome = seletor.replace(/\s+/g, ' ');
    const cores = [...decls.matchAll(/(?:^|;)\s*border(?:-bottom)?-color\s*:\s*([^;]+)/g)].map((m) => m[1].trim());
    assert.ok(cores.length > 0, `${nome} não diz a cor da marca`);
    for (const c of cores) {
      const r = contraste(cor(c, CLARO), cor('var(--fundo)', CLARO));
      assert.ok(r >= 3, `${nome}: ${c} dá ${r.toFixed(2)}:1`);
    }
    assert.ok(!/opacity\s*:/.test(decls), `${nome} apaga o dente com opacidade`);
  }
});
```

- [ ] **Step 2: Rodar e ver o que falha**

Run: `cd web && npm test`
Expected: os três primeiros passam (são guardas do que hoje está certo); FAIL em `a marca de condição do dente se lê a 3:1`, com `.odonto-dente[data-condicao="ausente"] não diz a cor da marca`.

- [ ] **Step 3: Provar que as guardas mordem**

Em `web/index.html`, troque temporariamente `content="width=device-width,initial-scale=1.0"` por `content="width=device-width,initial-scale=1.0,maximum-scale=1"`.
Run: `cd web && npm test`
Expected: FAIL em `a página deixa ampliar e diz o idioma` com `viewport bloqueia zoom`. Desfaça a troca (`git checkout web/index.html`).

- [ ] **Step 4: Corrigir o odontograma**

Em `web/src/app.css`, troque

```css
.odonto-dente:hover { background: rgba(255, 255, 255, .07); }
```
por
```css
.odonto-dente:hover { background: var(--hair-fraca); }
```

Troque

```css
/* A condição é cor E borda: cor sozinha não existe para quem não a distingue. */
.odonto-dente[data-condicao="cárie"] { border-color: var(--alarm); border-bottom-width: 3px; }
.odonto-dente[data-condicao="ausente"] { opacity: .38; border-style: dashed; }
.odonto-dente[data-condicao="restaurado"],
.odonto-dente[data-condicao="coroa"],
.odonto-dente[data-condicao="implante"] { border-bottom-width: 3px; }
```
por
```css
/* A condição é cor E borda: cor sozinha não existe para quem não a distingue.
   E a marca precisa de 3:1 (1.4.11): no --hair ela dava 1,69:1, e a opacidade
   do ausente apagava o número junto. */
.odonto-dente[data-condicao="cárie"] { border-color: var(--alarm); border-bottom-width: 3px; }
.odonto-dente[data-condicao="ausente"] { border-style: dashed; border-color: var(--tinta-fraca); }
.odonto-dente[data-condicao="restaurado"],
.odonto-dente[data-condicao="coroa"],
.odonto-dente[data-condicao="implante"] { border-bottom-width: 3px; border-bottom-color: var(--tinta-fraca); }
```

Troque

```css
.odonto-amostra[data-condicao="ausente"] { opacity: .38; border-style: dashed; }
.odonto-amostra[data-condicao="restaurado"],
.odonto-amostra[data-condicao="coroa"],
.odonto-amostra[data-condicao="implante"] { border-bottom-width: 3px; }
```
por
```css
.odonto-amostra[data-condicao="ausente"] { border-style: dashed; border-color: var(--tinta-fraca); }
.odonto-amostra[data-condicao="restaurado"],
.odonto-amostra[data-condicao="coroa"],
.odonto-amostra[data-condicao="implante"] { border-bottom-width: 3px; border-bottom-color: var(--tinta-fraca); }
```

- [ ] **Step 5: Rodar e ver passar**

Run: `cd web && npm test && npm run build`
Expected: tudo PASS; build sem erro.

- [ ] **Step 6: Commit**

```bash
git add web/src/acessibilidade.test.mjs web/src/app.css
git commit -m "fix(web): o estado do dente se lê, e guardas de idioma, zoom, foco e título" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: App móvel

**Files:**
- Create: `app/src/acessibilidade.test.mjs`
- Modify: `app/src/ui.tsx` (borda do `Campo`)

**Interfaces:**
- Consumes: nada (projeto separado; o cálculo de contraste é repetido de propósito, como o `contrato.test.mjs` repete o do web).
- Produces: nada.

- [ ] **Step 1: Escrever o teste que falha**

Crie `app/src/acessibilidade.test.mjs`:

```js
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
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd app && npm test`
Expected: FAIL em `o campo de digitação se distingue da obsidiana a 3:1`, com `a borda do Campo (cor.fio) dá 2.10:1`.

- [ ] **Step 3: Corrigir a borda do `Campo`**

Em `app/src/ui.tsx`, dentro de `Campo`, troque

```tsx
          borderColor: cor.fio,
```
por
```tsx
          // Borda de campo é controle, não divisória: cor.fio dava 2,10:1.
          borderColor: cor.cinza,
```

- [ ] **Step 4: Rodar e ver passar**

Run: `cd app && npm test && npm run typecheck`
Expected: PASS (6 testes) e typecheck sem erro. Se o `typecheck` não rodar por falta de `node_modules` no worktree, rode `npm ci` em `app/` antes.

- [ ] **Step 5: Commit**

```bash
git add app/src/acessibilidade.test.mjs app/src/ui.tsx
git commit -m "fix(app): a borda do campo se enxerga sobre o preto" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: O Clerk sai da landing, cada tela vira chunk, e a carga que falha tem saída

**Files:**
- Modify: `web/src/acessibilidade.test.mjs` (acrescentar ao fim)
- Create: `web/src/components/RecuperaCarga.jsx`
- Create: `web/src/ComClerk.jsx`
- Modify: `web/src/main.jsx` (reescrita)
- Modify: `web/src/components/Nav.jsx`
- Modify: `web/src/paginas/Layout.jsx` (`Suspense` em volta do `Outlet`)

**Interfaces:**
- Consumes: `ler` (Task 1).
- Produces: `web/src/ComClerk.jsx` com `export default function ComClerk()` e a constante `aparencia` com `variables.colorBackground` e `variables.colorBorder` como string literal (a Task 7 lê as duas por regex); `web/src/components/RecuperaCarga.jsx` com `export default class RecuperaCarga` que renderiza o texto `Algo não carregou` (a Task 10 procura exatamente esse texto); o chunk gerado para `ComClerk.jsx` se chama `assets/ComClerk-<hash>.js` (a Task 10 o bloqueia por esse nome).

- [ ] **Step 1: Escrever os testes que falham**

Acrescente ao fim de `web/src/acessibilidade.test.mjs`:

```js
/** Imports estáticos alcançáveis a partir de um arquivo: os locais e os de pacote. */
function grafoEstatico(inicio) {
  const arquivos = new Set();
  const pacotes = new Set();
  const visitar = (arquivo) => {
    if (arquivos.has(arquivo)) return;
    arquivos.add(arquivo);
    // `import x from '…'` e `import '…'`. Nunca `import('…')`: esse é o que
    // carrega sob demanda, e fica fora da landing de propósito.
    for (const m of ler(arquivo).matchAll(/^import\s+(?:[^'"]*?\s+from\s+)?['"]([^'"]+)['"]/gm)) {
      const alvo = m[1];
      if (!alvo.startsWith('.')) pacotes.add(alvo);
      else if (/\.jsx?$/.test(alvo)) {
        const pasta = arquivo.includes('/') ? arquivo.slice(0, arquivo.lastIndexOf('/') + 1) : '';
        visitar(new URL(alvo, `file:///r/${pasta}`).pathname.slice('/r/'.length));
      }
    }
  };
  visitar(inicio);
  return { arquivos, pacotes };
}

test('a landing e o 404 não carregam o Clerk', () => {
  // O ClerkProvider envolvia o site inteiro: quem abria a landing num 3G baixava
  // clerk-js e @clerk/ui para ler o preço. O Clerk vive sob o ComClerk, que
  // entra por import(), e este teste segue só o que entra de cara.
  const { arquivos, pacotes } = grafoEstatico('main.jsx');
  assert.ok(arquivos.has('App.jsx') && arquivos.has('paginas/NaoEncontrada.jsx'), 'o grafo não chegou à landing');
  const doClerk = [...pacotes].filter((p) => p.startsWith('@clerk/'));
  assert.deepEqual(doClerk, []);
});

test('trocar de tela não desmonta a casca clara', () => {
  // Cada tela é um chunk. Sem Suspense em volta do Outlet, a espera sobe até o
  // de fora, que desmonta a casca, e o tema claro pisca para o preto.
  assert.match(ler('paginas/Layout.jsx'), /<Suspense\b[\s\S]*?>\s*<Outlet\s*\/>\s*<\/Suspense>/);
});

test('a landing mantém Entrar sem depender de sessão', () => {
  // No menu e na barra: é o caminho de quem já tem conta, logado ou não.
  assert.equal((ler('components/Nav.jsx').match(/href=\{LOGIN\}/g) ?? []).length, 2);
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd web && npm test`
Expected: FAIL em `a landing e o 404 não carregam o Clerk` (lista `@clerk/localizations` e `@clerk/react`) e em `trocar de tela não desmonta a casca clara`. `a landing mantém Entrar…` já passa — é guarda para a remoção do Clerk do `Nav.jsx`.

- [ ] **Step 3: Criar `RecuperaCarga.jsx`**

Crie `web/src/components/RecuperaCarga.jsx`:

```jsx
import { Component } from 'react';

/**
 * O que aparece quando um pedaço do site não chega.
 *
 * <p>Com a divisão por rota, cada tela é um arquivo que o navegador busca na
 * hora. No 3G que cai no meio da navegação o `import()` rejeita, e sem este
 * limite o React desmonta tudo: tela branca, sem uma palavra. Aqui vira uma
 * frase e um botão.
 *
 * <p>Classe porque limite de erro no React ainda só existe assim.
 */
export default class RecuperaCarga extends Component {
  state = { falhou: false };

  static getDerivedStateFromError() {
    return { falhou: true };
  }

  render() {
    if (!this.state.falhou) return this.props.children;
    return (
      <main id="main" className="edge" role="alert" style={{ paddingBlock: 'var(--spacing-30)' }}>
        <p className="body">Algo não carregou — a conexão pode ter caído.</p>
        <p style={{ marginTop: 'var(--spacing-20)' }}>
          <button type="button" className="btn" onClick={() => window.location.reload()}>
            Tentar de novo
          </button>
        </p>
      </main>
    );
  }
}
```

- [ ] **Step 4: Criar `ComClerk.jsx`**

Crie `web/src/ComClerk.jsx` (o `aparencia` e o aviso de chave ausente saem do `main.jsx` como estão; a borda muda na Task 7):

```jsx
import { ptBR } from '@clerk/localizations';
import { ClerkProvider } from '@clerk/react';
import { Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import { CRIAR, LOGIN } from './rotas.js';

/* Variáveis em vez de @clerk/themes: o tema daqui é preto, branco e canto
   vivo — sete tokens cobrem isso e não entra dependência para reescrevê-los
   depois. */
const aparencia = {
  variables: {
    colorBackground: '#000000',
    colorForeground: '#ffffff',
    colorMuted: '#000000',
    colorMutedForeground: '#838383',
    colorPrimary: '#ffffff',
    colorPrimaryForeground: '#000000',
    colorInput: '#000000',
    colorInputForeground: '#ffffff',
    colorBorder: 'rgba(255, 255, 255, .26)',
    colorDanger: '#ed1c24',
    borderRadius: '0px',
    fontFamily: "'Inter', 'Neue Haas Grotesk', 'Helvetica Neue', Helvetica, sans-serif",
  },
};

/* Sem a chave o ClerkProvider não carrega NADA e não reclama: o próprio SDK faz
   `else if (this.#publishableKey) this.getEntryChunks()` — chave ausente é um
   ramo vazio. Aí todo <Show> devolve null para sempre e /login sobe com a
   metade direita em branco, sem os botões do Google/Microsoft/Apple. Era um
   sintoma sem nenhuma mensagem em lugar nenhum; agora a falta da variável no
   build aparece na tela em vez de virar depuração de página vazia. */
const chaveClerk = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY;

/**
 * Tudo que precisa de sessão passa por aqui, e só isso.
 *
 * <p>Rota de layout carregada sob demanda: a landing e o 404 não montam este
 * componente, então não baixam clerk-js nem @clerk/ui — e não caem quando a
 * chave falta no build, o que antes derrubava a página de vendas junto.
 *
 * <p>O Suspense é daqui de dentro: trocar entre /login, /cadastro e o sistema
 * espera sem desmontar o ClerkProvider.
 */
export default function ComClerk() {
  if (!chaveClerk) {
    return (
      <main className="edge" style={{ padding: 'var(--spacing-30)' }} role="alert">
        <h1 className="display display-sm">Configuração ausente.</h1>
        <p className="body body-ash">
          Este build subiu sem <code>VITE_CLERK_PUBLISHABLE_KEY</code>. Sem ela não há login:
          defina a variável no ambiente do build (Vercel → Environment Variables, ou
          <code> web/.env.local</code> em dev) e publique de novo.
        </p>
      </main>
    );
  }

  return (
    <ClerkProvider
      localization={ptBR}
      publishableKey={chaveClerk}
      appearance={aparencia}
      signInUrl={LOGIN}
      signUpUrl={CRIAR}
      afterSignOutUrl="/"
    >
      <Suspense fallback={<p className="body body-ash edge" role="status">Carregando…</p>}>
        <Outlet />
      </Suspense>
    </ClerkProvider>
  );
}
```

- [ ] **Step 5: Reescrever `main.jsx`**

Substitua todo o conteúdo de `web/src/main.jsx` por:

```jsx
import { StrictMode, Suspense, lazy } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import App from './App.jsx';
import RecuperaCarga from './components/RecuperaCarga.jsx';
import NaoEncontrada from './paginas/NaoEncontrada.jsx';
import './style.css';
import './app.css';
import {
  AGENDA, AUDITORIA, CADASTRO, EQUIPE, FINANCEIRO, LOGIN, PACIENTES, PRONTUARIO,
} from './rotas.js';

/* Router de verdade, e não o mapa de `window.location.pathname` que estava
   aqui. O comentário anterior dizia "vale um router quando existir rota com
   parâmetro" — `/pacientes/:id/prontuario` é essa rota. O mapa também não tinha
   404: qualquer caminho desconhecido caía na landing com 200, então um link
   errado parecia funcionar. */

/* A landing e o 404 entram de cara; todo o resto, sob demanda. Quem abre o site
   num 3G baixa só a página de vendas — nem o Clerk, nem Agenda, Prontuário e
   Odontograma. O Clerk mora no ComClerk, que só monta nas rotas que precisam de
   sessão. */
const ComClerk = lazy(() => import('./ComClerk.jsx'));
const Login = lazy(() => import('./components/Login.jsx'));
const Cadastro = lazy(() => import('./paginas/Cadastro.jsx'));
const Layout = lazy(() => import('./paginas/Layout.jsx'));
const Agenda = lazy(() => import('./paginas/Agenda.jsx'));
const Pacientes = lazy(() => import('./paginas/Pacientes.jsx'));
const Prontuario = lazy(() => import('./paginas/Prontuario.jsx'));
const Financeiro = lazy(() => import('./paginas/Financeiro.jsx'));
const Equipe = lazy(() => import('./paginas/Equipe.jsx'));
const Auditoria = lazy(() => import('./paginas/Auditoria.jsx'));

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <RecuperaCarga>
        <Suspense fallback={<p className="body body-ash edge" role="status">Carregando…</p>}>
          <Routes>
            <Route path="/" element={<App />} />

            <Route element={<ComClerk />}>
              <Route path={LOGIN} element={<Login />} />
              {/* Fora do Layout de propósito: quem chega aqui ainda não tem clínica,
                  e o menu do Layout só aponta para telas que responderiam 401. */}
              <Route path={CADASTRO} element={<Cadastro />} />

              {/* Tudo sob o Layout exige sessão — o gate fica lá, uma vez. */}
              <Route element={<Layout />}>
                <Route path={AGENDA} element={<Agenda />} />
                <Route path={PACIENTES} element={<Pacientes />} />
                <Route path={PRONTUARIO} element={<Prontuario />} />
                <Route path={FINANCEIRO} element={<Financeiro />} />
                <Route path={EQUIPE} element={<Equipe />} />
                <Route path={AUDITORIA} element={<Auditoria />} />
              </Route>
            </Route>

            <Route path="*" element={<NaoEncontrada />} />
          </Routes>
        </Suspense>
      </RecuperaCarga>
    </BrowserRouter>
  </StrictMode>,
);
```

- [ ] **Step 6: Tirar o Clerk do `Nav.jsx`**

Em `web/src/components/Nav.jsx`:

Apague a primeira linha, `import { Show, UserButton } from '@clerk/react';`, e troque `import { AGENDA, LOGIN } from '../rotas.js';` por `import { LOGIN } from '../rotas.js';`.

No `menu-foot`, troque

```jsx
          <a href={LOGIN} className="btn btn-fill btn-lg">Entrar</a>
          <Show when="signed-in">
            <a href={AGENDA} className="btn btn-fill btn-lg">Abrir o sistema</a>
            <UserButton />
          </Show>
```
por
```jsx
          <a href={LOGIN} className="btn btn-fill btn-lg">Entrar</a>
```

Na barra, troque

```jsx
        {/* Fora do <Show>: /login é href estático, não precisa do Clerk para
            existir. <Show> devolve null ENQUANTO o Clerk carrega — e para
            sempre se ele não carregar (chave ausente no build, script
            bloqueado, offline). O `fallback` não cobre isso: ele só entra
            quando a condição é falsa, nunca durante a carga. Era esse o
            motivo de a landing subir sem botão de entrar. */}
        <a href={LOGIN} className="btn btn-fill">Entrar</a>
        <Show when="signed-in">
          <a href={AGENDA} className="btn">Sistema</a>
          <UserButton />
        </Show>
```
por
```jsx
        {/* /login é href estático: a landing não carrega o Clerk (ver
            ComClerk.jsx), e quem já tem sessão encontra "Abrir o sistema" no
            próprio /login. */}
        <a href={LOGIN} className="btn btn-fill">Entrar</a>
```

- [ ] **Step 7: `Suspense` em volta do `Outlet` do `Layout`**

Em `web/src/paginas/Layout.jsx`, troque `import { createContext, useContext } from 'react';` por `import { createContext, Suspense, useContext } from 'react';` e troque

```jsx
        {recurso === null || pode(recurso)
          ? <Outlet />
          : <SemAcesso papel={eu.dados.papel} />}
```
por
```jsx
        {/* A tela é um chunk à parte. O Suspense é daqui, e não o de fora: o de
            fora desmontaria a casca, e o tema claro piscaria para o preto a
            cada troca de aba. */}
        {recurso === null || pode(recurso)
          ? (
            <Suspense fallback={<Estado status="carregando" />}>
              <Outlet />
            </Suspense>
          )
          : <SemAcesso papel={eu.dados.papel} />}
```

- [ ] **Step 8: Rodar e ver passar**

Run: `cd web && npm test && npm run build && ls dist/assets`
Expected: tudo PASS; o build lista vários chunks, entre eles `ComClerk-<hash>.js`, `Login-<hash>.js`, `Agenda-<hash>.js` e o `index-<hash>.js` da entrada, bem menor que os 517 KB de antes.

- [ ] **Step 9: Conferir no navegador**

Com o preview rodando:
1. `/` — a landing aparece igual; na aba de rede, nenhuma requisição para o domínio do Clerk (`*.clerk.accounts.dev`).
2. `/login` — o widget aparece; "Entrar" da landing leva até ele.
3. `/nao-existe` — o 404 aparece.

- [ ] **Step 10: Commit**

```bash
git add web/src/acessibilidade.test.mjs web/src/components/RecuperaCarga.jsx web/src/ComClerk.jsx web/src/main.jsx web/src/components/Nav.jsx web/src/paginas/Layout.jsx
git commit -m "perf(web): a landing sai do Clerk, cada tela vira chunk, e a carga que falha tem saída" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: A aparência do Clerk

**Files:**
- Modify: `web/src/acessibilidade.test.mjs` (acrescentar ao fim)
- Modify: `web/src/components/Login.jsx` (objeto `APARENCIA`)
- Modify: `web/src/ComClerk.jsx` (`colorBorder`)
- Modify: `web/src/paginas/Layout.jsx` (`APARENCIA_CLARA.colorBorder`)

**Interfaces:**
- Consumes: `ler`, `semComentario`, `cor`, `contraste` (Task 1); `arquivosJs()` (Task 4); `ComClerk.jsx` com `colorBackground` e `colorBorder` literais (Task 6).
- Produces: nada.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente ao fim de `web/src/acessibilidade.test.mjs`:

```js
test('todo token que o JSX usa existe no CSS', () => {
  // A APARENCIA do Login pedia --brand, --ink, --surface e --line, de outro
  // design system. Nenhum existe aqui, e o fallback pintou o link de teal.
  const css = semComentario(ler('style.css') + ler('app.css'));
  const definidos = new Set([...css.matchAll(/(--[\w-]+)\s*:/g)].map((m) => m[1]));
  const orfaos = new Set();
  for (const f of arquivosJs()) {
    for (const m of ler(f).matchAll(/var\(\s*(--[\w-]+)/g)) {
      if (!definidos.has(m[1])) orfaos.add(`${f}: ${m[1]}`);
    }
  }
  assert.deepEqual([...orfaos], []);
});

test('a borda de campo do Clerk se enxerga nos dois temas', () => {
  // O widget não lê o nosso CSS: a cor vem do appearance. Branco a 26% sobre o
  // preto dava 2,10:1; preto a 22% sobre o branco, 1,69:1.
  for (const f of ['ComClerk.jsx', 'paginas/Layout.jsx']) {
    const fonte = ler(f);
    const fundo = fonte.match(/colorBackground:\s*'([^']+)'/)?.[1];
    const borda = fonte.match(/colorBorder:\s*'([^']+)'/)?.[1];
    assert.ok(fundo && borda, `${f}: não achei colorBackground e colorBorder`);
    const r = contraste(cor(borda, {}), cor(fundo, {}));
    assert.ok(r >= 3, `${f}: colorBorder ${borda} dá ${r.toFixed(2)}:1`);
  }
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd web && npm test`
Expected: FAIL nos dois. O primeiro lista `components/Login.jsx: --brand`, `--ink`, `--danger`, `--surface`, `--line` e `--radius-sm`; o segundo, `ComClerk.jsx: colorBorder rgba(255, 255, 255, .26) dá 2.10:1`.

- [ ] **Step 3: A `APARENCIA` do Login cuida só de encaixe**

Em `web/src/components/Login.jsx`, substitua o objeto inteiro — de `const APARENCIA = {` até o `};` que o fecha, logo antes do comentário "Entrar e criar são a mesma rota…" — por:

```js
/* Só encaixe. Cor, fonte e canto vêm do `aparencia` do ComClerk — preto, osso
   e canto vivo. A versão anterior repintava o widget com tokens de outro design
   system (--brand, --ink, --surface, --line) que aqui não existem: o fallback
   deixava o "Registre-se" em teal a 3,84:1, abaixo do AA, e o campo com canto de
   10px. */
const APARENCIA = {
  elements: {
    /* O widget monta em volta do card; tiramos a sombra e a moldura dele para
       ele se fundir com a coluna .who-half. */
    rootBox: { width: '100%' },
    card: {
      background: 'transparent',
      boxShadow: 'none',
      border: 'none',
      padding: '0',
      width: '100%',
    },

    header: { display: 'none' }, // o <Lines> acima já faz esse papel
    logoBox: { display: 'none' },

    /* Botões OAuth — reaproveitam .btn para herdar borda, altura e foco. */
    socialButtonsBlockButton: 'btn btn-lg',
    socialButtonsIconButton: 'btn btn-lg',

    dividerRow: { marginBlock: 'var(--spacing-30)' },
    dividerText: 'credit',

    formFieldLabel: 'credit',
    formFieldInputShowPasswordButton: 'credit',

    formButtonPrimary: 'btn btn-lg btn-fill',
    formButtonReset: 'btn',

    /* "Usar código por e-mail" / "esqueci a senha" / "reenviar" — tudo no
       mesmo tom discreto dos links .credit. */
    formFieldAction: 'credit',
    formResendCodeLink: 'credit',
    identityPreviewEditButton: 'credit',
    footerAction: { marginTop: 'var(--spacing-20)' },
    footerActionText: 'body body-ash',
    footerActionLink: 'credit',

    alertText: 'body',
    formFieldErrorText: 'credit',
  },
};
```

- [ ] **Step 4: As duas bordas de campo do Clerk**

Em `web/src/ComClerk.jsx`, troque

```js
    colorBorder: 'rgba(255, 255, 255, .26)',
```
por
```js
    /* Borda de campo é controle, não decoração: branco a 26% dava 2,10:1 sobre
       o preto, e o 1.4.11 pede 3:1. #838383 é o cinza da marca, 5,54:1. */
    colorBorder: '#838383',
```

Em `web/src/paginas/Layout.jsx`, no `APARENCIA_CLARA`, troque

```js
  colorBorder: 'rgba(0, 0, 0, .22)',
```
por
```js
  /* Controle, não hairline: preto a 22% dava 1,69:1 nos campos do perfil.
     #5c5c5c é a --tinta-fraca do tema claro. */
  colorBorder: '#5c5c5c',
```

- [ ] **Step 5: Rodar e ver passar**

Run: `cd web && npm test && npm run build`
Expected: tudo PASS; build sem erro.

- [ ] **Step 6: Conferir o `/login` no navegador**

Com o preview rodando, em `/login`, depois que o widget carregar, rode no console:

```js
const s = (q) => { const c = getComputedStyle(document.querySelector(q)); return [c.backgroundColor, c.borderTopColor, c.borderTopLeftRadius, c.color]; };
[s('.cl-formFieldInput'), s('.cl-footerActionLink')]
```
Expected: o campo com fundo `rgb(0, 0, 0)`, borda `rgb(131, 131, 131)` e raio `0px`; o link "Registre-se" com cor `rgb(255, 255, 255)` — nada de `rgb(15, 118, 110)`.

- [ ] **Step 7: Commit**

```bash
git add web/src/acessibilidade.test.mjs web/src/components/Login.jsx web/src/ComClerk.jsx web/src/paginas/Layout.jsx
git commit -m "fix(web): o login volta a ser preto e de canto vivo, e o campo do Clerk se enxerga" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Fontes do próprio domínio

**Files:**
- Modify: `web/src/acessibilidade.test.mjs` (acrescentar ao fim)
- Modify: `web/package.json` e `web/package-lock.json` (via `npm install`)
- Modify: `web/src/main.jsx` (imports das fontes)
- Modify: `web/index.html` (sai o Google Fonts)

**Interfaces:**
- Consumes: `ler` (Task 1), `indexHtml()` (Task 4).
- Produces: nada.

- [ ] **Step 1: Escrever o teste que falha**

Acrescente ao fim de `web/src/acessibilidade.test.mjs`:

```js
test('as fontes vêm do próprio domínio', () => {
  // O Google Fonts custava duas negociações de DNS e TLS no 3G e mandava o IP
  // de cada visitante para um terceiro.
  assert.ok(!/fonts\.(googleapis|gstatic)\.com/.test(indexHtml()), 'o index.html ainda chama o Google Fonts');
  const main = ler('main.jsx');
  for (const familia of ['antonio', 'cormorant-sc', 'inter']) {
    assert.match(main, new RegExp(`^import '@fontsource/${familia}/`, 'm'), `falta a fonte ${familia}`);
  }
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd web && npm test`
Expected: FAIL em `as fontes vêm do próprio domínio`, com `o index.html ainda chama o Google Fonts`.

- [ ] **Step 3: Instalar as fontes**

Run: `cd web && npm install @fontsource/antonio@^5.3.0 @fontsource/cormorant-sc@^5.3.0 @fontsource/inter@^5.3.0`
Expected: as três entram em `dependencies` do `web/package.json`.

- [ ] **Step 4: Importar no `main.jsx`**

Em `web/src/main.jsx`, logo antes de `import './style.css';`, acrescente:

```jsx
/* As fontes vêm do próprio domínio, e não do Google Fonts: duas negociações de
   DNS e TLS a menos no 3G, e o IP de quem abre o site não vai para um terceiro.
   Os mesmos pesos do <link> que saiu do index.html. */
import '@fontsource/antonio/400.css';
import '@fontsource/antonio/700.css';
import '@fontsource/cormorant-sc/400.css';
import '@fontsource/inter/400.css';
import '@fontsource/inter/700.css';
```

- [ ] **Step 5: Tirar o Google Fonts do `index.html`**

Em `web/index.html`, apague as três linhas:

```html
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Antonio:wght@400..700&family=Cormorant+SC:wght@400&family=Inter:wght@400;700&display=swap" rel="stylesheet">
```

- [ ] **Step 6: Rodar e ver passar**

Run: `cd web && npm test && npm run build`
Expected: tudo PASS; o build emite os `.woff2` em `dist/assets/`.

- [ ] **Step 7: Conferir no navegador**

Com o preview rodando, na landing: `document.fonts.check('1em Antonio') && document.fonts.check('1em Inter') && document.fonts.check('1em "Cormorant SC"')` devolve `true`; os títulos estão em Antonio; na aba de rede, nenhuma requisição para `fonts.googleapis.com` nem `fonts.gstatic.com`.

- [ ] **Step 8: Commit**

```bash
git add web/src/acessibilidade.test.mjs web/package.json web/package-lock.json web/src/main.jsx web/index.html
git commit -m "perf(web): fontes servidas do próprio domínio" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: A triagem de saúde em linguagem simples

**Files:**
- Create: `web/src/legibilidade.test.mjs`
- Create: `web/src/triagem.js`
- Modify: `web/src/components/FichaPaciente.jsx`

**Interfaces:**
- Consumes: nada.
- Produces: `web/src/triagem.js` com `export const TRIAGEM` — as chaves `aviso`, `emTratamentoMedico`, `condicaoSistemica`, `medicamentoContinuo`, `alergia`, `gravidez`, `motivoConsulta`, `sensibilidade` e `sangramentoGengival`, todas string.

- [ ] **Step 1: Escrever o teste que falha**

Crie `web/src/legibilidade.test.mjs`:

```js
/**
 * Linguagem simples no que o paciente lê (ODS-19).
 *
 *   cd web && npm test
 *
 * Índice de Flesch adaptado ao português (Martins et al., 1996):
 *
 *   248,835 − 1,015 × (palavras / frases) − 84,6 × (sílabas / palavras)
 *
 * De 75 a 100 é "muito fácil"; de 50 a 75, "fácil". A sílaba é contada por
 * grupo vocálico, o que erra em hiato ("saúde" conta duas): é heurística, e
 * serve para comparar versões do mesmo texto, não para laudo.
 */
import assert from 'node:assert/strict';
import test from 'node:test';
import { TRIAGEM } from './triagem.js';

const VOGAIS = /[aeiouáéíóúâêôãõàü]+/gi;

function flesch(textos) {
  let frases = 0;
  let palavras = 0;
  let silabas = 0;
  for (const texto of textos) {
    frases += Math.max(1, (texto.match(/[.!?]+/g) ?? []).length);
    for (const bruta of texto.split(/\s+/)) {
      const palavra = bruta.replace(/[^a-záéíóúâêôãõàüç]/gi, '');
      if (!palavra) continue;
      palavras += 1;
      silabas += Math.max(1, (palavra.match(VOGAIS) ?? []).length);
    }
  }
  return 248.835 - 1.015 * (palavras / frases) - 84.6 * (silabas / palavras);
}

test('a fórmula dá os valores de referência', () => {
  assert.ok(Math.abs(flesch(['O gato bebe leite.']) - 96.725) < 0.001);
  assert.ok(Math.abs(flesch(['O gato bebe leite. A casa é grande.']) - 107.3) < 0.001);
});

test('a triagem de saúde se lê sem esforço', () => {
  // O paciente preenche isto sozinho no pré-cadastro. "Cardíacos",
  // "hipertensão" e "uso contínuo" davam 58,1 — passariam num piso de 50.
  const indice = flesch(Object.values(TRIAGEM));
  assert.ok(indice >= 75, `índice ${indice.toFixed(1)}; "muito fácil" começa em 75`);
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd web && npm test`
Expected: FAIL com `Cannot find module` para `./triagem.js`.

- [ ] **Step 3: Tirar o texto da triagem do JSX, sem mudar uma palavra**

Crie `web/src/triagem.js`:

```js
/**
 * O texto da triagem de saúde da ficha, fora do JSX.
 *
 * <p>É o trecho que o próprio paciente lê no pré-cadastro, e por isso passa
 * pelo teste de legibilidade (legibilidade.test.mjs), que só consegue ler o
 * que não está preso no JSX.
 */
export const TRIAGEM = {
  aviso: 'Dado de saúde é sensível (LGPD art. 11) e só a equipe clínica lê.',
  emTratamentoMedico: 'Está em tratamento médico atualmente?',
  condicaoSistemica: 'Problemas cardíacos, diabetes ou hipertensão?',
  medicamentoContinuo: 'Medicamento de uso contínuo — qual?',
  alergia: 'Alergia a medicamento ou látex — qual?',
  gravidez: 'Está grávida?',
  motivoConsulta: 'Motivo principal da consulta',
  sensibilidade: 'Sente sensibilidade (frio, calor, doces)?',
  sangramentoGengival: 'Sangra ao escovar ou passar fio dental?',
};
```

Em `web/src/components/FichaPaciente.jsx`, acrescente `import { TRIAGEM } from '../triagem.js';` depois do import de `../documento.js`, e troque:
- `Dado de saúde é sensível (LGPD art. 11) e só a equipe clínica lê.` (o texto dentro do `<p className="cap cap-ash">` da seção 3) por `{TRIAGEM.aviso}`;
- `rotulo="Está em tratamento médico atualmente?"` por `rotulo={TRIAGEM.emTratamentoMedico}`;
- `rotulo="Problemas cardíacos, diabetes ou hipertensão?"` por `rotulo={TRIAGEM.condicaoSistemica}`;
- `<span className="cap cap-ash">Medicamento de uso contínuo — qual?</span>` por `<span className="cap cap-ash">{TRIAGEM.medicamentoContinuo}</span>`;
- `<span className="cap cap-ash">Alergia a medicamento ou látex — qual?</span>` por `<span className="cap cap-ash">{TRIAGEM.alergia}</span>`;
- `<span className="cap cap-ash">Está grávida?</span>` por `<span className="cap cap-ash">{TRIAGEM.gravidez}</span>`;
- `<span className="cap cap-ash">Motivo principal da consulta</span>` por `<span className="cap cap-ash">{TRIAGEM.motivoConsulta}</span>`;
- `rotulo="Sente sensibilidade (frio, calor, doces)?"` por `rotulo={TRIAGEM.sensibilidade}`;
- `rotulo="Sangra ao escovar ou passar fio dental?"` por `rotulo={TRIAGEM.sangramentoGengival}`.

- [ ] **Step 4: Rodar e ver o gate morder o jargão**

Run: `cd web && npm test`
Expected: FAIL em `a triagem de saúde se lê sem esforço` com `índice 58.1; "muito fácil" começa em 75`. É a medida do texto de hoje.

- [ ] **Step 5: Reescrever em linguagem simples**

Em `web/src/triagem.js`, troque o objeto por:

```js
export const TRIAGEM = {
  aviso: 'Suas respostas sobre saúde são protegidas pela LGPD. Só a equipe que cuida de você lê.',
  emTratamentoMedico: 'Faz algum tratamento médico agora?',
  condicaoSistemica: 'Tem problema de coração, diabetes ou pressão alta?',
  medicamentoContinuo: 'Toma algum remédio todo dia? Qual?',
  alergia: 'Tem alergia a remédio ou a látex? Qual?',
  gravidez: 'Está grávida?',
  motivoConsulta: 'Por que você quer a consulta?',
  sensibilidade: 'Sente dor nos dentes com frio, calor ou doce?',
  sangramentoGengival: 'A gengiva sangra quando você escova ou passa fio dental?',
};
```

- [ ] **Step 6: Rodar e ver passar**

Run: `cd web && npm test && npm run build`
Expected: tudo PASS (índice 89,4); build sem erro.

- [ ] **Step 7: Commit**

```bash
git add web/src/legibilidade.test.mjs web/src/triagem.js web/src/components/FichaPaciente.jsx
git commit -m "feat(web): a triagem de saúde em linguagem simples, com índice medido" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: axe nas rotas públicas, reflow e carga que falha

**Files:**
- Modify: `web/package.json` e `web/package-lock.json` (devDependencies e script `a11y`)
- Modify: `web/.gitignore`
- Create: `web/a11y/auditar.mjs`
- Modify: `.github/workflows/ci.yml` (job `acessibilidade`)

**Interfaces:**
- Consumes: o texto `Algo não carregou` do `RecuperaCarga` e o chunk `assets/ComClerk-<hash>.js` (Task 6).
- Produces: `web/a11y-relatorio.json` (ignorado pelo git; artefato do CI) e o job `acessibilidade`, que a Task 11 estende.

- [ ] **Step 1: Instalar as ferramentas**

Run: `cd web && npm install -D playwright-core@^1.63.0 @axe-core/playwright@^4.13.0`
Expected: as duas entram em `devDependencies`; nenhum navegador é baixado.

- [ ] **Step 2: Ignorar o relatório**

Acrescente ao fim de `web/.gitignore`:

```
a11y-relatorio.json
```

- [ ] **Step 3: Escrever o script**

Crie `web/a11y/auditar.mjs`:

```js
/**
 * O gate de acessibilidade no navegador (ODS-17).
 *
 *   cd web && npm run a11y
 *
 * Sobe o dist/ com o preview() do próprio Vite, abre o Chrome já instalado
 * (o playwright-core não baixa navegador) e, em cada rota pública:
 *
 *   - roda o axe-core com as regras A e AA das WCAG 2.0, 2.1 e 2.2, e reprova
 *     com QUALQUER violação, não só as críticas;
 *   - confere o reflow: a 320px de largura, nada de rolagem horizontal (1.4.10).
 *
 * E confere a carga que falha: com o chunk do ComClerk bloqueado, o /login
 * mostra a recuperação, não uma tela branca.
 *
 * O relatório vai para a11y-relatorio.json — é a evidência do indicador do
 * ODS-17 ("violações críticas = zero, origem: CI (axe-core)"). As telas
 * autenticadas não passam por aqui: exigiriam o segredo do Clerk no CI e um
 * mock por endpoint. Elas têm o roteiro manual de docs/acessibilidade.md.
 */
import AxeBuilder from '@axe-core/playwright';
import { appendFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright-core';
import { loadEnv, preview } from 'vite';

const RAIZ = fileURLToPath(new URL('..', import.meta.url));
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

/* O mesmo env que o build leu (.env.local e variáveis do processo). Sem a chave,
   o /login é só "Configuração ausente" — auditá-lo seria auditar o aviso. */
const TEM_CHAVE = Boolean(loadEnv('production', RAIZ, 'VITE_').VITE_CLERK_PUBLISHABLE_KEY);

const ROTAS = [
  { nome: 'landing', caminho: '/' },
  { nome: '404', caminho: '/nao-existe' },
  /* O widget vem de um script remoto do Clerk: espera o botão principal. */
  { nome: 'login', caminho: '/login', precisaDaChave: true, esperar: '.cl-formButtonPrimary' },
];

/* O selo "Development mode" é da instância de teste do Clerk e não existe em
   produção. É a única coisa que o axe não olha. */
async function marcarSeloDeDesenvolvimento(pagina) {
  await pagina.evaluate(() => {
    for (const el of document.querySelectorAll('body *')) {
      if (el.children.length === 0 && el.textContent.trim() === 'Development mode') {
        el.setAttribute('data-a11y-fora', '');
      }
    }
  });
}

async function auditarRota(contexto, base, rota) {
  const pagina = await contexto.newPage();
  try {
    await pagina.goto(new URL(rota.caminho, base).href, { waitUntil: 'load' });
    if (rota.esperar) await pagina.waitForSelector(rota.esperar, { timeout: 30_000 });
    await marcarSeloDeDesenvolvimento(pagina);

    const r = await new AxeBuilder({ page: pagina }).withTags(TAGS).exclude('[data-a11y-fora]').analyze();

    await pagina.setViewportSize({ width: 320, height: 640 });
    const largura = await pagina.evaluate(() => document.documentElement.scrollWidth);

    return {
      rota: rota.nome,
      caminho: rota.caminho,
      axe: r.testEngine.version,
      aprovadas: r.passes.length,
      incompletas: r.incomplete.length,
      violacoes: r.violations.map((v) => ({
        regra: v.id,
        impacto: v.impact,
        ajuda: v.help,
        onde: v.nodes.map((n) => n.target.join(' ')),
      })),
      reflow: { largura, passou: largura <= 320 },
    };
  } finally {
    await pagina.close();
  }
}

/* Dividir em chunks cria uma falha que o bundle único não tinha: o 3G cai no
   meio da navegação e o import() rejeita. Tem de aparecer a recuperação. */
async function cargaQueFalha(contexto, base) {
  const pagina = await contexto.newPage();
  try {
    await pagina.route(/\/assets\/ComClerk[-.][^/]*\.js$/, (r) => r.abort());
    await pagina.goto(new URL('/login', base).href);
    await pagina.getByText('Algo não carregou').waitFor({ timeout: 10_000 });
    return { passou: true };
  } catch (erro) {
    return { passou: false, erro: erro.message.split('\n')[0] };
  } finally {
    await pagina.close();
  }
}

const servidor = await preview({ root: RAIZ, preview: { port: 4173, strictPort: true }, logLevel: 'warn' });
const base = servidor.resolvedUrls.local[0];
const navegador = await chromium.launch({ channel: 'chrome' });

const relatorio = { gerado: new Date().toISOString(), tags: TAGS, parcial: !TEM_CHAVE, rotas: [], cargaQueFalha: null };
try {
  /* Movimento reduzido: o [data-reveal] da landing aparece de uma vez, e o axe
     avalia o estado final, não o meio de uma animação. */
  const contexto = await navegador.newContext({ reducedMotion: 'reduce' });
  for (const rota of ROTAS) {
    if (rota.precisaDaChave && !TEM_CHAVE) continue;
    relatorio.rotas.push(await auditarRota(contexto, base, rota));
  }
  relatorio.cargaQueFalha = await cargaQueFalha(contexto, base);
} finally {
  await navegador.close();
  await servidor.close();
}

await writeFile(new URL('../a11y-relatorio.json', import.meta.url), `${JSON.stringify(relatorio, null, 2)}\n`);

const linhas = [];
let falhou = false;
for (const r of relatorio.rotas) {
  const ok = r.violacoes.length === 0 && r.reflow.passou;
  falhou ||= !ok;
  linhas.push(`${ok ? 'ok   ' : 'FALHA'} ${r.caminho} — ${r.violacoes.length} violação(ões), `
    + `${r.aprovadas} regras aprovadas, reflow a 320px: ${r.reflow.largura}px`);
  for (const v of r.violacoes) linhas.push(`      ${v.regra} (${v.impacto}): ${v.ajuda} → ${v.onde.join(' | ')}`);
}
const carga = relatorio.cargaQueFalha;
falhou ||= !carga.passou;
linhas.push(`${carga.passou ? 'ok   ' : 'FALHA'} carga que falha no /login${carga.erro ? ` — ${carga.erro}` : ''}`);
if (relatorio.parcial) {
  linhas.push('AUDITORIA PARCIAL: /login pulado — o build não tem VITE_CLERK_PUBLISHABLE_KEY.');
}

const resumo = linhas.join('\n');
console.log(resumo);
if (process.env.GITHUB_STEP_SUMMARY) {
  await appendFile(process.env.GITHUB_STEP_SUMMARY, `### Acessibilidade\n\n\`\`\`\n${resumo}\n\`\`\`\n`);
}
if (falhou) process.exitCode = 1;
```

- [ ] **Step 4: O script `a11y`**

Em `web/package.json`, em `scripts`, acrescente depois de `"test"`:

```json
    "a11y": "vite build && node a11y/auditar.mjs"
```
(com a vírgula na linha de `"test"`).

- [ ] **Step 5: Rodar**

Run: `cd web && npm run a11y`
Expected: três linhas `ok` (`/`, `/nao-existe`, `/login`), reflow ≤ 320px em cada uma, e `ok carga que falha no /login`. O `a11y-relatorio.json` é gravado.

- [ ] **Step 6: Se o axe apontar violação, corrigir e rodar de novo**

Cada violação listada vem com a regra, o impacto e o seletor. A correção segue as regras deste plano — token semântico no lugar de literal, vermelho como marca, rótulo no controle, alvo de pelo menos 24×24px — e nunca uma exclusão do axe: a única exclusão é o selo "Development mode". Anote cada violação corrigida (regra, onde, o que mudou) para a seção de achados da Task 12. Repita `npm run a11y` até dar zero.

- [ ] **Step 7: Rodar sem a chave (Review Focus 1)**

Run (no Git Bash): `cd web && VITE_CLERK_PUBLISHABLE_KEY= npm run a11y`
Expected: `ok` em `/` e em `/nao-existe`, `ok carga que falha no /login`, e a linha `AUDITORIA PARCIAL: /login pulado — o build não tem VITE_CLERK_PUBLISHABLE_KEY.` O processo termina com código 0: a landing e o 404 não dependem mais da chave. Depois, `npm run build` de novo, para o `dist/` voltar a ter a chave.

- [ ] **Step 8: O job no CI**

Em `.github/workflows/ci.yml`, acrescente ao fim (no mesmo nível de `api:` e `contratos:`):

```yaml
  # Acessibilidade no navegador (ODS-17): axe-core nas rotas públicas, reflow a
  # 320px e carga que falha. A chave publicável do Clerk é pública por definição
  # e vem de uma variável do repositório; sem ela o /login é pulado e o resumo
  # diz "AUDITORIA PARCIAL". O Chrome já vem no runner: o playwright-core não
  # baixa navegador.
  acessibilidade:
    runs-on: ubuntu-latest
    env:
      VITE_CLERK_PUBLISHABLE_KEY: ${{ vars.VITE_CLERK_PUBLISHABLE_KEY }}
    defaults:
      run:
        working-directory: web
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-node@v5
        with:
          node-version: '24'
          cache: npm
          cache-dependency-path: web/package-lock.json
      - run: npm ci
      - run: npm run build
      - run: node a11y/auditar.mjs
      - uses: actions/upload-artifact@v7
        if: always()
        with:
          name: a11y-relatorio
          path: web/a11y-relatorio.json
```

- [ ] **Step 9: Commit**

```bash
git add web/package.json web/package-lock.json web/.gitignore web/a11y/auditar.mjs .github/workflows/ci.yml
git commit -m "ci(web): axe-core nas rotas públicas, reflow e carga que falha" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

(Inclua no commit os arquivos que o Step 6 tiver corrigido.)

---

### Task 11: Orçamento de peso da landing

**Files:**
- Create: `web/a11y/peso.mjs`
- Modify: `web/package.json` (script `a11y`)
- Modify: `.github/workflows/ci.yml` (passo no job `acessibilidade`)

**Interfaces:**
- Consumes: o `dist/` do build; o job `acessibilidade` (Task 10).
- Produces: nada.

- [ ] **Step 1: Escrever o script com o limite zerado**

Crie `web/a11y/peso.mjs`:

```js
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

const LIMITE_KB = 0;

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
```

- [ ] **Step 2: Rodar e ver falhar — é a medição**

Run: `cd web && npm run build && node a11y/peso.mjs`
Expected: FAIL com `Acima do orçamento de peso da landing.` A última linha traz o total medido, `T KB total em gzip`, bem abaixo dos 148,9 KB de antes.

- [ ] **Step 3: Fixar o limite no medido mais 10%**

Troque `const LIMITE_KB = 0;` pelo teto de `T × 1,1` (arredondado para cima, em KB inteiro), com o comentário que diz de onde veio — por exemplo, se `T` for 71,3:

```js
/* Medido em 26/09/2026, depois da divisão por rota, do Clerk fora da landing e
   das fontes da casa: 71,3 KB. O limite é isso mais 10%, arredondado para cima.
   Antes destas mudanças a landing carregava 148,9 KB. Subir este número é
   decisão, não ajuste: o commit que o sobe diz por quê. */
const LIMITE_KB = 79;
```
(use o `T` que o Step 2 imprimiu, não o do exemplo).

- [ ] **Step 4: Rodar e ver passar**

Run: `cd web && node a11y/peso.mjs`
Expected: PASS, com o total abaixo do limite.

- [ ] **Step 5: Ligar no `a11y` e no CI**

Em `web/package.json`, troque `"a11y": "vite build && node a11y/auditar.mjs"` por `"a11y": "vite build && node a11y/peso.mjs && node a11y/auditar.mjs"`.

Em `.github/workflows/ci.yml`, no job `acessibilidade`, entre `- run: npm run build` e `- run: node a11y/auditar.mjs`, acrescente:

```yaml
      - run: node a11y/peso.mjs
```

- [ ] **Step 6: Conferir no navegador**

Com o preview rodando, abra a landing com a aba de rede: nada do Clerk, nada do Google Fonts, e só o `index-<hash>.js` de JavaScript.

- [ ] **Step 7: Commit**

```bash
git add web/a11y/peso.mjs web/package.json .github/workflows/ci.yml
git commit -m "ci(web): orçamento de peso da landing" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Estudo de caso, roteiro manual e README

**Files:**
- Create: `docs/acessibilidade.md`
- Modify: `README.md` (seções "Verificar" e "Arquitetura")

**Interfaces:**
- Consumes: os números medidos nas Tasks 1–11 (o total do peso da Task 11 e os achados do axe da Task 10, Step 6).
- Produces: nada.

- [ ] **Step 1: Escrever `docs/acessibilidade.md`**

Crie `docs/acessibilidade.md` com o conteúdo abaixo, trocando `T` pelo total medido na Task 11 e preenchendo "Achados do axe" com o que a Task 10, Step 6 anotou (ou "nenhum", se o axe deu zero de primeira):

```markdown
# Acessibilidade

A DentiBot mira a WCAG 2.2 nível AA — ODS-17, 18, 19 e 16 do Documento 04. Este
documento diz o que já é provado por máquina, o que não é, e como conferir o
resto à mão.

## O que prova o quê

| Camada | Onde | Prova | Não prova |
|---|---|---|---|
| Invariantes | `web/src/acessibilidade.test.mjs`, `web/src/legibilidade.test.mjs`, `app/src/acessibilidade.test.mjs` — em todo PR, no job `contratos` | contraste de todo token de texto e de foco nos dois temas; borda de campo a 3:1; nenhum literal da marca pintando o papel; vermelho nunca como texto no tema claro; idioma, zoom, foco, `h1`; token do JSX existe no CSS; landing sem Clerk; fontes do próprio domínio; triagem com Flesch ≥ 75 | nada que dependa do DOM renderizado |
| Navegador | `web/a11y/auditar.mjs` — job `acessibilidade` | axe-core (A e AA das WCAG 2.0, 2.1 e 2.2) em `/`, `/login` e no 404; reflow a 320px; carga que falha vira mensagem | as telas autenticadas; o que o axe não mede (cerca de dois terços dos problemas reais) |
| Peso | `web/a11y/peso.mjs` — job `acessibilidade` | JS e CSS que a landing carrega de cara abaixo do orçamento | tempo de carga em rede real |
| Roteiro manual | este documento, a cada release | as telas autenticadas, com teclado, NVDA, zoom e reflow | — |

Rodar localmente, com o Chrome instalado:

    cd web && npm test && npm run a11y
    cd app && npm test

No CI, o `/login` só é auditado se o repositório tiver a variável
`VITE_CLERK_PUBLISHABLE_KEY` (Settings → Secrets and variables → Actions →
Variables). É a chave publicável — pública por definição. Sem ela, o resumo do
job diz **AUDITORIA PARCIAL**.

## As decisões, como estudo de caso

**O defeito era sempre o mesmo.** Um literal da marca pensado para a obsidiana,
usado sobre o papel do tema claro. O cinza `#838383` dá 5,54:1 no preto e 3,6:1
no papel; o osso some no papel; o vermelho `#ed1c24` dá 4,79:1 no preto e 4,16:1
no papel. A correção nunca criou cor: trocou o literal pelo token semântico
(`--tinta`, `--tinta-fraca`, `--fundo`), que no escuro vale o mesmo literal — a
landing não mudou um pixel — e no claro vale o que passa.

- `.cap-ash` e `.body-ash`: 67 usos em 9 arquivos, corrigidos numa linha cada.
- `.btn`, `.btn-fill` e `.skip-link`: borda e fundo em osso sumiam no papel; o
  botão do sistema parecia texto solto. Agora é a caixa preta de canto vivo.

**Vermelho é marca, não tinta.** No tema claro o vermelho é filete, borda ou
fundo; o texto de erro, de alarme e de perigo é tinta (17,95:1). No hover do
botão de perigo o texto é obsidiana, e não tinta: `#111` sobre o vermelho dá
4,31:1; `#000`, 4,79:1.

**Hairline decora; controle se enxerga.** O critério 1.4.11 pede 3:1 para o que
identifica um controle ou um estado. A borda de campo, a do widget do Clerk, a do
app móvel e as marcas de condição do odontograma passaram de 1,69:1 ou 2,10:1
para `--tinta-fraca`/`#838383`. As hairlines de tabela ficaram finas: são
separação.

**O `/login` tinha outro design system.** A aparência do widget pedia `--brand`,
`--ink`, `--surface` e `--line`, que aqui não existem; o fallback deixava o link
em teal a 3,84:1 e o campo com canto de 10px. Agora ela cuida só de encaixe, e o
teste de tokens do JSX impede a volta.

**Rede modesta.** A landing carregava 148,9 KB de JavaScript em gzip, com o app
inteiro, mais o clerk-js e o @clerk/ui do CDN do Clerk, mais o Google Fonts.
Agora carrega T KB: o Clerk só entra nas rotas de sessão, cada tela é um chunk,
e as fontes vêm do próprio domínio — o que também tira as origens do Google da
CSP da borda do web. Dividir em chunks criou uma falha nova, a do `import()` que
rejeita quando o 3G cai; o `RecuperaCarga` troca a tela branca por uma frase e
"Tentar de novo".

**Linguagem simples.** A triagem de saúde da ficha, que o paciente lê sozinho no
pré-cadastro, dava 58,1 no Flesch adaptado ao português — "cardíacos",
"hipertensão", "uso contínuo". Reescrita ("pressão alta", "remédio todo dia"),
dá 89,4. A contagem de sílabas é heurística e serve para comparar versões.

### Achados do axe

(Preencher com o que a primeira auditoria encontrou e como foi corrigido.)

## Roteiro manual — a cada release

Com a API e o web rodando (`iniciar.bat`), entrar com uma conta de teste de cada
papel que alcance a tela. Em cada tela — Agenda, Pacientes, Prontuário,
Financeiro, Equipe, Auditoria, Cadastro:

1. **Só teclado.** Chegar a todas as ações com Tab e Shift+Tab, na ordem da
   leitura; o anel de foco sempre visível; `Esc` fecha o diálogo de confirmação e
   devolve o foco a quem o abriu; setas trocam as abas do Prontuário.
2. **NVDA** (Windows, gratuito). O título da tela é anunciado; o aviso de sucesso
   é lido sem interromper; o erro interrompe; o selo de status é lido com o texto
   dele; cada campo diz o próprio rótulo.
3. **Zoom a 200%** (Ctrl +). Nada cortado nem sobreposto.
4. **Reflow a 320px** (DevTools, largura 320). Sem rolagem horizontal no corpo; a
   tabela vira blocos empilhados.
5. **Contraste de estado.** Hover e foco de botão, campo com erro, selo de
   alarme e dente do odontograma legíveis — o axe não mede estado.

### Registro

| Data | Versão (commit) | Quem | Telas | Achados |
|---|---|---|---|---|
| | | | | |

## Declaração pública

Uma página pública de declaração de acessibilidade entra depois da primeira
linha deste registro: declarar conformidade antes de medir é o que o Documento
04 proíbe.
```

- [ ] **Step 2: Atualizar o README**

Em `README.md`, na seção `## Verificar`, depois da linha `cd web && npm run build` dentro do bloco de código, acrescente:

```bash
cd web && npm run a11y        # axe-core, reflow e peso da landing — precisa do Chrome
```

E, na seção `## Arquitetura`, acrescente um item à lista:

```markdown
- **[docs/acessibilidade.md](docs/acessibilidade.md)** — o que o gate de
  acessibilidade prova, o que não prova, e o roteiro manual de cada release.
```

- [ ] **Step 3: Verificação final**

Run:
```bash
cd web && npm test && npm run a11y
cd ../app && npm test
```
Expected: tudo PASS; `npm run a11y` com as três rotas `ok`, a carga que falha `ok` e o peso abaixo do limite.

No navegador, a 1280px e a 400px:
- landing idêntica à de antes, títulos em Antonio;
- `/login` com o widget preto, canto vivo, link em osso;
- uma tela clara (qualquer uma, logado) com o vermelho só em filete, os campos com borda visível e os botões como caixa preta.

- [ ] **Step 4: Commit**

```bash
git add docs/acessibilidade.md README.md
git commit -m "docs: acessibilidade como estudo de caso, com o roteiro manual" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
