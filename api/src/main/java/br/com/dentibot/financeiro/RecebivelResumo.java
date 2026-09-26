package br.com.dentibot.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma parcela a receber.
 *
 * <p>{@code valorLiquidado} e {@code saldoDevedor} vêm da view
 * {@code vw_saldo_recebivel}, que é SUM sobre o ledger — não são colunas
 * guardadas. É por isso que não existe caminho para eles discordarem do extrato.
 */
public record RecebivelResumo(
        long idRecebivel,
        Long idOrcamento,
        Long idPaciente,
        int parcelaNumero,
        int parcelaTotal,
        BigDecimal valorParcela,
        BigDecimal valorLiquidado,
        BigDecimal saldoDevedor,
        LocalDate vencimentoEm,
        String status) {
}
