import { Show, SignIn, SignUp } from '@clerk/react';
import { Lines, Reveal } from './Reveal.jsx';
import { CRIAR, LOGIN } from '../rotas.js';

/* Clerk cobre e-mail/senha, os três provedores OAuth e a verificação por
   código. Sumiram daqui: os SVGs das marcas, o <form>, o estado de envio e o
   redirect para /api/auth/:provedor — tudo isso agora é o widget, e o
   client_secret nunca chegou a passar por este lado mesmo. */

/* routing="virtual": o app roteia por window.location.pathname num mapa em
   main.jsx, sem router. Virtual mantém os passos do widget em memória, sem
   mexer na URL — hash routing brigaria com os dois widgets na mesma tela. */
const COMUM = { routing: 'virtual', forceRedirectUrl: '/clientes' };

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
          {/* Quem já tem sessão e volta em /login não fica olhando o widget. */}
          <Show when="signed-in">
            <p className="body">Você já está autenticado.</p>
            <p className="auth-swap">
              <a href="/clientes" className="btn btn-lg btn-fill">Ir para os clientes</a>
            </p>
          </Show>

          <Show when="signed-out">
            {criando ? <SignUp {...COMUM} /> : <SignIn {...COMUM} />}

            <p className="auth-swap">
              <a href={criando ? LOGIN : CRIAR} className="credit">
                {criando ? 'Já tenho conta — entrar' : 'Não tenho conta — criar grátis'}
              </a>
            </p>
          </Show>
        </section>
      </main>
    </>
  );
}
