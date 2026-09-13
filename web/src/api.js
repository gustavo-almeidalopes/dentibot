/**
 * Cliente do back-end DentiBot.
 *
 * <p>Um `req` genérico em vez de um método por rota: a lista de endpoints vive
 * no OpenAPI do back-end, não duplicada aqui.
 *
 * <p>Em dev o Vite faz proxy de /api para o back-end, então não há CORS. Em
 * produção, defina VITE_API_BASE se o back-end estiver em outro domínio.
 *
 * <p>Autenticação é do Clerk. Não há token em localStorage: o de sessão vive em
 * memória do ClerkJS, é curto e se renova sozinho — guardar credencial legível
 * no browser era exatamente o que o XSS levava embora.
 */

import { getToken } from '@clerk/react';

/* O prefixo é /api/v1 e não /api. O back-end monta todo controller sob
   /api/v1/... desde a V2; a versão anterior deste arquivo chamava /api/clients,
   que é rota de uma API .NET que não existe mais — 404 antes mesmo do 401. */
/* O `?.` não é paranoia: `import.meta.env` só existe sob o Vite, e sem ele
   este módulo não pode ser importado pelo `node --test` — que é justamente
   quem confere a lista de idempotência contra o fonte Java. */
const BASE = import.meta.env?.VITE_API_BASE || '/api/v1';

/**
 * Espelha `PREFIXOS_OBRIGATORIOS` do `FiltroIdempotencia` do back-end. Um POST
 * para estes caminhos sem `Idempotency-Key` é RECUSADO — inclusive
 * `/consultas/{id}/confirmar`, que parece inofensivo e não é.
 *
 * <p>Conferido por máquina em `api.test.mjs`, contra o fonte Java. Duplicata
 * silenciosa é a que apodrece: o back-end ganha um prefixo, o front continua
 * mandando sem a chave, e o usuário toma 400 sem entender.
 */
export const EXIGEM_IDEMPOTENCIA = [
  '/consultas',
  '/cobrancas',
  '/mensagens',
  '/orcamentos',
];

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

/* getToken do próprio SDK, não o hook: este módulo não é componente e o helper
   existe justamente para camada de dados — ele espera o ClerkJS carregar e
   devolve null se não há sessão. Offline ou timeout viram ausência de token, e
   quem decide o que fazer com isso é o 401 do back-end. */
async function tokenDeSessao() {
  try {
    return (await getToken()) ?? '';
  } catch {
    return '';
  }
}

const precisaDeChave = (metodo, caminho) =>
  metodo === 'POST' && EXIGEM_IDEMPOTENCIA.some((p) => caminho.startsWith(p));

async function req(metodo, caminho, corpo, { idempotencyKey } = {}) {
  const headers = {};
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';

  const token = await tokenDeSessao();
  if (token) headers.Authorization = `Bearer ${token}`;

  if (precisaDeChave(metodo, caminho)) {
    /* A chave é de uma REQUISIÇÃO LÓGICA, não de uma tentativa HTTP. Quem
       repete a chamada — um retry, um segundo clique — precisa passar a MESMA
       chave, senão "confirmar consulta" vira duas operações, que é exatamente o
       que o filtro existe para impedir. Por isso ela é parâmetro, e gerar uma
       nova aqui é só o caminho de quem não repetiu nada. */
    headers['Idempotency-Key'] = idempotencyKey ?? crypto.randomUUID();
  }

  const res = await fetch(`${BASE}${caminho}`, {
    method: metodo,
    headers,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });

  const data = res.status === 204 ? null : await res.json().catch(() => null);

  if (!res.ok) {
    /* O back-end responde RFC 7807: `detail` tem a frase pronta e
       `correlacaoId` é o que o suporte usa para achar a linha de log exata sem
       pedir print de tela. */
    throw new ApiError(data?.detail || `Erro ${res.status}`, res.status, data);
  }
  return data;
}

export const api = {
  get: (caminho) => req('GET', caminho),
  post: (caminho, corpo, opcoes) => req('POST', caminho, corpo ?? {}, opcoes),
  put: (caminho, corpo) => req('PUT', caminho, corpo ?? {}),
  del: (caminho) => req('DELETE', caminho),
};

/** Monta querystring pulando o que estiver vazio. */
export function query(params) {
  const q = new URLSearchParams();
  for (const [chave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== null && valor !== '') q.set(chave, valor);
  }
  const s = q.toString();
  return s ? `?${s}` : '';
}
