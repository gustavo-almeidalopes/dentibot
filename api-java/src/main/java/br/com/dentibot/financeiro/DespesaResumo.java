package br.com.dentibot.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaResumo(
        long idDespesa,
        String descricao,
        String categoria,
        BigDecimal valorDocumento,
        LocalDate vencimentoEm,
        LocalDate pagoEm,
        String status) {
}
