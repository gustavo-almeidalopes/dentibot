import { api } from './api.js';

/**
 * Envio de anexo clínico (ST-41) em três passos, e nenhum deles passa os bytes
 * pela API:
 *
 *   1. o navegador calcula o SHA-256 e declara o arquivo;
 *   2. manda os bytes direto ao bucket, pela URL que a API assinou — e o bucket
 *      recusa se o hash não bater;
 *   3. pede à API para confirmar, que confere no bucket antes de registrar.
 */
export async function sha256Hex(buffer) {
  const digest = await globalThis.crypto.subtle.digest('SHA-256', buffer);
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

export async function enviarAnexo(idPaciente, tipo, arquivo) {
  const declarado = {
    tipo,
    nomeArquivo: arquivo.name,
    contentType: arquivo.type,
    tamanhoBytes: arquivo.size,
    sha256: await sha256Hex(await arquivo.arrayBuffer()),
  };
  const base = `/pacientes/${idPaciente}/prontuario/anexos`;
  const envio = await api.post(base, declarado);

  const res = await fetch(envio.url, { method: 'PUT', headers: envio.cabecalhos, body: arquivo });
  if (!res.ok) {
    throw new Error('O armazenamento recusou o arquivo. Tente de novo.');
  }
  return api.post(`${base}/confirmar`, { chave: envio.chave, anexo: declarado });
}
