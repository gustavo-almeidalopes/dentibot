import { RedirectToSignIn, Show, UserButton } from '@clerk/react';
import { createContext, useContext } from 'react';
import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import { useRecurso } from '../dados.js';
import { CADASTRO, MENU, recursoDaTela } from '../rotas.js';

/**
 * Casca das telas autenticadas.
 *
 * <p>O gate fica AQUI e não dentro de cada tela: assim o `useEffect` que busca
 * dados nem monta para quem está deslogado, em vez de disparar a chamada e
 * descartar o 401 depois. É a mesma decisão que a versão anterior tomava em
 * Clientes.jsx, agora num lugar só para as seis telas.
 */
export default function Layout() {
  return (
    <>
      <Show when="signed-out">
        <RedirectToSignIn />
      </Show>

      <Show when="signed-in">
        <Autenticado />
      </Show>
    </>
  );
}

/* Recurso → ação → alcance, como o back-end calculou para ESTE token. Guardado
   em contexto porque as telas também precisam (esconder "Novo paciente" de quem
   só lê), e buscar de novo em cada uma seria a mesma resposta quatro vezes. */
const Permissoes = createContext(null);

/* A única leitura da resposta do /eu, e ela é burra de propósito: quem decidiu
   já foi o AvaliadorDePermissao do back-end. Traduzir "NENHUM" em booleano é o
   limite do que o front sabe sobre permissão — qualquer regra a mais aqui é a
   segunda fonte de verdade que a invariante 3 proíbe. */
const podeCom = (permissoes) => (recurso, acao = 'LER') =>
  (permissoes?.[recurso]?.[acao] ?? 'NENHUM') !== 'NENHUM';

/** `const pode = usePode(); pode('PACIENTE', 'CRIAR')` dentro de qualquer tela. */
export function usePode() {
  return podeCom(useContext(Permissoes));
}

function Autenticado() {
  const eu = useRecurso('/eu');
  const { pathname } = useLocation();

  /* Negativa aqui não é erro: é o intervalo legítimo entre criar a conta no
     Clerk e cadastrar a clínica — existe token, não existe linha em
     `identidade.usuarios`. Sem este desvio a pessoa que entra com Google cai
     numa tela de erro em vez do formulário que resolve o problema dela.

     403 e não só 401 porque a CadeiaDeSeguranca não configura
     authenticationEntryPoint: com httpBasic e formLogin desligados, o
     ExceptionTranslationFilter do Spring cai no Http403ForbiddenEntryPoint e
     responde 403 para quem não tem Authentication no contexto. Medido, não
     suposto — a primeira versão disto testava 401 e nunca desviava.

     Só vale para o /eu, e só porque este componente monta apenas quando o Clerk
     diz que há sessão: aqui 403 significa "sem conta nesta base", nunca "sem
     permissão", porque o endpoint não consulta a matriz. */
  if (eu.status === 'erro' && (eu.erro?.status === 401 || eu.erro?.status === 403)) {
    return <Navigate to={CADASTRO} replace />;
  }

  /* Sem o /eu não há menu: montar a casca com todos os itens e removê-los quando
     a resposta chegasse mostraria, por um instante, telas que a pessoa não
     alcança — e um clique é mais rápido que um instante. */
  if (eu.status !== 'ok') {
    return (
      <main id="main" className="app-main edge">
        <Estado status={eu.status} erro={eu.erro} onTentarDeNovo={eu.recarregar} />
      </main>
    );
  }

  const pode = podeCom(eu.dados.permissoes);
  const recurso = recursoDaTela(pathname);

  return (
    <Permissoes.Provider value={eu.dados.permissoes}>
      <a href="#main" className="skip-link">Ir para o conteúdo</a>

      <header className="app-topo edge">
        <a href="/" className="mbar-mark" aria-label="DentiBot — página inicial">DentiBot</a>

        <nav className="app-nav" aria-label="Navegação do sistema">
          {MENU.filter((item) => pode(item.recurso)).map((item) => (
            <NavLink
              key={item.href}
              to={item.href}
              /* aria-current vem do NavLink; a classe é só o estilo. Um item
                 ativo marcado apenas por cor não existe para leitor de tela. */
              className={({ isActive }) => `btn${isActive ? ' btn-fill' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <UserButton />
      </header>

      <main id="main" className="app-main edge">
        {/* Esconder o item do menu não impede digitar a URL, e o 403 do serviço
            viraria uma tela de erro técnica. Isto responde a mesma negativa em
            português, sem nunca ser a razão pela qual o acesso foi negado. */}
        {recurso === null || pode(recurso)
          ? <Outlet />
          : <SemAcesso papel={eu.dados.papel} />}
      </main>
    </Permissoes.Provider>
  );
}

function SemAcesso({ papel }) {
  return (
    <div role="alert">
      <h1 className="display display-sm">Sem acesso.</h1>
      <p className="body body-ash">
        O papel {papel ? <b>{papel.toLowerCase()}</b> : 'atual'} não alcança esta tela.
        Quem administra a clínica pode mudar isso em Equipe.
      </p>
    </div>
  );
}

/** Os três estados de carga, num componente só. */
export function Estado({ status, erro, vazio, children, onTentarDeNovo }) {
  if (status === 'carregando') {
    return <p className="body body-ash" aria-live="polite">Carregando…</p>;
  }
  if (status === 'erro') {
    return (
      <div aria-live="assertive">
        <p className="body" style={{ color: 'var(--alarm)' }}>
          {erro?.message || 'Não foi possível carregar.'}
        </p>
        {/* O correlacaoId vem do ProblemDetail do back-end e é o que o suporte
            usa para achar a linha de log exata sem pedir print de tela. */}
        {erro?.data?.correlacaoId && (
          <p className="cap cap-ash">Referência: {erro.data.correlacaoId}</p>
        )}
        {onTentarDeNovo && (
          <button type="button" className="btn" onClick={onTentarDeNovo}>
            Tentar de novo
          </button>
        )}
      </div>
    );
  }
  if (vazio) {
    return <p className="body body-ash">{vazio}</p>;
  }
  return children;
}

/** Cabeçalho de tela: título grande e uma linha de contexto. */
export function Cabecalho({ titulo, detalhe, acao }) {
  return (
    <div className="app-cabecalho">
      <div>
        <h1 className="display display-sm">{titulo}</h1>
        {detalhe && <p className="credit">{detalhe}</p>}
      </div>
      {acao}
    </div>
  );
}
