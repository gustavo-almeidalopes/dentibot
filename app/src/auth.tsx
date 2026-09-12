/**
 * Sessão do app.
 *
 * Na abertura o app NÃO sabe se tem sessão: o refresh está num cookie que o
 * JavaScript não lê. Então ele pergunta — um POST /auth/refresh. Se vier 200,
 * havia sessão; se vier 401, não havia. Uma requisição para descobrir é o preço
 * de não guardar credencial legível no aparelho.
 */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import {
  api,
  definirAccessToken,
  definirHandlerDeSessaoExpirada,
  pedir,
  type Identidade,
  type RespostaLogin,
} from './api';

type Estado =
  | { situacao: 'carregando' }
  | { situacao: 'anonimo' }
  | { situacao: 'autenticado'; identidade: Identidade };

type Contexto = Estado & {
  entrar: (email: string, senha: string) => Promise<void>;
  sair: () => Promise<void>;
};

const Ctx = createContext<Contexto | null>(null);

export function ProvedorDeAuth({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<Estado>({ situacao: 'carregando' });

  const derrubar = useCallback(() => {
    definirAccessToken(null);
    setEstado({ situacao: 'anonimo' });
  }, []);

  // Qualquer requisição que esgote o refresh derruba a sessão aqui, em vez de
  // cada tela ter de tratar 401 por conta própria.
  useEffect(() => {
    definirHandlerDeSessaoExpirada(derrubar);
    return () => definirHandlerDeSessaoExpirada(null);
  }, [derrubar]);

  useEffect(() => {
    let vivo = true;

    (async () => {
      try {
        const r = await pedir<RespostaLogin>('/api/v1/auth/refresh', {
          metodo: 'POST',
          semAuth: true,
        });
        definirAccessToken(r.accessToken);
        const identidade = await api.eu();
        if (vivo) setEstado({ situacao: 'autenticado', identidade });
      } catch {
        // 401 (não havia sessão) e falha de rede caem no mesmo lugar: a tela de
        // login, que é o único caminho de saída em ambos os casos.
        definirAccessToken(null);
        if (vivo) setEstado({ situacao: 'anonimo' });
      }
    })();

    return () => {
      vivo = false;
    };
  }, []);

  const entrar = useCallback(async (email: string, senha: string) => {
    const r = await api.login(email.trim().toLowerCase(), senha);
    definirAccessToken(r.accessToken);
    // `me` confirma o que o token carrega em vez de o app decodificar o JWT por
    // conta própria — decodificar no cliente é como se acredita numa claim que
    // não foi verificada.
    const identidade = await api.eu();
    setEstado({ situacao: 'autenticado', identidade });
  }, []);

  const sair = useCallback(async () => {
    try {
      await api.logout();
    } catch {
      // Servidor inalcançável não pode prender o usuário dentro do app: a
      // sessão local cai de todo jeito, e o access token expira em minutos.
    }
    derrubar();
  }, [derrubar]);

  const valor = useMemo<Contexto>(() => ({ ...estado, entrar, sair }), [estado, entrar, sair]);

  return <Ctx.Provider value={valor}>{children}</Ctx.Provider>;
}

export function useAuth(): Contexto {
  const c = useContext(Ctx);
  if (!c) throw new Error('useAuth exige <ProvedorDeAuth> acima na árvore.');
  return c;
}
