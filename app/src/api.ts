/**
 * Cliente da API do DentiBot.
 *
 * Três decisões que valem uma linha de explicação cada:
 *
 * 1. **O access token vive em memória, nunca em disco.** Ele dura 15 minutos
 *    (`validade-access: PT15M`) e persistir isso só criaria uma cópia de
 *    credencial para alguém achar. Fechar o app derruba a variável; o refresh
 *    é que reconstrói a sessão.
 *
 * 2. **O refresh viaja em cookie HttpOnly**, como no SPA — o backend só o lê de
 *    `@CookieValue("dentibot_refresh")`. O fetch do React Native usa o cookie
 *    store nativo (NSHTTPCookieStorage no iOS, CookieManager no Android), que
 *    persiste entre execuções. Nenhum código aqui toca no token: é ilegível
 *    para o JavaScript do app, o que é exatamente a propriedade desejada.
 *
 *    ponytail: cookie jar nativo. Trocar por expo-secure-store quando o
 *    backend aceitar o refresh no corpo — é o que MASVS pede, e é o único jeito
 *    de o app saber se tem sessão antes de gastar uma requisição descobrindo.
 *
 * 3. **A chave de idempotência é gerada uma vez por requisição lógica**, não por
 *    tentativa HTTP. Se um 401 disparar refresh e retry, a segunda tentativa
 *    manda a MESMA chave — senão o retry de "confirmar consulta" viraria uma
 *    segunda operação, que é precisamente o que o `FiltroIdempotencia` existe
 *    para impedir.
 */
import * as Crypto from 'expo-crypto';

const BASE = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';

/**
 * Espelha `PREFIXOS_OBRIGATORIOS` do `FiltroIdempotencia`. Um POST para estes
 * caminhos sem `Idempotency-Key` é recusado pelo backend — inclusive
 * `/consultas/{id}/confirmar`, que parece inofensivo e não é.
 */
const EXIGEM_IDEMPOTENCIA = [
  '/api/v1/consultas',
  '/api/v1/cobrancas',
  '/api/v1/mensagens',
  '/api/v1/orcamentos',
];

// ─── Estado de sessão ────────────────────────────────────────────────────────

let accessToken: string | null = null;
let aoExpirarSessao: (() => void) | null = null;
/** Um refresh por vez: cinco 401 simultâneos não viram cinco renovações. */
let renovacaoEmCurso: Promise<boolean> | null = null;

export function definirAccessToken(token: string | null): void {
  accessToken = token;
}

/** Chamado quando o refresh falha: a camada de auth derruba a sessão na UI. */
export function definirHandlerDeSessaoExpirada(fn: (() => void) | null): void {
  aoExpirarSessao = fn;
}

export class ErroApi extends Error {
  constructor(
    readonly status: number,
    mensagem: string,
  ) {
    super(mensagem);
    this.name = 'ErroApi';
  }
}

// ─── Transporte ──────────────────────────────────────────────────────────────

type Opcoes = {
  metodo?: 'GET' | 'POST';
  corpo?: unknown;
  /** Login e refresh não mandam Bearer nem tentam renovar. */
  semAuth?: boolean;
};

async function enviar(
  caminho: string,
  o: Opcoes,
  chaveIdempotencia: string | null,
): Promise<Response> {
  const metodo = o.metodo ?? 'GET';
  const cabecalhos: Record<string, string> = {
    Accept: 'application/json',
    // Atravessa requisição → evento → worker → banco. É o que permite
    // responder "sumiu a agenda de ontem" com log em vez de chute.
    'X-Correlation-Id': Crypto.randomUUID(),
  };
  if (o.corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
  if (!o.semAuth && accessToken) cabecalhos.Authorization = `Bearer ${accessToken}`;
  if (chaveIdempotencia) cabecalhos['Idempotency-Key'] = chaveIdempotencia;

  return fetch(BASE + caminho, {
    method: metodo,
    headers: cabecalhos,
    body: o.corpo === undefined ? undefined : JSON.stringify(o.corpo),
    // Manda o cookie de refresh. É o padrão no RN, mas explícito documenta.
    credentials: 'include',
  });
}

async function renovar(): Promise<boolean> {
  if (renovacaoEmCurso) return renovacaoEmCurso;

  renovacaoEmCurso = (async () => {
    try {
      const r = await enviar('/api/v1/auth/refresh', { metodo: 'POST', semAuth: true }, null);
      if (!r.ok) return false;
      const dados = (await r.json()) as RespostaLogin;
      accessToken = dados.accessToken;
      return true;
    } catch {
      // Rede caiu no meio da renovação. Não é sessão inválida — mas daqui não
      // há como distinguir, e tratar como expirada só custa um login.
      return false;
    } finally {
      renovacaoEmCurso = null;
    }
  })();

  return renovacaoEmCurso;
}

async function interpretar<T>(r: Response): Promise<T> {
  if (r.status === 204) return undefined as T;

  const texto = await r.text();
  let dados: unknown = null;
  if (texto) {
    try {
      dados = JSON.parse(texto);
    } catch {
      // Resposta não-JSON (proxy, 502 de gateway). O status é o que informa.
    }
  }

  if (!r.ok) {
    // O backend devolve ProblemDetail (RFC 9457): a mensagem útil está em
    // `detail`. Nunca inventar texto por cima: "Credenciais inválidas." é
    // deliberadamente vago no servidor, e a UI não deve tentar ser mais
    // específica do que ele.
    const detalhe =
      typeof dados === 'object' && dados !== null && 'detail' in dados
        ? String((dados as { detail: unknown }).detail)
        : mensagemPadrao(r.status);
    throw new ErroApi(r.status, detalhe);
  }

  return dados as T;
}

function mensagemPadrao(status: number): string {
  if (status === 401) return 'Sessão expirada.';
  if (status === 403) return 'Seu perfil não tem acesso a esta ação.';
  if (status === 429) return 'Muitas tentativas. Aguarde um instante.';
  if (status >= 500) return 'A API falhou. Tente de novo em instantes.';
  return `Falha inesperada (${status}).`;
}

export async function pedir<T>(caminho: string, o: Opcoes = {}): Promise<T> {
  const chave =
    (o.metodo ?? 'GET') === 'POST' && EXIGEM_IDEMPOTENCIA.some((p) => caminho.startsWith(p))
      ? Crypto.randomUUID()
      : null;

  let r: Response;
  try {
    r = await enviar(caminho, o, chave);
  } catch {
    throw new ErroApi(0, 'Sem conexão com a API.');
  }

  if (r.status === 401 && !o.semAuth) {
    if (await renovar()) {
      r = await enviar(caminho, o, chave);
    } else {
      accessToken = null;
      aoExpirarSessao?.();
      throw new ErroApi(401, 'Sessão expirada.');
    }
  }

  return interpretar<T>(r);
}

// ─── Contratos ───────────────────────────────────────────────────────────────
// Espelham os records do backend. Nomes idênticos de propósito: a tradução
// acontece na tela, não aqui, para que uma mudança de DTO apareça no typecheck.

export type RespostaLogin = {
  accessToken: string;
  expiraEm: string;
  tokenType: string;
};

export type Identidade = {
  usuarioId: number | null;
  clinicaId: number | null;
  papel: string | null;
  staffPapel: string | null;
};

export type StatusConsulta =
  | 'agendada'
  | 'confirmada'
  | 'em_atendimento'
  | 'realizada'
  | 'cancelada'
  | 'faltou';

export type Consulta = {
  idConsulta: number;
  idPaciente: number;
  nomePaciente: string;
  telefonePaciente: string;
  idDentista: number;
  inicioEm: string;
  terminoEm: string;
  status: StatusConsulta;
};

export type Paciente = {
  idPaciente: number;
  idPessoa: number;
  nomeCompleto: string;
  telefoneCelular: string;
  status: string;
};

// ─── Endpoints ───────────────────────────────────────────────────────────────

export const api = {
  login: (email: string, senha: string) =>
    pedir<RespostaLogin>('/api/v1/auth/login', {
      metodo: 'POST',
      corpo: { email, senha },
      semAuth: true,
    }),

  logout: () => pedir<void>('/api/v1/auth/logout', { metodo: 'POST' }),

  eu: () => pedir<Identidade>('/api/v1/auth/me'),

  /** `de`/`ate` em ISO-8601 UTC — o backend os recebe como Instant. */
  agenda: (de: Date, ate: Date) =>
    pedir<Consulta[]>(
      `/api/v1/consultas?de=${encodeURIComponent(de.toISOString())}` +
        `&ate=${encodeURIComponent(ate.toISOString())}`,
    ),

  confirmar: (id: number) => pedir<void>(`/api/v1/consultas/${id}/confirmar`, { metodo: 'POST' }),
  concluir: (id: number) => pedir<void>(`/api/v1/consultas/${id}/concluir`, { metodo: 'POST' }),
  registrarFalta: (id: number) => pedir<void>(`/api/v1/consultas/${id}/falta`, { metodo: 'POST' }),
  cancelar: (id: number, motivo: string) =>
    pedir<void>(`/api/v1/consultas/${id}/cancelar`, { metodo: 'POST', corpo: { motivo } }),

  /** Keyset: `apos` é o último idPaciente recebido, não um número de página. */
  pacientes: (apos = 0, limite = 50) =>
    pedir<Paciente[]>(`/api/v1/pacientes?limite=${limite}&apos=${apos}`),
};

// ─── Máquina de estados ──────────────────────────────────────────────────────

export type Acao = 'confirmar' | 'concluir' | 'falta' | 'cancelar';

/**
 * Quais ações a UI oferece em cada status. Espelha `AgendaServico` — mas o
 * backend é a autoridade: ele devolve 409 `transicao-invalida` se esta tabela
 * divergir, e é isso que a tela mostra. Aqui só evitamos oferecer um botão que
 * sabemos que vai falhar.
 */
export const ACOES_POR_STATUS: Record<StatusConsulta, readonly Acao[]> = {
  agendada: ['confirmar', 'concluir', 'falta', 'cancelar'],
  confirmada: ['concluir', 'falta', 'cancelar'],
  em_atendimento: ['concluir'],
  realizada: [],
  cancelada: [],
  faltou: [],
};
