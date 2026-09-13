package br.com.dentibot.lgpd;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @param finalidadesAceitas por que o dado pode ser tratado. Lista vazia não é
 *                           consentimento: consentimento sem finalidade é
 *                           consentimento genérico, que o art. 8º §4º não
 *                           aceita.
 */
public record NovoConsentimento(
        @NotNull Long idPaciente,
        @NotNull Long idTermo,
        @NotEmpty List<String> finalidadesAceitas) {
}
