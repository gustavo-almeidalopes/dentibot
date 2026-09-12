import { useState } from 'react';
import { Lines, Reveal } from './Reveal.jsx';
import { api } from '../api.js';

/* Marcas oficiais em SVG inline: as diretrizes dos três provedores exigem o
   símbolo no botão, e três <img> seriam três requisições para 1 KB de vetor.
   O "G" do Google não pode ser recolorido — ele segue colorido quando o wipe
   branco do .btn passa por baixo, que é justamente o fundo que a marca pede. */
function LogoGoogle() {
  return (
    <svg viewBox="0 0 48 48" width="21" height="21" aria-hidden="true" focusable="false">
      <path fill="#4285f4" d="M45.12 24.5c0-1.56-.14-3.06-.4-4.5H24v8.51h11.84c-.51 2.75-2.06 5.08-4.39 6.64v5.52h7.11c4.16-3.83 6.56-9.47 6.56-16.17z" />
      <path fill="#34a853" d="M24 46c5.94 0 10.92-1.97 14.56-5.33l-7.11-5.52c-1.97 1.32-4.49 2.1-7.45 2.1-5.73 0-10.58-3.87-12.31-9.07H4.34v5.7C7.96 41.07 15.4 46 24 46z" />
      <path fill="#fbbc05" d="M11.69 28.18C11.25 26.86 11 25.45 11 24s.25-2.86.69-4.18v-5.7H4.34C2.85 17.09 2 20.45 2 24s.85 6.91 2.34 9.88l7.35-5.7z" />
      <path fill="#ea4335" d="M24 10.75c3.23 0 6.13 1.11 8.41 3.29l6.31-6.31C34.91 4.18 29.93 2 24 2 15.4 2 7.96 6.93 4.34 14.12l7.35 5.7c1.73-5.2 6.58-9.07 12.31-9.07z" />
    </svg>
  );
}

/* Apple e Microsoft aceitam o símbolo monocromático herdando a cor do botão —
   por isso currentColor na maçã, que acompanha o wipe. O quadrado da Microsoft
   é colorido por especificação e não muda. */
function LogoApple() {
  return (
    <svg viewBox="0 0 24 24" width="21" height="21" fill="currentColor" aria-hidden="true" focusable="false">
      <path d="M16.365 1.43c0 1.14-.42 2.2-1.26 3.03-.9.9-2.02 1.42-3.1 1.34-.13-1.07.42-2.2 1.2-2.98.87-.88 2.3-1.5 3.16-1.39zM20.9 17.1c-.55 1.27-.82 1.84-1.53 2.96-1 1.56-2.4 3.5-4.14 3.52-1.55.01-1.95-1.01-4.05-1-2.1.01-2.54 1.02-4.09 1.01-1.74-.02-3.07-1.77-4.07-3.33-2.8-4.36-3.1-9.48-1.37-12.2 1.23-1.93 3.17-3.06 5-3.06 1.86 0 3.03 1.02 4.57 1.02 1.49 0 2.4-1.02 4.55-1.02 1.63 0 3.36.89 4.59 2.42-4.03 2.21-3.38 7.97.54 9.68z" />
    </svg>
  );
}

function LogoMicrosoft() {
  return (
    <svg viewBox="0 0 23 23" width="20" height="20" aria-hidden="true" focusable="false">
      <path fill="#f25022" d="M1 1h10v10H1z" />
      <path fill="#7fba00" d="M12 1h10v10H12z" />
      <path fill="#00a4ef" d="M1 12h10v10H1z" />
      <path fill="#ffb900" d="M12 12h10v10H12z" />
    </svg>
  );
}

/* OAuth server-side: o browser sai do app e volta já com a sessão. Nenhum SDK
   de provedor roda aqui — client_id e troca de código ficam no back-end, que
   é o único lugar onde o client_secret pode existir. */
const PROVEDORES = [
  { id: 'google', nome: 'Google', Logo: LogoGoogle },
  { id: 'apple', nome: 'Apple', Logo: LogoApple },
  { id: 'microsoft', nome: 'Microsoft', Logo: LogoMicrosoft },
];

const BASE = import.meta.env.VITE_API_BASE || '/api';

function Campo({ id, label, ...props }) {
  return (
    <p className="field">
      <label htmlFor={id} className="cap cap-ash">{label}</label>
      <input id={id} name={id} {...props} />
    </p>
  );
}

export default function Login() {
  const [modo, setModo] = useState('entrar');
  const [estado, setEstado] = useState({ status: 'idle' });
  const criando = modo === 'criar';
  const enviando = estado.status === 'enviando';

  /* FormData em vez de um useState por campo: são três inputs não controlados
     e o browser já guarda o valor deles. Menos estado, menos re-render. */
  async function enviar(e) {
    e.preventDefault();
    const dados = new FormData(e.currentTarget);
    setEstado({ status: 'enviando' });
    try {
      if (criando) {
        await api.signup({
          name: dados.get('nome'),
          email: dados.get('email'),
          password: dados.get('senha'),
        });
      } else {
        await api.login(dados.get('email'), dados.get('senha'));
      }
      window.location.href = '/clientes';
    } catch (erro) {
      setEstado({ status: 'erro', erro: erro.message });
    }
  }

  function trocarModo() {
    setModo(criando ? 'entrar' : 'criar');
    setEstado({ status: 'idle' });
  }

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
          <div className="auth-providers">
            {PROVEDORES.map(({ id, nome, Logo }) => (
              <button
                key={id}
                type="button"
                className="btn btn-lg"
                disabled={enviando}
                onClick={() => { window.location.href = `${BASE}/auth/${id}`; }}
              >
                <Logo />
                {criando ? `Criar conta com ${nome}` : `Entrar com ${nome}`}
              </button>
            ))}
          </div>

          <p className="auth-sep"><span>ou com e-mail</span></p>

          <form className="auth-form" onSubmit={enviar}>
            {criando && (
              <Campo id="nome" label="Nome completo" type="text" autoComplete="name" required maxLength={150} />
            )}
            <Campo id="email" label="E-mail" type="email" autoComplete="email" required />
            <Campo
              id="senha"
              label="Senha"
              type="password"
              autoComplete={criando ? 'new-password' : 'current-password'}
              required
              minLength={criando ? 8 : undefined}
            />

            {/* Reservado sempre: sem min-height a mensagem empurra o botão
                para baixo justo quando o usuário vai clicar de novo. */}
            <p className="auth-error" role="alert">
              {estado.status === 'erro' ? estado.erro : ''}
            </p>

            <button type="submit" className="btn btn-lg btn-fill auth-submit" disabled={enviando}>
              {enviando ? 'Enviando…' : criando ? 'Criar conta' : 'Entrar'}
            </button>
          </form>

          <p className="auth-swap">
            <button type="button" className="credit" onClick={trocarModo}>
              {criando ? 'Já tenho conta — entrar' : 'Não tenho conta — criar grátis'}
            </button>
          </p>
        </section>
      </main>
    </>
  );
}
