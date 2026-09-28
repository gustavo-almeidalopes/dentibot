import { RedirectToSignIn, Show, UserButton } from '@clerk/react';
import { createContext, Suspense, useContext } from 'react';
import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import { semApi } from '../api.js';
import { useSemConexao } from '../components/SessaoOffline.jsx';
import { Esqueleto } from '../components/primitivos.jsx';
import { useRecurso } from '../dados.js';
import { CADASTRO, itensDoMenu, recursoDaTela } from '../rotas.js';

/* Espelha os tokens de `:root:has(.app-main)` em app.css. O Clerk não lê
   variável CSS nossa; se a paleta do tema claro mudar lá, muda aqui também. */
const APARENCIA_CLARA = {
  colorBackground: '#ffffff',
  colorForeground: '#111111',
  colorMuted: '#faf9f7',
  colorMutedForeground: '#5c5c5c',
  colorPrimary: '#111111',
  colorPrimaryForeground: '#ffffff',
  colorInput: '#ffffff',
  colorInputForeground: '#111111',
  /* Controle, não hairline: preto a 22% dava 1,69:1 nos campos do perfil.
     #5c5c5c é a --tinta-fraca do tema claro. */
  colorBorder: '#5c5c5c',
  colorDanger: '#ed1c24',
  borderRadius: '0px',
};

/* O Clerk desenha o campo com um box-shadow do colorBorder a 11%, que some no
   branco. O anel vai declarado, opaco e com !important, como no ComClerk. */
const CAMPOS_CLAROS = {
  formFieldInput: {
    boxShadow: '0 0 0 1px #5c5c5c !important',
    '&:focus': { boxShadow: '0 0 0 2px #111111 !important' },
  },
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

  /* A casca existe em todo estado do /eu; o que depende dele é o menu e a tela.
     Antes, qualquer falha aqui trocava a página inteira por "Erro 404" e um
     botão: sem menu, sem sair, sem voltar ao site — as telas do sistema ficavam
     sem ligação nenhuma entre si. */
  const permissoes = eu.status === 'ok' ? eu.dados.permissoes : null;
  const papel = eu.status === 'ok' ? eu.dados.papel : null;
  const pode = podeCom(permissoes);
  const recurso = recursoDaTela(pathname);

  let conteudo;
  if (eu.status !== 'ok') {
    conteudo = semApi(eu.erro)
      ? <SemApi />
      : <Estado status={eu.status} erro={eu.erro} onTentarDeNovo={eu.recarregar} />;
  } else if (recurso === null || pode(recurso)) {
    /* A tela é um chunk à parte. O Suspense é daqui, e não o de fora: o de
       fora desmontaria a casca, e o tema claro piscaria para o preto a cada
       troca de aba. */
    conteudo = (
      <Suspense fallback={<Estado status="carregando" />}>
        <Outlet />
      </Suspense>
    );
  } else {
    /* Esconder o item do menu não impede digitar a URL, e o 403 do serviço
       viraria uma tela de erro técnica. Isto responde a mesma negativa em
       português, sem nunca ser a razão pela qual o acesso foi negado. */
    conteudo = <SemAcesso papel={papel} />;
  }

  return (
    <Permissoes.Provider value={permissoes}>
      <a href="#main" className="skip-link">Ir para o conteúdo</a>

      <header className="app-topo edge">
        <a href="/" className="mbar-mark" aria-label="DentiBot — página inicial">DentiBot</a>

        <nav className="app-nav" aria-label="Navegação do sistema">
          {itensDoMenu(eu.status, pode).map((item) => (
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

        {papel && <p className="cap cap-ash app-papel">{papel.toLowerCase()}</p>}
        {/* O provider inteiro está com aparência preta, para a landing e o
            login. Dentro do app o cabeçalho é claro, e o popover do Clerk
            entraria preto sobre papel. */}
        <UserButton appearance={{ variables: APARENCIA_CLARA, elements: CAMPOS_CLAROS }} />
      </header>

      <main id="main" className="app-main edge">
        <SemConexao />
        {conteudo}
      </main>
    </Permissoes.Provider>
  );
}

/* Quem respondeu no lugar da API foi o servidor deste site (ver `semApi`). Não
   é falha passageira: o build aponta para um endereço sem back-end, e nenhum
   "Tentar de novo" resolve isso sem publicar de novo. Por isso a mensagem é
   para quem publica, como a da chave ausente no ComClerk. */
function SemApi() {
  return (
    <div className="vazio" role="alert">
      <h1 className="display display-sm">API ausente.</h1>
      <p className="body body-ash">
        O login funcionou, mas quem respondeu no lugar da API foi o servidor deste site, com
        404: ele publica só a interface. Publique o back-end, defina <code>VITE_API_BASE</code>{' '}
        com o endereço dele no build (Vercel → Environment Variables) e publique o site de novo
        — o passo a passo está no README, na parte de publicar o <code>web/</code> na Vercel.
      </p>
    </div>
  );
}

function SemAcesso({ papel }) {
  return (
    <div className="vazio" role="alert">
      <h1 className="display display-sm">Sem acesso.</h1>
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
        <p className="aviso" data-tom="erro">
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

/* Sem rede, cada tela vira erro de carregamento. Dizer o porquê, e onde está
   a agenda que ficou salva (ST-54), vale mais que o "tente de novo". */
function SemConexao() {
  if (!useSemConexao()) return null;
  return (
    <p className="faixa-offline" role="status">
      Sem conexão. Nada do que você fizer agora é enviado.{' '}
      <a href="/agenda-offline.html">Ver a agenda salva</a>
    </p>
  );
}
