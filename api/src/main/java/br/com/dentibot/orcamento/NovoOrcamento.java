package br.com.dentibot.orcamento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record NovoOrcamento(
        @NotNull Long idPaciente,
        @NotNull Long idDentista,
        LocalDate validadeEm,
        @PositiveOrZero BigDecimal valorDesconto,
        String observacoes) {
}
