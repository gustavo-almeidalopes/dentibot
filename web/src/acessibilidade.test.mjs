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
