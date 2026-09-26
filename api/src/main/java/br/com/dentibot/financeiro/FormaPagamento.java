package br.com.dentibot.financeiro;

import java.math.BigDecimal;

public record FormaPagamento(
        long idForma,
        String nome,
        String tipo,
        BigDecimal taxaPercentual,
        int diasLiquidacao,
        boolean ativo) {
}
