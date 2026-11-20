/**
 * O app duplica duas coisas que moram no backend: a lista de rotas que exigem
 * `Idempotency-Key` e a máquina de estados da consulta. Duplicata silenciosa é
 * a que apodrece — o backend ganha uma transição, o app continua mostrando os
 * botões antigos, e o usuário toma 409 sem entender.
 *
 * Este teste lê o Java e compara. Segue o estilo do próprio repositório, onde
 * `FronteiraDeSchemaTest` varre SQL e `ConformidadeDoSchemaTest` consulta o
 * catálogo do Postgres: verificar por máquina, não por revisão.
 *
 *   node --test src/*.test.mjs
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const AQUI = dirname(fileURLToPath(import.meta.url));
const JAVA = join(AQUI, '..', '..', 'api', 'src', 'main', 'java', 'br', 'com', 'dentibot');

const ler = (...p) => readFileSync(join(...p), 'utf8');
const citadas = (texto) => [...texto.matchAll(/"([a-z_/0-9.]+)"/g)].map((m) => m[1]);

test('a lista de rotas idempotentes do app é a do FiltroIdempotencia', () => {
  const java = ler(JAVA, 'plataforma', 'idempotencia', 'FiltroIdempotencia.java');
  const bloco = java.match(/PREFIXOS_OBRIGATORIOS\s*=\s*Set\.of\(([\s\S]*?)\);/);
  assert.ok(bloco, 'não achei PREFIXOS_OBRIGATORIOS — o Java mudou de forma');

  const doBackend = citadas(bloco[1]).sort();

  const ts = ler(AQUI, 'api.ts');
  const blocoTs = ts.match(/EXIGEM_IDEMPOTENCIA\s*=\s*\[([\s\S]*?)\]/);
  assert.ok(blocoTs, 'não achei EXIGEM_IDEMPOTENCIA em api.ts');
  const doApp = [...blocoTs[1].matchAll(/'([^']+)'/g)].map((m) => m[1]).sort();

  assert.deepEqual(
    doApp,
    doBackend,
    'app e backend discordam sobre quais POSTs exigem Idempotency-Key',
  );
});

test('ACOES_POR_STATUS deriva das transições de TransicaoConsulta', () => {
  const java = ler(JAVA, 'agenda', 'application', 'TransicaoConsulta.java');

  const esperado = {
    agendada: [],
    confirmada: [],
    em_atendimento: [],
    realizada: [],
    cancelada: [],
    faltou: [],
  };

  // Cada constante do enum: CONFIRMAR("confirmar", List.of(origens...), "destino", ...).
  // O primeiro argumento é o caminho da ação — o mesmo nome que a UI usa.
  const transicoes = [...java.matchAll(/^\s*[A-Z_]+\(\s*"(\w+)",\s*List\.of\(([^)]*)\),\s*"(\w+)"/gm)];
  for (const [, acao, origens, destino] of transicoes) {
    assert.ok(destino in esperado, `destino "${destino}" não é um status conhecido`);
    for (const origem of citadas(origens)) {
      assert.ok(origem in esperado, `origem "${origem}" não é um status conhecido`);
      if (!esperado[origem].includes(acao)) esperado[origem].push(acao);
    }
  }

  // Quatro: confirmar, cancelar, falta, concluir. Se virarem três, alguém
  // removeu uma transição e o app ainda oferece o botão.
  assert.equal(transicoes.length, 4, `esperava 4 transições em TransicaoConsulta, achei ${transicoes.length}`);

  const ts = ler(AQUI, 'api.ts');
  const bloco = ts.match(/ACOES_POR_STATUS[^=]*=\s*\{([\s\S]*?)\n\};/);
  assert.ok(bloco, 'não achei ACOES_POR_STATUS em api.ts');

  const doApp = {};
  for (const [, chave, valores] of bloco[1].matchAll(/(\w+):\s*\[([^\]]*)\]/g)) {
    doApp[chave] = [...valores.matchAll(/'([^']+)'/g)].map((m) => m[1]).sort();
  }

  for (const status of Object.keys(esperado)) {
    assert.deepEqual(
      doApp[status] ?? [],
      esperado[status].sort(),
      `ações para "${status}" divergem do AgendaServico`,
    );
  }
});

test('a janela do dia cobre 24h a partir da meia-noite local', () => {
  // Mesma aritmética de janelaDoDia em agenda.tsx. Duplicada aqui de propósito:
  // importar .tsx exigiria transpilar, e o que se testa é a regra, não o import.
  const dia = new Date(2026, 1, 28, 17, 43); // 28/02/2026, fim de tarde
  const de = new Date(dia.getFullYear(), dia.getMonth(), dia.getDate());
  const ate = new Date(de);
  ate.setDate(ate.getDate() + 1);

  assert.equal(de.getHours(), 0, 'início não é meia-noite local');
  assert.equal(de.getMinutes(), 0);
  assert.equal(ate.getDate(), 1, 'virada de mês falhou');
  assert.equal(ate.getMonth(), 2, 'esperava março');

  const horas = (ate.getTime() - de.getTime()) / 3_600_000;
  // 23 ou 25 em dia de mudança de horário de verão; 2026 no Brasil não tem.
  assert.equal(horas, 24);
});

test('o app só chama o que existe no contrato da API (ST-55)', () => {
  // api/openapi.json é gerado do código e travado pelo ContratoOpenApiTest.
  // Foi este teste que achou o /api/v1/auth/me, removido na V19 e ainda
  // chamado pelo Perfil — que mostrava "—" em silêncio.
  const contrato = JSON.parse(ler(AQUI, '..', '..', 'api', 'openapi.json'));
  const forma = (c) => c.split('?')[0].replace(/\$\{[^}]*\}|\{[^}]+\}/g, '{}');
  const rotas = Object.entries(contrato.paths)
    .map(([c, ops]) => [forma(c), new Set(Object.keys(ops))]);

  const ts = ler(AQUI, 'api.ts');
  const chamadas = [...ts.matchAll(/pedir<[^>]*>\(\s*[`']([^`']+)[`']([^;]*?)\)\s*[,;]/g)]
    .map(([, caminho, resto]) => ({
      caminho,
      metodo: (resto.match(/metodo:\s*'(\w+)'/)?.[1] ?? 'GET').toLowerCase(),
    }));
  assert.ok(chamadas.length >= 6, `achei só ${chamadas.length} chamadas em api.ts`);

  const fora = chamadas.filter(({ caminho, metodo }) => {
    // Parâmetro casa com qualquer segmento dos DOIS lados: `/consultas/${id}/confirmar`
    // no app é atendido por `/consultas/{id}/{acao}` no contrato, como o Spring roteia.
    const casa = (molde, alvo) => new RegExp(`^${molde.replace(/\{\}/g, '[^/]+')}$`).test(alvo);
    const meu = forma(caminho);
    return !rotas.some(([c, ops]) => (casa(meu, c) || casa(c, meu)) && ops.has(metodo));
  }).map((c) => `${c.metodo.toUpperCase()} ${c.caminho}`);
  assert.deepEqual(fora, [], 'chamadas do app sem endpoint no contrato');
});
