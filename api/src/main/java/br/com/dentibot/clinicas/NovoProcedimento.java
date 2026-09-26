package br.com.dentibot.clinicas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record NovoProcedimento(
        @NotBlank @Size(max = 120) String nomeServico,
        @Size(max = 20) String codigoTuss,
        @NotNull @PositiveOrZero BigDecimal precoParticular,
        @Positive Integer duracaoMinutos) {
}
