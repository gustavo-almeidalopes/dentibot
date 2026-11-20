/**
 * Erro do navegador no Sentry (ST-28), com a mesma regra do back-end
 * (ScrubberDePii): nada que identifique pessoa sai do browser.
 *
 * <p>O SDK é importado sob demanda: sem VITE_SENTRY_DSN nada entra no bundle.
 * E a CSP não libera o Sentry por curinga — quem ligar o DSN põe o host exato
 * do projeto em connect-src, nos dois vercel.json. Um curinga para
 * *.ingest.sentry.io daria a um XSS um canal de saída para o Sentry de qualquer
 * pessoa.
 */

const PII = [
  /\d{3}\.?\d{3}\.?\d{3}-?\d{2}/g,
  /[\w.+-]+@[\w-]+(\.[\w-]+)+/g,
  /\(?\d{2}\)?\s?9?\d{4}-?\d{4}/g,
];

export const limparTexto = (texto) =>
  typeof texto === 'string' ? PII.reduce((s, re) => s.replace(re, '[REMOVIDO]'), texto) : texto;

export function limparEvento(evento) {
  evento.message = limparTexto(evento.message);
  for (const e of evento.exception?.values ?? []) e.value = limparTexto(e.value);
  // Busca por nome vive na query; o caminho só tem id numérico.
  if (evento.request?.url) evento.request.url = evento.request.url.split('?')[0];
  // Breadcrumb carrega texto de console e de clique — é onde o nome escapa.
  delete evento.breadcrumbs;
  return evento;
}

export async function ligarErros(dsn) {
  if (!dsn) return;
  const Sentry = await import('@sentry/browser');
  Sentry.init({
    dsn,
    sendDefaultPii: false,
    beforeBreadcrumb: () => null,
    beforeSend: limparEvento,
  });
}
