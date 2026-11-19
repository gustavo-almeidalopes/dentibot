/**
 * Cliente da API do DentiBot.
 *
 * Três decisões que valem uma linha de explicação cada:
 *
 * 1. **A sessão é do Clerk.** Quem guarda é o `tokenCache` do @clerk/expo,
 *    sobre expo-secure-store — Keychain no iOS, Keystore no Android — e o SDK
 *    renova sozinho.
 *
 * 2. **getClerkInstance, não useAuth.** Este módulo não é componente e não tem
 *    hook. O singleton é o caminho documentado para chamar a API fora do React,
 *    e `getToken()` nele renova sozinho quando o token está perto de vencer.
 *
 * 3. **A chave de idempotência é gerada uma vez por requisição lógica**, não por
 *    tentativa HTTP. Se um 401 disparar refresh e retry, a segunda tentativa
 *    manda a MESMA chave — senão o retry de "confirmar consulta" viraria uma
 *    segunda operação.
 */
import { getClerkInstance } from '@clerk/expo';
import * as Crypto from 'expo-crypto';

const BASE = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';

/**
 * Um POST para estes caminhos sem `Idempotency-Key` é recusado pelo backend —
 * inclusive
 * `/consultas/{id}/confirmar`, que parece inofensivo e não é.
 */
const EXIGEM_IDEMPOTENCIA = [
  '/api/v1/consultas',
  '/api/v1/estoque',
  '/api/v1/financeiro',
  '/api/v1/orcamentos',
  '/api/v1/pacientes',
];

// ─── Sessão ──────────────────────────────────────────────────────────────────

/**
 * `skipCache` força ida ao Clerk em vez de devolver o token em cache. Só o
 * retry usa: o caminho normal aproveita o cache, e sem isso o retry de um 401
 * remandaria exatamente o token que acabou de ser recusado.
 */
async function tokenDeSessao(skipCache = false): Promise<string | null> {
  try {
    return (await getClerkInstance().session?.getToken({ skipCache })) ?? null;
  } catch {
    // Sem sessão, ou o SDK ainda não carregou. Ir sem Authorization e deixar o
    // 401 do backend decidir é mais honesto do que adivinhar aqui.
    return null;
  }
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
  /** Rota pública: não manda Bearer nem tenta renovar no 401. */
  semAuth?: boolean;
};

async function enviar(
  caminho: string,
  o: Opcoes,
  chaveIdempotencia: string | null,
  skipCache = false,
): Promise<Response> {
  const metodo = o.metodo ?? 'GET';
  const cabecalhos: Record<string, string> = {
    Accept: 'application/json',
    // Atravessa requisição → evento → worker → banco. É o que permite
    // responder "sumiu a agenda de ontem" com log em vez de chute.
    'X-Correlation-Id': Crypto.randomUUID(),
  };
  if (o.corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
  if (!o.semAuth) {
    const token = await tokenDeSessao(skipCache);
    if (token) cabecalhos.Authorization = `Bearer ${token}`;
  }
  if (chaveIdempotencia) cabecalhos['Idempotency-Key'] = chaveIdempotencia;

  return fetch(BASE + caminho, {
    method: metodo,
    headers: cabecalhos,
    body: o.corpo === undefined ? undefined : JSON.stringify(o.corpo),
  });
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

  // Uma tentativa só, com token fora do cache: se o Clerk continua entregando
  // sessão e o backend continua recusando, o problema não é frescor de token e
  // repetir não conserta. Quem manda a UI de volta ao login é o estado do
  // Clerk, não este 401.
  if (r.status === 401 && !o.semAuth) {
    r = await enviar(caminho, o, chave, true);
  }

  return interpretar<T>(r);
}

// ─── Contratos ───────────────────────────────────────────────────────────────
// Espelham os records do backend. Nomes idênticos de propósito: a tradução
// acontece na tela, não aqui, para que uma mudança de DTO apareça no typecheck.

/** `GET /eu`: o papel e a matriz recurso → ação → alcance deste token. */
export type Eu = {
  papel: string | null;
  staffPapel: string | null;
  permissoes: Record<string, Record<string, string>>;
};

export type Acao = 'confirmar' | 'concluir' | 'falta' | 'cancelar';

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
  /** As transições que este usuário pode fazer agora. O backend calcula. */
  acoes: Acao[];
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
  // Quem cria e derruba sessão é o Clerk. Papel e clínica são do domínio, não
  // da identidade, e só o backend os sabe.
  eu: () => pedir<Eu>('/api/v1/eu'),

  /** `de`/`ate` em ISO-8601 UTC — o backend os recebe como Instant. */
  agenda: (de: Date, ate: Date) =>
    pedir<Consulta[]>(
      `/api/v1/consultas?de=${encodeURIComponent(de.toISOString())}` +
        `&ate=${encodeURIComponent(ate.toISOString())}`,
    ),

  /** `motivo` só para `cancelar`, que o exige. */
  transicionar: (id: number, acao: Acao, motivo?: string) =>
    pedir<void>(`/api/v1/consultas/${id}/${acao}`, {
      metodo: 'POST',
      corpo: motivo === undefined ? undefined : { motivo },
    }),

  /** Keyset: `apos` é o último idPaciente recebido, não um número de página. */
  pacientes: (apos = 0, limite = 50) =>
    pedir<Paciente[]>(`/api/v1/pacientes?limite=${limite}&apos=${apos}`),
};
