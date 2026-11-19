import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, query } from '../api.js';
import {
  casaBusca, fimDoDia, hoje, horarios, inicioDoDia, porExtenso, somarDias,
} from '../apresentacao.js';
import { useAcao, useRecurso } from '../dados.js';
import { formatarTelefone } from '../documento.js';
import { EQUIPE } from '../rotas.js';
import { Aviso, Esqueleto } from './primitivos.jsx';

const DURACOES = [30, 45, 60, 90];

/* A lista de pacientes vem de `/pacientes?limite=200`, que é o teto do
   controller. Passou disso, "ninguém com esse nome" pode ser mentira. */
const TETO_PACIENTES = 200;

/**
 * Marcar uma consulta: quem, com quem, quando — na ordem em que a recepção
 * pergunta ao telefone.
 *
 * <p>A grade mostra só o que cabe: é calculada sobre a agenda do dentista
 * naquele dia, com a mesma regra da EXCLUDE do banco. Sem ela a recepção
 * chutava um horário, tomava 409 e chutava outro com o paciente esperando na
 * linha.
 *
 * <p>Usado na Recepção (com `onCadastrar`, porque lá o paciente novo nasce no
 * mesmo fluxo) e na Agenda.
 */
export default function Agendar({
  pacientes, dentistas, diaInicial = hoje(), pacienteInicial = null,
  onCadastrar, onAgendado, podeVerEquipe = false,
}) {
  const [busca, setBusca] = useState('');
  const [paciente, setPaciente] = useState(pacienteInicial);
  const [idDentista, setIdDentista] = useState('');
  const [dia, setDia] = useState(diaInicial);
  const [duracao, setDuracao] = useState(30);
  const [escolhido, setEscolhido] = useState(null);
  const [obs, setObs] = useState('');

  /* `dentistas` null = ainda carregando. Tratar como lista vazia fazia o
     "Sem dentista" piscar a cada abertura da tela. */
  const carregandoDentistas = dentistas === null;
  const equipe = dentistas ?? [];

  /* Derivado, não estado: a lista de dentistas chega depois da montagem, e
     clínica com um dentista só não deveria precisar escolher. */
  const dentista = idDentista || (equipe.length === 1 ? String(equipe[0].idDentista) : '');
  const nomeDentista = equipe.find((d) => String(d.idDentista) === dentista)?.nomeCompleto;

  const ocupacao = useRecurso(dentista
    ? `/consultas${query({ de: inicioDoDia(dia), ate: fimDoDia(dia), idDentista: dentista })}`
    : null);
  const grade = useMemo(() => horarios(dia, ocupacao.dados ?? [], duracao),
    [dia, ocupacao.dados, duracao]);
  /* Trocar a duração pode fazer o horário escolhido deixar de caber. Aí ele
     some da seleção em vez de virar um 409. */
  const slot = grade.find((g) => g.hora === escolhido && g.livre);
  const livres = grade.filter((g) => g.livre).length;

  const encontrados = useMemo(() => (busca.trim()
    ? pacientes.filter((p) => casaBusca(busca, p.nomeCompleto, p.telefoneCelular)).slice(0, 6)
    : []), [busca, pacientes]);

  const escolher = (p) => { setPaciente(p); setBusca(''); };

  const { executar, enviando, erro, sucesso } = useAcao(() => {
    setPaciente(null);
    setEscolhido(null);
    setObs('');
    ocupacao.recarregar();
    onAgendado?.();
  });

  const enviar = (e) => {
    e.preventDefault();
    if (!paciente || !slot) return;
    executar(api.post('/consultas', {
      idPaciente: paciente.idPaciente,
      idDentista: Number(dentista),
      inicioEm: slot.inicio.toISOString(),
      terminoEm: slot.fim.toISOString(),
      observacoes: obs.trim() || null,
    }), `Consulta de ${paciente.nomeCompleto} marcada: ${porExtenso(dia)}, às ${slot.hora}.`)
      /* 409 de horário ocupado quer dizer que a grade envelheceu — outra
         pessoa marcou enquanto esta tela estava aberta. Recarregar mostra
         por quê. */
      .then((r) => { if (r === null) ocupacao.recarregar(); });
  };

  const mudarDia = (novo) => { setDia(novo); setEscolhido(null); };

  const falta = [!paciente && 'paciente', !dentista && 'dentista', !slot && 'horário']
    .filter(Boolean);

  return (
    <form className="agendar" onSubmit={enviar} aria-labelledby="agendar-titulo" id="agendar">
      <h2 className="display display-xs" id="agendar-titulo">Agendar.</h2>

      <Aviso texto={sucesso} />

      <div className="agendar-topo">
        <fieldset className="agendar-passo">
          <legend className="cap cap-ash">Paciente</legend>
          {paciente ? (
            <div className="agendar-escolha">
              <div>
                <p className="celula-nome">{paciente.nomeCompleto}</p>
                {paciente.telefoneCelular && (
                  <p className="cap cap-ash">{formatarTelefone(paciente.telefoneCelular)}</p>
                )}
              </div>
              <button type="button" className="btn btn-sm" onClick={() => setPaciente(null)}>
                Trocar
              </button>
            </div>
          ) : (
            <>
              <div className="campo-app">
                <input type="search" autoComplete="off" placeholder="Nome ou telefone"
                       aria-label="Buscar paciente por nome ou telefone"
                       value={busca} onChange={(e) => setBusca(e.target.value)}
                       /* Enter aqui enviaria o formulário inteiro. Com um
                          resultado só, ele vira o "é esse" de quem digita. */
                       onKeyDown={(e) => {
                         if (e.key !== 'Enter') return;
                         e.preventDefault();
                         if (encontrados.length === 1) escolher(encontrados[0]);
                       }} />
              </div>
              {encontrados.length > 0 && (
                <ul className="agendar-achados" aria-label="Pacientes encontrados">
                  {encontrados.map((p) => (
                    <li key={p.idPaciente}>
                      <button type="button" onClick={() => escolher(p)}>
                        <span className="celula-nome">{p.nomeCompleto ?? '—'}</span>
                        {p.telefoneCelular && (
                          <span className="cap cap-ash">{formatarTelefone(p.telefoneCelular)}</span>
                        )}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
              {busca.trim() && encontrados.length === 0 && (
                <p className="cap cap-ash">
                  Ninguém com esse nome ou telefone
                  {pacientes.length >= TETO_PACIENTES ? ' nos 200 primeiros do cadastro' : ''}.
                </p>
              )}
              {onCadastrar && (
                <button type="button" className="btn btn-sm agendar-novo"
                        onClick={() => onCadastrar(/\d/.test(busca) ? '' : busca.trim())}>
                  {busca.trim() && !/\d/.test(busca)
                    ? `Cadastrar “${busca.trim()}”`
                    : 'Cadastrar paciente novo'}
                </button>
              )}
            </>
          )}
        </fieldset>

        {equipe.length > 0 && (
          <div className="agendar-quando">
            <div className="agendar-linha">
              <label className="campo-app">
                <span className="cap cap-ash">Dentista</span>
                <select required value={dentista}
                        onChange={(e) => { setIdDentista(e.target.value); setEscolhido(null); }}>
                  <option value="" disabled>Escolha</option>
                  {equipe.map((d) => (
                    <option key={d.idDentista} value={d.idDentista}>{d.nomeCompleto}</option>
                  ))}
                </select>
              </label>
              <label className="campo-app">
                <span className="cap cap-ash">Dia</span>
                <input type="date" min={hoje()} value={dia}
                       onChange={(e) => mudarDia(e.target.value || hoje())} />
              </label>
            </div>

            <fieldset className="agendar-passo">
              <legend className="cap cap-ash">Duração</legend>
              <div className="acoes">
                {DURACOES.map((d) => (
                  <button key={d} type="button" className="btn btn-sm"
                          aria-pressed={duracao === d} onClick={() => setDuracao(d)}>
                    {d} min
                  </button>
                ))}
              </div>
            </fieldset>
          </div>
        )}
      </div>

      {carregandoDentistas && <Esqueleto linhas={3} colunas={4} />}
      {!carregandoDentistas && equipe.length === 0 && (
        <div className="agendar-sem">
          <p className="vazio-titulo">Sem dentista.</p>
          <p className="body body-ash">
            A agenda é por dentista, e esta clínica ainda não tem nenhum na equipe.{' '}
            {podeVerEquipe
              ? <>Admita um em <Link to={EQUIPE}>Equipe</Link>.</>
              : 'Quem administra a clínica admite um em Equipe.'}
          </p>
        </div>
      )}
      {equipe.length > 0 && (
        <>
          <fieldset className="agendar-passo">
            <legend className="cap cap-ash">
              Horário · {porExtenso(dia)}
              {dentista && ocupacao.status === 'ok' && grade.length > 0
                && ` · ${livres} ${livres === 1 ? 'livre' : 'livres'}`}
            </legend>
            {!dentista && (
              <p className="cap cap-ash">Escolha o dentista para ver os horários livres.</p>
            )}
            {dentista && ocupacao.status === 'carregando' && <Esqueleto linhas={2} colunas={6} />}
            {dentista && ocupacao.status === 'erro' && (
              <p className="erro-campo" role="alert">{ocupacao.erro?.message}</p>
            )}
            {dentista && ocupacao.status === 'ok' && (grade.length === 0 || livres === 0) && (
              <div className="acoes">
                <p className="body body-ash">
                  {grade.length === 0 ? 'O expediente deste dia já acabou.' : 'Nenhum horário livre neste dia.'}
                </p>
                <button type="button" className="btn btn-sm"
                        onClick={() => mudarDia(somarDias(dia, 1))}>
                  Dia seguinte
                </button>
              </div>
            )}
            {dentista && ocupacao.status === 'ok' && livres > 0 && (
              <div className="horarios">
                {grade.map((g) => (
                  <button key={g.hora} type="button" className="horario"
                          disabled={!g.livre}
                          aria-pressed={escolhido === g.hora}
                          aria-label={g.livre ? g.hora : `${g.hora}, ocupado`}
                          onClick={() => setEscolhido(g.hora)}>
                    {g.hora}
                  </button>
                ))}
              </div>
            )}
          </fieldset>

          <label className="campo-app">
            <span className="cap cap-ash">Observação</span>
            <textarea rows={2} maxLength={2000} value={obs} placeholder="ex.: retorno, trazer exame"
                      onChange={(e) => setObs(e.target.value)} />
          </label>

          {erro && <p className="erro-campo" role="alert">{erro.message}</p>}

          <div className="agendar-fim">
            {/* O botão desabilitado não explica por quê; a linha de cima sim. */}
            <p className="agendar-resumo" aria-live="polite">
              {falta.length === 0
                ? <><b>{paciente.nomeCompleto}</b> com {nomeDentista}, {porExtenso(dia)}, {slot.hora} às {slot.fim.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}.</>
                : `Falta escolher: ${falta.join(', ')}.`}
            </p>
            <button type="submit" className="btn btn-fill" disabled={falta.length > 0 || enviando}>
              {enviando ? 'Agendando…' : slot ? `Agendar ${slot.hora}` : 'Agendar'}
            </button>
          </div>
        </>
      )}
    </form>
  );
}
