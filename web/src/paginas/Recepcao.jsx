import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { api, query } from '../api.js';
import {
  STATUS_CONSULTA, cabeAgora, contagem, diaDe, fimDoDia, hora, inicioDoDia, jaPassou, momento,
  nomeDoPaciente, porExtenso, relativo, semDesfecho, somarDias, telHref,
  whatsappHref,
} from '../apresentacao.js';
import Agendar from '../components/Agendar.jsx';
import FichaPaciente from '../components/FichaPaciente.jsx';
import Transicoes from '../components/Transicao.jsx';
import {
  Aviso, Cabecalho, Celula, Estado, Selo, Tabela, Telefone,
} from '../components/primitivos.jsx';
import { useAcao, useDentistas, useRecurso } from '../dados.js';
import { usePode } from '../permissoes.js';

/* O relógio da tela. Meio minuto basta: "em 25 min" não precisa de segundos,
   e o dia vira sozinho à meia-noite porque `dia` deriva daqui. */
function useAgora(intervaloMs = 30000) {
  const [agora, setAgora] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setAgora(new Date()), intervaloMs);
    return () => clearInterval(id);
  }, [intervaloMs]);
  return agora;
}

/**
 * O balcão.
 *
 * <p>A Agenda responde "como está o dia de cada cadeira"; esta tela responde o
 * que a recepção pergunta de pé, com alguém na frente e o telefone tocando:
 * quem é o próximo, quem ainda falta, quem de amanhã não confirmou — e marca
 * consulta, cadastrando o paciente no meio do caminho se ele for novo.
 *
 * <p>Nenhum endpoint novo: hoje e amanhã saem de UMA chamada a `/consultas`, e
 * o resto é conta feita aqui sobre ela.
 */
export default function Recepcao() {
  const pode = usePode();
  const { state } = useLocation();
  const agora = useAgora();
  const dia = diaDe(agora);
  const amanha = somarDias(dia, 1);

  const consultas = useRecurso(`/consultas${query({ de: inicioDoDia(dia), ate: fimDoDia(amanha) })}`);
  const dentistas = useDentistas();
  const { nomeDoDentista } = dentistas;
  const clinica = useRecurso('/clinica');
  const { executar, enviando, sucesso } = useAcao(consultas.recarregar);

  /* Quem chega de Pacientes com "Agendar" traz o paciente; quem chega da
     Agenda com "Cadastrar" traz o nome digitado. */
  const [cadastrando, setCadastrando] = useState(state?.cadastrar ?? null);
  const [novo, setNovo] = useState(state?.paciente ?? null);
  const cadastro = useAcao();


  const todas = consultas.dados ?? [];
  const doDia = (d) => todas.filter((c) => diaDe(c.inicioEm) === d)
    .sort((a, b) => new Date(a.inicioEm) - new Date(b.inicioEm));
  const deHoje = doDia(dia);
  const deAmanha = doDia(amanha);
  const seguem = deHoje.filter((c) => !jaPassou(c, agora));
  const passaram = deHoje.filter((c) => jaPassou(c, agora));
  /* Passou do horário e ninguém fechou: nem realizada, nem falta. É o que a
     recepção precisa resolver antes de ir embora. */
  const pendentes = passaram.filter(semDesfecho);
  const aConfirmar = deAmanha.filter((c) => c.status === 'agendada');
  const confirmadasAmanha = deAmanha.filter((c) => c.status === 'confirmada').length;

  async function cadastrar(corpo) {
    const r = await cadastro.executar(api.post('/pacientes', corpo),
      `${corpo.nomeCompleto} cadastrado. Agora, o horário.`);
    if (!r) return;
    setNovo({ idPaciente: r.idPaciente, nomeCompleto: corpo.nomeCompleto,
      telefoneCelular: corpo.telefoneCelular });
    setCadastrando(null);
  }

  const mensagemConfirmacao = (c) => {
    const primeiro = (c.nomePaciente ?? '').split(' ')[0];
    const casa = clinica.dados?.nomeFantasia;
    return `Olá${primeiro ? `, ${primeiro}` : ''}! Aqui é da ${casa ?? 'clínica'}. `
      + `Passando para confirmar sua consulta amanhã, ${porExtenso(amanha)}, às `
      + `${hora(c.inicioEm)}, com ${nomeDoDentista(c.idDentista)}. Podemos confirmar?`;
  };

  return (
    <>
      <Cabecalho
        titulo="Recepção."
        detalhe={consultas.status === 'ok'
          ? [porExtenso(dia), `${contagem(deHoje.length, Infinity, 'consulta', 'consultas')} hoje`,
            aConfirmar.length > 0 && `${aConfirmar.length} a confirmar amanhã`]
            .filter(Boolean).join(' · ')
          : porExtenso(dia)}
        acao={pode('AGENDA', 'CRIAR') && (
          /* Abaixo de 1100px o painel de agendar desce para depois da fila. */
          <a href="#agendar" className="btn btn-fill recepcao-pular">Agendar</a>
        )}
      />

      <Aviso texto={sucesso ?? cadastro.sucesso} />

      {cadastrando !== null && (
        <section className="recepcao-cadastro" aria-labelledby="cadastro-titulo">
          <div className="app-cabecalho app-cabecalho-secao">
            <div>
              <h2 className="display display-xs" id="cadastro-titulo">Paciente novo.</h2>
              <p className="credit">Nome, nascimento e celular bastam para agendar. A triagem pode esperar a cadeira.</p>
            </div>
            <button type="button" className="btn" onClick={() => setCadastrando(null)}>Voltar</button>
          </div>
          <FichaPaciente
            inicial={{ nomeCompleto: cadastrando }}
            enviando={cadastro.enviando}
            erro={cadastro.erro}
            rotuloEnviar="Cadastrar e escolher horário"
            onEnviar={cadastrar}
          />
        </section>
      )}

      <div className="recepcao">
        <div className="recepcao-dia">
          <Estado status={consultas.status} erro={consultas.erro}
                  onTentarDeNovo={consultas.recarregar} esqueleto={{ linhas: 6, colunas: 4 }}>
            <Agora deHoje={deHoje} agora={agora} nomeDoDentista={nomeDoDentista}
                   executar={executar} enviando={enviando} />

            <section aria-labelledby="hoje-titulo">
              <div className="app-cabecalho app-cabecalho-secao">
                <div>
                  <h2 className="display display-xs" id="hoje-titulo">Ainda hoje.</h2>
                  <p className="credit">
                    {seguem.length === 0 ? 'Ninguém mais para chegar' : contagem(seguem.length, Infinity, 'consulta', 'consultas')}
                  </p>
                </div>
              </div>

              {seguem.length > 0 && (
                <Fila consultas={seguem} agora={agora} nomeDoDentista={nomeDoDentista}
                      executar={executar} enviando={enviando} />
              )}

              {passaram.length > 0 && (
                <details className="recepcao-passadas" open={pendentes.length > 0}>
                  <summary>
                    Já passaram · {passaram.length}
                    {pendentes.length > 0 && (
                      <span className="recepcao-pendente"> · {pendentes.length} sem desfecho</span>
                    )}
                  </summary>
                  <Fila consultas={passaram} agora={agora} nomeDoDentista={nomeDoDentista}
                        executar={executar} enviando={enviando} />
                </details>
              )}
            </section>

            <section aria-labelledby="amanha-titulo">
              <div className="app-cabecalho app-cabecalho-secao">
                <div>
                  <h2 className="display display-xs" id="amanha-titulo">Confirmar amanhã.</h2>
                  <p className="credit">
                    {porExtenso(amanha)}
                    {confirmadasAmanha > 0 && ` · ${confirmadasAmanha} já ${confirmadasAmanha === 1 ? 'confirmada' : 'confirmadas'}`}
                  </p>
                </div>
              </div>

              {aConfirmar.length === 0 ? (
                <div className="vazio">
                  <p className="vazio-titulo">{deAmanha.length === 0 ? 'Amanhã vazio.' : 'Tudo confirmado.'}</p>
                  <p className="body">
                    {deAmanha.length === 0
                      ? 'Nenhuma consulta marcada para amanhã.'
                      : 'Toda consulta de amanhã já tem confirmação.'}
                  </p>
                </div>
              ) : (
                <Tabela colunas={[
                  { chave: 'hora', rotulo: 'Horário', num: true },
                  { chave: 'paciente', rotulo: 'Paciente' },
                  { chave: 'dentista', rotulo: 'Dentista' },
                  { chave: 'acoes', rotulo: '' },
                ]}>
                  {aConfirmar.map((c) => {
                    const zap = whatsappHref(c.telefonePaciente, mensagemConfirmacao(c));
                    return (
                      <tr key={c.idConsulta}>
                        <Celula rotulo="Horário" num className="num recepcao-hora">{hora(c.inicioEm)}</Celula>
                        <CelulaPaciente c={c} />
                        <Celula rotulo="Dentista">{nomeDoDentista(c.idDentista)}</Celula>
                        <Celula rotulo="">
                          <div className="acoes recepcao-acoes">
                            {/* A mensagem sai pronta, com dia, hora e dentista: a
                                recepção revisa e envia, em vez de digitar trinta
                                vezes a mesma frase. */}
                            {zap && (
                              <a className="btn btn-sm" href={zap} target="_blank" rel="noopener">
                                WhatsApp
                              </a>
                            )}
                            <Transicoes consulta={c} cabe={(a) => a === 'confirmar'} className={null}
                                        executar={executar} enviando={enviando} />
                          </div>
                        </Celula>
                      </tr>
                    );
                  })}
                </Tabela>
              )}
            </section>
          </Estado>
        </div>

        {pode('AGENDA', 'CRIAR') && (
          <aside className="recepcao-lado">
            <Agendar
              key={novo?.idPaciente ?? 'agendar'}
              dentistas={dentistas.status === 'ok' ? dentistas.dados ?? [] : null}
              pacienteInicial={novo}
              onCadastrar={pode('PACIENTE', 'CRIAR') ? (nome) => setCadastrando(nome) : undefined}
              /* `novo` fica: trocá-lo mudaria a `key`, remontaria o painel e
                 apagaria o aviso de "consulta marcada" que ele acabou de dar. */
              onAgendado={consultas.recarregar}
            />
          </aside>
        )}
      </div>
    </>
  );
}

/**
 * O próximo, em tamanho de ser lido do outro lado do balcão.
 *
 * <p>É o único bloco invertido do sistema: a recepção olha para cá dezenas de
 * vezes por hora, e o resto da tela é contexto dele. A hora sobe da própria
 * máscara quando o próximo muda — o único movimento da tela, e ele diz que
 * alguém chegou à vez.
 */
function Agora({ deHoje, agora, nomeDoDentista, executar, enviando }) {
  const { naCadeira, proxima } = momento(deHoje, agora);
  const realizadas = deHoje.filter((c) => c.status === 'realizada').length;
  const faltas = deHoje.filter((c) => c.status === 'faltou').length;

  return (
    <section className="agora" aria-labelledby="agora-titulo">
      <h2 className="sr-only" id="agora-titulo">Agora</h2>

      {proxima ? (
        <div className="agora-proximo">
          <div className="agora-relogio">
            <p className="agora-hora">
              <span className="ln"><i key={proxima.idConsulta}>{hora(proxima.inicioEm)}</i></span>
            </p>
            <p className="cap">Próximo · {relativo(proxima.inicioEm, agora)}</p>
          </div>
          <div className="agora-quem">
            <p className="agora-nome">{nomeDoPaciente(proxima)}</p>
            <p className="agora-linha">
              {nomeDoDentista(proxima.idDentista)}
              {telHref(proxima.telefonePaciente) && (
                <> · <Telefone numero={proxima.telefonePaciente} /></>
              )}
            </p>
          </div>
          <div className="agora-acoes">
            <Selo mapa={STATUS_CONSULTA} valor={proxima.status} />
            <Transicoes consulta={proxima} cabe={(a) => a === 'confirmar'} className={null}
                        executar={executar} enviando={enviando} />
          </div>
        </div>
      ) : (
        <div className="agora-proximo agora-fim">
          <div className="agora-quem">
            <p className="agora-nome">
              {deHoje.length === 0 ? 'Dia sem consultas.' : 'Ninguém mais para chegar.'}
            </p>
            <p className="agora-linha">
              {deHoje.length === 0
                ? 'A grade de horários está toda livre.'
                : `${realizadas} ${realizadas === 1 ? 'atendida' : 'atendidas'} · ${faltas} ${faltas === 1 ? 'falta' : 'faltas'}`}
            </p>
          </div>
        </div>
      )}

      {naCadeira.length > 0 && (
        <ul className="agora-cadeira">
          {naCadeira.map((c) => (
            <li key={c.idConsulta}>
              <span className="cap">Na cadeira</span>
              <b>{nomeDoPaciente(c)}</b>
              <span>{nomeDoDentista(c.idDentista)} · até {hora(c.terminoEm)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

/* Nome e telefone na mesma célula: é o par que a recepção usa junto. */
function CelulaPaciente({ c }) {
  return (
    <Celula rotulo="Paciente" className="celula-nome">
      {nomeDoPaciente(c)}
      {telHref(c.telefonePaciente) && (
        <p className="cap cap-ash"><Telefone numero={c.telefonePaciente} /></p>
      )}
    </Celula>
  );
}

/**
 * A fila: o que segue hoje, ou o que já passou.
 *
 * <p>As ações mudam com o relógio, não só com o status: "Faltou" antes do
 * horário é chute, e é o tipo de clique que não tem volta.
 */
function Fila({ consultas, agora, nomeDoDentista, executar, enviando }) {
  return (
    <Tabela colunas={[
      { chave: 'hora', rotulo: 'Horário', num: true },
      { chave: 'paciente', rotulo: 'Paciente' },
      { chave: 'dentista', rotulo: 'Dentista' },
      { chave: 'status', rotulo: 'Status' },
      { chave: 'acoes', rotulo: '' },
    ]}>
      {consultas.map((c) => (
        <tr key={c.idConsulta} data-status={c.status}>
          <Celula rotulo="Horário" num className="num recepcao-hora">
            {hora(c.inicioEm)}<span className="cap cap-ash"> – {hora(c.terminoEm)}</span>
          </Celula>
          <CelulaPaciente c={c} />
          <Celula rotulo="Dentista">{nomeDoDentista(c.idDentista)}</Celula>
          <Celula rotulo="Status"><Selo mapa={STATUS_CONSULTA} valor={c.status} /></Celula>
          <Celula rotulo="">
            <Transicoes consulta={c} cabe={(a) => cabeAgora(a, c, agora)}
                        className="acoes recepcao-acoes" executar={executar} enviando={enviando} />
          </Celula>
        </tr>
      ))}
    </Tabela>
  );
}
