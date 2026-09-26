package br.com.dentibot.financeiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record NovaDespesa(
        @NotBlank @Size(max = 200) String descricao,
        @NotBlank @Size(max = 80) String categoria,
        @NotNull @Positive BigDecimal valorDocumento,
        @NotNull LocalDate vencimentoEm,
        String observacoes) {
}
