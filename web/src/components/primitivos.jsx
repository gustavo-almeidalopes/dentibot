/**
 * O vocabulário visual das telas do sistema.
 *
 * <p>Um arquivo e não cinco: são quatro peças pequenas com a mesma
 * responsabilidade — dizer estado sem fazer nada. Quem fizer alguma coisa
 * (o <dialog>) mora separado.
 */
import { descrever } from '../apresentacao.js';

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
