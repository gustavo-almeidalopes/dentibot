/**
 * Cliente do back-end DentiBot.
 *
 * <p>Um `req` genérico em vez de um método por rota: a lista de endpoints vive
 * no OpenAPI do back-end, não duplicada aqui.
 *
 * <p>Em dev o Vite faz proxy de /api para o back-end, então não há CORS. No
 * build da Vercel o padrão é a API da Render (`vite.config.js`), e
 * VITE_API_BASE, quando definida, manda.
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
  '/estoque',
  '/financeiro',
  '/orcamentos',
  '/pacientes',
];

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

/**
 * O erro diz que não há API neste endereço — e não que a API negou algo.
 *
 * <p>Todo 404 do back-end sai em ProblemDetail, com corpo JSON
 * (`TratadorGlobalDeErros`). 404 sem corpo que se leia é o servidor da página
 * respondendo no lugar dela: o `web/` publicado na Vercel sem o back-end, ou
 * um `VITE_API_BASE` que não aponta para ele. Na tela isso virava "Erro 404"
 * dentro da /agenda, e parecia que a própria tela não existia.
 */
export const semApi = (erro) =>
  erro instanceof ApiError && erro.status === 404 && erro.data === null;

/**
 * Nem houve resposta que o navegador entregasse: o `fetch` rejeita com
 * TypeError. Com a API em outro domínio é o que sobra de "API fora do ar",
 * "CORS sem a origem deste site" e "CSP sem a origem da API" — o navegador não
 * diz qual, de propósito. Sem rede também cai aqui, e o SemConexao já avisa.
 */
export const apiInacessivel = (erro) => erro instanceof TypeError;

/** O endereço que o build gravou, para a mensagem dizer a quem ele chamou. */
export const ENDERECO_DA_API = BASE;

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

/**
 * O `fetch`, com a falha de rede dita em português. Sem resposta legível o
 * navegador rejeita com "Failed to fetch", em inglês e sem endereço, e é isso
 * que o Cadastro, o Titular e o erro de cada tela mostravam. Continua sendo
 * TypeError, então `apiInacessivel` vale igual.
 */
async function chamar(url, init) {
  try {
    return await fetch(url, init);
  } catch (erro) {
    if (!(erro instanceof TypeError)) throw erro;
    throw new TypeError(
      `A API (${BASE}) não respondeu. Se ela estava parada, acordar leva cerca de um minuto: tente de novo.`,
      { cause: erro });
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

  const res = await chamar(`${BASE}${caminho}`, {
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

/**
 * Download autenticado (IA-47, portabilidade).
 *
 * <p>Fora do `req` porque a resposta é arquivo, não JSON: quem chama precisa
 * dos bytes, do nome que o back-end deu e do SHA-256 que ele calculou — o
 * paciente confere o arquivo recebido com `sha256sum`.
 */
export async function baixar(caminho) {
  const token = await tokenDeSessao();
  const res = await chamar(`${BASE}${caminho}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    const data = await res.json().catch(() => null);
    throw new ApiError(data?.detail || `Erro ${res.status}`, res.status, data);
  }
  const disposicao = res.headers.get('Content-Disposition') ?? '';
  return {
    blob: await res.blob(),
    nomeArquivo: /filename="?([^";]+)"?/.exec(disposicao)?.[1] ?? 'exportacao.json',
    sha256: res.headers.get('X-Conteudo-Sha256'),
  };
}
