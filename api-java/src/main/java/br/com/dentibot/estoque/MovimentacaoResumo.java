package br.com.dentibot.estoque;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MovimentacaoResumo(
        long idMovimentacao,
        long idProduto,
        String tipo,
        BigDecimal quantidade,
        String lote,
        LocalDate validade,
        BigDecimal custoUnitario,
        String observacao,
        Long idUsuario,
        Instant movimentadoEm) {
}
