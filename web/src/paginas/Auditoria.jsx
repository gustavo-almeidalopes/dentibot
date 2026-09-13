import { useMemo, useState } from 'react';
import { query } from '../api.js';
import { useRecurso } from '../dados.js';
import { Cabecalho, Estado } from './Layout.jsx';

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short', timeStyle: 'medium',
});

/** Espelha o CHECK de `auditoria.eventos.acao`. */
const ACOES = {
  leitura: 'Leitura',
  criacao: 'Criação',
  alteracao: 'Alteração',
  exclusao: 'Exclusão',
  login: 'Login',
  logout: 'Logout',
  falha_login: 'Falha de login',
  exportacao: 'Exportação',
};

const diasAtras = (n) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toLocaleDateString('sv-SE');
};
const hoje = () => new Date().toLocaleDateString('sv-SE');

export default function Auditoria() {
  const [filtros, setFiltros] = useState({
    de: diasAtras(30), ate: hoje(), acao: '', recurso: '',
  });

  /* A faixa de datas é sempre enviada. A tabela é particionada por
     `ocorrido_em`, e uma consulta sem intervalo varre todas as partições — o
     back-end tem um default, mas mandar daqui mantém a tela honesta sobre o que
     está mostrando. */
  const caminho = useMemo(() => `/auditoria${query({
    de: new Date(`${filtros.de}T00:00:00`).toISOString(),
    ate: new Date(`${filtros.ate}T23:59:59.999`).toISOString(),
    acao: filtros.acao,
    recurso: filtros.recurso,
    limite: 200,
  })}`, [filtros]);

  const trilha = useRecurso(caminho);
  const equipe = useRecurso('/equipe');
  const lista = trilha.dados ?? [];

  /* O evento traz `idUsuario`, não o nome: `auditoria` não pode depender de
     `identidade` (o inverso já existe, e o par fecharia ciclo de construtor).
     A tela casa os dois lados, que é onde isso custa uma linha. */
  const nomeDoUsuario = useMemo(() => {
    const mapa = new Map((equipe.dados ?? []).map((m) => [m.idUsuario, m.nomeCompleto]));
    return (id) => (id == null ? 'Sistema' : mapa.get(id) ?? `Usuário ${id}`);
  }, [equipe.dados]);

  const mudar = (campo) => (e) =>
    setFiltros((f) => ({ ...f, [campo]: e.target.value }));

  return (
    <>
      <Cabecalho
        titulo="Auditoria."
        detalhe={trilha.status === 'ok'
          ? `${lista.length} ${lista.length === 1 ? 'evento' : 'eventos'} no período`
          : 'GET /api/v1/auditoria'}
      />

      <div className="filtros filtros-linha">
        <label className="campo-app">
          <span className="cap cap-ash">De</span>
          <input type="date" value={filtros.de} onChange={mudar('de')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Até</span>
          <input type="date" value={filtros.ate} onChange={mudar('ate')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Ação</span>
          <select value={filtros.acao} onChange={mudar('acao')}>
            <option value="">Todas</option>
            {Object.entries(ACOES).map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Recurso</span>
          <input value={filtros.recurso} onChange={mudar('recurso')}
                 placeholder="prontuario.evolucao" />
        </label>
      </div>

      <p className="cap cap-ash" style={{ marginBottom: 'var(--spacing-20, 20px)' }}>
        Consultar esta tela também gera um evento — a trilha registra quem a leu.
      </p>

      <Estado status={trilha.status} erro={trilha.erro} onTentarDeNovo={trilha.recarregar}
              vazio={lista.length === 0 ? 'Nenhum evento no período.' : null}>
        <div className="tabela-rolagem">
          <table className="tabela">
            <thead>
              <tr>
                <th scope="col">Quando</th>
                <th scope="col">Quem</th>
                <th scope="col">Ação</th>
                <th scope="col">Recurso</th>
                <th scope="col">Origem</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((e) => (
                <tr key={e.idEvento}>
                  <td className="num">{DATA_HORA.format(new Date(e.ocorridoEm))}</td>
                  <td>
                    {nomeDoUsuario(e.idUsuario)}
                    {e.staffPapel && (
                      <span className="selo-staff"> staff · {e.staffPapel}</span>
                    )}
                  </td>
                  <td>{ACOES[e.acao] ?? e.acao}</td>
                  <td>
                    {e.recurso}
                    {e.idRecurso && <span className="cap cap-ash"> #{e.idRecurso}</span>}
                  </td>
                  <td className="num">{e.ipOrigem || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Estado>
    </>
  );
}
