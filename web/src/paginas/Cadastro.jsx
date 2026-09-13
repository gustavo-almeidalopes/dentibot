import { RedirectToSignIn, Show, useUser } from '@clerk/react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import FichaPaciente from '../components/FichaPaciente.jsx';
import { useAcao } from '../dados.js';
import { documentoValido, formatarCnpj, formatarCpf, somenteDigitos, tipoDeDocumento } from '../documento.js';
import { AGENDA } from '../rotas.js';

/**
 * O que acontece DEPOIS de criar a conta — inclusive pelo Google, Microsoft ou
 * Apple.
 *
 * <p>Existia um buraco aqui: o Clerk cria a conta e devolve um `sub`, mas o
 * back-end só conhece quem tem linha em `identidade.usuarios`. Sem passar por
 * `POST /auth/cadastro`, o {@code ResolvedorDeAcessoClerk} devolve
 * {@code Optional.empty()} e a conta nova toma 401 em toda tela. O login "dava
 * certo" e o sistema inteiro respondia não. Este é o passo que faltava, e ele
 * vale igual para OAuth e para e-mail e senha — criar conta com o Google pula o
 * formulário do Clerk, não este.
 *
 * <p>A bifurcação é a do documento:
 * <ul>
 *   <li><b>CNPJ</b> — é clínica. Vai direto para o painel de admin.</li>
 *   <li><b>CPF</b> — pode ser o dentista autônomo (que também precisa de um
 *       tenant para trabalhar) ou o paciente. Quem responde é o usuário; o
 *       sistema não tem como adivinhar por um número de 11 dígitos.</li>
 * </ul>
 */
export default function Cadastro() {
  return (
    <>
      <a href="#main" className="skip-link">Ir para o conteúdo</a>
      <header className="auth-top edge">
        <a href="/" className="mbar-mark" aria-label="DentiBot — página inicial">DentiBot</a>
      </header>

      <Show when="signed-out">
        <RedirectToSignIn />
      </Show>
      <Show when="signed-in">
        <main id="main" className="app-main edge">
          <Passos />
        </main>
      </Show>
    </>
  );
}

function Passos() {
  const { user } = useUser();
  const [documento, setDocumento] = useState('');
  const [perfil, setPerfil] = useState('');

  const tipo = tipoDeDocumento(documento);
  const valido = documentoValido(documento);
  /* CNPJ não pergunta nada: pessoa jurídica no cadastro de uma clínica é a
     clínica. Só o CPF é ambíguo. */
  const caminho = tipo === 'cnpj' ? 'clinica' : perfil;

  const nome = user?.fullName ?? '';
  const email = user?.primaryEmailAddress?.emailAddress ?? '';

  return (
    <>
      <div className="app-cabecalho">
        <div>
          <h1 className="display display-sm">Falta o cadastro.</h1>
          <p className="credit">A conta existe. O cadastro é o que abre o sistema.</p>
        </div>
      </div>

      <div className="form-bloco">
        <label className="campo-app" style={{ maxWidth: '22rem' }}>
          <span className="cap cap-ash">CPF ou CNPJ *</span>
          <input
            inputMode="numeric"
            maxLength={18}
            autoFocus
            placeholder="000.000.000-00 ou 00.000.000/0000-00"
            aria-invalid={(tipo !== null && !valido) || undefined}
            value={documento}
            /* Uma máscara só, escolhida pelo comprimento: dois campos com uma
               aba "PF / PJ" seriam um clique a mais para dizer o que o próprio
               número já diz. */
            onChange={(e) => {
              const d = somenteDigitos(e.target.value);
              setDocumento(d.length > 11 ? formatarCnpj(d) : formatarCpf(d));
              setPerfil('');
            }}
          />
        </label>

        {tipo !== null && !valido && (
          <p className="pagamento-erro" role="alert">
            {tipo === 'cpf' ? 'CPF inválido' : 'CNPJ inválido'} — confira os dígitos.
          </p>
        )}

        {valido && tipo === 'cnpj' && (
          <p className="body body-ash">CNPJ: cadastro de clínica.</p>
        )}

        {valido && tipo === 'cpf' && (
          <label className="campo-app" style={{ maxWidth: '22rem' }}>
            <span className="cap cap-ash">Com CPF, você é *</span>
            <select value={perfil} onChange={(e) => setPerfil(e.target.value)}>
              <option value="">—</option>
              <option value="clinica">Dentista autônomo</option>
              <option value="paciente">Paciente</option>
            </select>
          </label>
        )}
      </div>

      {valido && caminho === 'clinica' && (
        <FormularioClinica documento={documento} tipo={tipo} nome={nome} email={email} />
      )}
      {valido && caminho === 'paciente' && (
        <FormularioPaciente nome={nome} email={email} />
      )}
    </>
  );
}

/** Clínica ou dentista autônomo: os dois precisam de tenant para existir. */
function FormularioClinica({ documento, tipo, nome, email }) {
  const navegar = useNavigate();
  const autonomo = tipo === 'cpf';
  const [f, setF] = useState({
    razaoSocial: autonomo ? nome : '',
    nomeFantasia: autonomo ? nome : '',
    plano: 'solo',
    nomeAdmin: nome,
    emailAdmin: email,
  });
  const { executar, enviando, erro } = useAcao(() => navegar(AGENDA));
  const mudar = (campo) => (e) => setF((a) => ({ ...a, [campo]: e.target.value }));

  return (
    <form
      className="form-bloco"
      onSubmit={(e) => {
        e.preventDefault();
        executar(api.post('/auth/cadastro', {
          cnpj: somenteDigitos(documento),
          razaoSocial: f.razaoSocial.trim(),
          nomeFantasia: f.nomeFantasia.trim(),
          plano: f.plano,
          nomeAdmin: f.nomeAdmin.trim(),
          emailAdmin: f.emailAdmin.trim(),
        }));
      }}
    >
      <h2 className="sub">{autonomo ? 'Dentista autônomo' : 'Dados da clínica'}</h2>

      <div className="form-linha">
        <label className="campo-app" style={{ flex: '2 1 320px' }}>
          <span className="cap cap-ash">
            {autonomo ? 'Nome do profissional *' : 'Razão social *'}
          </span>
          <input required maxLength={144} value={f.razaoSocial} onChange={mudar('razaoSocial')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Nome que aparece para o paciente *</span>
          <input required maxLength={60} value={f.nomeFantasia} onChange={mudar('nomeFantasia')} />
        </label>
      </div>

      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Plano</span>
          <select value={f.plano} onChange={mudar('plano')}>
            <option value="solo">Solo</option>
            <option value="clinica">Clínica</option>
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Seu nome *</span>
          <input required maxLength={150} value={f.nomeAdmin} onChange={mudar('nomeAdmin')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Seu e-mail *</span>
          <input required type="email" maxLength={254}
                 value={f.emailAdmin} onChange={mudar('emailAdmin')} />
        </label>
      </div>

      <p className="cap cap-ash">Trinta dias de teste. Sem cartão.</p>

      {erro && <p className="pagamento-erro" role="alert">{erro.message}</p>}

      <button type="submit" className="btn btn-fill btn-lg" disabled={enviando}>
        {enviando ? 'Criando…' : 'Criar e abrir o sistema'}
      </button>
    </form>
  );
}

/** Paciente: a ficha inteira, guardada como pré-cadastro até uma clínica vincular. */
function FormularioPaciente({ nome, email }) {
  const [pronto, setPronto] = useState(false);
  const { executar, enviando, erro } = useAcao(() => setPronto(true));

  if (pronto) {
    return (
      <div className="form-bloco">
        <h2 className="sub">Ficha enviada.</h2>
        <p className="body body-ash">
          Ela fica guardada com o seu CPF. Quando a sua clínica te vincular, o histórico
          já estará lá — e você não repete nada na recepção.
        </p>
        <p className="auth-swap"><a href="/" className="btn btn-lg">Voltar ao site</a></p>
      </div>
    );
  }

  return (
    <FichaPaciente
      inicial={{ nomeCompleto: nome, email }}
      enviando={enviando}
      erro={erro}
      rotuloEnviar="Enviar ficha"
      onEnviar={(corpo) => executar(api.post('/auth/pre-cadastro', corpo))}
    />
  );
}
