package br.com.dentibot.estoque;

import java.math.BigDecimal;

/** Uma linha da view {@code estoque.vw_posicao}: o SUM, não uma coluna guardada. */
public record PosicaoProduto(
        long idProduto,
        String nomeProduto,
        String unidadeMedida,
        BigDecimal pontoPedido,
        BigDecimal quantidadeAtual,
        boolean abaixoDoPontoPedido) {
}
