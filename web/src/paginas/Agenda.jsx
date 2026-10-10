import { useMemo, useState } from 'react';
import { api, query } from '../api.js';
import {
  STATUS_CONSULTA, agruparPorHora, contagem, faixaHoraria, hoje, porExtenso,
  somarDias, telHref,
} from '../apresentacao.js';
import Confirmar from '../components/Confirmar.jsx';
import LinkDoPaciente from '../components/LinkDoPaciente.jsx';
import ResumoPaciente from '../components/ResumoPaciente.jsx';
import { Aviso, Selo } from '../components/primitivos.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { Cabecalho, Estado } from './Layout.jsx';

/* O back-end recebe `de` e `ate` como instantes ISO. O <input type="date"> dá
   'AAAA-MM-DD', que é dia civil — converter no fuso do browser é o certo: a
   agenda de terça é a terça de quem está na clínica, não a de UTC. */
const inicioDoDia = (dia) => new Date(`${dia}T00:00:00`).toISOString();
const fimDoDia = (dia) => new Date(`${dia}T23:59:59.999`).toISOString();

export default function Agenda() {
  const [dia, setDia] = useState(hoje);
  const [idDentista, setIdDentista] = useState('');
  // Um resumo aberto por vez: dois falando ao mesmo tempo seria ruído.
  const [resumoDe, setResumoDe] = useState(null);

  const caminho = useMemo(
    () => `/consultas${query({ de: inicioDoDia(dia), ate: fimDoDia(dia), idDentista })}`,
    [dia, idDentista],
  );

  const consultas = useRecurso(caminho);
  const dentistas = useRecurso('/equipe/dentistas');
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
      />

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
            ? 'Nenhuma consulta deste dentista neste dia. O filtro de dentista está ativo.'
            : 'Nenhuma consulta neste dia.')
          : null}
      >
        <div className="dia">
          {agruparPorHora(lista).map(({ hora, consultas: doHorario }) => (
            <div className="dia-degrau" key={hora}>
              <p className="dia-hora">{hora}h</p>
              <div className="dia-blocos">
                {doHorario.map((c) => (
                  <article className="consulta" key={c.idConsulta}>
                    <p className="consulta-faixa">{faixaHoraria(c.inicioEm, c.terminoEm)}</p>
                    <p className="sub">
                      <LinkDoPaciente idPaciente={c.idPaciente} nome={c.nomePaciente} />
                    </p>
                    <p className="body body-ash">{nomeDoDentista(c.idDentista)}</p>
                    {/* Já vinha no ConsultaResumo e era descartado. É o dado
                        que a recepção mais usa: ela liga para o paciente. */}
                    {telHref(c.telefonePaciente) && (
                      <a className="consulta-tel" href={telHref(c.telefonePaciente)}>
                        {c.telefonePaciente}
                      </a>
                    )}
                    {/* IA-15: contagem, não probabilidade — a recepção confere e explica. */}
                    {c.faltasRecentes > 0 && (
                      <p className="cap consulta-faltas">
                        Faltou {c.faltasRecentes} de {c.consultasRecentes}
                      </p>
                    )}
                    <Selo mapa={STATUS_CONSULTA} valor={c.status} />
                    <div className="acoes">
                      <button type="button" className="btn btn-sm"
                              aria-expanded={resumoDe === c.idConsulta}
                              onClick={() => setResumoDe((atual) =>
                                (atual === c.idConsulta ? null : c.idConsulta))}>
                        Resumo
                      </button>
                      {/* Só as transições que o estado atual permite. Mostrar
                          um botão que o back-end recusa com 409 é ensinar o
                          usuário a ignorar mensagem de erro. */}
                      {c.status === 'agendada' && (
                        <Transicao id={c.idConsulta} acao="confirmar" rotulo="Confirmar"
                                   executar={executar} enviando={enviando} />
                      )}
                      {(c.status === 'agendada' || c.status === 'confirmada') && (
                        <>
                          <Transicao id={c.idConsulta} acao="concluir" rotulo="Concluir"
                                     confirmacao="Concluir esta consulta?"
                                     executar={executar} enviando={enviando} />
                          <Transicao id={c.idConsulta} acao="falta" rotulo="Faltou"
                                     confirmacao="Registrar falta do paciente?"
                                     executar={executar} enviando={enviando} />
                        </>
                      )}
                    </div>
                    {resumoDe === c.idConsulta && (
                      <ResumoPaciente idPaciente={c.idPaciente} falarAoAbrir />
                    )}
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

/**
 * Um POST de transição.
 *
 * <p>A `Idempotency-Key` é gerada UMA VEZ por montagem do botão, não por
 * clique: dois toques em "Confirmar" são a mesma requisição lógica, e uma chave
 * nova no segundo clique faria o filtro do back-end tratá-lo como operação
 * distinta — que é exatamente o que ele existe para impedir.
 *
 * <p>`confirmacao` marca o que não tem volta: "Faltou" e "Concluir" mudam o
 * estado para um lugar de onde o back-end responde 409, e os dois ficavam a um
 * clique, encostados no botão que não faz mal nenhum.
 */
function Transicao({ id, acao, rotulo, confirmacao, executar, enviando }) {
  const [chave] = useState(() => crypto.randomUUID());
  const [perguntando, setPerguntando] = useState(false);

  const disparar = () => {
    setPerguntando(false);
    executar(
      api.post(`/consultas/${id}/${acao}`, {}, { idempotencyKey: chave }),
      `${rotulo}: consulta atualizada.`,
    );
  };

  return (
    <>
      <button type="button" className={`btn btn-sm${confirmacao ? ' btn-perigo' : ''}`}
              disabled={enviando}
              onClick={() => (confirmacao ? setPerguntando(true) : disparar())}>
        {rotulo}
      </button>
      {confirmacao && (
        <Confirmar
          aberto={perguntando}
          titulo={confirmacao}
          corpo="Esta mudança não tem volta pela tela: o back-end recusa o caminho inverso."
          rotuloConfirmar={rotulo}
          perigo
          onConfirmar={disparar}
          onCancelar={() => setPerguntando(false)}
        />
      )}
    </>
  );
}
