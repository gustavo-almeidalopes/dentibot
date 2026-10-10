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
    // assert.ok e não doesNotMatch: o doesNotMatch despeja o arquivo inteiro
    // no erro, e a falha vira 200 linhas de JSX escapado.
    assert.ok(!/GET \/api\//.test(fonte), `${nome} mostra endpoint na tela`);
  }
});

test('as telas do sistema não usam o .row da landing', () => {
  // `.row` é grid de DUAS colunas com padding-block de 42px. As telas passavam
  // TRÊS filhos, e a coluna de ações caía numa linha implícita.
  for (const [nome, fonte] of telas()) {
    assert.ok(!/className="row\b|className="[^"]*\brow /.test(fonte),
      `${nome} ainda usa .row`);
  }
});

test('nenhuma tela usa a classe de erro do formulário de pagamento', () => {
  // `.pagamento-erro` é do cartão de crédito. Quatro telas o tomaram emprestado
  // como estilo de erro de campo; a classe certa é `.erro-campo`.
  for (const [nome, fonte] of telas()) {
    assert.ok(!/pagamento-erro/.test(fonte), `${nome} usa pagamento-erro`);
  }
});

test('o cabeçalho da tabela não gruda dentro de um contêiner que rola', () => {
  // `position: sticky` conta a partir do ancestral que rola mais próximo, e a
  // .tabela-rolagem rola (overflow-x). O `top: 56px` pensado para a janela
  // empurrava o <th> 56px para dentro da tabela, por cima da primeira linha —
  // que parava de receber clique. Ou a rolagem sai, ou o sticky não volta.
  const css = cssDoApp();
  const rola = /\.tabela-rolagem\s*\{[^}]*overflow(?:-x)?\s*:\s*(?:auto|scroll)/.test(css);
  const gruda = /\.tabela[^{]*\bth\b[^{]*\{[^}]*position\s*:\s*sticky/.test(css);
  assert.ok(!(rola && gruda), 'th sticky dentro da .tabela-rolagem cobre a primeira linha');
});

test('nome de paciente nas telas liga ao prontuário pelo LinkDoPaciente', () => {
  // Agenda, Conversas e Financeiro mostravam o nome como texto; só duas telas
  // ligavam, cada uma do seu jeito. O rótulo de reserva "Paciente N" só existe
  // dentro do componente — e no título do próprio prontuário.
  for (const [nome, fonte] of telas()) {
    if (nome === 'Prontuario.jsx') continue;
    assert.ok(!/`Paciente \$\{/.test(fonte), `${nome} monta nome de paciente fora do LinkDoPaciente`);
  }
});
