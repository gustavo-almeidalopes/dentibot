import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useAcao, useRecurso } from '../dados.js';
import { prontuarioDe } from '../rotas.js';
import { Cabecalho, Estado } from './Layout.jsx';

/**
 * Lista de pacientes.
 *
 * <p>Substitui a antiga tela de "Clientes", que chamava `GET /api/clients` — uma
 * rota da API .NET que saiu na V2 — e esperava campos (`name`, `email`,
 * `endereco`) que o `PacienteResumo` nunca teve. Ela dava 404 antes mesmo do 401.
 *
 * <p>O que o back-end devolve aqui é deliberadamente pouco: id, nome, telefone e
 * status. A camada 5 diz que a recepção vê "só nome e horário", e a forma
 * correta de implementar isso é a consulta NÃO TRAZER o resto — campo que não
 * veio do banco não vaza em log, em erro nem em telemetria.
 */
export default function Pacientes() {
  const [criando, setCriando] = useState(false);
  const pacientes = useRecurso('/pacientes?limite=200');
  const lista = pacientes.dados ?? [];

  return (
    <>
      <Cabecalho
        titulo="Pacientes."
        detalhe={pacientes.status === 'ok'
          ? `${lista.length} no cadastro`
          : 'GET /api/v1/pacientes'}
        acao={(
          <button type="button" className="btn btn-fill"
                  onClick={() => setCriando((c) => !c)}>
            {criando ? 'Cancelar' : 'Novo paciente'}
          </button>
        )}
      />

      {criando && (
        <NovoPaciente onCriado={() => { setCriando(false); pacientes.recarregar(); }} />
      )}

      <Estado
        status={pacientes.status}
        erro={pacientes.erro}
        onTentarDeNovo={pacientes.recarregar}
        vazio={lista.length === 0 ? 'Nenhum paciente cadastrado ainda.' : null}
      >
        <ul className="lista">
          {lista.map((p) => (
            <li className="row" key={p.idPaciente}>
              <div>
                <p className="sub">{p.nomeCompleto ?? '—'}</p>
                <p className="cap cap-ash">{p.status}</p>
              </div>
              <div>
                <p className="body">{p.telefoneCelular || '—'}</p>
              </div>
              <div className="acoes">
                <Link className="btn" to={prontuarioDe(p.idPaciente)}>Prontuário</Link>
              </div>
            </li>
          ))}
        </ul>
      </Estado>
    </>
  );
}

function NovoPaciente({ onCriado }) {
  const [form, setForm] = useState({
    nomeCompleto: '', cpf: '', telefoneCelular: '', email: '',
  });
  const { executar, enviando, erro } = useAcao(onCriado);

  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  return (
    <form
      className="form-bloco"
      onSubmit={(e) => {
        e.preventDefault();
        /* Campos vazios viram null e não string vazia: o CHECK de CPF do banco
           recusa '' e o e-mail vazio ocuparia o índice único à toa. */
        executar(api.post('/pacientes', {
          nomeCompleto: form.nomeCompleto.trim(),
          cpf: form.cpf.replace(/\D/g, '') || null,
          telefoneCelular: form.telefoneCelular.trim() || null,
          email: form.email.trim() || null,
        }));
      }}
    >
      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Nome completo</span>
          <input required maxLength={150} value={form.nomeCompleto} onChange={mudar('nomeCompleto')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">CPF</span>
          <input inputMode="numeric" maxLength={14} value={form.cpf} onChange={mudar('cpf')}
                 placeholder="somente dígitos" />
        </label>
      </div>
      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Celular</span>
          <input maxLength={20} value={form.telefoneCelular} onChange={mudar('telefoneCelular')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">E-mail</span>
          <input type="email" maxLength={254} value={form.email} onChange={mudar('email')} />
        </label>
      </div>

      {erro && <p className="pagamento-erro" role="alert">{erro.message}</p>}

      <button type="submit" className="btn btn-fill" disabled={enviando}>
        {enviando ? 'Cadastrando…' : 'Cadastrar'}
      </button>
    </form>
  );
}
