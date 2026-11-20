import { useState } from 'react';
import { quantidade } from '../apresentacao.js';
import { Celula, Tabela } from '../components/primitivos.jsx';
import { useRecurso } from '../dados.js';
import { Cabecalho, Estado } from './Layout.jsx';

const COBERTURAS = [15, 30, 60, 90];

/**
 * Estoque (IA-43): o que comprar primeiro, e a posição de tudo.
 *
 * <p>A sugestão é conta à vista — consumo médio de 90 dias × cobertura, mais o
 * que falta para o ponto de pedido — e a tela mostra os termos da conta, não só
 * o resultado. Quem compra precisa poder discordar.
 */
export default function Estoque() {
  const [cobertura, setCobertura] = useState(30);
  const sugestao = useRecurso(`/estoque/sugestao-compra?cobertura=${cobertura}`);
  const posicao = useRecurso('/estoque/posicao');

  const comprar = sugestao.dados ?? [];
  const produtos = posicao.dados ?? [];

  return (
    <>
      <Cabecalho titulo="Estoque." detalhe="O que comprar, e quanto há." />

      <div className="filtros">
        <label className="campo-app">
          <span className="cap cap-ash">Comprar para durar</span>
          <select value={cobertura} onChange={(e) => setCobertura(Number(e.target.value))}>
            {COBERTURAS.map((d) => <option key={d} value={d}>{d} dias</option>)}
          </select>
        </label>
      </div>

      <h2 className="sub secao-titulo">Comprar</h2>
      <Estado status={sugestao.status} erro={sugestao.erro} onTentarDeNovo={sugestao.recarregar}
              esqueleto={{ linhas: 3, colunas: 4 }}
              vazio={comprar.length === 0
                ? `Nada a comprar: o estoque atravessa ${cobertura} dias acima do ponto de pedido.`
                : null}>
        <Tabela colunas={[
          { chave: 'produto', rotulo: 'Produto' },
          { chave: 'saldo', rotulo: 'Saldo', num: true },
          { chave: 'ponto', rotulo: 'Ponto de pedido', num: true },
          { chave: 'consumo', rotulo: 'Consumo por dia', num: true },
          { chave: 'comprar', rotulo: 'Comprar', num: true },
        ]}>
          {comprar.map((s) => (
            <tr key={s.idProduto}>
              <Celula rotulo="Produto">{s.nomeProduto}</Celula>
              <Celula rotulo="Saldo" num>{quantidade(s.quantidadeAtual)} {s.unidadeMedida}</Celula>
              <Celula rotulo="Ponto de pedido" num>{quantidade(s.pontoPedido)}</Celula>
              <Celula rotulo="Consumo por dia" num>{quantidade(s.consumoDiario)}</Celula>
              <Celula rotulo="Comprar" num>
                <strong>{quantidade(s.quantidadeSugerida)} {s.unidadeMedida}</strong>
              </Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>

      <h2 className="sub secao-titulo">Posição</h2>
      <Estado status={posicao.status} erro={posicao.erro} onTentarDeNovo={posicao.recarregar}
              esqueleto={{ linhas: 5, colunas: 3 }}
              vazio={produtos.length === 0 ? 'Nenhum produto cadastrado.' : null}>
        <Tabela colunas={[
          { chave: 'produto', rotulo: 'Produto' },
          { chave: 'saldo', rotulo: 'Saldo', num: true },
          { chave: 'situacao', rotulo: 'Situação' },
        ]}>
          {produtos.map((p) => (
            <tr key={p.idProduto}>
              <Celula rotulo="Produto">{p.nomeProduto}</Celula>
              <Celula rotulo="Saldo" num>{quantidade(p.quantidadeAtual)} {p.unidadeMedida}</Celula>
              <Celula rotulo="Situação">
                <span className="selo" data-tom={p.abaixoDoPontoPedido ? 'alarme' : 'fraco'}>
                  {p.abaixoDoPontoPedido ? 'Abaixo do ponto' : 'Ok'}
                </span>
              </Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}
