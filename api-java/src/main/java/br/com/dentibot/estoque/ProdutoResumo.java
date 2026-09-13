package br.com.dentibot.estoque;

import java.math.BigDecimal;

public record ProdutoResumo(
        long idProduto,
        String nomeProduto,
        String unidadeMedida,
        BigDecimal pontoPedido,
        boolean controlaLote,
        Long idFornecedorPadrao,
        boolean ativo) {
}
