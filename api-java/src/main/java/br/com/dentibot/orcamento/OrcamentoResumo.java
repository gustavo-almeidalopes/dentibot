package br.com.dentibot.orcamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OrcamentoResumo(
        long idOrcamento,
        long idPaciente,
        long idDentista,
        String status,
        BigDecimal valorBruto,
        BigDecimal valorDesconto,
        BigDecimal valorFinal,
        LocalDate validadeEm,
        Instant aprovadoEm,
        Instant criadoEm) {
}
