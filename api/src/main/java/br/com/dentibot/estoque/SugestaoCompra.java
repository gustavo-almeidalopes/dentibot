package br.com.dentibot.estoque;

import java.math.BigDecimal;

/**
 * IA-43: quanto comprar para atravessar a cobertura pedida sem cair abaixo do
 * ponto de pedido.
 *
 * <p>Regra à vista, sem modelo: {@code consumoDiario} é a média das saídas dos
 * últimos 90 dias. A sugestão é o que falta para que o saldo, depois de
 * {@code coberturaDias} consumindo nesse ritmo, ainda fique no ponto de pedido
 * — arredondado para cima, porque insumo se compra em unidade inteira.
 */
public record SugestaoCompra(
        long idProduto,
        String nomeProduto,
        String unidadeMedida,
        BigDecimal quantidadeAtual,
        BigDecimal pontoPedido,
        BigDecimal consumoDiario,
        BigDecimal quantidadeSugerida) {
}
