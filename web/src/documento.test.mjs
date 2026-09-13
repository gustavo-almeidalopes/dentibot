/**
 * As regras do cadastro que não são óbvias. Mesmo estilo de cartao.test.mjs.
 *
 *   cd web && npm test
 *
 * Os CPF/CNPJ daqui são gerados para fechar o dígito verificador e não
 * pertencem a ninguém.
 */
import assert from 'node:assert/strict';
import test from 'node:test';
import {
  cnpjValido, cpfValido, documentoValido, formatarCep, formatarCnpj, formatarCpf,
  formatarTelefone, idadeEm, menorDeIdade, tipoDeDocumento,
} from './documento.js';

test('máscaras crescem junto com a digitação, sem literal órfão no fim', () => {
  assert.equal(formatarCpf('529'), '529');
  assert.equal(formatarCpf('529982'), '529.982');
  assert.equal(formatarCpf('52998224725'), '529.982.247-25');
  assert.equal(formatarCpf('529.982.247-25999'), '529.982.247-25'); // não passa de 11
  assert.equal(formatarCnpj('11222333000181'), '11.222.333/0001-81');
  assert.equal(formatarCep('01310100'), '01310-100');
});

test('telefone troca de molde entre fixo e celular', () => {
  assert.equal(formatarTelefone('1132654321'), '(11) 3265-4321');
  assert.equal(formatarTelefone('11987654321'), '(11) 98765-4321');
});

test('CPF confere o dígito verificador', () => {
  assert.equal(cpfValido('529.982.247-25'), true);
  assert.equal(cpfValido('529.982.247-26'), false);
  assert.equal(cpfValido('5299822472'), false); // curto demais
});

test('CPF de dígitos repetidos é recusado — o módulo 11 aprovaria', () => {
  assert.equal(cpfValido('111.111.111-11'), false);
  assert.equal(cpfValido('000.000.000-00'), false);
});

test('CNPJ confere o dígito verificador', () => {
  assert.equal(cnpjValido('11.222.333/0001-81'), true);
  assert.equal(cnpjValido('11.222.333/0001-82'), false);
  assert.equal(cnpjValido('11.111.111/1111-11'), false);
});

test('o tipo sai do comprimento, para o erro ser "CPF inválido" e não "não sei o que é"', () => {
  assert.equal(tipoDeDocumento('529.982.247-26'), 'cpf');
  assert.equal(documentoValido('529.982.247-26'), false);
  assert.equal(tipoDeDocumento('123'), null);
  assert.equal(tipoDeDocumento('11.222.333/0001-81'), 'cnpj');
  assert.equal(documentoValido('11.222.333/0001-81'), true);
});

test('idade só conta o aniversário já feito', () => {
  const hoje = new Date(2026, 8, 13); // 13/09/2026
  assert.equal(idadeEm('2008-09-13', hoje), 18); // faz hoje
  assert.equal(idadeEm('2008-09-14', hoje), 17); // faz amanhã
  assert.equal(menorDeIdade('2008-09-14', hoje), true);
  assert.equal(menorDeIdade('2008-09-13', hoje), false);
  assert.equal(menorDeIdade('', hoje), false); // sem data não se afirma nada
});
