import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { query } from '../api.js';
import {
  STATUS_CONSULTA, agruparPorHora, contagem, faixaHoraria, fimDoDia, hoje, inicioDoDia,
  nomeDoPaciente, nomesPorId, porExtenso, somarDias,
} from '../apresentacao.js';
import Agendar from '../components/Agendar.jsx';
import Transicoes from '../components/Transicao.jsx';
import { Aviso, Cabecalho, Estado, Selo, Telefone } from '../components/primitivos.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { usePode } from '../permissoes.js';
import { RECEPCAO } from '../rotas.js';

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
  const { executar, enviando, sucesso } = useAcao(consultas.recarregar);

  const lista = consultas.dados ?? [];

  const nomeDoDentista = useMemo(
    () => nomesPorId(dentistas.dados, 'idDentista', 'Dentista'),
    [dentistas.dados],
  );

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
          dentistas={dentistas.status === 'ok' ? dentistas.dados ?? [] : null}
          /* O cadastro com a ficha inteira mora na Recepção; o nome digitado
             vai junto para não ser digitado de novo. */
          onCadastrar={pode('PACIENTE', 'CRIAR')
            ? (nome) => navegar(RECEPCAO, { state: { cadastrar: nome } })
            : undefined}
          onAgendado={consultas.recarregar}
        />
      )}

      <div className="filtros filtros-linha">
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
                    <p className="sub">{nomeDoPaciente(c)}</p>
                    <p className="body body-ash">{nomeDoDentista(c.idDentista)}</p>
                    <Telefone numero={c.telefonePaciente} className="consulta-tel" />
                    <Selo mapa={STATUS_CONSULTA} valor={c.status} />
                    <Transicoes consulta={c} executar={executar} enviando={enviando} />
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
