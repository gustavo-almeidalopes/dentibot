/**
 * O casamento de rota → recurso, que é o que decide se a tela abre.
 *
 *   cd web && npm test
 *
 * Um bug aqui não aparece na tela: ele abre a tela errada para o papel errado.
 * O back-end continua barrando a CHAMADA, então o vazamento não é de dado — mas
 * a recepcionista veria o prontuário montar e falhar, o que já conta como
 * prometer o que a camada 5 nega.
 */
import assert from 'node:assert/strict';
import test from 'node:test';
import { AGENDA, MENU, PACIENTES, prontuarioDe, recursoDaTela } from './rotas.js';

test('prontuário não é confundido com pacientes', () => {
  // O prefixo é o mesmo; o recurso não. Recepcionista tem PACIENTE e não tem
  // PRONTUARIO — casar por `startsWith` abriria exatamente a tela proibida.
  assert.equal(recursoDaTela(PACIENTES), 'PACIENTE');
  assert.equal(recursoDaTela(prontuarioDe(7)), 'PRONTUARIO');
  assert.equal(recursoDaTela(prontuarioDe(7) + '/'), 'PRONTUARIO');
});

test('rota desconhecida devolve null em vez de um recurso qualquer', () => {
  // null = "não sei", e o Layout deixa passar para o back-end decidir. Devolver
  // um recurso adivinhado esconderia tela por engano, ou pior, mostraria.
  assert.equal(recursoDaTela('/'), null);
  assert.equal(recursoDaTela('/pacientes/7'), null);
  assert.equal(recursoDaTela('/pacientes/7/prontuario/anexos'), null);
  assert.equal(recursoDaTela('/financeiro/recebiveis'), null);
});

test('todo item de menu tem recurso, e é o nome do enum do back-end', () => {
  // Item sem recurso cairia em `pode(undefined)` → NENHUM → item invisível para
  // todo mundo, incluindo o admin. Falha silenciosa, e é assim que uma tela
  // desaparece sem ninguém mexer nela.
  const RECURSOS = new Set([
    'AGENDA', 'PACIENTE', 'PRONTUARIO', 'ORCAMENTO', 'FINANCEIRO',
    'ESTOQUE', 'CONFIGURACAO', 'EQUIPE', 'LGPD', 'AUDITORIA', 'BILLING',
  ]);
  for (const item of MENU) {
    assert.ok(RECURSOS.has(item.recurso), `${item.href}: recurso ${item.recurso}`);
    assert.equal(recursoDaTela(item.href), item.recurso);
  }
  assert.equal(recursoDaTela(AGENDA), 'AGENDA');
});
