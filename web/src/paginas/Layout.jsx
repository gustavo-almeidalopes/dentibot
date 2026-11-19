import { RedirectToSignIn, Show, UserButton } from '@clerk/react';
import { createContext, useContext, useState } from 'react';
import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import { Esqueleto } from '../components/primitivos.jsx';
import { useRecurso } from '../dados.js';
import { CADASTRO, MENU, recursoDaTela } from '../rotas.js';
import { aplicarTema, lerTema } from '../tema.js';

/* Espelha os tokens de `:root[data-tema="claro"]:has(.app-main)` em app.css. O
   Clerk não lê variável CSS nossa; se a paleta clara mudar lá, muda aqui. No
   escuro não precisa de nada: é a aparência do provider, a mesma da landing. */
const APARENCIA_CLARA = {
  colorBackground: '#ffffff',
  colorForeground: '#111111',
  colorMuted: '#faf9f7',
  colorMutedForeground: '#5c5c5c',
  colorPrimary: '#111111',
  colorPrimaryForeground: '#ffffff',
  colorInput: '#ffffff',
  colorInputForeground: '#111111',
  colorBorder: 'rgba(0, 0, 0, .22)',
  colorDanger: '#ed1c24',
  borderRadius: '0px',
};

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
    <Casca pode={pode} papel={eu.dados.papel} permissoes={eu.dados.permissoes}>
      {/* Esconder o item do menu não impede digitar a URL, e o 403 do serviço
          viraria uma tela de erro técnica. Isto responde a mesma negativa em
          português, sem nunca ser a razão pela qual o acesso foi negado. */}
      {recurso === null || pode(recurso)
        ? <Outlet />
        : <SemAcesso papel={eu.dados.papel} />}
    </Casca>
  );
}

/* Separada do Autenticado para o estado do tema não morar junto da carga do
   /eu: trocar de tema re-renderiza a barra e a tela, não refaz a chamada. */
function Casca({ pode, papel, permissoes, children }) {
  const [tema, setTema] = useState(lerTema);
  const claro = tema === 'claro';

  const alternar = () => {
    const novo = claro ? 'escuro' : 'claro';
    aplicarTema(novo);
    setTema(novo);
  };

  return (
    <Permissoes.Provider value={permissoes}>
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
              className={({ isActive }) => `app-aba${isActive ? ' app-aba-ativa' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="app-conta">
          {papel && <p className="cap cap-ash app-papel">{papel.toLowerCase()}</p>}
          {/* Rótulo fixo e aria-pressed: o botão diz o que é, e o estado diz se
              está ligado — "Escuro"/"Claro" trocando de texto não diz se o
              rótulo é o tema atual ou o próximo. */}
          <button type="button" className="btn btn-sm" aria-pressed={claro} onClick={alternar}>
            Tema claro
          </button>
          {/* O provider inteiro tem a aparência preta da landing. No tema claro
              o popover do Clerk entraria preto sobre papel. */}
          <UserButton appearance={claro ? { variables: APARENCIA_CLARA } : undefined} />
        </div>
      </header>

      <main id="main" className="app-main edge">{children}</main>
    </Permissoes.Provider>
  );
}

function SemAcesso({ papel }) {
  return (
    <div className="vazio" role="alert">
      <h1 className="display display-sm titulo-tela"><Linha>Sem acesso.</Linha></h1>
      <p className="body body-ash">
        O papel {papel ? <b>{papel.toLowerCase()}</b> : 'atual'} não alcança esta tela.
        Quem administra a clínica pode mudar isso em Equipe.
      </p>
    </div>
  );
}

/** Os três estados de carga, num componente só. */
export function Estado({ status, erro, vazio, esqueleto, children, onTentarDeNovo }) {
  if (status === 'carregando') {
    /* Quem passa a forma do que vai chegar não vê a página saltar quando ela
       chega — e o salto é o que faz alguém clicar no lugar errado. Quem não
       passa continua com o texto, então nenhuma tela quebrou na troca. */
    return esqueleto
      ? <Esqueleto linhas={esqueleto.linhas} colunas={esqueleto.colunas} />
      : <p className="body body-ash" aria-live="polite">Carregando…</p>;
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
          <p className="cap cap-ash">
            Referência: <code>{erro.data.correlacaoId}</code>{' '}
            {/* O suporte pede este número por telefone. Ler 36 caracteres de
                UUID em voz alta é onde a pessoa desiste e desliga. */}
            <button
              type="button"
              className="btn btn-sm"
              onClick={() => navigator.clipboard?.writeText(erro.data.correlacaoId)}
            >
              Copiar
            </button>
          </p>
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
    /* `vazio` aceita só a frase, ou { titulo, texto } quando o vazio é uma
       resposta e não um defeito — "Dia livre." diz mais que "nenhuma". */
    const { titulo, texto } = typeof vazio === 'string' ? { texto: vazio } : vazio;
    return (
      <div className="vazio">
        {titulo && <p className="vazio-titulo">{titulo}</p>}
        <p className="body">{texto}</p>
      </div>
    );
  }
  return children;
}

/* A máscara de onde o título sobe. As classes `.ln` são as do <Lines> da
   landing; a animação é CSS puro, porque o título já nasce na tela e o
   IntersectionObserver do Reveal não teria nada a observar. */
const Linha = ({ children }) => <span className="ln"><i>{children}</i></span>;

/**
 * Cabeçalho de tela: o nome dela no corpo da landing, e uma linha de contexto.
 *
 * <p>`secao` faz o mesmo papel para um bloco dentro da tela (Recebíveis, abaixo
 * dos indicadores) — h2, menor, sem subir: só o título da tela anuncia a rota.
 */
export function Cabecalho({ titulo, detalhe, acao, secao = false }) {
  const Titulo = secao ? 'h2' : 'h1';
  return (
    <div className={`app-cabecalho${secao ? ' app-cabecalho-secao' : ''}`}>
      <div>
        <Titulo className={`display ${secao ? 'display-xs' : 'display-sm titulo-tela'}`}>
          {secao ? titulo : <Linha>{titulo}</Linha>}
        </Titulo>
        {detalhe && <p className="credit">{detalhe}</p>}
      </div>
      {acao}
    </div>
  );
}
