import { ClerkLoaded, ClerkLoading, Show, SignIn, SignUp } from '@clerk/react';
import { Lines, Reveal } from './Reveal.jsx';
import { AGENDA, CADASTRO, CRIAR, LOGIN } from '../rotas.js';

/* Clerk cobre e-mail/senha, os três provedores OAuth e a verificação por
   código: o formulário inteiro é o widget. */

/* routing="virtual": os passos do widget ficam em memória, sem mexer na URL.
   Path routing exigiria uma rota coringa /login/* só para o Clerk, e hash
   routing brigaria com o `?criar=1` que decide qual widget aparece. */
const COMUM = { routing: 'virtual' };

/* Entrar vai para o sistema; criar conta vai para o cadastro, SEMPRE — é lá que
   nasce a linha em `identidade.usuarios` sem a qual o back-end responde 401 a
   tudo. `forceRedirectUrl` e não `fallbackRedirectUrl`: o Clerk devolve o OAuth
   ao ponto de partida, e sem o "force" quem clicou em Google voltaria direto
   para /agenda, pulando justamente o passo que faltava. */
const ENTRAR = { ...COMUM, forceRedirectUrl: AGENDA };
const CRIAR_CONTA = { ...COMUM, forceRedirectUrl: CADASTRO };

/* Quais provedores aparecem NÃO se decide aqui — o widget mostra o que estiver
   habilitado no dashboard do Clerk (SSO connections). */
const CAMPO = {
  fontFamily: 'inherit',
  fontSize: '1rem',
  background: 'transparent',
  color: 'var(--bone)',
  border: '1px solid var(--hair)',
  borderRadius: '0',
  boxShadow: 'none',
};

/* Cores e canto vivo vêm do ClerkProvider (APARENCIA.escuro, em tema.js). Aqui
   só o que as variáveis não alcançam: classes do site e o que precisa sumir. */
const APARENCIA = {
  elements: {
    /* O widget monta em volta do card; tiramos a sombra e a moldura dele para
       ele se fundir com a coluna .who-half. */
    rootBox: { width: '100%' },
    cardBox: { width: '100%', boxShadow: 'none', border: 'none', borderRadius: '0' },
    card: { background: 'transparent', boxShadow: 'none', border: 'none', padding: '0', width: '100%' },

    header: { display: 'none' }, // o <Lines> ao lado já faz esse papel
    logoBox: { display: 'none' },
    /* "Não possui conta? Registre-se", "Secured by Clerk" e o selo de dev: o
       nosso .auth-swap logo abaixo já troca entre entrar e criar. */
    footer: { display: 'none' },

    /* Botões OAuth — reaproveitam .btn para herdar borda, altura e foco. */
    socialButtonsBlockButton: 'btn btn-lg',
    socialButtonsIconButton: { ...CAMPO, height: '52px' },
    socialButtonsBlockButtonText: { fontFamily: 'inherit', color: 'var(--bone)' },

    dividerRow: { marginBlock: 'var(--spacing-30)' },
    dividerLine: { background: 'var(--hair)' },
    dividerText: 'credit',

    formFieldLabel: 'credit',
    formFieldInput: { ...CAMPO, padding: '0.75rem 0.9rem' },
    formFieldInputShowPasswordButton: 'credit',

    formButtonPrimary: 'btn btn-lg btn-fill',
    formButtonReset: 'btn',

    /* "Usar código por e-mail" / "esqueci a senha" / "reenviar" — tudo no
       mesmo tom discreto dos links .credit. */
    formFieldAction: 'credit',
    formResendCodeLink: 'credit',
    identityPreviewEditButton: 'credit',

    otpCodeFieldInput: { ...CAMPO, fontSize: '1.125rem', textAlign: 'center' },

    alert: { borderRadius: '0', border: '1px solid var(--alarm)', background: 'transparent' },
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
              <a href={AGENDA} className="btn btn-lg btn-fill">Abrir o sistema</a>
            </p>
          </Show>

          <Show when="signed-out">
            {criando ? (
              <SignUp
                {...CRIAR_CONTA}
                appearance={APARENCIA}
                /* O widget decide sozinho entre senha e código conforme o que
                   o dashboard oferece; não passe `initialValues` aqui para não
                   pré-preencher e-mail de quem chegou pelo link público. */
              />
            ) : (
              <SignIn
                {...ENTRAR}
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
