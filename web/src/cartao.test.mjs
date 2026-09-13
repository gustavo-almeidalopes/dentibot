/**
 * As regras de cartão que não são óbvias. Mesmo estilo do app/src/contrato.test.mjs.
 *
 *   cd web && npm test
 *
 * Nenhum destes números é um cartão real: são os de teste publicados pelas
 * próprias bandeiras e pelo Stripe, que passam no Luhn de propósito.
 */
import assert from 'node:assert/strict';
import test from 'node:test';
import {
  cartaoCompleto, cvvCompleto, detectarBandeira, formatarNumero,
  formatarValidade, luhnValido, numeroExibido, validadeNoFuturo,
} from './cartao.js';

test('detecta as bandeiras pelo prefixo', () => {
  assert.equal(detectarBandeira('4111111111111111').id, 'visa');
  assert.equal(detectarBandeira('5555555555554444').id, 'mastercard');
  assert.equal(detectarBandeira('2223003122003222').id, 'mastercard'); // faixa 2-series
  assert.equal(detectarBandeira('378282246310005').id, 'amex');
  assert.equal(detectarBandeira(''), null);
  assert.equal(detectarBandeira('9999'), null);
});

test('Elo ganha da Visa nos prefixos em que as duas colidem', () => {
  // 4011 é Elo e começa com 4. Uma busca na ordem do array daria Visa, e o
  // cliente veria o logo errado no próprio cartão dele.
  assert.equal(detectarBandeira('4011780000000000').id, 'elo');
  assert.equal(detectarBandeira('4312740000000000').id, 'elo');
  // ...mas um 4 genérico continua Visa.
  assert.equal(detectarBandeira('4222222222222').id, 'visa');
});

test('Hipercard é reconhecida', () => {
  assert.equal(detectarBandeira('6062825624254001').id, 'hipercard');
});

test('agrupa 4-4-4-4, e 4-6-5 quando é Amex', () => {
  assert.equal(formatarNumero('4111111111111111'), '4111 1111 1111 1111');
  assert.equal(formatarNumero('378282246310005'), '3782 822463 10005');
});

test('o número exibido nasce completo e vai sendo ocupado', () => {
  // O cartão já tem a forma final: os dígitos entram nos lugares em vez de a
  // linha crescer da esquerda, que é o que dá a sensação de preenchimento.
  assert.equal(numeroExibido('4111'), '4111 •••• •••• ••••');
  assert.equal(numeroExibido(''), '•••• •••• •••• ••••');
  assert.equal(numeroExibido('378282246310005'), '3782 822463 10005');
  // Visa aceita 16 OU 19: o esqueleto começa em 16 e só cresce se passar disso.
  assert.equal(numeroExibido('4111111111111111'), '4111 1111 1111 1111');
  assert.equal(numeroExibido('4111111111111111222'), '4111 1111 1111 1111 222');
});

test('mascarar deixa só os quatro últimos', () => {
  assert.equal(numeroExibido('4111111111111111', { mascarar: true }), '•••• •••• •••• 1111');
});

test('formata a validade com a barra', () => {
  assert.equal(formatarValidade('12'), '12');
  assert.equal(formatarValidade('1230'), '12/30');
  assert.equal(formatarValidade('12/30'), '12/30');
});

test('a validade vale o mês inteiro do vencimento', () => {
  // Vence em 09/2026: no dia 30 de setembro ainda vale. Recusar no dia 1º é o
  // bug clássico, e ele rejeita cartão bom no balcão.
  const dentro = new Date(2026, 8, 30, 12, 0, 0);
  assert.equal(validadeNoFuturo('09/26', dentro), true);

  const depois = new Date(2026, 9, 1, 0, 0, 1);
  assert.equal(validadeNoFuturo('09/26', depois), false);
});

test('mês inválido é recusado antes da data', () => {
  const agora = new Date(2026, 0, 1);
  assert.equal(validadeNoFuturo('00/30', agora), false);
  assert.equal(validadeNoFuturo('13/30', agora), false);
});

test('CVV tem 4 dígitos na Amex e 3 no resto', () => {
  assert.equal(cvvCompleto('123', '4111111111111111'), true);
  assert.equal(cvvCompleto('1234', '4111111111111111'), false);
  assert.equal(cvvCompleto('1234', '378282246310005'), true);
  assert.equal(cvvCompleto('123', '378282246310005'), false);
});

test('Luhn pega dígito trocado', () => {
  assert.equal(luhnValido('4111111111111111'), true);
  assert.equal(luhnValido('4111111111111112'), false);
  // Transposição: 41 → 14 na ponta.
  assert.equal(luhnValido('1411111111111111'), false);
});

test('cartaoCompleto exige as quatro coisas', () => {
  const bom = {
    numero: '4111 1111 1111 1111',
    nome: 'ANA MELLO',
    validade: '12/30',
    cvv: '123',
  };
  assert.equal(cartaoCompleto(bom), true);
  assert.equal(cartaoCompleto({ ...bom, cvv: '12' }), false);
  assert.equal(cartaoCompleto({ ...bom, nome: 'A' }), false);
  assert.equal(cartaoCompleto({ ...bom, validade: '12/20' }), false);
  assert.equal(cartaoCompleto({ ...bom, numero: '4111 1111 1111 1112' }), false);
  // 15 dígitos num cartão Visa: passa no Luhn de nada, o comprimento barra.
  assert.equal(cartaoCompleto({ ...bom, numero: '411111111111111' }), false);
});
