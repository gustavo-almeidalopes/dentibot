/**
 * Linguagem simples no que o paciente lê (ODS-19).
 *
 *   cd web && npm test
 *
 * Índice de Flesch adaptado ao português (Martins et al., 1996):
 *
 *   248,835 − 1,015 × (palavras / frases) − 84,6 × (sílabas / palavras)
 *
 * De 75 a 100 é "muito fácil"; de 50 a 75, "fácil". A sílaba é contada por
 * grupo vocálico, o que erra em hiato ("saúde" conta duas): é heurística, e
 * serve para comparar versões do mesmo texto, não para laudo.
 */
import assert from 'node:assert/strict';
import test from 'node:test';
import { TRIAGEM } from './triagem.js';

const VOGAIS = /[aeiouáéíóúâêôãõàü]+/gi;

function flesch(textos) {
  let frases = 0;
  let palavras = 0;
  let silabas = 0;
  for (const texto of textos) {
    frases += Math.max(1, (texto.match(/[.!?]+/g) ?? []).length);
    for (const bruta of texto.split(/\s+/)) {
      const palavra = bruta.replace(/[^a-záéíóúâêôãõàüç]/gi, '');
      if (!palavra) continue;
      palavras += 1;
      silabas += Math.max(1, (palavra.match(VOGAIS) ?? []).length);
    }
  }
  return 248.835 - 1.015 * (palavras / frases) - 84.6 * (silabas / palavras);
}

test('a fórmula dá os valores de referência', () => {
  assert.ok(Math.abs(flesch(['O gato bebe leite.']) - 96.725) < 0.001);
  assert.ok(Math.abs(flesch(['O gato bebe leite. A casa é grande.']) - 107.3) < 0.001);
});

test('a pergunta de remédio contínuo não se limita ao que é diário', () => {
  // Bisfosfonato semanal, ácido zoledrônico anual, denosumabe semestral: é o que
  // o dentista precisa saber antes de extração ou implante (osteonecrose dos
  // maxilares). "Toma algum remédio todo dia?" fazia quem toma um deles deixar
  // o campo em branco — a legibilidade não pode custar o sentido clínico.
  assert.match(TRIAGEM.medicamentoContinuo, /mesmo que não seja todo dia/);
});

test('a triagem de saúde se lê sem esforço', () => {
  // O paciente preenche isto sozinho no pré-cadastro. "Cardíacos",
  // "hipertensão" e "uso contínuo" davam 58,1 — passariam num piso de 50.
  const indice = flesch(Object.values(TRIAGEM));
  assert.ok(indice >= 75, `índice ${indice.toFixed(1)}; "muito fácil" começa em 75`);
});
