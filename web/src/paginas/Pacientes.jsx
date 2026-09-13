import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import FichaPaciente from '../components/FichaPaciente.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { prontuarioDe } from '../rotas.js';
import { Cabecalho, Estado, usePode } from './Layout.jsx';

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
  const pode = usePode();
  const pacientes = useRecurso('/pacientes?limite=200');
  const lista = pacientes.dados ?? [];

  return (
    <>
      <Cabecalho
        titulo="Pacientes."
        detalhe={pacientes.status === 'ok'
          ? `${lista.length} no cadastro`
          : 'GET /api/v1/pacientes'}
        /* Financeiro e auxiliar leem o cadastro e não criam. Mostrar o botão
           para eles seria oferecer um formulário que termina em 403. */
        acao={pode('PACIENTE', 'CRIAR') && (
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
                {/* Recepção e financeiro não alcançam prontuário — dado de
                    saúde, LGPD art. 11. Sem o link a tela não convida ao 403. */}
                {pode('PRONTUARIO') && (
                  <Link className="btn" to={prontuarioDe(p.idPaciente)}>Prontuário</Link>
                )}
              </div>
            </li>
          ))}
        </ul>
      </Estado>
    </>
  );
}

/**
 * O mesmo componente do auto-cadastro em /cadastro.
 *
 * <p>Antes eram quatro campos aqui — nome, CPF, celular, e-mail — e a ficha
 * completa em lugar nenhum. Faltava tudo o que muda conduta: data de
 * nascimento (dose de anestésico), alergia, condição sistêmica, gravidez.
 */
function NovoPaciente({ onCriado }) {
  const { executar, enviando, erro } = useAcao(onCriado);

  return (
    <FichaPaciente
      enviando={enviando}
      erro={erro}
      onEnviar={(corpo) => executar(api.post('/pacientes', corpo))}
    />
  );
}
