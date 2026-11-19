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
 * memória do ClerkJS, é curto e se renova sozinho.
 */

import { getToken } from '@clerk/react';

/* `import.meta.env` só existe sob o Vite; o `?.` deixa o `node --test`
   importar este módulo. */
const BASE = import.meta.env?.VITE_API_BASE || '/api/v1';

/**
 * Um POST para estes caminhos sem `Idempotency-Key` é RECUSADO pelo back-end —
 * inclusive `/consultas/{id}/confirmar`, que parece inofensivo e não é.
 */
export const EXIGEM_IDEMPOTENCIA = [
  '/consultas',
  '/estoque',
  '/financeiro',
  '/orcamentos',
  '/pacientes',
];

/** O teto de toda listagem do back-end. Uma lista que veio com exatamente
    isto provavelmente não é a lista inteira. */
export const LIMITE_LISTA = 200;

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

/* getToken do SDK, não o hook: serve fora de componente, espera o ClerkJS
   carregar e devolve null sem sessão. Offline ou timeout viram ausência de
   token, e quem decide o que fazer é o 401 do back-end. */
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
    /* A chave é de uma REQUISIÇÃO LÓGICA, não de uma tentativa HTTP: quem
       repete a chamada (retry, segundo clique) precisa passar a MESMA chave,
       senão "confirmar consulta" vira duas operações. */
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
