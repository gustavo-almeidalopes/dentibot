import { useMemo, useState } from 'react';
import { api, query } from '../api.js';
import { useAcao, useRecurso } from '../dados.js';
import { Cabecalho, Estado } from './Layout.jsx';

/* O back-end recebe `de` e `ate` como instantes ISO. O <input type="date"> dá
   'AAAA-MM-DD', que é dia civil — converter no fuso do browser é o certo: a
   agenda de terça é a terça de quem está na clínica, não a de UTC. */
const inicioDoDia = (dia) => new Date(`${dia}T00:00:00`).toISOString();
const fimDoDia = (dia) => new Date(`${dia}T23:59:59.999`).toISOString();

const hoje = () => new Date().toLocaleDateString('sv-SE'); // sv-SE dá AAAA-MM-DD

const HORA = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });

/** Os status que a máquina de estados do back-end aceita, e o que mostrar. */
const STATUS = {
  agendada: 'Agendada',
  confirmada: 'Confirmada',
  em_atendimento: 'Em atendimento',
  realizada: 'Realizada',
  cancelada: 'Cancelada',
  faltou: 'Faltou',
};

export default function Agenda() {
  const [dia, setDia] = useState(hoje);
  const [idDentista, setIdDentista] = useState('');

  const caminho = useMemo(
    () => `/consultas${query({ de: inicioDoDia(dia), ate: fimDoDia(dia), idDentista })}`,
    [dia, idDentista],
  );

  const consultas = useRecurso(caminho);
  const dentistas = useRecurso('/equipe/dentistas');
  const { executar, enviando } = useAcao(consultas.recarregar);

  const lista = consultas.dados ?? [];

  /* ConsultaResumo traz `idDentista` e não o nome — a projeção do back-end é
     deliberadamente enxuta. A tela já carrega a lista de dentistas para o
     filtro, então o nome sai daí sem uma segunda ida à API. */
  const nomeDoDentista = useMemo(() => {
    const mapa = new Map((dentistas.dados ?? []).map((d) => [d.idDentista, d.nomeCompleto]));
    return (id) => mapa.get(id) ?? `Dentista ${id}`;
  }, [dentistas.dados]);

  return (
    <>
      <Cabecalho
        titulo="Agenda."
        detalhe={consultas.status === 'ok'
          ? `${lista.length} ${lista.length === 1 ? 'consulta' : 'consultas'} neste dia`
          : 'GET /api/v1/consultas'}
        acao={(
          <div className="filtros">
            <label className="cap cap-ash" htmlFor="agenda-dia">Dia</label>
            <input id="agenda-dia" type="date" value={dia}
                   onChange={(e) => setDia(e.target.value || hoje())} />

            <label className="cap cap-ash" htmlFor="agenda-dentista">Dentista</label>
            <select id="agenda-dentista" value={idDentista}
                    onChange={(e) => setIdDentista(e.target.value)}>
              <option value="">Todos</option>
              {(dentistas.dados ?? []).map((d) => (
                <option key={d.idDentista} value={d.idDentista}>{d.nomeCompleto}</option>
              ))}
            </select>
          </div>
        )}
      />

      <Estado
        status={consultas.status}
        erro={consultas.erro}
        onTentarDeNovo={consultas.recarregar}
        vazio={lista.length === 0 ? 'Nenhuma consulta neste dia.' : null}
      >
        <ul className="lista">
          {lista.map((c) => (
            <li className="row row-consulta" key={c.idConsulta}>
              <div>
                <p className="sub">{HORA.format(new Date(c.inicioEm))}</p>
                <p className="cap cap-ash">{STATUS[c.status] ?? c.status}</p>
              </div>
              <div>
                <p className="body">{c.nomePaciente ?? `Paciente ${c.idPaciente}`}</p>
                <p className="body body-ash">{nomeDoDentista(c.idDentista)}</p>
              </div>
              <div className="acoes">
                {/* Só as transições que o estado atual permite. Mostrar um botão
                    que o back-end vai recusar com 409 é ensinar o usuário a
                    ignorar mensagem de erro. */}
                {c.status === 'agendada' && (
                  <Transicao id={c.idConsulta} acao="confirmar" rotulo="Confirmar"
                             executar={executar} enviando={enviando} />
                )}
                {(c.status === 'agendada' || c.status === 'confirmada') && (
                  <>
                    <Transicao id={c.idConsulta} acao="concluir" rotulo="Concluir"
                               executar={executar} enviando={enviando} />
                    <Transicao id={c.idConsulta} acao="falta" rotulo="Faltou"
                               executar={executar} enviando={enviando} />
                  </>
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
 * Um POST de transição.
 *
 * <p>A `Idempotency-Key` é gerada UMA VEZ por montagem do botão, não por
 * clique: dois toques em "Confirmar" são a mesma requisição lógica, e uma chave
 * nova no segundo clique faria o filtro do back-end tratá-lo como operação
 * distinta — que é exatamente o que ele existe para impedir.
 */
function Transicao({ id, acao, rotulo, executar, enviando }) {
  const [chave] = useState(() => crypto.randomUUID());
  return (
    <button
      type="button"
      className="btn"
      disabled={enviando}
      onClick={() => executar(
        api.post(`/consultas/${id}/${acao}`, {}, { idempotencyKey: chave }),
      )}
    >
      {rotulo}
    </button>
  );
}
