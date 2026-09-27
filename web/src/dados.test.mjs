import assert from 'node:assert/strict';
import test from 'node:test';
import { estadoVisivel } from './dados.js';

test('sem caminho não há o que carregar', () => {
  assert.deepEqual(estadoVisivel(null, { chave: null, status: 'carregando' }, []),
    { status: 'ok', dados: [] });
});

test('resposta de outra chave é velha: carregando, sem apagar o que está na tela', () => {
  const velha = { chave: '/pacientes#0', status: 'ok', dados: ['Ana'] };
  assert.deepEqual(estadoVisivel('/pacientes#1', velha, null),
    { status: 'carregando', dados: ['Ana'] });
});

test('resposta da chave atual é o que aparece, inclusive erro', () => {
  const erro = { chave: '/eu#0', status: 'erro', erro: new Error('x'), dados: null };
  assert.equal(estadoVisivel('/eu#0', erro, null), erro);
});
