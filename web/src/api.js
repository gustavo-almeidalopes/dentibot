/**
 * Cliente do back-end DentiBot.
 *
 * Um `req` genérico em vez de um método por rota: a lista de endpoints vive
 * no OpenAPI do back-end, não duplicada aqui.
 *
 * Em dev o Vite faz proxy de /api para o back-end, então não há CORS.
 * Em produção, defina VITE_API_BASE se o back-end estiver em outro domínio.
 *
 * Autenticação é do Clerk. Não há mais token em localStorage: o de sessão vive
 * em memória do ClerkJS, é curto e se renova sozinho — guardar credencial
 * legível no browser era exatamente o que o XSS levava embora.
 */

import { getToken } from '@clerk/react';

const BASE = import.meta.env.VITE_API_BASE || '/api';

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

async function req(method, path, body) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const token = await tokenDeSessao();
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const data = res.status === 204 ? null : await res.json().catch(() => null);

  if (!res.ok) {
    throw new ApiError(data?.detail || `Erro ${res.status}`, res.status, data);
  }
  return data;
}

export const api = {
  get: (path) => req('GET', path),
  post: (path, body) => req('POST', path, body ?? {}),
  put: (path, body) => req('PUT', path, body ?? {}),
  del: (path) => req('DELETE', path),
};
