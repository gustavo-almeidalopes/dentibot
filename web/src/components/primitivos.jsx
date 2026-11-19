/**
 * O vocabulário visual das telas do sistema.
 *
 * <p>Um arquivo e não cinco: são quatro peças pequenas com a mesma
 * responsabilidade — dizer estado sem fazer nada. Quem fizer alguma coisa
 * (o <dialog>) mora separado.
 */
import { descrever, telHref } from '../apresentacao.js';
import { formatarTelefone } from '../documento.js';

/**
 * Status com forma, nunca só cor.
 *
 * <p>O tom vira `data-tom` e o CSS decide: contorno, preenchido, riscado. Cor
 * sozinha não existe para quem não a distingue — e status é o campo mais
 * escaneado de toda lista desta ferramenta.
 */
export function Selo({ mapa, valor }) {
  const { rotulo, tom } = descrever(mapa, valor);
  return <span className="selo" data-tom={tom}>{rotulo}</span>;
}

/**
 * A forma do que vai chegar, enquanto não chegou.
 *
 * <p>"Carregando…" como parágrafo solto faz a página saltar quando os dados
 * chegam, e o salto é o que faz alguém clicar no lugar errado.
 */
export function Esqueleto({ linhas = 5, colunas = 3 }) {
  return (
    <div className="esqueleto" aria-hidden="true">
      {Array.from({ length: linhas }, (_, l) => (
        <div className="esqueleto-linha" key={l}>
          {Array.from({ length: colunas }, (_, c) => (
            <span className="esqueleto-celula" key={c} />
          ))}
        </div>
      ))}
    </div>
  );
}

/**
 * O que acabou de acontecer.
 *
 * <p>Nenhuma ação bem-sucedida avisava que deu certo: `useAcao` só expunha
 * erro, e confirmar uma consulta apenas recarregava a lista. Quem clicou não
 * sabia se clicou.
 *
 * <p>`role="status"` e não `alert` para o sucesso: o leitor de tela anuncia
 * quando terminar a frase atual, em vez de interromper.
 */
export function Aviso({ texto, tom = 'sucesso' }) {
  if (!texto) return null;
  return (
    <p className="aviso" data-tom={tom} role={tom === 'erro' ? 'alert' : 'status'}>
      {texto}
    </p>
  );
}

/**
 * A tabela do sistema.
 *
 * <p>Substitui o `.row` da landing, que é grid de duas colunas com 42px de
 * padding e recebia três filhos — a coluna de ações caía numa linha implícita.
 *
 * <p>Abaixo de 620px cada linha vira bloco empilhado, e é `<Celula>` quem
 * carrega o rótulo que aparece ali: rolagem horizontal numa tabela de trabalho
 * esconde justamente a coluna de ação.
 */
export function Tabela({ colunas, children }) {
  return (
    <div className="tabela-rolagem">
      <table className="tabela">
        <thead>
          <tr>
            {colunas.map((c) => (
              <th key={c.chave} scope="col" className={c.num ? 'num' : undefined}>
                {c.rotulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}

/** Uma célula que sabe o próprio nome, para quando a tabela empilha. */
export function Celula({ rotulo, num, children, ...resto }) {
  return (
    <td data-rotulo={rotulo} className={num ? 'num' : undefined} {...resto}>
      {children}
    </td>
  );
}

/** Os três estados de carga, num componente só. */
export function Estado({ status, erro, vazio, esqueleto, children, onTentarDeNovo }) {
  if (status === 'carregando') {
    /* Quem passa a forma do que vai chegar não vê a página saltar quando ela
       chega — e o salto é o que faz alguém clicar no lugar errado. Quem não
       passa continua com o texto, então nenhuma tela quebrou na troca. */
    return esqueleto
      ? <Esqueleto linhas={esqueleto.linhas} colunas={esqueleto.colunas} />
      : <p className="body body-ash" aria-live="polite">Carregando…</p>;
  }
  if (status === 'erro') {
    return (
      <div aria-live="assertive">
        <p className="body" style={{ color: 'var(--alarm)' }}>
          {erro?.message || 'Não foi possível carregar.'}
        </p>
        {/* O correlacaoId vem do ProblemDetail do back-end e é o que o suporte
            usa para achar a linha de log exata sem pedir print de tela. */}
        {erro?.data?.correlacaoId && (
          <p className="cap cap-ash">
            Referência: <code>{erro.data.correlacaoId}</code>{' '}
            {/* O suporte pede este número por telefone. Ler 36 caracteres de
                UUID em voz alta é onde a pessoa desiste e desliga. */}
            <button
              type="button"
              className="btn btn-sm"
              onClick={() => navigator.clipboard?.writeText(erro.data.correlacaoId)}
            >
              Copiar
            </button>
          </p>
        )}
        {onTentarDeNovo && (
          <button type="button" className="btn" onClick={onTentarDeNovo}>
            Tentar de novo
          </button>
        )}
      </div>
    );
  }
  if (vazio) {
    /* `vazio` aceita só a frase, ou { titulo, texto } quando o vazio é uma
       resposta e não um defeito — "Dia livre." diz mais que "nenhuma". */
    const { titulo, texto } = typeof vazio === 'string' ? { texto: vazio } : vazio;
    return (
      <div className="vazio">
        {titulo && <p className="vazio-titulo">{titulo}</p>}
        <p className="body">{texto}</p>
      </div>
    );
  }
  return children;
}

/* A máscara de onde o título sobe. As classes `.ln` são as do <Lines> da
   landing; a animação é CSS puro, porque o título já nasce na tela e o
   IntersectionObserver do Reveal não teria nada a observar. */
export const Linha = ({ children }) => <span className="ln"><i>{children}</i></span>;

/**
 * Cabeçalho de tela: o nome dela no corpo da landing, e uma linha de contexto.
 *
 * <p>`secao` faz o mesmo papel para um bloco dentro da tela (Recebíveis, abaixo
 * dos indicadores) — h2, menor, sem subir: só o título da tela anuncia a rota.
 */
export function Cabecalho({ titulo, detalhe, acao, secao = false }) {
  const Titulo = secao ? 'h2' : 'h1';
  return (
    <div className={`app-cabecalho${secao ? ' app-cabecalho-secao' : ''}`}>
      <div>
        <Titulo className={`display ${secao ? 'display-xs' : 'display-sm titulo-tela'}`}>
          {secao ? titulo : <Linha>{titulo}</Linha>}
        </Titulo>
        {detalhe && <p className="credit">{detalhe}</p>}
      </div>
      {acao}
    </div>
  );
}

/** Telefone que liga, ou `vazio` quando o número não é discável. */
export function Telefone({ numero, className, vazio = null }) {
  const href = telHref(numero);
  return href ? <a className={className} href={href}>{formatarTelefone(numero)}</a> : vazio;
}
