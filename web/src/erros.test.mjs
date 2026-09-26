import assert from 'node:assert/strict';
import test from 'node:test';
import { limparEvento, limparTexto } from './erros.js';

test('CPF, e-mail e celular saem do texto', () => {
  const t = limparTexto('falhou 123.456.789-09 ana@clinica.com.br (11) 91234-5678');
  assert.doesNotMatch(t, /123\.456|ana@|91234/);
});

test('o evento sai sem PII na mensagem, na exceção e na query da URL', () => {
  const e = limparEvento({
    message: 'erro com 12345678909',
    exception: { values: [{ value: 'duplicado: ana@clinica.com.br' }] },
    request: { url: 'https://app.dentibot.com.br/pacientes?busca=Ana%20Souza' },
    breadcrumbs: [{ message: 'digitou Ana Souza' }],
  });
  assert.doesNotMatch(JSON.stringify(e), /12345678909|ana@|Ana/);
  assert.equal(e.request.url, 'https://app.dentibot.com.br/pacientes');
});
