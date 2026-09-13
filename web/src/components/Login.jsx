import { ClerkLoaded, ClerkLoading, Show, SignIn, SignUp } from '@clerk/react';
import { Lines, Reveal } from './Reveal.jsx';
import { CRIAR, LOGIN } from '../rotas.js';

/* Clerk cobre e-mail/senha, os três provedores OAuth e a verificação por
   código. Sumiram daqui: os SVGs das marcas, o <form>, o estado de envio e o
   redirect para /api/auth/:provedor — tudo isso agora é o widget, e o
   client_secret nunca chegou a passar por este lado mesmo. */

/* routing="virtual": o app roteia por window.location.pathname num mapa em
   main.jsx, sem router. Virtual mantém os passos do widget em memória, sem
   mexer na URL — hash routing brigaria com os dois widgets na mesma tela. */
const COMUM = { routing: 'virtual', forceRedirectUrl: '/agenda' };

/* Quais provedores aparecem NÃO se decide aqui — o widget mostra o que estiver
   habilitado no dashboard do Clerk (SSO connections). Este objeto só pinta o
   que já vem. Se um dia precisar esconder um provedor pontual sem mexer no
   dashboard, dá pra fazer via elements: { socialButtonsBlockButton__apple:
   { display: 'none' } }. */
const APARENCIA = {
  variables: {
    /* Tokens da marca — caem para valores sóbrios se o token não existir. */
    colorPrimary: 'var(--brand, #0f766e)',
    colorText: 'var(--ink, #111418)',
    colorTextSecondary: 'var(--ash, #6b7280)',
    colorBackground: 'transparent',
    colorInputBackground: 'var(--surface, #ffffff)',
    colorInputText: 'var(--ink, #111418)',
    colorDanger: 'var(--danger, #b42318)',
    colorSuccess: 'var(--brand, #0f766e)',
    /* Nada de border-radius por variável: cada elemento arredonda por conta
       própria em `elements`, senão o card e o botão brigam pelo mesmo token. */
    fontFamily: 'inherit',
    fontSize: '1rem',
  },
  elements: {
    /* O widget monta em volta do card; tiramos a sombra e a moldura dele para
       ele se fundir com a coluna .who-half. */
    rootBox: { width: '100%' },
    card: {
      background: 'transparent',
      boxShadow: 'none',
      border: 'none',
      padding: '0',
      width: '100%',
    },

    header: { display: 'none' }, // o <Lines> acima já faz esse papel
    logoBox: { display: 'none' },

    /* Botões OAuth — reaproveitam .btn para herdar borda, altura e foco. */
    socialButtonsBlockButton: 'btn btn-lg',
    socialButtonsBlockButtonText: {
      fontFamily: 'inherit',
      fontWeight: '500',
      color: 'var(--ink, #111418)',
    },
    socialButtonsIconButton: 'btn btn-lg',

    dividerRow: { marginBlock: 'var(--spacing-30, 1.5rem)' },
    dividerLine: { background: 'var(--line, #e5e7eb)' },
    dividerText: 'credit',

    formFieldLabel: 'credit',
    formFieldInput: {
      fontFamily: 'inherit',
      fontSize: '1rem',
      background: 'var(--surface, #ffffff)',
      color: 'var(--ink, #111418)',
      border: '1px solid var(--line, #e5e7eb)',
      borderRadius: 'var(--radius-sm, 10px)',
      padding: '0.75rem 0.9rem',
    },
    formFieldInputShowPasswordButton: 'credit',

    formButtonPrimary: 'btn btn-lg btn-fill',
    formButtonReset: 'btn',

    /* "Usar código por e-mail" / "esqueci a senha" / "reenviar" — tudo no
       mesmo tom discreto dos links .credit. */
    formFieldAction: 'credit',
    formResendCodeLink: 'credit',
    identityPreviewEditButton: 'credit',
    footerAction: { marginTop: 'var(--spacing-20, 1rem)' },
    footerActionText: 'body body-ash',
    footerActionLink: 'credit',

    otpCodeFieldInput: {
      fontFamily: 'inherit',
      fontSize: '1.125rem',
      textAlign: 'center',
      border: '1px solid var(--line, #e5e7eb)',
      borderRadius: 'var(--radius-sm, 10px)',
    },

    alert: { borderRadius: 'var(--radius-sm, 10px)' },
    alertText: 'body',
    formFieldErrorText: 'credit',
  },
};

/* Entrar e criar são a mesma rota com ?criar=1, e não estado de React, para que
   o link "criar conta" que o próprio widget monta caia aqui em vez de recarregar
   o sign-in. Ler a URL dispensa o useState: a resposta já está nela. */
const criando = new URLSearchParams(window.location.search).has('criar');

export default function Login() {
  return (
    <>
      <a href="#main" className="skip-link">Ir para o conteúdo</a>

      <header className="auth-top edge">
        <a href="/" className="mbar-mark" aria-label="DentiBot — página inicial">DentiBot</a>
        <a href="/" className="btn">Voltar ao site</a>
      </header>

      <main id="main" className="who edge">
        <section className="who-half">
          <Lines as="h1" className="display" lines={criando ? ['Criar', 'conta.'] : ['Entrar.']} />
          <Reveal as="p" className="credit" delay="60ms">
            Agenda · Prontuário · Cobrança · Estoque · LGPD
          </Reveal>
          <Reveal
            as="p"
            className="body body-ash"
            delay="90ms"
            style={{ marginTop: 'var(--spacing-30)' }}
          >
            {criando
              ? 'Trinta dias para testar a clínica inteira, sem cartão.'
              : 'Acesso à sua clínica. Prontuário é dado sigiloso — a senha não se compartilha entre a equipe.'}
          </Reveal>
        </section>

        <section className="who-half">
          {/* O widget vem de um script remoto do Clerk e demora. <Show> devolve
              null enquanto isso, então sem este aviso a metade da tela fica
              vazia — que é indistinguível de estar quebrada. */}
          <ClerkLoading>
            <p className="body body-ash" aria-live="polite">Carregando o acesso…</p>
          </ClerkLoading>

          <ClerkLoaded>
          {/* Quem já tem sessão e volta em /login não fica olhando o widget. */}
          <Show when="signed-in">
            <p className="body">Você já está autenticado.</p>
            <p className="auth-swap">
              <a href="/agenda" className="btn btn-lg btn-fill">Abrir o sistema</a>
            </p>
          </Show>

          <Show when="signed-out">
            {criando ? (
              <SignUp
                {...COMUM}
                appearance={APARENCIA}
                /* O widget decide sozinho entre senha e código conforme o que
                   o dashboard oferece; não passe `initialValues` aqui para não
                   pré-preencher e-mail de quem chegou pelo link público. */
              />
            ) : (
              <SignIn
                {...COMUM}
                appearance={APARENCIA}
                /* Deixa o usuário alternar entre senha e código de e-mail
                   sem sair da mesma tela. O Clerk chama isso de "email code"
                   first factor — precisa estar ligado no dashboard. */
              />
            )}

            <p className="auth-swap">
              <a href={criando ? LOGIN : CRIAR} className="credit">
                {criando ? 'Já tenho conta — entrar' : 'Não tenho conta — criar grátis'}
              </a>
            </p>
          </Show>
          </ClerkLoaded>
        </section>
      </main>
    </>
  );
}
