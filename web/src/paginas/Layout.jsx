import { RedirectToSignIn, Show, UserButton } from '@clerk/react';
import { NavLink, Outlet } from 'react-router-dom';
import { MENU } from '../rotas.js';

/**
 * Casca das telas autenticadas.
 *
 * <p>O gate fica AQUI e não dentro de cada tela: assim o `useEffect` que busca
 * dados nem monta para quem está deslogado, em vez de disparar a chamada e
 * descartar o 401 depois. É a mesma decisão que a versão anterior tomava em
 * Clientes.jsx, agora num lugar só para as cinco telas.
 */
export default function Layout() {
  return (
    <>
      <Show when="signed-out">
        <RedirectToSignIn />
      </Show>

      <Show when="signed-in">
        <a href="#main" className="skip-link">Ir para o conteúdo</a>

        <header className="app-topo edge">
          <a href="/" className="mbar-mark" aria-label="DentiBot — página inicial">DentiBot</a>

          <nav className="app-nav" aria-label="Navegação do sistema">
            {MENU.map((item) => (
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
          <Outlet />
        </main>
      </Show>
    </>
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
