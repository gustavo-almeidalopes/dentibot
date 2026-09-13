package br.com.dentibot.lgpd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NovoTermo(
        @NotBlank String tipo,
        @NotBlank @Size(max = 10) String versao,
        @NotBlank String textoIntegral,
        @NotNull LocalDate ativoDesde) {
}
