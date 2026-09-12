/**
 * Cliente do back-end DentiBot.
 *
 * Um `req` genérico em vez de um método por rota: a lista de endpoints vive
 * no OpenAPI do FastAPI (http://localhost:8000/docs), não duplicada aqui.
 *
 * Em dev o Vite faz proxy de /api para o back-end, então não há CORS.
 * Em produção, defina VITE_API_BASE se o back-end estiver em outro domínio.
 */

const BASE = import.meta.env.VITE_API_BASE || '/api';
const TOKEN_KEY = 'dentibot_token';

let token = localStorage.getItem(TOKEN_KEY) || '';

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function req(method, path, body) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const data = res.status === 204 ? null : await res.json().catch(() => null);

  if (!res.ok) {
    if (res.status === 401) setToken('');
    throw new ApiError(data?.detail || `Erro ${res.status}`, res.status, data);
  }
  return data;
}

function setToken(value) {
  token = value;
  if (value) localStorage.setItem(TOKEN_KEY, value);
  else localStorage.removeItem(TOKEN_KEY);
}

export const api = {
  get: (path) => req('GET', path),
  post: (path, body) => req('POST', path, body ?? {}),
  put: (path, body) => req('PUT', path, body ?? {}),
  del: (path) => req('DELETE', path),

  get authenticated() {
    return Boolean(token);
  },

  async signup(payload) {
    const data = await req('POST', '/auth/signup', payload);
    setToken(data.access_token);
    return data.user;
  },

  async login(email, password) {
    const data = await req('POST', '/auth/login', { email, password });
    setToken(data.access_token);
    return data.user;
  },

  me: () => req('GET', '/auth/me'),

  logout() {
    setToken('');
  },
};
