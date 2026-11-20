import { useState } from 'react';
import { api } from '../api.js';
import { useAcao, useRecurso } from '../dados.js';
import { Cabecalho, Estado, usePode } from '../paginas/Layout.jsx';
import { Aviso, Celula, Tabela } from './primitivos.jsx';

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export const TIPOS = {
  lembrete: 'Lembrete', confirmacao: 'Confirmação', reforco: 'Reforço',
  oferta_vaga: 'Vaga ofertada', pos_procedimento: 'Pós-atendimento', orientacao: 'Orientação',
  resposta: 'Resposta', recebida: 'Do paciente',
};

export const SITUACOES = {
  agendada: 'Programada', enviando: 'Enviando', enviada: 'Enviada', entregue: 'Entregue',
  lida: 'Lida', falhou: 'Falhou', cancelada: 'Não enviada', recebida: 'Recebida',
};

/**
 * O paciente pelo WhatsApp, dentro do prontuário: a orientação que o dentista
 * manda (IA-38) com a prova de leitura, o histórico da conversa, e — para
 * quem tem LGPD — as preferências e o link do titular (IA-52, IA-53).
 */
export default function MensagensDoPaciente({ idPaciente }) {
  const pode = usePode();
  const mensagens = useRecurso(`/comunicacao/pacientes/${idPaciente}/mensagens`);
  const lista = mensagens.dados ?? [];

  return (
    <>
      {pode('PRONTUARIO', 'CRIAR') && (
        <Orientar idPaciente={idPaciente} onEnviada={mensagens.recarregar} />
      )}

      <h2 className="sub secao-titulo">Conversa pelo WhatsApp</h2>
      <Estado status={mensagens.status} erro={mensagens.erro} onTentarDeNovo={mensagens.recarregar}
              esqueleto={{ linhas: 3, colunas: 4 }}
              vazio={lista.length === 0 ? 'Nenhuma mensagem com este paciente.' : null}>
        <Tabela colunas={[
          { chave: 'quando', rotulo: 'Quando' },
          { chave: 'tipo', rotulo: 'Tipo' },
          { chave: 'texto', rotulo: 'Mensagem' },
          { chave: 'situacao', rotulo: 'Situação' },
        ]}>
          {lista.map(({ mensagem: m }) => (
            <tr key={m.idMensagem}>
              <Celula rotulo="Quando">
                {DATA_HORA.format(new Date(m.enviadaEm ?? m.enviarEm ?? m.criadaEm))}
              </Celula>
              <Celula rotulo="Tipo">{TIPOS[m.tipo] ?? m.tipo}</Celula>
              <Celula rotulo="Mensagem"><span className="mensagem-texto">{m.texto}</span></Celula>
              <Celula rotulo="Situação">
                {SITUACOES[m.status] ?? m.status}
                {m.lidaEm && ` em ${DATA_HORA.format(new Date(m.lidaEm))}`}
                {m.motivo && <span className="cap cap-ash"> · {m.motivo}</span>}
              </Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>

      {pode('LGPD') && <Privacidade idPaciente={idPaciente} />}
    </>
  );
}

function Orientar({ idPaciente, onEnviada }) {
  const temas = useRecurso('/comunicacao/orientacoes');
  const [tema, setTema] = useState('');
  const { executar, enviando, erro, sucesso } = useAcao(onEnviada);
  const escolhido = (temas.dados ?? []).find((t) => t.chave === tema);

  const enviar = (e) => {
    e.preventDefault();
    executar(api.post('/comunicacao/orientacoes', { idPaciente: Number(idPaciente), tema }),
      'Orientação programada. A leitura aparece aqui quando o paciente abrir.');
  };

  return (
    <form className="form-bloco" onSubmit={enviar}>
      <label className="campo-app">
        <span className="cap cap-ash">Orientação por WhatsApp (texto fixo, revisado)</span>
        <select value={tema} onChange={(e) => setTema(e.target.value)} required>
          <option value="">Escolha…</option>
          {(temas.dados ?? []).map((t) => <option key={t.chave} value={t.chave}>{t.titulo}</option>)}
        </select>
      </label>
      {escolhido && <p className="body body-ash orientacao-previa">{escolhido.texto}</p>}
      <Aviso texto={sucesso} />
      <Aviso texto={erro?.message} tom="erro" />
      <div className="acoes">
        <button type="submit" className="btn btn-sm" disabled={!tema || enviando}>
          {enviando ? 'Enviando…' : 'Enviar orientação'}
        </button>
      </div>
    </form>
  );
}

function Privacidade({ idPaciente }) {
  const preferencias = useRecurso(`/lgpd/pacientes/${idPaciente}/preferencias`);
  const [link, setLink] = useState(null);
  const { executar, enviando, erro, sucesso } = useAcao(preferencias.recarregar);

  const alternar = (p) => executar(
    api.put(`/lgpd/pacientes/${idPaciente}/preferencias/${p.finalidade}`, { permitido: !p.permitido }),
    'Preferência registrada, a pedido do paciente.');

  const gerar = async () => {
    const r = await executar(api.post(`/lgpd/pacientes/${idPaciente}/link-titular`));
    if (r) setLink({ url: `${window.location.origin}/titular#${r.token}`, expiraEm: r.expiraEm });
  };

  return (
    <section aria-label="Privacidade do paciente">
      <Cabecalho titulo="Privacidade." detalhe="O que o paciente autorizou, finalidade por finalidade." />
      <Aviso texto={sucesso} />
      <Aviso texto={erro?.message} tom="erro" />
      <Estado status={preferencias.status} erro={preferencias.erro}
              onTentarDeNovo={preferencias.recarregar} esqueleto={{ linhas: 3, colunas: 2 }}>
        <ul className="lista">
          {(preferencias.dados ?? []).map((p) => (
            <li key={p.finalidade} className="preferencia">
              <span>
                {p.descricao}
                <span className="cap cap-ash">
                  {' · '}{p.permitido ? 'autorizado' : 'não autorizado'}
                  {p.alteradaEm ? ` em ${DATA_HORA.format(new Date(p.alteradaEm))} (${p.origem})` : ' (padrão)'}
                </span>
              </span>
              <button type="button" className="btn btn-sm" disabled={enviando} onClick={() => alternar(p)}>
                {p.permitido ? 'Desligar' : 'Ligar'}
              </button>
            </li>
          ))}
        </ul>
      </Estado>
      <div className="acoes">
        <button type="button" className="btn btn-sm" disabled={enviando} onClick={gerar}>
          Gerar link do paciente
        </button>
      </div>
      {link && (
        <div className="form-bloco">
          <p className="cap cap-ash">
            Válido até {DATA_HORA.format(new Date(link.expiraEm))}. Aparece uma vez só; gerar outro
            derruba este. Entregue ao paciente — com ele, ele vê quem acessou os dados e decide o que autoriza.
          </p>
          <p className="body link-titular">{link.url}</p>
          <button type="button" className="btn btn-sm"
                  onClick={() => navigator.clipboard.writeText(link.url)}>Copiar link</button>
        </div>
      )}
    </section>
  );
}
