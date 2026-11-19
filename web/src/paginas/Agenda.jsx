import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { query } from '../api.js';
import {
  STATUS_CONSULTA, agruparPorHora, contagem, faixaHoraria, fimDoDia, hoje, inicioDoDia,
  porExtenso, somarDias, telHref,
} from '../apresentacao.js';
import Agendar from '../components/Agendar.jsx';
import Transicao from '../components/Transicao.jsx';
import { Aviso, Selo } from '../components/primitivos.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { RECEPCAO } from '../rotas.js';
import { Cabecalho, Estado, usePode } from './Layout.jsx';

export default function Agenda() {
  const [dia, setDia] = useState(hoje);
  const [idDentista, setIdDentista] = useState('');
  const [agendando, setAgendando] = useState(false);
  const pode = usePode();
  const navegar = useNavigate();

  const caminho = useMemo(
    () => `/consultas${query({ de: inicioDoDia(dia), ate: fimDoDia(dia), idDentista })}`,
    [dia, idDentista],
  );

  const consultas = useRecurso(caminho);
  const dentistas = useRecurso('/equipe/dentistas');
  /* Só quando o painel abre: a lista de pacientes serve à busca do agendar, e
     quem só consulta o dia não precisa pagar por ela. */
  const pacientes = useRecurso(agendando ? '/pacientes?limite=200' : null);
  const { executar, enviando, sucesso } = useAcao(consultas.recarregar);

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
          ? `${porExtenso(dia)} · ${contagem(lista.length, Infinity, 'consulta', 'consultas')}`
          : porExtenso(dia)}
        acao={pode('AGENDA', 'CRIAR') && (
          <button type="button" className="btn btn-fill" onClick={() => setAgendando((a) => !a)}>
            {agendando ? 'Fechar' : 'Agendar'}
          </button>
        )}
      />

      {agendando && (
        <Agendar
          key={dia}
          diaInicial={dia}
          pacientes={pacientes.dados ?? []}
          dentistas={dentistas.status === 'ok' ? dentistas.dados ?? [] : null}
          podeVerEquipe={pode('EQUIPE')}
          /* O cadastro com a ficha inteira mora na Recepção; o nome digitado
             vai junto para não ser digitado de novo. */
          onCadastrar={pode('PACIENTE', 'CRIAR')
            ? (nome) => navegar(RECEPCAO, { state: { cadastrar: nome } })
            : undefined}
          onAgendado={consultas.recarregar}
        />
      )}

      <div className="filtros filtros-linha">
        {/* Trocar de dia exigia abrir o date picker para andar uma casa. */}
        <div className="acoes">
          <button type="button" className="btn btn-sm"
                  onClick={() => setDia((d) => somarDias(d, -1))}>← Ontem</button>
          <button type="button" className="btn btn-sm"
                  onClick={() => setDia(hoje())}>Hoje</button>
          <button type="button" className="btn btn-sm"
                  onClick={() => setDia((d) => somarDias(d, 1))}>Amanhã →</button>
        </div>
        <label className="campo-app">
          <span className="cap cap-ash">Dia</span>
          <input type="date" value={dia}
                 onChange={(e) => setDia(e.target.value || hoje())} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Dentista</span>
          <select value={idDentista} onChange={(e) => setIdDentista(e.target.value)}>
            <option value="">Todos</option>
            {(dentistas.dados ?? []).map((d) => (
              <option key={d.idDentista} value={d.idDentista}>{d.nomeCompleto}</option>
            ))}
          </select>
        </label>
      </div>

      <Aviso texto={sucesso} />

      <Estado
        status={consultas.status}
        erro={consultas.erro}
        onTentarDeNovo={consultas.recarregar}
        esqueleto={{ linhas: 6, colunas: 3 }}
        /* Sem dizer do filtro, "nenhuma consulta" faz a pessoa concluir que o
           dia está livre quando ela só esqueceu um dentista selecionado. */
        vazio={lista.length === 0
          ? (idDentista
            ? { titulo: 'Nada neste filtro.',
              texto: 'Nenhuma consulta deste dentista neste dia. O filtro de dentista está ativo — em Todos pode haver outras.' }
            : { titulo: 'Dia livre.', texto: 'Nenhuma consulta marcada neste dia.' })
          : null}
      >
        <div className="dia">
          {agruparPorHora(lista).map(({ hora, consultas: doHorario }) => (
            <div className="dia-degrau" key={hora}>
              <p className="dia-hora">{hora}<small>h</small></p>
              <div className="dia-blocos">
                {doHorario.map((c) => (
                  <article className="consulta" key={c.idConsulta} data-status={c.status}>
                    <p className="consulta-faixa">{faixaHoraria(c.inicioEm, c.terminoEm)}</p>
                    <p className="sub">{c.nomePaciente ?? `Paciente ${c.idPaciente}`}</p>
                    <p className="body body-ash">{nomeDoDentista(c.idDentista)}</p>
                    {/* Já vinha no ConsultaResumo e era descartado. É o dado
                        que a recepção mais usa: ela liga para o paciente. */}
                    {telHref(c.telefonePaciente) && (
                      <a className="consulta-tel" href={telHref(c.telefonePaciente)}>
                        {c.telefonePaciente}
                      </a>
                    )}
                    <Selo mapa={STATUS_CONSULTA} valor={c.status} />
                    <div className="acoes">
                      {/* Só as transições que o estado atual permite. Mostrar
                          um botão que o back-end recusa com 409 é ensinar o
                          usuário a ignorar mensagem de erro. */}
                      {c.status === 'agendada' && (
                        <Transicao id={c.idConsulta} acao="confirmar" paciente={c.nomePaciente}
                                   executar={executar} enviando={enviando} />
                      )}
                      {(c.status === 'agendada' || c.status === 'confirmada') && (
                        <>
                          <Transicao id={c.idConsulta} acao="concluir" paciente={c.nomePaciente}
                                     executar={executar} enviando={enviando} />
                          <Transicao id={c.idConsulta} acao="falta" paciente={c.nomePaciente}
                                     executar={executar} enviando={enviando} />
                          <Transicao id={c.idConsulta} acao="cancelar" paciente={c.nomePaciente}
                                     executar={executar} enviando={enviando} />
                        </>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </div>
          ))}
        </div>
      </Estado>
    </>
  );
}
