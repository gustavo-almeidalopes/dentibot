/* Service worker do DentiBot (ST-54): a agenda do dia continua legível quando o
   consultório perde a internet.

   Offline, o app de verdade não abre — o login depende do Clerk, que é rede.
   Então a navegação sem rede cai numa página estática (agenda-offline.html)
   que lê daqui a última agenda boa, sem sessão, sem React, sem Clerk.

   O que fica guardado, e nada além:
   · a agenda (GET /consultas) e a lista de dentistas — rede primeiro, a última
     resposta boa fica para quando a rede cair;
   · a página offline e o script dela.
   Prontuário, financeiro e o resto NÃO vão para o disco do navegador: dado
   clínico em cache de computador compartilhado de recepção é vazamento
   esperando acontecer. E a agenda salva é apagada quando a sessão termina ou
   troca de usuário (SessaoOffline.jsx).

   Escrita offline não existe de propósito: POST sem rede falha na cara de quem
   clicou, em vez de entrar numa fila que ninguém vê. */

const VERSAO = 'v1';
const CASCA = `dentibot-casca-${VERSAO}`;
const AGENDA = 'dentibot-agenda';
const PAGINA_OFFLINE = '/agenda-offline.html';

// O que pode ir para o disco. Mudar esta linha é decisão de LGPD, não de cache.
const GUARDA = /\/api\/v1\/(consultas|equipe\/dentistas)(\?|$)/;

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CASCA)
    .then((c) => c.addAll([PAGINA_OFFLINE, '/agenda-offline.js']))
    .then(() => self.skipWaiting()));
});

self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys()
    .then((nomes) => Promise.all(nomes
      .filter((n) => n.startsWith('dentibot-casca-') && n !== CASCA)
      .map((n) => caches.delete(n))))
    .then(() => self.clients.claim()));
});

/* A cópia guardada leva a hora em que foi salva: "agenda de 7h40" é
   informação; uma agenda sem data é uma mentira possível. */
async function guardar(request, resposta) {
  const cabecalhos = new Headers(resposta.headers);
  cabecalhos.set('x-dentibot-salvo-em', new Date().toISOString());
  const copia = new Response(await resposta.clone().blob(),
    { status: resposta.status, headers: cabecalhos });
  const cache = await caches.open(AGENDA);
  await cache.put(request.url, copia);
}

self.addEventListener('fetch', (e) => {
  const { request } = e;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);

  if (request.mode === 'navigate') {
    e.respondWith(fetch(request).catch(() => caches.match(PAGINA_OFFLINE)));
    return;
  }
  if (GUARDA.test(url.pathname + url.search)) {
    e.respondWith(fetch(request).then((r) => {
      if (r.ok) e.waitUntil(guardar(request, r));
      return r;
    }).catch(() => caches.open(AGENDA)
      .then((c) => c.match(request.url))
      .then((salvo) => salvo ?? Response.error())));
  }
});
