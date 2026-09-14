import { useMemo, useState } from 'react';
import { query } from '../api.js';
import { hoje, somarDias } from '../apresentacao.js';
import { Celula, Selo, Tabela } from '../components/primitivos.jsx';
import { useRecurso } from '../dados.js';
import { Cabecalho, Estado } from './Layout.jsx';

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short', timeStyle: 'medium',
});

const LIMITE = 200;

/* Espelha o CHECK de `auditoria.eventos.acao`. Exclusão, falha de login e
   exportação são as três que precisam saltar numa trilha — o resto é o ruído
   normal de um dia de trabalho. */
const ACOES = {
  leitura: { rotulo: 'Leitura', tom: 'fraco' },
  criacao: { rotulo: 'Criação', tom: 'contorno' },
  alteracao: { rotulo: 'Alteração', tom: 'contorno' },
  exclusao: { rotulo: 'Exclusão', tom: 'alarme' },
  login: { rotulo: 'Login', tom: 'fraco' },
  logout: { rotulo: 'Logout', tom: 'fraco' },
  falha_login: { rotulo: 'Falha de login', tom: 'alarme' },
  exportacao: { rotulo: 'Exportação', tom: 'alarme' },
};

export default function Auditoria() {
  const [filtros, setFiltros] = useState({
    de: somarDias(hoje(), -30), ate: hoje(), acao: '', recurso: '',
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
    limite: LIMITE,
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

  const periodo = (dias) => setFiltros((f) => ({
    ...f, de: somarDias(hoje(), -dias), ate: hoje(),
  }));

  return (
    <>
      <Cabecalho
        titulo="Auditoria."
        detalhe={trilha.status === 'ok'
          ? `${lista.length} ${lista.length === 1 ? 'evento' : 'eventos'} no período`
          : 'Quem leu e quem mudou o quê'}
      />

      <div className="filtros filtros-linha">
        <div className="acoes">
          {[7, 30, 90].map((n) => (
            <button type="button" className="btn btn-sm" key={n} onClick={() => periodo(n)}>
              {n} dias
            </button>
          ))}
        </div>
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
            {Object.entries(ACOES).map(([v, { rotulo }]) => (
              <option key={v} value={v}>{rotulo}</option>
            ))}
          </select>
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">Recurso</span>
          {/* Texto livre num vocabulário que ninguém decorou é filtro que não
              se usa. Os valores da resposta atual já ensinam a nomenclatura. */}
          <input value={filtros.recurso} onChange={mudar('recurso')}
                 list="recursos-vistos" placeholder="prontuario.evolucao" />
          <datalist id="recursos-vistos">
            {[...new Set(lista.map((e) => e.recurso))].map((r) => <option key={r} value={r} />)}
          </datalist>
        </label>
      </div>

      <p className="cap cap-ash" style={{ marginBottom: 'var(--spacing-20, 20px)' }}>
        Consultar esta tela também gera um evento — a trilha registra quem a leu.
      </p>

      <Estado status={trilha.status} erro={trilha.erro} onTentarDeNovo={trilha.recarregar}
              esqueleto={{ linhas: 10, colunas: 5 }}
              vazio={lista.length === 0 ? 'Nenhum evento no período.' : null}>
        {/* Truncar calado numa trilha de auditoria é a diferença entre "não
            houve" e "não coube". */}
        {lista.length >= LIMITE && (
          <p className="aviso" data-tom="sucesso" role="status">
            Mostrando os {LIMITE} eventos mais recentes do período. Estreite as datas
            para alcançar o resto.
          </p>
        )}

        <Tabela colunas={[
          { chave: 'quando', rotulo: 'Quando', num: true },
          { chave: 'quem', rotulo: 'Quem' },
          { chave: 'acao', rotulo: 'Ação' },
          { chave: 'recurso', rotulo: 'Recurso' },
          { chave: 'origem', rotulo: 'Origem', num: true },
        ]}>
          {lista.map((e) => (
            <tr key={e.idEvento}>
              <Celula rotulo="Quando" num>{DATA_HORA.format(new Date(e.ocorridoEm))}</Celula>
              <Celula rotulo="Quem">
                {nomeDoUsuario(e.idUsuario)}
                {/* Staff da plataforma tocando dado de clínica é exatamente o
                    que deve saltar aos olhos numa trilha. */}
                {e.staffPapel && (
                  <span className="selo-staff"> staff · {e.staffPapel}</span>
                )}
              </Celula>
              <Celula rotulo="Ação"><Selo mapa={ACOES} valor={e.acao} /></Celula>
              <Celula rotulo="Recurso">
                {e.recurso}
                {e.idRecurso && <span className="cap cap-ash"> #{e.idRecurso}</span>}
              </Celula>
              <Celula rotulo="Origem" num>{e.ipOrigem || '—'}</Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}
