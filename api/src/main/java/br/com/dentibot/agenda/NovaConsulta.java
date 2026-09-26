package br.com.dentibot.agenda;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Sem {@code idClinica}: o tenant vem do token (invariante 5).
 *
 * @param inicioEm instante em UTC. O fuso da clínica serve para RENDERIZAR, não
 *                 para armazenar — {@code TIMESTAMPTZ} no banco, invariante 7.
 */
public record NovaConsulta(
        @NotNull @Positive Long idPaciente,
        @NotNull @Positive Long idDentista,
        Long idProcedimento,
        @NotNull @Future Instant inicioEm,
        @NotNull Instant terminoEm,
        @Size(max = 2000) String observacoes) {
}
