package br.com.dentibot.lgpd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NovaSolicitacao(
        @NotNull Long idPaciente,
        @NotBlank String direito) {
}
