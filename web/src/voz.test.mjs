import assert from 'node:assert/strict';
import test from 'node:test';
import { falar } from './voz.js';

class FalaFalsa { constructor(texto) { this.texto = texto; } }

test('fala os avisos em pt-BR, interrompendo o que estava falando', () => {
  const chamadas = [];
  const synth = { cancel: () => chamadas.push('cancel'), speak: (f) => chamadas.push(f) };

  assert.equal(falar(['Alergia: dipirona.', 'Gestante.'], synth, FalaFalsa), true);

  assert.equal(chamadas[0], 'cancel');
  assert.equal(chamadas[1].texto, 'Atenção. Alergia: dipirona. Gestante.');
  assert.equal(chamadas[1].lang, 'pt-BR');
});

test('sem aviso, silêncio — e não um "atenção" vazio', () => {
  const synth = { cancel: () => assert.fail(), speak: () => assert.fail() };
  assert.equal(falar([], synth, FalaFalsa), false);
});
