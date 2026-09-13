package br.com.dentibot.financeiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record NovaFormaPagamento(
        @NotBlank @Size(max = 80) String nome,
        @NotBlank String tipo,
        @PositiveOrZero BigDecimal taxaPercentual,
        @PositiveOrZero Integer diasLiquidacao) {
}
