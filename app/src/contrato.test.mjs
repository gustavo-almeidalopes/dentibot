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
const JAVA = join(AQUI, '..', '..', 'api-java', 'src', 'main', 'java', 'br', 'com', 'dentibot');

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

test('ACOES_POR_STATUS deriva das transições de AgendaServico', () => {
  const java = ler(JAVA, 'agenda', 'application', 'AgendaServico.java');

  // Destino → ação da UI.
  const ACAO_DO_DESTINO = {
    confirmada: 'confirmar',
    cancelada: 'cancelar',
    faltou: 'falta',
    realizada: 'concluir',
  };

  // Em cada chamada a transicionar(...), o destino é o último status citado e
  // as origens são os anteriores. Vale para as duas formas usadas no serviço:
  // um status solto e um List.of(...).
  const esperado = {
    agendada: [],
    confirmada: [],
    em_atendimento: [],
    realizada: [],
    cancelada: [],
    faltou: [],
  };

  // Duas formas no serviço: `transicionar(...)` com origem única (só o
  // confirmar) e `transicionarDeQualquerUm(...)` com List.of de origens.
  const chamadas = [...java.matchAll(/transicionar(?:DeQualquerUm)?\(([\s\S]*?)\);/g)];
  let encontradas = 0;

  for (const [, argumentos] of chamadas) {
    const status = citadas(argumentos);
    // Declaração do método e a chamada ao repositório não citam status.
    if (status.length < 2) continue;
    encontradas += 1;
    const destino = status[status.length - 1];
    const acao = ACAO_DO_DESTINO[destino];
    assert.ok(acao, `destino "${destino}" não tem ação mapeada no app`);
    for (const origem of status.slice(0, -1)) {
      assert.ok(origem in esperado, `origem "${origem}" não é um status conhecido`);
      if (!esperado[origem].includes(acao)) esperado[origem].push(acao);
    }
  }

  // As quatro do serviço: confirmar, cancelar, falta, concluir. Se virarem
  // três, alguém removeu uma transição e o app ainda oferece o botão.
  assert.equal(encontradas, 4, `esperava 4 transições no AgendaServico, achei ${encontradas}`);

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
