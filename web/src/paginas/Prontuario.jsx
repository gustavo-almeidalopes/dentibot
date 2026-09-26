import { useMemo, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { api, baixar } from '../api.js';
import { enviarAnexo } from '../anexos.js';
import ResumoPaciente from '../components/ResumoPaciente.jsx';
import { Aviso, Celula, Tabela } from '../components/primitivos.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { PACIENTES } from '../rotas.js';
import { Cabecalho, Estado, usePode } from './Layout.jsx';

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short', timeStyle: 'short',
});

/* Arcadas em notação FDI (ISO 3950), na ordem em que o dentista as vê. */
const SUPERIOR = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28];
const INFERIOR = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38];

const ABAS = [['evolucoes', 'Evolução'], ['odontograma', 'Odontograma'], ['anexos', 'Anexos']];

const TIPOS_DE_ANEXO = [
  ['radiografia', 'Radiografia'], ['foto_intraoral', 'Foto intraoral'],
  ['documento', 'Documento'], ['laudo', 'Laudo'], ['modelo_3d', 'Modelo 3D'],
];

const CONDICOES = ['hígido', 'cárie', 'restaurado', 'ausente', 'implante', 'coroa', 'fraturado'];
const FACES = ['V', 'L', 'M', 'D', 'O', 'I', 'P'];

export default function Prontuario() {
  const { idPaciente } = useParams();
  const { state } = useLocation();
  const [aba, setAba] = useState('evolucoes');

  const evolucoes = useRecurso(`/pacientes/${idPaciente}/prontuario/evolucoes`);
  const odontograma = useRecurso(`/pacientes/${idPaciente}/prontuario/odontograma`);

  /* Veio da tela de Pacientes, onde o nome já estava carregado. Quem entra pela
     URL direta cai no id, que é honesto — e não custa uma chamada a mais. */
  const nomePaciente = state?.nomePaciente ?? `Paciente ${idPaciente}`;

  return (
    <>
      <Cabecalho
        titulo={nomePaciente}
        detalhe="Prontuário"
        acao={(
          <div className="acoes">
            <Exportar idPaciente={idPaciente} />
            <Link className="btn btn-sm" to={PACIENTES}>Voltar aos pacientes</Link>
          </div>
        )}
      />

      {/* Não é decoração: toda abertura desta tela grava uma linha de auditoria
          de LEITURA, e quem abre precisa saber disso. Em letra miúda na linha
          de detalhe, ninguém lia. */}
      <p className="faixa-auditoria">
        Abrir este prontuário grava uma linha na trilha de auditoria, com o seu nome.
      </p>

      <ResumoPaciente idPaciente={idPaciente} />

      <div className="abas" role="tablist" aria-label="Seções do prontuário">
        {ABAS.map(([id, rotulo]) => (
          <button
            key={id}
            type="button"
            role="tab"
            id={`aba-${id}`}
            aria-controls={`painel-${id}`}
            aria-selected={aba === id}
            /* Aba não selecionada sai da ordem de Tab: no padrão ARIA o grupo
               inteiro é UMA parada, e as setas andam dentro dele. Sem isto o
               leitor de tela anunciava "guia" e o teclado se comportava como
               uma fileira de botões — ARIA pela metade mente sobre o que a
               coisa é. */
            tabIndex={aba === id ? 0 : -1}
            onKeyDown={(e) => {
              if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
              const i = ABAS.findIndex(([a]) => a === aba);
              const passo = e.key === 'ArrowRight' ? 1 : ABAS.length - 1;
              const outra = ABAS[(i + passo) % ABAS.length][0];
              setAba(outra);
              document.getElementById(`aba-${outra}`)?.focus();
            }}
            onClick={() => setAba(id)}
            className="btn"
          >
            {rotulo}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`painel-${aba}`} aria-labelledby={`aba-${aba}`}>
        {aba === 'evolucoes' && <Evolucoes idPaciente={idPaciente} recurso={evolucoes} />}
        {aba === 'odontograma' && <Odontograma idPaciente={idPaciente} recurso={odontograma} />}
        {aba === 'anexos' && <Anexos idPaciente={idPaciente} />}
      </div>
    </>
  );
}

function Evolucoes({ idPaciente, recurso }) {
  const lista = recurso.dados ?? [];
  const [texto, setTexto] = useState('');
  const { executar, enviando, erro, sucesso } = useAcao(() => {
    setTexto('');
    recurso.recarregar();
  });

  /* Exceção deliberada ao "nenhum endpoint novo": a evolução traz `idDentista`
     e não o nome, e "Dentista 3" assinando registro clínico é o tipo de coisa
     que derruba uma demonstração. A Auditoria já chama /equipe pelo mesmo
     motivo — `auditoria` não pode depender de `identidade`, e quem casa os
     dois lados é a tela. */
  const equipe = useRecurso('/equipe');
  const nomeDoDentista = useMemo(() => {
    const mapa = new Map((equipe.dados ?? []).map((m) => [m.idUsuario, m.nomeCompleto]));
    return (id) => mapa.get(id) ?? `Dentista ${id}`;
  }, [equipe.dados]);

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
            { descricao: texto.trim() }), 'Evolução registrada.');
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
        {erro && <p className="erro-campo" role="alert">{erro.message}</p>}
        <button type="submit" className="btn btn-fill" disabled={enviando || !texto.trim()}>
          {enviando ? 'Registrando…' : 'Registrar evolução'}
        </button>
      </form>

      <Aviso texto={sucesso} />

      <Estado status={recurso.status} erro={recurso.erro} onTentarDeNovo={recurso.recarregar}
              esqueleto={{ linhas: 4, colunas: 2 }}
              vazio={lista.length === 0 ? 'Nenhuma evolução registrada.' : null}>
        <ol className="lista">
          {lista.map((e) => (
            <li className="evolucao" key={e.idEvolucao}
                data-retificada={retificadas.has(e.idEvolucao) ? 'sim' : 'nao'}>
              <div>
                <p className="cap cap-ash">{DATA_HORA.format(new Date(e.registradoEm))}</p>
                <p className="cap cap-ash">{nomeDoDentista(e.idDentista)}</p>
              </div>
              <div>
                {e.retificaEvolucao && (
                  <p className="cap" style={{ color: 'var(--alarm)' }}>
                    Retifica a evolução #{e.retificaEvolucao} · {e.motivoRetificacao}
                  </p>
                )}
                {/* Selo e não opacidade: o texto precisa continuar legível.
                    Opacidade em registro clínico é perda de informação. */}
                {retificadas.has(e.idEvolucao) && (
                  <p><span className="selo" data-tom="alarme">Retificada depois</span></p>
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
  const { executar, enviando, erro, sucesso } = useAcao(() => {
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
      {/* Sete condições codificadas em borda e nenhuma legenda: ninguém sabia
          ler o que a tela estava dizendo. */}
      <ul className="odonto-legenda">
        {CONDICOES.map((c) => (
          <li key={c}><span className="odonto-amostra" data-condicao={c} />{c}</li>
        ))}
      </ul>

      <Aviso texto={sucesso} />

      {/* Em tela de mesa o formulário do dente abre AO LADO da arcada: abrindo
          abaixo, cada clique num dente rolava a página. */}
      <div className="odonto-area">
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
              `Dente ${selecionado}: condição lançada.`,
            )}
          />
        )}
      </div>
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

      {erro && <p className="erro-campo" role="alert">{erro.message}</p>}
      <button type="submit" className="btn btn-fill" disabled={enviando}>
        {enviando ? 'Lançando…' : 'Lançar'}
      </button>
    </form>
  );
}

/**
 * Portabilidade (IA-47, LGPD art. 18): o arquivo com tudo o que a clínica tem
 * sobre o paciente. Só aparece para quem o back-end deixa exportar — hoje, o
 * admin —, e o hash que ele calculou fica na tela para ser passado ao paciente
 * junto com o arquivo.
 */
function Exportar({ idPaciente }) {
  const pode = usePode();
  const [estado, setEstado] = useState({ enviando: false, sha256: null, erro: null });
  if (!pode('LGPD', 'CRIAR')) return null;

  const exportar = async () => {
    setEstado({ enviando: true, sha256: null, erro: null });
    try {
      const { blob, nomeArquivo, sha256 } = await baixar(`/pacientes/${idPaciente}/exportacao`);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = nomeArquivo;
      a.click();
      URL.revokeObjectURL(url);
      setEstado({ enviando: false, sha256, erro: null });
    } catch (e) {
      setEstado({ enviando: false, sha256: null, erro: e.message });
    }
  };

  return (
    <>
      <button type="button" className="btn btn-sm" disabled={estado.enviando} onClick={exportar}>
        {estado.enviando ? 'Exportando…' : 'Exportar dados (LGPD)'}
      </button>
      <Aviso texto={estado.sha256 && `Arquivo exportado. SHA-256: ${estado.sha256}`} />
      <Aviso texto={estado.erro} tom="erro" />
    </>
  );
}

/**
 * Anexos clínicos (ST-41). O arquivo vai do navegador direto ao bucket; a tela
 * só pede a URL assinada e depois a confirmação. "Abrir" pede uma URL de cinco
 * minutos a cada clique — e cada clique fica na trilha de auditoria.
 */
function Anexos({ idPaciente }) {
  const anexos = useRecurso(`/pacientes/${idPaciente}/prontuario/anexos`);
  const [tipo, setTipo] = useState('radiografia');
  const [arquivo, setArquivo] = useState(null);
  const { executar, enviando, erro, sucesso } = useAcao(() => {
    setArquivo(null);
    anexos.recarregar();
  });
  const [erroAbrir, setErroAbrir] = useState(null);
  const lista = anexos.dados ?? [];

  const abrir = async (idAnexo) => {
    setErroAbrir(null);
    try {
      const { url } = await api.get(`/pacientes/${idPaciente}/prontuario/anexos/${idAnexo}/url`);
      window.open(url, '_blank', 'noopener');
    } catch (e) {
      setErroAbrir(e.message);
    }
  };

  return (
    <>
      <form className="filtros" onSubmit={(e) => {
        e.preventDefault();
        if (arquivo) executar(enviarAnexo(idPaciente, tipo, arquivo), 'Anexo enviado e conferido.');
      }}>
        <label className="campo-app">
          <span className="cap cap-ash">Tipo</span>
          <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
            {TIPOS_DE_ANEXO.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Arquivo (até 50 MB)</span>
          <input type="file" accept="image/jpeg,image/png,image/webp,application/pdf,.dcm,.stl,.obj"
                 onChange={(e) => setArquivo(e.target.files?.[0] ?? null)} />
        </label>
        <button type="submit" className="btn btn-sm" disabled={!arquivo || enviando}>
          {enviando ? 'Enviando…' : 'Anexar'}
        </button>
      </form>
      <Aviso texto={sucesso} />
      <Aviso texto={erro?.message ?? erroAbrir} tom="erro" />

      <Estado status={anexos.status} erro={anexos.erro} onTentarDeNovo={anexos.recarregar}
              esqueleto={{ linhas: 3, colunas: 3 }}
              vazio={lista.length === 0 ? 'Nenhum anexo neste prontuário.' : null}>
        <Tabela colunas={[
          { chave: 'quando', rotulo: 'Enviado' },
          { chave: 'tipo', rotulo: 'Tipo' },
          { chave: 'nome', rotulo: 'Arquivo' },
          { chave: 'tamanho', rotulo: 'Tamanho', num: true },
          { chave: 'acao', rotulo: '' },
        ]}>
          {lista.map((a) => (
            <tr key={a.idAnexo}>
              <Celula rotulo="Enviado">{DATA_HORA.format(new Date(a.enviadoEm))}</Celula>
              <Celula rotulo="Tipo">
                {TIPOS_DE_ANEXO.find(([v]) => v === a.tipo)?.[1] ?? a.tipo}
              </Celula>
              <Celula rotulo="Arquivo">{a.nomeArquivo}</Celula>
              <Celula rotulo="Tamanho" num>{(a.tamanhoBytes / 1024).toFixed(0)} KB</Celula>
              <Celula rotulo="">
                <button type="button" className="btn btn-sm" onClick={() => abrir(a.idAnexo)}>
                  Abrir
                </button>
              </Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}
