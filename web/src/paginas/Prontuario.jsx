import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useAcao, useRecurso } from '../dados.js';
import { PACIENTES } from '../rotas.js';
import { Cabecalho, Estado } from './Layout.jsx';

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short', timeStyle: 'short',
});

/* Arcadas em notação FDI (ISO 3950), na ordem em que o dentista as vê. */
const SUPERIOR = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28];
const INFERIOR = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38];

const CONDICOES = ['hígido', 'cárie', 'restaurado', 'ausente', 'implante', 'coroa', 'fraturado'];
const FACES = ['V', 'L', 'M', 'D', 'O', 'I', 'P'];

export default function Prontuario() {
  const { idPaciente } = useParams();
  const [aba, setAba] = useState('evolucoes');

  const evolucoes = useRecurso(`/pacientes/${idPaciente}/prontuario/evolucoes`);
  const odontograma = useRecurso(`/pacientes/${idPaciente}/prontuario/odontograma`);

  return (
    <>
      <Cabecalho
        titulo="Prontuário."
        /* O aviso não é decoração: toda abertura desta tela grava uma linha de
           auditoria de LEITURA, e quem abre precisa saber disso. */
        detalhe={`Paciente ${idPaciente} · cada abertura fica registrada na trilha de auditoria`}
        acao={<Link className="btn" to={PACIENTES}>Voltar aos pacientes</Link>}
      />

      <div className="abas" role="tablist" aria-label="Seções do prontuário">
        <button type="button" role="tab" className="btn"
                aria-selected={aba === 'evolucoes'}
                onClick={() => setAba('evolucoes')}>Evolução</button>
        <button type="button" role="tab" className="btn"
                aria-selected={aba === 'odontograma'}
                onClick={() => setAba('odontograma')}>Odontograma</button>
      </div>

      {aba === 'evolucoes' ? (
        <Evolucoes idPaciente={idPaciente} recurso={evolucoes} />
      ) : (
        <Odontograma idPaciente={idPaciente} recurso={odontograma} />
      )}
    </>
  );
}

function Evolucoes({ idPaciente, recurso }) {
  const lista = recurso.dados ?? [];
  const [texto, setTexto] = useState('');
  const { executar, enviando, erro } = useAcao(() => { setTexto(''); recurso.recarregar(); });

  /* Uma entrada que retifica outra aponta para ela, e as DUAS continuam
     visíveis. Prontuário se corrige somando, nunca sobrescrevendo (CFO-226) —
     por isso não existe botão de editar aqui, em lugar nenhum. */
  const retificadas = new Set(lista.map((e) => e.retificaEvolucao).filter(Boolean));

  return (
    <>
      <form
        className="form-bloco"
        onSubmit={(e) => {
          e.preventDefault();
          executar(api.post(`/pacientes/${idPaciente}/prontuario/evolucoes`,
            { descricao: texto.trim() }));
        }}
      >
        <label className="campo-app">
          <span className="cap cap-ash">Nova evolução</span>
          <textarea required rows={4} maxLength={20000} value={texto}
                    onChange={(e) => setTexto(e.target.value)}
                    placeholder="Procedimento realizado, materiais, intercorrências…" />
        </label>
        <p className="cap cap-ash">
          Registro clínico é append-only: depois de gravado não se apaga nem se edita.
          Uma correção entra como retificação, e as duas ficam visíveis.
        </p>
        {erro && <p className="pagamento-erro" role="alert">{erro.message}</p>}
        <button type="submit" className="btn btn-fill" disabled={enviando || !texto.trim()}>
          {enviando ? 'Registrando…' : 'Registrar evolução'}
        </button>
      </form>

      <Estado status={recurso.status} erro={recurso.erro} onTentarDeNovo={recurso.recarregar}
              vazio={lista.length === 0 ? 'Nenhuma evolução registrada.' : null}>
        <ol className="lista">
          {lista.map((e) => (
            <li className="row row-evolucao" key={e.idEvolucao}
                data-retificada={retificadas.has(e.idEvolucao) ? 'sim' : 'nao'}>
              <div>
                <p className="cap cap-ash">{DATA_HORA.format(new Date(e.registradoEm))}</p>
                <p className="cap cap-ash">Dentista {e.idDentista}</p>
              </div>
              <div>
                {e.retificaEvolucao && (
                  <p className="cap" style={{ color: 'var(--alarm)' }}>
                    Retifica a evolução #{e.retificaEvolucao} · {e.motivoRetificacao}
                  </p>
                )}
                {retificadas.has(e.idEvolucao) && (
                  <p className="cap cap-ash">Esta entrada foi retificada depois.</p>
                )}
                <p className="body" style={{ whiteSpace: 'pre-wrap' }}>{e.descricao}</p>
              </div>
            </li>
          ))}
        </ol>
      </Estado>
    </>
  );
}

function Odontograma({ idPaciente, recurso }) {
  const lancamentos = recurso.dados ?? [];
  const [selecionado, setSelecionado] = useState(null);
  const { executar, enviando, erro } = useAcao(() => {
    setSelecionado(null);
    recurso.recarregar();
  });

  /* O estado ATUAL de um dente é o ÚLTIMO lançamento dele — a tabela é
     append-only e o histórico sai de graça. A lista vem ordenada por data
     crescente, então o último a escrever no mapa é o mais novo. */
  const atual = new Map();
  for (const l of lancamentos) atual.set(l.dente, l);

  return (
    <>
      <Estado status={recurso.status} erro={recurso.erro} onTentarDeNovo={recurso.recarregar}>
        <div className="odonto">
          {[SUPERIOR, INFERIOR].map((arcada, i) => (
            <div className="odonto-arcada" key={i}>
              {arcada.map((dente) => {
                const l = atual.get(dente);
                return (
                  <button
                    key={dente}
                    type="button"
                    className="odonto-dente"
                    data-condicao={l?.condicao ?? 'higido'}
                    aria-pressed={selecionado === dente}
                    aria-label={`Dente ${dente}${l ? `: ${l.condicao}` : ''}`}
                    onClick={() => setSelecionado(selecionado === dente ? null : dente)}
                  >
                    {dente}
                  </button>
                );
              })}
            </div>
          ))}
        </div>
      </Estado>

      {selecionado && (
        <LancarCondicao
          dente={selecionado}
          atual={atual.get(selecionado)}
          enviando={enviando}
          erro={erro}
          onLancar={(corpo) => executar(
            api.post(`/pacientes/${idPaciente}/prontuario/odontograma`,
              { dente: selecionado, ...corpo }),
          )}
        />
      )}
    </>
  );
}

function LancarCondicao({ dente, atual, onLancar, enviando, erro }) {
  const [condicao, setCondicao] = useState(CONDICOES[1]);
  const [face, setFace] = useState('');
  const [observacao, setObservacao] = useState('');

  return (
    <form
      className="form-bloco"
      onSubmit={(e) => {
        e.preventDefault();
        onLancar({ face: face || null, condicao, observacao: observacao.trim() || null });
      }}
    >
      <p className="sub">Dente {dente}</p>
      {atual && (
        <p className="cap cap-ash">
          Hoje: {atual.condicao}{atual.face ? ` · face ${atual.face}` : ''}
          {atual.observacao ? ` · ${atual.observacao}` : ''}
        </p>
      )}

      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Condição</span>
          <select value={condicao} onChange={(e) => setCondicao(e.target.value)}>
            {CONDICOES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Face</span>
          <select value={face} onChange={(e) => setFace(e.target.value)}>
            {/* Vazio é o dente inteiro, não "não informado": há condição que não
                é de face nenhuma (ausente, implante). */}
            <option value="">Dente inteiro</option>
            {FACES.map((f) => <option key={f} value={f}>{f}</option>)}
          </select>
        </label>
      </div>

      <label className="campo-app">
        <span className="cap cap-ash">Observação</span>
        <input maxLength={300} value={observacao} onChange={(e) => setObservacao(e.target.value)} />
      </label>

      {erro && <p className="pagamento-erro" role="alert">{erro.message}</p>}
      <button type="submit" className="btn btn-fill" disabled={enviando}>
        {enviando ? 'Lançando…' : 'Lançar'}
      </button>
    </form>
  );
}
