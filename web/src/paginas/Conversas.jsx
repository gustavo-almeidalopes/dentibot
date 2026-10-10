import { useState } from 'react';
import { api } from '../api.js';
import LinkDoPaciente from '../components/LinkDoPaciente.jsx';
import { Aviso, Celula, Tabela } from '../components/primitivos.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { Cabecalho, Estado, usePode } from './Layout.jsx';

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

const PERIODOS = [['qualquer', 'Qualquer horário'], ['manha', 'Manhã'], ['tarde', 'Tarde']];
const URGENCIAS = [[1, 'Normal'], [2, 'Alta'], [3, 'Muito alta']];
const TIPOS_DO_CANAL = {
  confirmacao: 'Lembrete e confirmação de consulta (IA-16)',
  oferta_vaga: 'Vaga liberada para a lista de espera (IA-18)',
  pos_procedimento: 'Acompanhamento no dia seguinte (IA-37)',
};

/**
 * O WhatsApp da clínica, do lado de quem atende (Doc 03-D).
 *
 * <p>A recepção virtual resolve confirmar, cancelar e aceitar vaga sozinha, a
 * qualquer hora; o que chega aqui é o que precisa de gente — urgência no topo,
 * marcada com o único vermelho da tela, porque é exatamente para isso que ele
 * existe.
 */
export default function Conversas() {
  const pode = usePode();
  return (
    <>
      <Cabecalho titulo="Conversas." detalhe="O que o robô passou para gente, e quem espera vaga." />
      <Escaladas />
      <ListaDeEspera />
      {pode('CONFIGURACAO') && <Canal podeAlterar={pode('CONFIGURACAO', 'ALTERAR')} />}
    </>
  );
}

function Escaladas() {
  const escaladas = useRecurso('/comunicacao/escaladas');
  const lista = escaladas.dados ?? [];
  return (
    <>
      <h2 className="sub secao-titulo">Precisam de gente</h2>
      <Estado status={escaladas.status} erro={escaladas.erro} onTentarDeNovo={escaladas.recarregar}
              esqueleto={{ linhas: 2, colunas: 1 }}
              vazio={lista.length === 0 ? 'Ninguém esperando resposta.' : null}>
        <ul className="lista conversas">
          {lista.map(({ mensagem, nomePaciente }) => (
            <Escalada key={mensagem.idMensagem} m={mensagem} nome={nomePaciente}
                      onFeito={escaladas.recarregar} />
          ))}
        </ul>
      </Estado>
    </>
  );
}

function Escalada({ m, nome, onFeito }) {
  const [texto, setTexto] = useState('');
  const { executar, enviando, erro, sucesso } = useAcao(onFeito);

  const responder = async (e) => {
    e.preventDefault();
    if (await executar(api.post(`/comunicacao/mensagens/${m.idMensagem}/respostas`, { texto }),
      'Resposta na fila de envio.')) setTexto('');
  };
  const resolver = () => executar(api.post(`/comunicacao/mensagens/${m.idMensagem}/resolucao`));

  return (
    <li className="conversa" data-alarme={m.escalada === 'urgente' ? 'sim' : undefined}>
      <p className="cap">
        {m.escalada === 'urgente' && <span className="conversa-urgente">Urgente · </span>}
        {/* Sem nome, quem escreveu não é paciente cadastrado: só o número. */}
        {nome ? <LinkDoPaciente idPaciente={m.idPaciente} nome={nome} /> : `+${m.telefone}`}
        {' · '}{DATA_HORA.format(new Date(m.criadaEm))}
      </p>
      <p className="body conversa-texto">{m.texto}</p>
      <form className="conversa-resposta" onSubmit={responder}>
        <label className="campo-app">
          <span className="cap cap-ash">Responder pelo WhatsApp</span>
          <textarea rows={2} maxLength={1000} value={texto} onChange={(e) => setTexto(e.target.value)} />
        </label>
        <div className="acoes">
          <button type="submit" className="btn btn-sm" disabled={!texto.trim() || enviando}>Responder</button>
          <button type="button" className="btn btn-sm" disabled={enviando} onClick={resolver}>Resolvido</button>
        </div>
      </form>
      <Aviso texto={sucesso} />
      <Aviso texto={erro?.message} tom="erro" />
    </li>
  );
}

function ListaDeEspera() {
  const espera = useRecurso('/comunicacao/espera');
  const pacientes = useRecurso('/pacientes?limite=200');
  const [nova, setNova] = useState({ idPaciente: '', periodo: 'qualquer', urgencia: 1, observacao: '' });
  const { executar, enviando, erro, sucesso } = useAcao(espera.recarregar);
  const lista = espera.dados ?? [];

  const entrar = async (e) => {
    e.preventDefault();
    const r = await executar(api.post('/comunicacao/espera', {
      ...nova, idPaciente: Number(nova.idPaciente), urgencia: Number(nova.urgencia),
      observacao: nova.observacao || null,
    }), 'Paciente na lista de espera.');
    if (r) setNova({ idPaciente: '', periodo: 'qualquer', urgencia: 1, observacao: '' });
  };

  return (
    <>
      <h2 className="sub secao-titulo">Lista de espera</h2>
      <p className="body body-ash">
        Quando uma consulta é cancelada, a vaga vai pelo WhatsApp para até três pessoas daqui,
        por urgência e antiguidade. Quem responder SIM primeiro fica com ela.
      </p>
      <form className="form-bloco form-linha" onSubmit={entrar}>
        <label className="campo-app">
          <span className="cap cap-ash">Paciente</span>
          <select value={nova.idPaciente} required
                  onChange={(e) => setNova({ ...nova, idPaciente: e.target.value })}>
            <option value="">Escolha…</option>
            {(pacientes.dados ?? []).map((p) => (
              <option key={p.idPaciente} value={p.idPaciente}>{p.nomeCompleto}</option>
            ))}
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Período</span>
          <select value={nova.periodo} onChange={(e) => setNova({ ...nova, periodo: e.target.value })}>
            {PERIODOS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Urgência</span>
          <select value={nova.urgencia} onChange={(e) => setNova({ ...nova, urgencia: e.target.value })}>
            {URGENCIAS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Observação</span>
          <input maxLength={200} value={nova.observacao}
                 onChange={(e) => setNova({ ...nova, observacao: e.target.value })} />
        </label>
        <div className="acoes">
          <button type="submit" className="btn btn-sm" disabled={enviando}>Pôr na lista</button>
        </div>
      </form>
      <Aviso texto={sucesso} />
      <Aviso texto={erro?.message} tom="erro" />
      <Estado status={espera.status} erro={espera.erro} onTentarDeNovo={espera.recarregar}
              esqueleto={{ linhas: 2, colunas: 4 }}
              vazio={lista.length === 0 ? 'Ninguém na lista de espera.' : null}>
        <Tabela colunas={[
          { chave: 'paciente', rotulo: 'Paciente' },
          { chave: 'periodo', rotulo: 'Período' },
          { chave: 'urgencia', rotulo: 'Urgência' },
          { chave: 'desde', rotulo: 'Desde' },
          { chave: 'acao', rotulo: '' },
        ]}>
          {lista.map((e) => (
            <tr key={e.idEspera}>
              <Celula rotulo="Paciente">
                <LinkDoPaciente idPaciente={e.idPaciente} nome={e.nomePaciente} />
                {e.observacao && <span className="cap cap-ash"> · {e.observacao}</span>}
              </Celula>
              <Celula rotulo="Período">{PERIODOS.find(([v]) => v === e.periodo)?.[1]}</Celula>
              <Celula rotulo="Urgência">{URGENCIAS.find(([v]) => v === e.urgencia)?.[1]}</Celula>
              <Celula rotulo="Desde">{DATA_HORA.format(new Date(e.desde))}</Celula>
              <Celula rotulo="">
                <button type="button" className="btn btn-sm" disabled={enviando}
                        onClick={() => executar(api.del(`/comunicacao/espera/${e.idEspera}`),
                          'Saiu da lista.')}>
                  Tirar da lista
                </button>
              </Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}

function Canal({ podeAlterar }) {
  const canal = useRecurso('/comunicacao/canal');
  return (
    <>
      <h2 className="sub secao-titulo">Canal</h2>
      <Estado status={canal.status} erro={canal.erro} onTentarDeNovo={canal.recarregar}
              esqueleto={{ linhas: 2, colunas: 2 }}>
        {canal.dados && (
          <FormularioDoCanal atual={canal.dados} podeAlterar={podeAlterar} onSalvo={canal.recarregar} />
        )}
      </Estado>
    </>
  );
}

function FormularioDoCanal({ atual, podeAlterar, onSalvo }) {
  const [numero, setNumero] = useState(atual.phoneNumberId ?? '');
  const [ativo, setAtivo] = useState(atual.ativo);
  const [desligados, setDesligados] = useState(atual.tiposDesligados);
  const { executar, enviando, erro, sucesso } = useAcao(onSalvo);

  const salvar = (e) => {
    e.preventDefault();
    executar(api.put('/comunicacao/canal', { phoneNumberId: numero, ativo, tiposDesligados: desligados }),
      'Canal salvo.');
  };

  return (
    <form className="form-bloco" onSubmit={salvar}>
      {!atual.provedorConfigurado && (
        <Aviso tom="erro" texto="Esta instância não tem o token do WhatsApp (DENTIBOT_WHATSAPP_TOKEN): as mensagens ficam programadas e nada sai." />
      )}
      <label className="campo-app">
        <span className="cap cap-ash">Phone number ID (Meta Business)</span>
        <input inputMode="numeric" pattern="[0-9]{5,30}" required value={numero}
               disabled={!podeAlterar} onChange={(e) => setNumero(e.target.value)} />
      </label>
      <label className="campo-check">
        <input type="checkbox" checked={ativo} disabled={!podeAlterar}
               onChange={(e) => setAtivo(e.target.checked)} />
        {' '}Canal ativo
      </label>
      {atual.tiposDisponiveis.map((t) => (
        <label key={t} className="campo-check">
          <input type="checkbox" checked={!desligados.includes(t)} disabled={!podeAlterar}
                 onChange={(e) => setDesligados((d) => (e.target.checked
                   ? d.filter((x) => x !== t) : [...d, t]))} />
          {' '}{TIPOS_DO_CANAL[t] ?? t}
        </label>
      ))}
      <Aviso texto={sucesso} />
      <Aviso texto={erro?.message} tom="erro" />
      {podeAlterar && (
        <div className="acoes">
          <button type="submit" className="btn btn-fill" disabled={enviando}>Salvar</button>
        </div>
      )}
    </form>
  );
}
