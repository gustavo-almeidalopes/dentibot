# UX e UI das telas do sistema — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** As seis telas autenticadas do `web/` mais a casca deixam de usar os primitivos da landing e passam a ser uma ferramenta de trabalho em tema claro, sem nenhum fluxo novo.

**Architecture:** Uma camada de tokens semânticos sobre os literais da marca; o tema claro entra por `:root:has(.app-main)`, sem JavaScript. O CSS do sistema sai de `style.css` para `app.css`. A lógica de apresentação que dá para testar (status, horários, contagens, telefone) sai das telas para `apresentacao.js` e ganha teste `node --test`; o que não dá para testar com o ferramental deste projeto (layout, foco, `<dialog>`) é verificado por build, por testes-guarda que leem o fonte, e por conferência em dois tamanhos de tela.

**Tech Stack:** React 19, Vite 8, react-router-dom 7, `@clerk/react` 6, `node --test` (sem jsdom, sem testing-library — nenhuma dependência nova entra).

**Spec:** `docs/superpowers/specs/2026-09-13-ux-ui-telas-do-sistema-design.md`

## Global Constraints

- **Nenhuma dependência nova.** Nem de teste, nem de UI, nem de ícone.
- **Nenhum endpoint novo**, com exatamente duas exceções: `GET /equipe` no Prontuário (Task 9) e `GET /pacientes` no Financeiro (Task 10).
- **Nenhum fluxo novo.** Não existe "agendar consulta", "cancelar consulta" nem escrita no financeiro neste plano.
- **Paleta clara, valores exatos:** `--fundo: #faf9f7`, `--superficie: #ffffff`, `--tinta: #111111`, `--tinta-fraca: #5c5c5c`, `--hair: rgba(0,0,0,.22)`, `--hair-fraca: rgba(0,0,0,.10)`, `--foco: #111111`.
- **`--ash` (`#838383`) nunca é texto no tema claro.** Dá 3,66:1 sobre `#faf9f7`, abaixo do mínimo AA de 4,5:1. `#5c5c5c` dá 6,22:1.
- **Literais da marca não mudam:** `--obsidian`, `--bone`, `--ash`, `--alarm` ficam como estão.
- **Landing e `NaoEncontrada.jsx` continuam escuras.** Só quem monta `.app-main` clareia.
- **Raio zero em tudo.** `--radius-*` continuam `0px`; o canto vivo é a marca.
- **Comentário explica por quê, não o quê.** É o padrão desta base; siga o tom dos arquivos que você está editando.
- **Português em tudo:** nome de variável, de classe CSS, de teste e de commit.
- Rodar sempre de `web/`: `npm test` e `npm run build`.

---

### Task 1: Testes-guarda que provam os defeitos

Abre o trabalho provando, por máquina, que os defeitos do spec existem. Estes testes ficam vermelhos até as tasks seguintes e depois impedem a volta — são o tipo de defeito que retorna sem ninguém notar.

**Files:**
- Create: `web/src/estilo.test.mjs`

**Interfaces:**
- Consumes: nada.
- Produces: `npm test` passa a cobrir invariantes de estilo. Nenhuma exportação.

- [ ] **Step 1: Escrever o teste que falha**

Crie `web/src/estilo.test.mjs`:

```js
/**
 * Invariantes de estilo que nenhum teste de unidade pegaria.
 *
 *   cd web && npm test
 *
 * Este projeto não tem jsdom nem testing-library, e não vai ganhar: layout se
 * confere olhando. O que dá para provar por máquina é o que apodrece calado —
 * token usado e nunca definido, endpoint vazando para a interface, primitivo da
 * landing usado como linha de dado. Foi exatamente assim que `--ink` viveu em 9
 * lugares sem ninguém ver.
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
  // descartada. `background: var(--ink)` virava transparente, não vermelho.
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
  const fonte = cssDaLanding() + cssDoApp();
  const bloco = (re) => (fonte.match(re) ?? [''])[0];

  const semanticos = (texto) => new Set(
    [...texto.matchAll(/(--(?:fundo|superficie|tinta|tinta-fraca|hair|hair-fraca|foco))\s*:/g)]
      .map((m) => m[1]),
  );

  const escuro = semanticos(bloco(/:root\s*\{[^}]*\}/));
  const claro = semanticos(bloco(/:root:has\(\.app-main\)\s*\{[^}]*\}/));

  // Token definido só no escuro é o bug do --ink de novo, com outro nome: a
  // tela clara herda o valor do preto e o texto some no fundo.
  const faltando = [...escuro].filter((t) => !claro.has(t));
  assert.deepEqual(faltando, [], `sem valor no tema claro: ${faltando}`);
  assert.ok(escuro.size >= 7, 'os sete tokens semânticos precisam existir');
});

test('nenhuma tela imprime o endpoint na interface', () => {
  // O `detalhe` do Cabecalho mostrava "GET /api/v1/consultas" enquanto
  // carregava. Resquício de depuração, visível para o cliente pagante.
  for (const [nome, fonte] of telas()) {
    assert.doesNotMatch(fonte, /GET \/api\//, `${nome} mostra endpoint na tela`);
  }
});

test('as telas do sistema não usam o .row da landing', () => {
  // `.row` é grid de DUAS colunas com padding-block de 42px. As telas passavam
  // TRÊS filhos, e a coluna de ações caía numa linha implícita.
  for (const [nome, fonte] of telas()) {
    assert.doesNotMatch(fonte, /className="row\b|className="[^"]*\brow /,
      `${nome} ainda usa .row`);
  }
});

test('nenhuma tela usa a classe de erro do formulário de pagamento', () => {
  // `.pagamento-erro` é do cartão de crédito. Quatro telas o tomaram emprestado
  // como estilo de erro de campo; a classe certa é `.erro-campo`.
  for (const [nome, fonte] of telas()) {
    assert.doesNotMatch(fonte, /pagamento-erro/, `${nome} usa pagamento-erro`);
  }
});
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd web && npm test
```

Esperado: **FALHA**. `app.css` ainda não existe, então o primeiro teste morre em `ENOENT`. Crie o arquivo vazio para seguir:

```bash
cd web && printf '/* telas do sistema — preenchido na Task 2 */\n' > src/app.css && npm test
```

Agora esperado: cinco falhas reais — tokens órfãos (`--ink`, `--hair-dim`), bloco do tema claro inexistente, `GET /api/` em cinco telas, `.row` em quatro telas, `pagamento-erro` em três telas.

- [ ] **Step 3: Commit**

```bash
cd web && git add src/estilo.test.mjs src/app.css
git commit -m "test(web): invariantes de estilo, vermelhos de propósito

Provam por máquina os defeitos que o spec levantou: token usado e nunca
definido, endpoint vazando para a tela, .row da landing como linha de dado.
Ficam vermelhos até as próximas tasks e depois impedem a volta."
```

---

### Task 2: Tokens semânticos, tema claro e `app.css`

**Files:**
- Modify: `web/src/style.css` (`:root` nas linhas 6-57; recortar a seção "TELAS DO SISTEMA", hoje a partir da linha ~860)
- Modify: `web/src/app.css`
- Modify: `web/src/main.jsx:17` (import)

**Interfaces:**
- Consumes: `estilo.test.mjs` da Task 1.
- Produces: os sete tokens semânticos, disponíveis para todas as tasks seguintes. `app.css` como lar do CSS do sistema.

- [ ] **Step 1: Acrescentar a camada semântica ao `:root`**

Em `web/src/style.css`, logo depois do bloco de cor existente (após `--hair-red`), acrescente:

```css
  /* ── Camada semântica ──────────────────────────────────────────
     Os literais acima são a MARCA e não mudam. Estes são o PAPEL, e
     é só deles que componente pode depender. Enquanto `--bone` foi ao
     mesmo tempo "o branco da DentiBot" e "a cor do texto", claro e
     escuro não tinham como coexistir. */
  --fundo:       var(--obsidian);
  --superficie:  var(--obsidian);
  --tinta:       var(--bone);
  --tinta-fraca: var(--ash);
  --hair-fraca:  rgba(255, 255, 255, .12);
  --foco:        var(--bone);
```

`--hair` já existe na linha 14 e continua valendo como token semântico.

- [ ] **Step 2: Trocar os dois tokens que nunca existiram**

```bash
cd web/src
sed -i 's/var(--ink, #fff)/var(--tinta)/g; s/var(--ink)/var(--tinta)/g' style.css
sed -i 's/var(--hair-dim, rgba(255,255,255,\.12))/var(--hair-fraca)/g; s/var(--hair-dim)/var(--hair-fraca)/g' style.css
grep -n -- "--ink\|--hair-dim" style.css
```

Esperado do `grep`: nada. Se sobrar alguma forma que o `sed` não pegou, troque à mão por `--tinta` ou `--hair-fraca`.

- [ ] **Step 3: Mover o CSS do sistema para `app.css`**

Recorte de `style.css` tudo a partir do comentário `/* ── TELAS DO SISTEMA ─── */` até o fim do arquivo e cole em `app.css`, sob este cabeçalho:

```css
/* ═══════════════════════════════════════════════════════════════
   DENTIBOT — telas do sistema. Papel clínico, não manifesto.
   A landing é preta e grita; a ferramenta é clara e cala.
   ═══════════════════════════════════════════════════════════════ */
```

**Não mova** `.cartao*`, `.pagamento*` nem `.tilt*`: `CartaoCredito.jsx`, `FormularioCartao.jsx` e `TiltCard.jsx` não são montados por ninguém, e esse CSS não pertence ao sistema. Deixe onde está.

- [ ] **Step 4: O bloco do tema claro**

No topo de `app.css`, logo depois do cabeçalho:

```css
/* O tema não precisa de JavaScript. As telas do sistema são as únicas que
   montam `.app-main`, e `:has` é vivo: troca na hora da rota, sem useEffect,
   sem limpeza no desmonte, sem piscada. A landing e o 404 não montam
   `.app-main` e por isso continuam pretos. */
:root:has(.app-main) {
  color-scheme: light;

  --fundo:       #faf9f7;   /* papel, não branco de tela */
  --superficie:  #ffffff;
  --tinta:       #111111;
  /* NÃO é o --ash da marca: #838383 sobre #faf9f7 dá 3,66:1, abaixo do mínimo
     AA de 4,5:1. O cinza que funciona sobre obsidiana reprova sobre papel. */
  --tinta-fraca: #5c5c5c;
  --hair:        rgba(0, 0, 0, .22);
  --hair-fraca:  rgba(0, 0, 0, .10);
  --foco:        #111111;
}

:root:has(.app-main) body { background: var(--fundo); color: var(--tinta); }
```

- [ ] **Step 5: Trocar literal por semântico dentro de `app.css`**

Toda regra movida que usa `var(--bone)`, `var(--obsidian)` ou `var(--ash)` passa a usar `var(--tinta)`, `var(--fundo)` e `var(--tinta-fraca)`. Confira principalmente `.app-topo` (`background: var(--obsidian)` → `var(--fundo)`), `.abas [aria-selected="true"]`, `.odonto-dente[aria-pressed="true"]` e `.tabela th`.

`var(--alarm)` fica: alarme é alarme nos dois temas.

- [ ] **Step 6: Importar no `main.jsx`**

Em `web/src/main.jsx:17`, depois de `import './style.css';`:

```js
import './app.css';
```

- [ ] **Step 7: Rodar os testes**

```bash
cd web && npm test && npm run build
```

Esperado: os dois primeiros testes de `estilo.test.mjs` **passam**. Os três últimos (`GET /api/`, `.row`, `pagamento-erro`) continuam vermelhos — são das Tasks 6 a 12.

- [ ] **Step 8: Conferir na tela**

```bash
cd web && npm run dev
```

`/` preta. `/agenda` clara. Alternar entre as duas não pisca.

- [ ] **Step 9: Commit**

```bash
cd web && git add src/style.css src/app.css src/main.jsx
git commit -m "feat(web): camada semântica de token e tema claro no app

--bone era ao mesmo tempo o branco da marca e a cor do texto; enquanto foi a
mesma variável, claro e escuro não coexistiam. Os literais da marca ficam, os
papéis entram por cima, e o app clareia por :root:has(.app-main) — sem
useEffect e sem o bug de limpeza que ele traria.

--ink e --hair-dim, que nunca existiram em 9 usos, viram --tinta e
--hair-fraca. O CSS do sistema sai do arquivo do manifesto."
```

---

### Task 3: `apresentacao.js` — a lógica que dá para testar

**Files:**
- Create: `web/src/apresentacao.js`
- Create: `web/src/apresentacao.test.mjs`

**Interfaces:**
- Consumes: nada.
- Produces, e as tasks 6-12 dependem destes nomes exatos:
  - `STATUS_CONSULTA`, `STATUS_RECEBIVEL`, `STATUS_MEMBRO`: objetos `{ [valor]: { rotulo: string, tom: string } }`
  - `descrever(mapa, valor) → { rotulo, tom }`
  - `faixaHoraria(inicioEm, terminoEm) → string`
  - `agruparPorHora(consultas) → Array<{ hora: string, consultas: Array }>`
  - `telHref(telefone) → string | null`
  - `somarDias(dia, n) → string` (`'AAAA-MM-DD'`)
  - `hoje() → string`
  - `porExtenso(dia) → string`
  - `contagem(n, limite, singular, plural) → string`

- [ ] **Step 1: Escrever o teste que falha**

Crie `web/src/apresentacao.test.mjs`:

```js
/**
 * A parte das telas que dá para provar sem navegador.
 *
 *   cd web && npm test
 *
 * Layout se confere olhando, mas "14:00–14:45", "200 primeiros" e o tom do selo
 * são decisões com resposta certa — e todas estavam espalhadas dentro do JSX,
 * onde não havia como testá-las.
 */
import assert from 'node:assert/strict';
import test from 'node:test';
import {
  STATUS_CONSULTA, agruparPorHora, contagem, descrever, faixaHoraria,
  porExtenso, somarDias, telHref,
} from './apresentacao.js';

test('status conhecido vira rótulo em português e tom de selo', () => {
  assert.deepEqual(descrever(STATUS_CONSULTA, 'em_atendimento'),
    { rotulo: 'Em atendimento', tom: 'alarme' });
  assert.equal(descrever(STATUS_CONSULTA, 'cancelada').tom, 'riscado');
});

test('status desconhecido aparece como veio, em vez de sumir', () => {
  // O CHECK do banco pode ganhar um valor antes desta tela. Mostrar o valor cru
  // é feio; esconder a linha é perder informação clínica.
  assert.deepEqual(descrever(STATUS_CONSULTA, 'remarcada'),
    { rotulo: 'remarcada', tom: 'fraco' });
  assert.equal(descrever(STATUS_CONSULTA, null).rotulo, '—');
});

test('a faixa mostra início e término', () => {
  assert.equal(
    faixaHoraria('2026-09-13T17:00:00Z', '2026-09-13T17:45:00Z').replace(/\s/g, ''),
    '14:00–14:45',
  );
});

test('sem término a faixa não inventa um', () => {
  // `new Date(null)` é 01/01/1970, não erro — numa agenda isso passa por hora
  // de verdade.
  assert.equal(faixaHoraria('2026-09-13T17:00:00Z', null).replace(/\s/g, ''), '14:00');
});

test('o dia é agrupado por hora cheia, em ordem', () => {
  const consultas = [
    { idConsulta: 2, inicioEm: '2026-09-13T18:30:00Z' },
    { idConsulta: 1, inicioEm: '2026-09-13T17:00:00Z' },
    { idConsulta: 3, inicioEm: '2026-09-13T17:00:00Z' },
  ];
  const calha = agruparPorHora(consultas);

  assert.deepEqual(calha.map((d) => d.hora), ['14', '15']);
  // Duas cadeiras atendendo às 14h é o caso normal de uma clínica, não anomalia.
  assert.deepEqual(calha[0].consultas.map((c) => c.idConsulta), [1, 3]);
});

test('telefone vira link discável, vazio não vira nada', () => {
  assert.equal(telHref('(11) 99999-8888'), 'tel:+5511999998888');
  assert.equal(telHref('+55 11 99999-8888'), 'tel:+5511999998888');
  assert.equal(telHref(''), null);
  assert.equal(telHref(null), null);
});

test('somar dias atravessa mês e ano sem fuso no caminho', () => {
  assert.equal(somarDias('2026-09-30', 1), '2026-10-01');
  assert.equal(somarDias('2026-01-01', -1), '2025-12-31');
  assert.equal(somarDias('2026-03-01', -1), '2026-02-28');
});

test('a contagem não mente quando bateu o limite', () => {
  // A tela pedia limite=200, recebia 200 e escrevia "200 no cadastro". Numa
  // clínica com 900 pacientes a frase é falsa.
  assert.equal(contagem(200, 200, 'paciente', 'pacientes'), '200 primeiros');
  assert.equal(contagem(7, 200, 'paciente', 'pacientes'), '7 pacientes');
  assert.equal(contagem(1, 200, 'paciente', 'pacientes'), '1 paciente');
  assert.equal(contagem(0, 200, 'paciente', 'pacientes'), 'nenhum paciente');
});

test('o zero concorda em gênero com o substantivo', () => {
  // "nenhum consulta" é o tipo de erro que só aparece em produção, no dia em
  // que a agenda amanhece vazia.
  assert.equal(contagem(0, Infinity, 'consulta', 'consultas'), 'nenhuma consulta');
  assert.equal(contagem(0, 200, 'parcela', 'parcelas'), 'nenhuma parcela');
});

test('o dia por extenso é o dia civil, não o de UTC', () => {
  assert.match(porExtenso('2026-09-13'), /domingo/i);
  assert.match(porExtenso('2026-09-13'), /13 de setembro/i);
});
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd web && npm test
```

Esperado: **FALHA** — `Cannot find module './apresentacao.js'`.

- [ ] **Step 3: Implementar**

Crie `web/src/apresentacao.js`:

```js
/**
 * O que as telas mostram, separado de como elas montam.
 *
 * <p>Existe porque cada tela tinha o próprio `Intl`, o próprio mapa de status e
 * a própria frase de contagem — e nenhum deles tinha teste, porque estavam
 * dentro do JSX. Aqui em cima é função pura, e `node --test` alcança.
 */

const HORA = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
const EXTENSO = new Intl.DateTimeFormat('pt-BR', {
  weekday: 'long', day: 'numeric', month: 'long',
});

/* Status → rótulo e tom do selo. O tom é forma, não cor: `riscado` e `contorno`
   continuam legíveis para quem não distingue vermelho de cinza. */
export const STATUS_CONSULTA = {
  agendada: { rotulo: 'Agendada', tom: 'contorno' },
  confirmada: { rotulo: 'Confirmada', tom: 'cheio' },
  em_atendimento: { rotulo: 'Em atendimento', tom: 'alarme' },
  realizada: { rotulo: 'Realizada', tom: 'fraco' },
  cancelada: { rotulo: 'Cancelada', tom: 'riscado' },
  faltou: { rotulo: 'Faltou', tom: 'alarme' },
};

export const STATUS_RECEBIVEL = {
  aberto: { rotulo: 'Em aberto', tom: 'contorno' },
  pago: { rotulo: 'Pago', tom: 'fraco' },
  vencido: { rotulo: 'Vencido', tom: 'alarme' },
  cancelado: { rotulo: 'Cancelado', tom: 'riscado' },
};

export const STATUS_MEMBRO = {
  ativo: { rotulo: 'Ativo', tom: 'cheio' },
  bloqueado: { rotulo: 'Bloqueado', tom: 'alarme' },
  desativado: { rotulo: 'Desativado', tom: 'riscado' },
};

/**
 * Traduz um valor de status, sem inventar.
 *
 * <p>Valor que o mapa não conhece aparece como veio. O CHECK do banco pode
 * ganhar um estado antes desta tela, e esconder a linha seria perder
 * informação; mostrar o valor cru é só feio.
 */
export function descrever(mapa, valor) {
  if (valor == null || valor === '') return { rotulo: '—', tom: 'fraco' };
  return mapa[valor] ?? { rotulo: String(valor), tom: 'fraco' };
}

/** `14:00 – 14:45`, ou só o início quando não há término utilizável. */
export function faixaHoraria(inicioEm, terminoEm) {
  const inicio = HORA.format(new Date(inicioEm));
  /* `new Date(null)` é 01/01/1970 e não erro: sem esta guarda, consulta sem
     término mostraria "14:00 – 21:00" com cara de dado verdadeiro. */
  if (!terminoEm) return inicio;
  const fim = new Date(terminoEm);
  if (Number.isNaN(fim.getTime())) return inicio;
  return `${inicio} – ${HORA.format(fim)}`;
}

/**
 * Agrupa o dia por hora cheia, em ordem.
 *
 * <p>A ordem é imposta aqui e não confiada à resposta: a agenda é o eixo do
 * tempo, e uma consulta fora de lugar na calha é pior que uma lista sem calha.
 */
export function agruparPorHora(consultas) {
  const porHora = new Map();

  for (const c of [...consultas].sort((a, b) =>
    new Date(a.inicioEm) - new Date(b.inicioEm))) {
    const hora = String(new Date(c.inicioEm).getHours()).padStart(2, '0');
    if (!porHora.has(hora)) porHora.set(hora, []);
    porHora.get(hora).push(c);
  }

  return [...porHora].map(([hora, lista]) => ({ hora, consultas: lista }));
}

/**
 * Telefone → `tel:`, no formato E.164.
 *
 * <p>Assume Brasil quando não vem código de país, porque o cadastro é de uma
 * clínica brasileira e o campo é `telefoneCelular`. Número curto demais para
 * ser discável devolve null — link que não liga é pior que texto.
 */
export function telHref(telefone) {
  const digitos = String(telefone ?? '').replace(/\D/g, '');
  if (digitos.length < 10) return null;
  return `tel:+${digitos.startsWith('55') ? digitos : `55${digitos}`}`;
}

/** Hoje em 'AAAA-MM-DD'. sv-SE é o atalho de ISO que o toLocaleDateString dá. */
export const hoje = () => new Date().toLocaleDateString('sv-SE');

/**
 * Soma dias a um 'AAAA-MM-DD' sem passar por fuso.
 *
 * <p>`new Date('2026-09-30')` é meia-noite UTC; somar um dia e formatar em São
 * Paulo devolveria o próprio 30. O construtor de três argumentos é local, e
 * ele normaliza mês e ano sozinho.
 */
export function somarDias(dia, n) {
  const [ano, mes, d] = dia.split('-').map(Number);
  return new Date(ano, mes - 1, d + n).toLocaleDateString('sv-SE');
}

/** 'domingo, 13 de setembro'. */
export function porExtenso(dia) {
  const [ano, mes, d] = dia.split('-').map(Number);
  return EXTENSO.format(new Date(ano, mes - 1, d));
}

/**
 * A frase de contagem, honesta quando bateu o teto.
 *
 * <p>A tela pedia 200 — que é o `LIMITE_MAXIMO` do controller — recebia 200 e
 * escrevia "200 no cadastro". Se vieram exatamente `limite`, há provavelmente
 * mais, e a única frase verdadeira é "os primeiros".
 */
export function contagem(n, limite, singular, plural) {
  if (n === 0) {
    /* ponytail: gênero pela terminação. Acerta consulta, parcela e paciente,
       que é o que esta tela usa; um substantivo feminino terminado em -ão
       ("evolução") sairia errado. Vira tabela se um terceiro caso aparecer. */
    return `nenhum${singular.endsWith('a') ? 'a' : ''} ${singular}`;
  }
  if (n >= limite) return `${n} primeiros`;
  return `${n} ${n === 1 ? singular : plural}`;
}
```

- [ ] **Step 4: Rodar e confirmar que passa**

```bash
cd web && npm test
```

Esperado: os nove testes de `apresentacao.test.mjs` **passam**.

- [ ] **Step 5: Commit**

```bash
cd web && git add src/apresentacao.js src/apresentacao.test.mjs
git commit -m "feat(web): apresentacao.js, a parte das telas que dá para testar

Cada tela tinha o próprio Intl, o próprio mapa de status e a própria frase de
contagem, todos dentro do JSX, onde node --test não alcança. Sobe para função
pura e ganha os casos que ninguém conferia: consulta sem término, status que o
mapa não conhece, virada de mês, e a contagem que mentia ao bater o limite."
```

---

### Task 4: Primitivos — selo, esqueleto, aviso e tabela

**Files:**
- Create: `web/src/components/primitivos.jsx`
- Modify: `web/src/app.css`

**Interfaces:**
- Consumes: `descrever` de `apresentacao.js`.
- Produces, usados pelas Tasks 6-12:
  - `<Selo mapa={STATUS_X} valor={s} />`
  - `<Esqueleto linhas={n} colunas={n} />`
  - `<Aviso texto={string|null} tom="sucesso|erro" />`
  - `<Tabela colunas={[{chave, rotulo, num?}]} children />` e `<Celula rotulo="…">`

- [ ] **Step 1: Escrever os componentes**

Crie `web/src/components/primitivos.jsx`:

```jsx
/**
 * O vocabulário visual das telas do sistema.
 *
 * <p>Um arquivo e não cinco: são quatro peças pequenas com a mesma
 * responsabilidade — dizer estado sem fazer nada. Quem fizer alguma coisa
 * (o <dialog>) mora separado.
 */
import { descrever } from '../apresentacao.js';

/**
 * Status com forma, nunca só cor.
 *
 * <p>O tom vira `data-tom` e o CSS decide: contorno, preenchido, riscado. Cor
 * sozinha não existe para quem não a distingue — e status é o campo mais
 * escaneado de toda lista desta ferramenta.
 */
export function Selo({ mapa, valor }) {
  const { rotulo, tom } = descrever(mapa, valor);
  return <span className="selo" data-tom={tom}>{rotulo}</span>;
}

/**
 * A forma do que vai chegar, enquanto não chegou.
 *
 * <p>"Carregando…" como parágrafo solto faz a página saltar quando os dados
 * chegam, e o salto é o que faz alguém clicar no lugar errado.
 */
export function Esqueleto({ linhas = 5, colunas = 3 }) {
  return (
    <div className="esqueleto" aria-hidden="true">
      {Array.from({ length: linhas }, (_, l) => (
        <div className="esqueleto-linha" key={l}>
          {Array.from({ length: colunas }, (_, c) => (
            <span className="esqueleto-celula" key={c} />
          ))}
        </div>
      ))}
    </div>
  );
}

/**
 * O que acabou de acontecer.
 *
 * <p>Nenhuma ação bem-sucedida avisava que deu certo: `useAcao` só expunha
 * erro, e confirmar uma consulta apenas recarregava a lista. Quem clicou não
 * sabia se clicou.
 *
 * <p>`role="status"` e não `alert` para o sucesso: o leitor de tela anuncia
 * quando terminar a frase atual, em vez de interromper.
 */
export function Aviso({ texto, tom = 'sucesso' }) {
  if (!texto) return null;
  return (
    <p className="aviso" data-tom={tom} role={tom === 'erro' ? 'alert' : 'status'}>
      {texto}
    </p>
  );
}

/**
 * A tabela do sistema.
 *
 * <p>Substitui o `.row` da landing, que é grid de duas colunas com 42px de
 * padding e recebia três filhos — a coluna de ações caía numa linha implícita.
 *
 * <p>Abaixo de 620px cada linha vira bloco empilhado, e é `<Celula>` quem
 * carrega o rótulo que aparece ali: rolagem horizontal numa tabela de trabalho
 * esconde justamente a coluna de ação.
 */
export function Tabela({ colunas, children }) {
  return (
    <div className="tabela-rolagem">
      <table className="tabela">
        <thead>
          <tr>
            {colunas.map((c) => (
              <th key={c.chave} scope="col" className={c.num ? 'num' : undefined}>
                {c.rotulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}

/** Uma célula que sabe o próprio nome, para quando a tabela empilha. */
export function Celula({ rotulo, num, children, ...resto }) {
  return (
    <td data-rotulo={rotulo} className={num ? 'num' : undefined} {...resto}>
      {children}
    </td>
  );
}
```

- [ ] **Step 2: O CSS dos primitivos**

Acrescente ao fim de `web/src/app.css`:

```css
/* ── SELO ─────────────────────────────────────────────────────────────────── */
.selo {
  display: inline-block;
  padding: 3px 8px;
  font-size: 11px;
  letter-spacing: .08em;
  text-transform: uppercase;
  border: 1px solid var(--hair);
  white-space: nowrap;
}
.selo[data-tom="cheio"]   { background: var(--tinta); color: var(--fundo); border-color: var(--tinta); }
.selo[data-tom="fraco"]   { color: var(--tinta-fraca); }
.selo[data-tom="alarme"]  { color: var(--alarm); border-color: var(--alarm); border-left-width: 3px; }
.selo[data-tom="riscado"] { color: var(--tinta-fraca); text-decoration: line-through; }

/* ── ESQUELETO ────────────────────────────────────────────────────────────── */
.esqueleto-linha {
  display: flex;
  gap: 40px;
  padding-block: 14px;
  border-bottom: 1px solid var(--hair-fraca);
}
.esqueleto-celula {
  height: 12px;
  flex: 1;
  background: var(--hair-fraca);
}
.esqueleto-celula:first-child { flex: 2; }
@media (prefers-reduced-motion: no-preference) {
  .esqueleto-celula { animation: esqueleto-pulsa 1.4s ease-in-out infinite; }
  @keyframes esqueleto-pulsa { 50% { opacity: .45; } }
}

/* ── AVISO ────────────────────────────────────────────────────────────────── */
.aviso {
  margin: 0 0 var(--spacing-20, 20px);
  padding: 12px 16px;
  border: 1px solid var(--hair);
  border-left-width: 4px;
  font-size: 14px;
}
.aviso[data-tom="sucesso"] { border-left-color: var(--tinta); }
.aviso[data-tom="erro"]    { border-left-color: var(--alarm); color: var(--alarm); }

/* Erro de campo. Antes era `.pagamento-erro`, emprestado do formulário de
   cartão de crédito por quatro telas que não têm nada com pagamento. */
.erro-campo { margin: 0; font-size: 12px; color: var(--alarm); }

/* ── TABELA EMPILHADA ─────────────────────────────────────────────────────── */
/* Rolagem horizontal numa tabela de trabalho esconde a coluna de ação, que é a
   da direita. No celular a linha vira bloco e cada célula diz o próprio nome. */
@media (max-width: 620px) {
  .tabela, .tabela tbody, .tabela tr, .tabela td { display: block; width: 100%; }
  .tabela thead { display: none; }
  .tabela tr {
    padding-block: 14px;
    border-bottom: 1px solid var(--hair);
  }
  .tabela td { border: 0; padding: 3px 0; }
  .tabela td[data-rotulo]::before {
    content: attr(data-rotulo) " ";
    display: inline-block;
    min-width: 9ch;
    font-size: 11px;
    letter-spacing: .08em;
    text-transform: uppercase;
    color: var(--tinta-fraca);
  }
}

/* ── TABELA, CABEÇALHO FIXO ───────────────────────────────────────────────── */
@media (min-width: 621px) {
  .tabela thead th {
    position: sticky;
    top: 56px;            /* abaixo do .app-topo, que também é sticky */
    background: var(--fundo);
    z-index: 1;
  }
}
```

- [ ] **Step 3: Verificar**

```bash
cd web && npm test && npm run build
```

Esperado: continua no mesmo lugar (três testes de `estilo.test.mjs` ainda vermelhos, o resto verde). Nada monta os primitivos ainda.

- [ ] **Step 4: Commit**

```bash
cd web && git add src/components/primitivos.jsx src/app.css
git commit -m "feat(web): selo, esqueleto, aviso e tabela do sistema

Status era texto cinza sendo o campo mais escaneado de toda lista; agora é
selo com forma, não só cor. A tabela substitui o .row da landing e, no
celular, empilha em vez de rolar na horizontal — rolagem esconde a coluna de
ação, que é a da direita."
```

---

### Task 5: `Confirmar` — o `<dialog>` nativo

**Files:**
- Create: `web/src/components/Confirmar.jsx`
- Modify: `web/src/app.css`

**Interfaces:**
- Consumes: nada.
- Produces: `<Confirmar aberto={bool} titulo={string} corpo={node} rotuloConfirmar={string} perigo={bool} onConfirmar={fn} onCancelar={fn} />`, usado nas Tasks 6 e 11.

- [ ] **Step 1: Escrever o componente**

Crie `web/src/components/Confirmar.jsx`:

```jsx
/**
 * Confirmação de ato irreversível.
 *
 * <p>Sobre o `<dialog>` nativo, e não sobre uma div com `role="dialog"`: foco
 * preso, `Esc`, fundo inerte e devolução do foco ao elemento anterior vêm da
 * plataforma. A versão em div precisaria de todas as quatro coisas escritas à
 * mão, e é sempre a devolução do foco que fica faltando.
 *
 * <p>`showModal()` e não o atributo `open`: só a chamada dá o modo modal — com
 * `open` o resto da página continua alcançável por Tab.
 */
import { useEffect, useRef } from 'react';

export default function Confirmar({
  aberto, titulo, corpo, rotuloConfirmar = 'Confirmar', perigo = false,
  onConfirmar, onCancelar,
}) {
  const ref = useRef(null);

  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (aberto && !dialogo.open) dialogo.showModal();
    if (!aberto && dialogo.open) dialogo.close();
  }, [aberto]);

  return (
    <dialog
      className="dialogo"
      ref={ref}
      /* Esc e clique no fundo disparam `cancel`/`close` sem passar pelos
         botões. Sem isto o estado do React continuaria "aberto" com o diálogo
         fechado, e o segundo clique não abriria nada. */
      onCancel={onCancelar}
      onClose={onCancelar}
    >
      <h2 className="sub">{titulo}</h2>
      {corpo && <div className="body">{corpo}</div>}
      <div className="dialogo-acoes">
        <button type="button" className="btn" onClick={onCancelar}>Cancelar</button>
        <button
          type="button"
          className={`btn ${perigo ? 'btn-perigo' : 'btn-fill'}`}
          onClick={onConfirmar}
          /* O foco começa no cancelar, que é o botão seguro: `autofocus` no
             destrutivo transforma Enter distraído em ato irreversível. */
        >
          {rotuloConfirmar}
        </button>
      </div>
    </dialog>
  );
}
```

- [ ] **Step 2: O CSS**

Acrescente ao fim de `web/src/app.css`:

```css
/* ── DIÁLOGO ──────────────────────────────────────────────────────────────── */
.dialogo {
  border: 1px solid var(--tinta);
  background: var(--superficie);
  color: var(--tinta);
  padding: var(--spacing-30, 30px);
  max-width: min(46ch, calc(100vw - 32px));
}
.dialogo::backdrop { background: rgba(0, 0, 0, .55); }
.dialogo .sub { margin-top: 0; }
.dialogo-acoes {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: var(--spacing-20, 20px);
}

.btn-perigo { border-color: var(--alarm); color: var(--alarm); }
.btn-perigo::before { background: var(--alarm); }
.btn-perigo:hover, .btn-perigo:focus-visible { color: var(--superficie); }

/* ── BOTÃO NO APP ─────────────────────────────────────────────────────────── */
/* O wipe do `.btn::before` fica na landing. Numa barra de navegação e numa
   coluna de ações são oito varreduras simultâneas, e isso é ruído numa
   ferramenta que a pessoa usa oito horas por dia. */
:root:has(.app-main) .btn::before { transition: none; }
.btn-sm { padding: 6px 12px; font-size: 12px; }
```

- [ ] **Step 3: Verificar**

```bash
cd web && npm run build
```

Esperado: build passa.

- [ ] **Step 4: Commit**

```bash
cd web && git add src/components/Confirmar.jsx src/app.css
git commit -m "feat(web): confirmação de ato irreversível sobre o <dialog> nativo

Foco preso, Esc, fundo inerte e devolução do foco vêm da plataforma; a versão
em div precisaria das quatro escritas à mão e é sempre a devolução do foco que
fica faltando. O foco começa no cancelar: autofocus no destrutivo transforma
Enter distraído em ato irreversível."
```

---

### Task 6: `useAcao` avisa, e a casca vira ferramenta

**Files:**
- Modify: `web/src/dados.js:38-58` (`useAcao`)
- Modify: `web/src/paginas/Layout.jsx` (`Autenticado`, `Estado`, `Cabecalho`, `SemAcesso`)
- Modify: `web/src/main.jsx:31-46` (`aparencia` do Clerk)
- Modify: `web/src/app.css`

**Interfaces:**
- Consumes: `Esqueleto`, `Aviso` de `primitivos.jsx`.
- Produces:
  - `useAcao(aoConcluir)` passa a devolver `{ executar, enviando, erro, sucesso, limparSucesso }`; `executar(promessa, mensagemDeSucesso)`.
  - `<Estado status erro vazio esqueleto={{linhas, colunas}} onTentarDeNovo>`.

- [ ] **Step 1: `useAcao` guarda o sucesso**

Em `web/src/dados.js`, substitua o corpo de `useAcao`:

```js
/** Dispara uma escrita e devolve o estado dela, sem segurar a tela inteira. */
export function useAcao(aoConcluir) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  const executar = useCallback(async (promessa, mensagem) => {
    setEnviando(true);
    setErro(null);
    setSucesso(null);
    try {
      const resultado = await promessa;
      aoConcluir?.(resultado);
      /* Sucesso mudo era o buraco: a lista recarregava e quem clicou não sabia
         se clicou. Quem não passar mensagem continua sem aviso — nem toda
         escrita precisa de uma, e "Salvo." em cima de um formulário que já
         fechou é ruído. */
      if (mensagem) setSucesso(mensagem);
      return resultado;
    } catch (e) {
      setErro(e);
      return null;
    } finally {
      setEnviando(false);
    }
  }, [aoConcluir]);

  return { executar, enviando, erro, sucesso, limparSucesso: () => setSucesso(null) };
}
```

- [ ] **Step 2: `Estado` ganha esqueleto**

Em `web/src/paginas/Layout.jsx`, no componente `Estado`, troque o ramo de carregamento:

```jsx
  if (status === 'carregando') {
    return esqueleto
      ? <Esqueleto linhas={esqueleto.linhas} colunas={esqueleto.colunas} />
      : <p className="body body-ash" aria-live="polite">Carregando…</p>;
  }
```

e a assinatura para `export function Estado({ status, erro, vazio, esqueleto, children, onTentarDeNovo })`. Importe `Esqueleto` de `../components/primitivos.jsx`.

- [ ] **Step 3: O `correlacaoId` ganha botão de copiar**

No ramo de erro do mesmo `Estado`, substitua o parágrafo do `correlacaoId`:

```jsx
        {erro?.data?.correlacaoId && (
          <p className="cap cap-ash">
            Referência: <code>{erro.data.correlacaoId}</code>{' '}
            {/* O suporte pede este número por telefone. Ler 36 caracteres de
                UUID em voz alta é onde a pessoa desiste e desliga. */}
            <button
              type="button"
              className="btn btn-sm"
              onClick={() => navigator.clipboard?.writeText(erro.data.correlacaoId)}
            >
              Copiar
            </button>
          </p>
        )}
```

- [ ] **Step 4: A navegação vira barra de abas**

Em `Autenticado`, troque a `className` do `NavLink`:

```jsx
              className={({ isActive }) => `app-aba${isActive ? ' app-aba-ativa' : ''}`}
```

E acrescente o papel ao lado do `UserButton`, com a aparência clara:

```jsx
        <p className="cap cap-ash app-papel">{eu.dados.papel?.toLowerCase()}</p>
        {/* O provider inteiro está com aparência preta, para a landing e o
            login. Dentro do app o cabeçalho é claro, e o popover do Clerk
            entraria preto sobre papel. */}
        <UserButton appearance={{ variables: APARENCIA_CLARA }} />
```

No topo do arquivo:

```js
/* Espelha os tokens de `:root:has(.app-main)` em app.css. O Clerk não lê
   variável CSS nossa; se a paleta do tema claro mudar lá, muda aqui também. */
const APARENCIA_CLARA = {
  colorBackground: '#ffffff',
  colorForeground: '#111111',
  colorMuted: '#faf9f7',
  colorMutedForeground: '#5c5c5c',
  colorPrimary: '#111111',
  colorPrimaryForeground: '#ffffff',
  colorInput: '#ffffff',
  colorInputForeground: '#111111',
  colorBorder: 'rgba(0, 0, 0, .22)',
  colorDanger: '#ed1c24',
  borderRadius: '0px',
};
```

- [ ] **Step 5: `SemAcesso` vira estado vazio de verdade**

```jsx
function SemAcesso({ papel }) {
  return (
    <div className="vazio" role="alert">
      <h1 className="display display-sm">Sem acesso.</h1>
      <p className="body body-ash">
        O papel {papel ? <b>{papel.toLowerCase()}</b> : 'atual'} não alcança esta tela.
        Quem administra a clínica pode mudar isso em Equipe.
      </p>
    </div>
  );
}
```

- [ ] **Step 6: O CSS da casca**

Acrescente ao fim de `web/src/app.css`:

```css
/* ── NAVEGAÇÃO ────────────────────────────────────────────────────────────── */
/* Deixa de ser fileira de blocos preenchidos. Um bloco invertido para o item
   ativo grita tanto quanto o botão de ação primária da tela, e eles competem. */
.app-aba {
  padding: 8px 2px;
  margin-right: 18px;
  border-bottom: 2px solid transparent;
  color: var(--tinta-fraca);
  font-size: 14px;
  text-decoration: none;
}
.app-aba:hover { color: var(--tinta); }
.app-aba-ativa { color: var(--tinta); border-bottom-color: var(--alarm); font-weight: 700; }
.app-aba:focus-visible { outline: 2px solid var(--foco); outline-offset: 2px; }

.app-papel { text-transform: capitalize; }

.vazio {
  padding-block: var(--spacing-50, 50px);
  border-top: 1px solid var(--hair);
}

/* Anel de foco em tudo que recebe foco, no tema certo. A regra existente
   apontava para `var(--ink, #fff)`: sobre papel, branco sobre branco. */
:root:has(.app-main) :focus-visible { outline: 2px solid var(--foco); outline-offset: 2px; }

/* Título de tela: 51px era altura de manifesto. Numa ferramenta o título é
   orientação, não declaração. */
.display-sm { font-size: clamp(1.6rem, 3vw, 1.9rem); }
:root:has(.app-main) { font-size: 15px; }
:root:has(.app-main) .body { line-height: 1.45; }
```

- [ ] **Step 7: Verificar**

```bash
cd web && npm test && npm run build && npm run dev
```

Confira: a barra de navegação é sublinhada, não blocos; o papel aparece ao lado do avatar; o popover do `UserButton` abre **claro**; forçar um erro (parar o back-end e recarregar `/agenda`) mostra o botão Copiar.

- [ ] **Step 8: Commit**

```bash
cd web && git add src/dados.js src/paginas/Layout.jsx src/main.jsx src/app.css
git commit -m "feat(web): a casca do app vira ferramenta

useAcao passa a guardar sucesso: confirmar uma consulta só recarregava a
lista, e quem clicou não sabia se clicou. Estado ganha esqueleto, para a
página não saltar. O correlacaoId ganha botão de copiar — o suporte pede 36
caracteres de UUID por telefone. E o UserButton ganha aparência clara: o
provider está preto para a landing, e o popover entraria preto sobre papel."
```

---

### Task 7: Agenda — o eixo do tempo

**Files:**
- Modify: `web/src/paginas/Agenda.jsx` (arquivo inteiro)
- Modify: `web/src/app.css`

**Interfaces:**
- Consumes: `STATUS_CONSULTA`, `agruparPorHora`, `faixaHoraria`, `telHref`, `somarDias`, `hoje`, `porExtenso`, `contagem` de `apresentacao.js`; `Selo`, `Aviso` de `primitivos.jsx`; `Confirmar`.
- Produces: nada.

- [ ] **Step 1: Reescrever a tela**

Apague de `Agenda.jsx` os helpers locais `hoje`, `HORA` e `STATUS` (agora em `apresentacao.js`) e mantenha `inicioDoDia`/`fimDoDia` — são da consulta, não da apresentação.

Mudanças obrigatórias:

1. `detalhe` do `Cabecalho`, sem endpoint nenhum:

```jsx
        detalhe={consultas.status === 'ok'
          ? `${porExtenso(dia)} · ${contagem(lista.length, Infinity, 'consulta', 'consultas')}`
          : porExtenso(dia)}
```

2. Os filtros saem do slot `acao` e viram barra própria, com os atalhos de dia:

```jsx
      <div className="filtros filtros-linha">
        <div className="acoes">
          <button type="button" className="btn btn-sm"
                  onClick={() => setDia((d) => somarDias(d, -1))}>← Ontem</button>
          <button type="button" className="btn btn-sm"
                  onClick={() => setDia(hoje())}>Hoje</button>
          <button type="button" className="btn btn-sm"
                  onClick={() => setDia((d) => somarDias(d, 1))}>Amanhã →</button>
        </div>
        <label className="campo-app">
          <span className="cap cap-ash">Dia</span>
          <input type="date" value={dia} onChange={(e) => setDia(e.target.value || hoje())} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Dentista</span>
          <select value={idDentista} onChange={(e) => setIdDentista(e.target.value)}>
            <option value="">Todos</option>
            {(dentistas.dados ?? []).map((d) => (
              <option key={d.idDentista} value={d.idDentista}>{d.nomeCompleto}</option>
            ))}
          </select>
        </label>
      </div>
```

3. A lista vira calha:

```jsx
        <div className="dia">
          {agruparPorHora(lista).map(({ hora, consultas: doHorario }) => (
            <div className="dia-degrau" key={hora}>
              <p className="dia-hora">{hora}h</p>
              <div className="dia-blocos">
                {doHorario.map((c) => (
                  <article className="consulta" key={c.idConsulta}>
                    <p className="consulta-faixa">{faixaHoraria(c.inicioEm, c.terminoEm)}</p>
                    <p className="sub">{c.nomePaciente ?? `Paciente ${c.idPaciente}`}</p>
                    <p className="body body-ash">{nomeDoDentista(c.idDentista)}</p>
                    {/* Já vinha no ConsultaResumo e era descartado. É o dado
                        que a recepção mais usa: ela liga para o paciente. */}
                    {telHref(c.telefonePaciente) && (
                      <a className="consulta-tel" href={telHref(c.telefonePaciente)}>
                        {c.telefonePaciente}
                      </a>
                    )}
                    <Selo mapa={STATUS_CONSULTA} valor={c.status} />
                    <div className="acoes">
                      {/* Só as transições que o estado atual permite. Mostrar
                          um botão que o back-end recusa com 409 é ensinar o
                          usuário a ignorar mensagem de erro. */}
                      {c.status === 'agendada' && (
                        <Transicao id={c.idConsulta} acao="confirmar" rotulo="Confirmar"
                                   executar={executar} enviando={enviando} />
                      )}
                      {(c.status === 'agendada' || c.status === 'confirmada') && (
                        <>
                          <Transicao id={c.idConsulta} acao="concluir" rotulo="Concluir"
                                     confirmacao="Concluir esta consulta?"
                                     executar={executar} enviando={enviando} />
                          <Transicao id={c.idConsulta} acao="falta" rotulo="Faltou"
                                     confirmacao="Registrar falta do paciente?"
                                     executar={executar} enviando={enviando} />
                        </>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </div>
          ))}
        </div>
```

4. `Estado` ganha `esqueleto={{ linhas: 6, colunas: 3 }}` e o vazio passa a saber do filtro:

```jsx
        vazio={lista.length === 0
          ? (idDentista
            ? 'Nenhuma consulta deste dentista neste dia. O filtro de dentista está ativo.'
            : 'Nenhuma consulta neste dia.')
          : null}
```

5. `Transicao` passa a confirmar o irreversível. Substitua o componente inteiro:

```jsx
/**
 * Um POST de transição.
 *
 * <p>A `Idempotency-Key` é gerada UMA VEZ por montagem do botão, não por
 * clique: dois toques em "Confirmar" são a mesma requisição lógica, e uma chave
 * nova no segundo clique faria o filtro do back-end tratá-lo como operação
 * distinta — que é exatamente o que ele existe para impedir.
 *
 * <p>`confirmacao` marca o que não tem volta: "Faltou" e "Concluir" mudam o
 * estado para um lugar de onde o back-end responde 409, e os dois ficavam a um
 * clique, encostados no botão que não faz mal nenhum.
 */
function Transicao({ id, acao, rotulo, confirmacao, executar, enviando }) {
  const [chave] = useState(() => crypto.randomUUID());
  const [perguntando, setPerguntando] = useState(false);

  const disparar = () => {
    setPerguntando(false);
    executar(
      api.post(`/consultas/${id}/${acao}`, {}, { idempotencyKey: chave }),
      `${rotulo}: consulta atualizada.`,
    );
  };

  return (
    <>
      <button type="button" className={`btn btn-sm${confirmacao ? ' btn-perigo' : ''}`}
              disabled={enviando}
              onClick={() => (confirmacao ? setPerguntando(true) : disparar())}>
        {rotulo}
      </button>
      {confirmacao && (
        <Confirmar
          aberto={perguntando}
          titulo={confirmacao}
          corpo="Esta mudança não tem volta pela tela: o back-end recusa o caminho inverso."
          rotuloConfirmar={rotulo}
          perigo
          onConfirmar={disparar}
          onCancelar={() => setPerguntando(false)}
        />
      )}
    </>
  );
}
```

Nas chamadas: `Confirmar` fica sem `confirmacao`; `Concluir` recebe `confirmacao="Concluir esta consulta?"`; `Faltou` recebe `confirmacao="Registrar falta do paciente?"`.

6. `<Aviso texto={sucesso} />` logo abaixo do `Cabecalho`, com `sucesso` vindo de `useAcao`.

- [ ] **Step 2: O CSS da calha**

```css
/* ── AGENDA ───────────────────────────────────────────────────────────────── */
/* A agenda não é tabela: o eixo é o tempo, e a hora é a coluna que o olho
   procura primeiro. */
.dia-degrau {
  display: grid;
  grid-template-columns: 5ch minmax(0, 1fr);
  gap: 20px;
  padding-block: 14px;
  border-top: 1px solid var(--hair-fraca);
}
.dia-hora {
  margin: 0;
  font-variant-numeric: tabular-nums;
  color: var(--tinta-fraca);
  font-size: 13px;
  padding-top: 14px;
}
/* Duas cadeiras atendendo às 14h é o caso normal de uma clínica. */
.dia-blocos { display: flex; flex-wrap: wrap; gap: 12px; }
.consulta {
  flex: 1 1 22rem;
  border: 1px solid var(--hair);
  background: var(--superficie);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
.consulta-faixa { margin: 0; font-variant-numeric: tabular-nums; font-size: 13px; }
.consulta-tel { color: var(--tinta); font-size: 13px; }
.consulta .acoes { margin-top: 8px; }
@media (max-width: 620px) {
  .dia-degrau { grid-template-columns: 1fr; gap: 6px; }
  .dia-hora { padding-top: 0; }
}
```

- [ ] **Step 3: Verificar**

```bash
cd web && npm test && npm run build
```

Esperado: o teste `nenhuma tela imprime o endpoint` deixa de acusar `Agenda.jsx`; o de `.row` também. Os dois continuam vermelhos pelas outras telas.

Na tela: trocar de dia com os atalhos não abre o date picker; telefone disca; "Faltou" abre o diálogo e `Esc` fecha sem disparar.

- [ ] **Step 4: Commit**

```bash
cd web && git add src/paginas/Agenda.jsx src/app.css
git commit -m "feat(web): a agenda vira eixo do tempo

terminoEm e telefonePaciente já vinham no ConsultaResumo e eram descartados —
a recepção liga para o paciente, é o dado que ela mais usa. Trocar de dia
exigia abrir o date picker. E Faltou e Concluir, que o back-end recusa
desfazer, ficavam a um clique encostados no botão inofensivo."
```

---

### Task 8: Pacientes — tabela, busca honesta e contagem que não mente

**Files:**
- Modify: `web/src/paginas/Pacientes.jsx`
- Modify: `web/src/components/FichaPaciente.jsx` (foco no primeiro campo)

**Interfaces:**
- Consumes: `contagem`, `telHref` de `apresentacao.js`; `Tabela`, `Celula`, `Selo`, `Aviso`.
- Produces: nada.

- [ ] **Step 1: Reescrever a lista**

1. O limite deixa de ser número mágico solto:

```js
/* O `LIMITE_MAXIMO` do PacienteController é 200, e pedir mais não traz mais.
   A constante existe para a frase de contagem saber quando a lista bateu o
   teto — sem isso a tela escrevia "200 no cadastro" para uma clínica de 900. */
const LIMITE = 200;
```

2. `detalhe` honesto, sem endpoint:

```jsx
        detalhe={pacientes.status === 'ok'
          ? contagem(lista.length, LIMITE, 'paciente', 'pacientes')
          : 'Cadastro da clínica'}
```

3. Busca sobre o que está carregado, rotulada:

```jsx
  const [busca, setBusca] = useState('');
  const filtrada = busca.trim()
    ? lista.filter((p) => `${p.nomeCompleto ?? ''} ${p.telefoneCelular ?? ''}`
      .toLowerCase().includes(busca.trim().toLowerCase()))
    : lista;
```

```jsx
      <div className="filtros filtros-linha">
        <label className="campo-app" style={{ maxWidth: '24rem' }}>
          <span className="cap cap-ash">Buscar</span>
          <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)}
                 placeholder="Nome ou telefone" />
        </label>
        {/* Dizer o alcance da busca em vez de prometer o cadastro inteiro: o
            back-end tem keyset mas não tem busca, e uma caixa que parece
            procurar em tudo faria alguém concluir que o paciente não existe. */}
        <p className="cap cap-ash">Filtra os {lista.length} carregados nesta tela.</p>
      </div>
```

4. A lista vira `Tabela`:

```jsx
        <Tabela colunas={[
          { chave: 'nome', rotulo: 'Nome' },
          { chave: 'telefone', rotulo: 'Telefone' },
          { chave: 'status', rotulo: 'Status' },
          { chave: 'acoes', rotulo: '' },
        ]}>
          {filtrada.map((p) => (
            <tr key={p.idPaciente}>
              <Celula rotulo="Nome">{p.nomeCompleto ?? '—'}</Celula>
              <Celula rotulo="Telefone" num>
                {telHref(p.telefoneCelular)
                  ? <a href={telHref(p.telefoneCelular)}>{p.telefoneCelular}</a>
                  : '—'}
              </Celula>
              {/* Mapa vazio de propósito: os valores do CHECK de
                  `pacientes.status` não foram verificados, e inventar rótulo
                  para um estado clínico é pior que mostrar o valor cru.
                  `descrever` devolve o valor como veio, com tom neutro. */}
              <Celula rotulo="Status"><Selo mapa={{}} valor={p.status} /></Celula>
              <Celula rotulo="">
                {pode('PRONTUARIO') && (
                  <Link className="btn btn-sm"
                        to={prontuarioDe(p.idPaciente)}
                        /* O nome já está aqui. Passar por state evita o
                           "Paciente 7" do cabeçalho do prontuário sem custar
                           uma requisição. */
                        state={{ nomePaciente: p.nomeCompleto }}>
                    Prontuário
                  </Link>
                )}
              </Celula>
            </tr>
          ))}
        </Tabela>
```

Não crie `STATUS_PACIENTE` em `apresentacao.js`: os valores do CHECK de `pacientes.status` não foram verificados contra o banco, e um mapa com rótulos inventados esconderia um estado real atrás de um travessão.

5. `Estado` com `esqueleto={{ linhas: 8, colunas: 4 }}`, e o vazio com o botão dentro:

```jsx
        vazio={filtrada.length === 0
          ? (busca ? 'Nenhum paciente carregado corresponde a esta busca.'
            : 'Nenhum paciente cadastrado ainda.')
          : null}
```

- [ ] **Step 2: Foco ao abrir o formulário**

Em `web/src/components/FichaPaciente.jsx`, no primeiro `<input>` do formulário, acrescente `autoFocus`. Hoje o formulário abre fora da vista e empurra a lista; o `autoFocus` traz o foco e rola até ele sem nenhum `useRef`.

- [ ] **Step 3: Verificar**

```bash
cd web && npm test && npm run build
```

- [ ] **Step 4: Commit**

```bash
cd web && git add src/paginas/Pacientes.jsx src/components/FichaPaciente.jsx
git commit -m "feat(web): pacientes em tabela, com contagem que não mente

A tela pedia limite=200, que é o LIMITE_MAXIMO do controller, recebia 200 e
escrevia '200 no cadastro' — falso para uma clínica de 900. A busca filtra os
carregados e diz isso: o back-end tem keyset e não tem busca, e uma caixa que
parece procurar em tudo faria concluir que o paciente não existe."
```

---

### Task 9: Prontuário — nome, abas de verdade e legenda

**Files:**
- Modify: `web/src/paginas/Prontuario.jsx`
- Modify: `web/src/app.css`

**Interfaces:**
- Consumes: `useRecurso` para `/equipe` (**exceção 1 do spec**); `useLocation` do react-router; `Aviso`.
- Produces: nada.

**Imports que faltam no arquivo hoje:** `useMemo` de `react` (a linha 1 importa só `useState`) e `useLocation` de `react-router-dom`.

- [ ] **Step 1: O nome do paciente, sem requisição nova**

```jsx
  const { state } = useLocation();
  /* Veio da tela de Pacientes, onde o nome já estava carregado. Quem entra pela
     URL direta cai no id, que é honesto — e não custa uma chamada a mais. */
  const nomePaciente = state?.nomePaciente ?? `Paciente ${idPaciente}`;
```

Use no `titulo` do `Cabecalho`: `titulo={nomePaciente}`, e `detalhe="Prontuário · cada abertura fica registrada na trilha de auditoria"`.

- [ ] **Step 2: O nome de quem assinou a evolução**

Em `Evolucoes`, a exceção 1:

```jsx
  /* Exceção deliberada ao "nenhum endpoint novo": a evolução traz `idDentista`
     e não o nome, e "Dentista 3" assinando registro clínico é o tipo de coisa
     que derruba uma demonstração. A Auditoria já chama /equipe pelo mesmo
     motivo. */
  const equipe = useRecurso('/equipe');
  const nomeDoDentista = useMemo(() => {
    const mapa = new Map((equipe.dados ?? []).map((m) => [m.idUsuario, m.nomeCompleto]));
    return (id) => mapa.get(id) ?? `Dentista ${id}`;
  }, [equipe.dados]);
```

- [ ] **Step 3: As abas ganham o padrão ARIA completo**

```jsx
      <div className="abas" role="tablist" aria-label="Seções do prontuário">
        {[['evolucoes', 'Evolução'], ['odontograma', 'Odontograma']].map(([id, rotulo]) => (
          <button
            key={id}
            type="button"
            role="tab"
            id={`aba-${id}`}
            aria-controls={`painel-${id}`}
            aria-selected={aba === id}
            /* Aba não selecionada sai da ordem de Tab: no padrão ARIA o grupo
               inteiro é UMA parada, e as setas andam dentro dele. Sem isto o
               leitor de tela anunciava "guia" e o teclado se comportava como
               uma fileira de botões. */
            tabIndex={aba === id ? 0 : -1}
            onKeyDown={(e) => {
              if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
              setAba((a) => (a === 'evolucoes' ? 'odontograma' : 'evolucoes'));
            }}
            onClick={() => setAba(id)}
            className="btn"
          >
            {rotulo}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`painel-${aba}`} aria-labelledby={`aba-${aba}`}>
        {aba === 'evolucoes'
          ? <Evolucoes idPaciente={idPaciente} recurso={evolucoes} />
          : <Odontograma idPaciente={idPaciente} recurso={odontograma} />}
      </div>
```

- [ ] **Step 4: A retificada para de sumir**

Remova de `app.css`:

```css
.row-evolucao[data-retificada="sim"] { opacity: .62; }
```

Opacidade em registro clínico é perda de informação — o texto precisa continuar legível. No lugar, na linha da evolução retificada, um selo e uma marca de margem:

```css
.evolucao { border-top: 1px solid var(--hair-fraca); padding-block: 16px; display: grid;
            grid-template-columns: 14rem minmax(0, 1fr); gap: 20px; }
.evolucao[data-retificada="sim"] { border-left: 3px solid var(--alarm); padding-left: 14px; }
@media (max-width: 620px) { .evolucao { grid-template-columns: 1fr; gap: 4px; } }
```

Troque `className="row row-evolucao"` por `className="evolucao"` no `<li>`.

- [ ] **Step 5: O aviso de auditoria vira faixa, e o odontograma ganha legenda**

Abaixo do `Cabecalho`:

```jsx
      <p className="faixa-auditoria" role="note">
        Abrir este prontuário grava uma linha na trilha de auditoria, com o seu nome.
      </p>
```

E no componente `Odontograma`, antes das arcadas:

```jsx
      {/* Sete condições codificadas em borda e nenhuma legenda: ninguém sabe
          ler o que a tela está dizendo. */}
      <ul className="odonto-legenda">
        {CONDICOES.map((c) => (
          <li key={c}><span className="odonto-amostra" data-condicao={c} />{c}</li>
        ))}
      </ul>
```

```css
.faixa-auditoria {
  border: 1px solid var(--hair);
  border-left: 3px solid var(--tinta);
  padding: 10px 14px;
  margin-bottom: var(--spacing-20, 20px);
  font-size: 13px;
  color: var(--tinta-fraca);
}
.odonto-legenda { list-style: none; display: flex; flex-wrap: wrap; gap: 14px;
                  margin: 0 0 14px; padding: 0; font-size: 12px; color: var(--tinta-fraca); }
.odonto-legenda li { display: flex; align-items: center; gap: 6px; }
.odonto-amostra { width: 16px; height: 16px; border: 1px solid var(--hair); display: inline-block; }
.odonto-amostra[data-condicao="cárie"] { border-color: var(--alarm); border-bottom-width: 3px; }
.odonto-amostra[data-condicao="ausente"] { opacity: .38; border-style: dashed; }
.odonto-amostra[data-condicao="restaurado"],
.odonto-amostra[data-condicao="coroa"],
.odonto-amostra[data-condicao="implante"] { border-bottom-width: 3px; }

/* Em tela de mesa o formulário do dente abre AO LADO da arcada: abrindo
   abaixo, cada clique num dente rola a página. */
@media (min-width: 900px) {
  .odonto-area { display: grid; grid-template-columns: minmax(0, 1fr) 22rem; gap: var(--spacing-30, 30px); align-items: start; }
}
```

Envolva arcadas e formulário em `<div className="odonto-area">`.

- [ ] **Step 6: Trocar `pagamento-erro` por `erro-campo`**

```bash
cd web/src && sed -i 's/pagamento-erro/erro-campo/g' paginas/Prontuario.jsx
```

- [ ] **Step 7: Verificar e commitar**

```bash
cd web && npm test && npm run build
git add src/paginas/Prontuario.jsx src/app.css
git commit -m "feat(web): prontuário com nome, abas de verdade e legenda

O cabeçalho dizia 'Paciente 7' e a evolução era assinada por 'Dentista 3'. As
abas declaravam role=tablist sem tabpanel, aria-controls nem setas — ARIA pela
metade mente para o leitor de tela sobre o que aquilo é. E a evolução
retificada ficava com opacity .62: opacidade em registro clínico é perda de
informação, não ênfase."
```

---

### Task 10: Financeiro — indicadores com o recorte certo

**Files:**
- Modify: `web/src/paginas/Financeiro.jsx`
- Modify: `web/src/app.css`

**Interfaces:**
- Consumes: `STATUS_RECEBIVEL`, `contagem`; `Tabela`, `Celula`, `Selo`; `useRecurso('/pacientes?limite=200')` (**exceção 2 do spec**).
- Produces: nada.

**Imports que faltam no arquivo hoje:** `useMemo` de `react` — `Financeiro.jsx` não importa nada de `react` ainda.

**O segundo `Cabecalho`** (o de "Recebíveis.") também imprime endpoint: troque `detalhe` para `contagem(lista.length, 200, 'parcela', 'parcelas')` quando `recebiveis.status === 'ok'`, e `'Parcelas em aberto'` enquanto carrega.

- [ ] **Step 1: Corrigir o recorte de cada indicador**

O erro de informação: `detalhe="Do primeiro dia do mês até hoje"` está no cabeçalho como se valesse para os cinco números, e só `recebidoNoPeriodo` é do período.

```jsx
const INDICADORES = [
  ['Recebido no período', 'recebidoNoPeriodo', 'do dia 1 até hoje'],
  ['A receber', 'aReceber', 'saldo em aberto, todas as datas'],
  ['Vencido', 'vencido', 'saldo em aberto já vencido'],
  ['A pagar', 'aPagar', 'despesas em aberto'],
  ['Comissões previstas', 'comissoesPrevistas', 'ainda não liberadas'],
];
```

```jsx
      <Cabecalho titulo="Financeiro." detalhe="Só leitura nesta tela" />
```

```jsx
        <div className="indicadores">
          {INDICADORES.map(([rotulo, campo, recorte]) => (
            <div className="indicador" key={campo}
                 data-alarme={campo === 'vencido' && Number(resumo.dados?.vencido) > 0 ? 'sim' : 'nao'}>
              <p className="cap cap-ash">{rotulo}</p>
              <p className="indicador-valor">{brl(resumo.dados?.[campo])}</p>
              <p className="cap cap-ash">{recorte}</p>
            </div>
          ))}
        </div>
```

- [ ] **Step 2: Dizer que é só leitura**

Abaixo dos indicadores:

```jsx
      {/* Lançar recebimento, despesa, comissão e estorno existem no back-end e
          não têm tela. Sem esta frase a pessoa procura o botão, não acha, e
          conclui que o sistema está quebrado. */}
      <p className="cap cap-ash">
        Lançamentos, despesas e comissões ainda não têm tela. Esta é a visão de leitura.
      </p>
```

- [ ] **Step 3: Recebíveis em tabela, ordenados por vencimento, com nome**

```jsx
  /* Exceção deliberada ao "nenhum endpoint novo": RecebivelResumo traz
     idPaciente e não o nome, e uma cobrança sem nome não dá para conferir. */
  const pacientes = useRecurso('/pacientes?limite=200');
  const nomeDoPaciente = useMemo(() => {
    const mapa = new Map((pacientes.dados ?? []).map((p) => [p.idPaciente, p.nomeCompleto]));
    return (id) => (id == null ? '—' : mapa.get(id) ?? `Paciente ${id}`);
  }, [pacientes.dados]);

  /* Ordem imposta aqui: a pergunta desta tabela é "o que vence primeiro". */
  const ordenada = [...lista].sort((a, b) =>
    String(a.vencimentoEm ?? '9999').localeCompare(String(b.vencimentoEm ?? '9999')));
```

```jsx
        <Tabela colunas={[
          { chave: 'paciente', rotulo: 'Paciente' },
          { chave: 'parcela', rotulo: 'Parcela' },
          { chave: 'vencimento', rotulo: 'Vencimento', num: true },
          { chave: 'valor', rotulo: 'Valor', num: true },
          { chave: 'saldo', rotulo: 'Saldo', num: true },
          { chave: 'status', rotulo: 'Status' },
        ]}>
          {ordenada.map((r) => (
            <tr key={r.idRecebivel}>
              <Celula rotulo="Paciente">{nomeDoPaciente(r.idPaciente)}</Celula>
              <Celula rotulo="Parcela">{r.parcelaNumero}/{r.parcelaTotal}</Celula>
              {/* `new Date(null)` é 01/01/1970, não erro — numa coluna de
                  vencimento isso passa por dado de verdade. */}
              <Celula rotulo="Vencimento" num>
                {r.vencimentoEm ? DATA.format(new Date(r.vencimentoEm)) : '—'}
              </Celula>
              <Celula rotulo="Valor" num>{brl(r.valorParcela)}</Celula>
              {/* saldoDevedor é SUM sobre o ledger (vw_saldo_recebivel), não
                  coluna guardada — não existe caminho para discordar do
                  extrato. Por isso é ele que aparece, e não uma subtração. */}
              <Celula rotulo="Saldo" num>{brl(r.saldoDevedor)}</Celula>
              <Celula rotulo="Status"><Selo mapa={STATUS_RECEBIVEL} valor={r.status} /></Celula>
            </tr>
          ))}
        </Tabela>
```

- [ ] **Step 4: O CSS dos indicadores**

```css
/* ── INDICADORES ──────────────────────────────────────────────────────────── */
.indicadores {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
  gap: 1px;
  background: var(--hair);
  border: 1px solid var(--hair);
  margin-bottom: var(--spacing-30, 30px);
}
.indicador { background: var(--superficie); padding: 16px 18px; }
.indicador-valor {
  margin: 6px 0;
  font-size: clamp(1.4rem, 2.4vw, 1.8rem);
  font-variant-numeric: tabular-nums;
}
.indicador[data-alarme="sim"] .indicador-valor { color: var(--alarm); }
```

- [ ] **Step 5: Verificar e commitar**

```bash
cd web && npm test && npm run build
git add src/paginas/Financeiro.jsx src/app.css
git commit -m "feat(web): financeiro diz o recorte de cada número

'Do primeiro dia do mês até hoje' estava no cabeçalho como se valesse para os
cinco indicadores, e só recebidoNoPeriodo é do período: aReceber, vencido e
aPagar são saldo corrente. Erro de informação numa tela de dinheiro. O
vencimento sai da coluna de ações, onde o olho procura botão, e a cobrança
passa a ter nome de paciente."
```

---

### Task 11: Equipe

**Files:**
- Modify: `web/src/paginas/Equipe.jsx`

**Interfaces:**
- Consumes: `STATUS_MEMBRO`; `Tabela`, `Celula`, `Selo`, `Aviso`; `Confirmar`.
- Produces: nada.

- [ ] **Step 1: Tabela, descartar, e o vínculo fora do vermelho**

1. `detalhe` sem endpoint: quando `limite.status !== 'ok'`, use `'Quem tem acesso a esta clínica'`.

2. Em `Membro`, ao lado de "Salvar", um "Descartar" — hoje só recarregando a página:

```jsx
        {mudou && (
          <>
            <button type="button" className="btn btn-sm btn-fill" disabled={enviando}
                    onClick={() => pedirSalvar()}>Salvar</button>
            <button type="button" className="btn btn-sm" disabled={enviando}
                    onClick={() => { setPapel(membro.papel); setStatus(membro.status); }}>
              Descartar
            </button>
          </>
        )}
```

3. Desativar pede confirmação:

```jsx
  const [perguntando, setPerguntando] = useState(false);

  const salvar = () => {
    setPerguntando(false);
    executar(api.put(`/equipe/${membro.idUsuario}`, { papel, status }),
      `${membro.nomeCompleto}: alteração salva.`);
  };

  /* Desativar tira o acesso de uma pessoa. Papel e status mudam pelo mesmo
     botão, e só um dos dois caminhos manda alguém para casa. */
  const pedirSalvar = () => (status === 'desativado' && membro.status !== 'desativado'
    ? setPerguntando(true)
    : salvar());
```

```jsx
      <Confirmar
        aberto={perguntando}
        titulo={`Desativar ${membro.nomeCompleto}?`}
        corpo="A pessoa perde o acesso ao sistema na próxima requisição. O histórico dela na auditoria continua."
        rotuloConfirmar="Desativar"
        perigo
        onConfirmar={salvar}
        onCancelar={() => setPerguntando(false)}
      />
```

4. O vínculo sai do vermelho — remova `[data-vinculado="nao"] { color: var(--alarm); }` de `app.css` e use um selo neutro:

```jsx
              {/* Vermelho é alarme. "Aguardando primeira entrada" é o estado
                  normal de quem foi admitido ontem, não um problema. */}
              <Selo mapa={{
                sim: { rotulo: 'Acesso ativo', tom: 'cheio' },
                nao: { rotulo: 'Aguardando 1ª entrada', tom: 'contorno' },
              }} valor={membro.vinculado ? 'sim' : 'nao'} />
```

5. O aviso de plano gruda no botão:

```jsx
        acao={(
          <div>
            <button type="button" className="btn btn-fill" disabled={!cabe}
                    aria-describedby={!cabe ? 'limite-plano' : undefined}
                    onClick={() => setAdmitindo((a) => !a)}>
              {admitindo ? 'Cancelar' : 'Admitir membro'}
            </button>
            {!cabe && limite.dados?.motivo && (
              <p className="cap cap-ash" id="limite-plano">{limite.dados.motivo}</p>
            )}
          </div>
        )}
```

Remova o `<p className="aviso-plano">` separado.

6. `sed -i 's/pagamento-erro/erro-campo/g' src/paginas/Equipe.jsx`.

- [ ] **Step 2: Verificar e commitar**

```bash
cd web && npm test && npm run build
git add src/paginas/Equipe.jsx src/app.css
git commit -m "feat(web): equipe em tabela, com desativar confirmado

Mudar alguém para desativado tira o acesso dessa pessoa e acontecia pelo mesmo
botão que troca papel, sem pergunta. 'Aguardando primeira entrada' estava em
vermelho, que é alarme, sendo o estado normal de quem foi admitido ontem. E o
que você alterou nos selects só se desfazia recarregando a página."
```

---

### Task 12: Auditoria

**Files:**
- Modify: `web/src/paginas/Auditoria.jsx`

**Interfaces:**
- Consumes: `Tabela`, `Celula`, `Selo`; `somarDias`, `hoje` de `apresentacao.js`.
- Produces: nada.

**Apague os helpers locais** `diasAtras` e `hoje` do topo de `Auditoria.jsx`: `somarDias(hoje(), -30)` faz o mesmo e é o que tem teste. O mapa local `ACOES` também sai, substituído por `ACOES_SELO` no Step 1.

- [ ] **Step 1: Atalhos, datalist e o truncamento dito em voz alta**

1. `detalhe` sem endpoint; use `'Quem leu e quem mudou o quê'` enquanto carrega.

2. Atalhos de período na barra de filtros:

```jsx
        <div className="acoes">
          {[7, 30, 90].map((n) => (
            <button type="button" className="btn btn-sm" key={n}
                    onClick={() => setFiltros((f) => ({ ...f, de: somarDias(hoje(), -n), ate: hoje() }))}>
              {n} dias
            </button>
          ))}
        </div>
```

3. O campo Recurso ganha os valores que vieram:

```jsx
          <input value={filtros.recurso} onChange={mudar('recurso')}
                 list="recursos-vistos" placeholder="prontuario.evolucao" />
          {/* Texto livre num vocabulário que ninguém decorou é filtro que não
              se usa. Os valores da resposta atual já ensinam a nomenclatura. */}
          <datalist id="recursos-vistos">
            {[...new Set(lista.map((e) => e.recurso))].map((r) => <option key={r} value={r} />)}
          </datalist>
```

4. O teto de 200 dito:

```jsx
      {lista.length >= 200 && (
        <p className="cap cap-ash">
          Mostrando os 200 eventos mais recentes do período. Estreite as datas para ver o resto.
        </p>
      )}
```

5. A tabela passa a usar `Tabela`/`Celula` com `rotulo` em cada célula, e a ação vira `<Selo mapa={ACOES_SELO} valor={e.acao} />`, com:

```js
/* Espelha o CHECK de `auditoria.eventos.acao`. Exclusão e falha de login são
   as duas que precisam saltar numa tela de trilha. */
const ACOES_SELO = {
  leitura: { rotulo: 'Leitura', tom: 'fraco' },
  criacao: { rotulo: 'Criação', tom: 'contorno' },
  alteracao: { rotulo: 'Alteração', tom: 'contorno' },
  exclusao: { rotulo: 'Exclusão', tom: 'alarme' },
  login: { rotulo: 'Login', tom: 'fraco' },
  logout: { rotulo: 'Logout', tom: 'fraco' },
  falha_login: { rotulo: 'Falha de login', tom: 'alarme' },
  exportacao: { rotulo: 'Exportação', tom: 'alarme' },
};
```

- [ ] **Step 2: Verificar e commitar**

```bash
cd web && npm test && npm run build
git add src/paginas/Auditoria.jsx
git commit -m "feat(web): auditoria com atalhos de período e vocabulário à mão

O filtro de recurso era texto livre num vocabulário que ninguém decorou; os
valores da resposta atual agora alimentam um datalist. E a tela truncava em
200 eventos calada, o que numa trilha de auditoria é a diferença entre 'não
houve' e 'não coube'."
```

---

### Task 13: Verificação final

**Files:** nenhum novo.

- [ ] **Step 1: Suíte e build**

```bash
cd web && npm test && npm run build
```

Esperado: **tudo verde**, incluindo os cinco testes de `estilo.test.mjs` que abriram vermelhos na Task 1.

- [ ] **Step 2: Os greps do spec**

```bash
cd web/src
grep -rn -- "var(--ink\|var(--hair-dim" . ; echo "^ vazio"
grep -rn "GET /api/" paginas/ ; echo "^ vazio"
grep -rn "pagamento-erro" paginas/ components/FichaPaciente.jsx components/Cadastro.jsx 2>/dev/null ; echo "^ vazio"
```

`FichaPaciente.jsx` e `Cadastro.jsx` ainda têm `pagamento-erro` e não foram cobertos por nenhuma task — troque agora:

```bash
cd web/src && sed -i 's/pagamento-erro/erro-campo/g' components/FichaPaciente.jsx paginas/Cadastro.jsx
```

- [ ] **Step 3: Conferência em dois tamanhos**

```bash
cd web && npm run dev
```

Para cada uma de `/agenda`, `/pacientes`, `/pacientes/1/prontuario`, `/financeiro`, `/equipe`, `/auditoria`, a 1280px e a 400px:

- Sem rolagem horizontal no corpo da página.
- A coluna de ações visível nas duas larguras.
- Foco visível ao percorrer com Tab.
- `/` e uma URL inexistente continuam **pretas**; as seis telas e `/cadastro`, **claras**.
- O popover do `UserButton` abre claro.

- [ ] **Step 4: Commit**

```bash
cd web && git add -A
git commit -m "chore(web): erro-campo também na ficha e no cadastro

Últimos dois usos de .pagamento-erro, a classe do formulário de cartão que
quatro telas tomaram emprestada como erro de campo."
```

---

## Fora deste plano

Do spec, e confirmado com o dono antes de começar:

- Agendar e cancelar consulta pelo web (`POST /consultas`, `POST /{id}/cancelar`).
- Escritas do financeiro.
- Busca e paginação de pacientes no servidor.
- Telas para Orçamentos, Estoque, LGPD, Billing e Procedimentos.
- Modo escuro no app, com chave por usuário.

Achado durante o planejamento, **decisão pendente do dono**:

- `CartaoCredito.jsx` (141 linhas), `FormularioCartao.jsx` (203) e `TiltCard.jsx` (95) não são importados por ninguém, e com eles ~180 linhas de CSS em `style.css`. `cartao.js` e `cartao.test.mjs` sustentam só esses componentes. Nada neste plano os toca; deletar ou montá-los numa tela de billing é assunto de outro ciclo.
