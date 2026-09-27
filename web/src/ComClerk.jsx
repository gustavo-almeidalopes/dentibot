import { ptBR } from '@clerk/localizations';
import { ClerkProvider } from '@clerk/react';
import { Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import SessaoOffline from './components/SessaoOffline.jsx';
import { CRIAR, LOGIN } from './rotas.js';

/* Variáveis em vez de @clerk/themes: o tema daqui é preto, branco e canto
   vivo — sete tokens cobrem isso e não entra dependência para reescrevê-los
   depois. */
const aparencia = {
  variables: {
    colorBackground: '#000000',
    colorForeground: '#ffffff',
    colorMuted: '#000000',
    colorMutedForeground: '#838383',
    colorPrimary: '#ffffff',
    colorPrimaryForeground: '#000000',
    colorInput: '#000000',
    colorInputForeground: '#ffffff',
    /* Borda de campo é controle, não decoração: branco a 26% dava 2,10:1 sobre
       o preto, e o 1.4.11 pede 3:1. #838383 é o cinza da marca, 5,54:1. */
    colorBorder: '#838383',
    colorDanger: '#ed1c24',
    borderRadius: '0px',
    fontFamily: "'Inter', 'Neue Haas Grotesk', 'Helvetica Neue', Helvetica, sans-serif",
  },
  /* Medido no navegador: o Clerk desenha o campo com um box-shadow do
     colorBorder a 11% (28% no foco) — perto de 1,1:1 no preto, qualquer que
     seja a cor. O anel vai declarado aqui, opaco: 1px cinza em repouso
     (5,54:1) e 2px osso no foco. O !important é porque a regra de variante do
     Clerk (.cl-internal-…[data-variant]) tem mais especificidade que o
     elements, e sem ele o anel perde calado. */
  elements: {
    formFieldInput: {
      boxShadow: '0 0 0 1px #838383 !important',
      '&:focus': { boxShadow: '0 0 0 2px #ffffff !important' },
    },
    otpCodeFieldInput: {
      boxShadow: '0 0 0 1px #838383 !important',
      '&:focus': { boxShadow: '0 0 0 2px #ffffff !important' },
    },
  },
};

/* Sem a chave o ClerkProvider não carrega NADA e não reclama: o próprio SDK faz
   `else if (this.#publishableKey) this.getEntryChunks()` — chave ausente é um
   ramo vazio. Aí todo <Show> devolve null para sempre e /login sobe com a
   metade direita em branco, sem os botões do Google/Microsoft/Apple. Era um
   sintoma sem nenhuma mensagem em lugar nenhum; agora a falta da variável no
   build aparece na tela em vez de virar depuração de página vazia. */
const chaveClerk = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY;

/**
 * Tudo que precisa de sessão passa por aqui, e só isso.
 *
 * <p>Rota de layout carregada sob demanda: a landing e o 404 não montam este
 * componente, então não baixam clerk-js nem @clerk/ui — e não caem quando a
 * chave falta no build, o que antes derrubava a página de vendas junto.
 *
 * <p>O Suspense é daqui de dentro: trocar entre /login, /cadastro e o sistema
 * espera sem desmontar o ClerkProvider.
 */
export default function ComClerk() {
  if (!chaveClerk) {
    return (
      <main className="edge" style={{ padding: 'var(--spacing-30)' }} role="alert">
        <h1 className="display display-sm">Configuração ausente.</h1>
        <p className="body body-ash">
          Este build subiu sem <code>VITE_CLERK_PUBLISHABLE_KEY</code>. Sem ela não há login:
          defina a variável no ambiente do build (Vercel → Environment Variables, ou
          <code> web/.env.local</code> em dev) e publique de novo.
        </p>
      </main>
    );
  }

  return (
    <ClerkProvider
      localization={ptBR}
      publishableKey={chaveClerk}
      appearance={aparencia}
      signInUrl={LOGIN}
      signUpUrl={CRIAR}
      afterSignOutUrl="/"
    >
      <SessaoOffline />
      <Suspense fallback={<p className="body body-ash edge" role="status">Carregando…</p>}>
        <Outlet />
      </Suspense>
    </ClerkProvider>
  );
}
