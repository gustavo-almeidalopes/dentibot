package br.com.dentibot.agenda;

import java.time.Instant;

/**
 * Linha da agenda como a recepção a vê.
 *
 * <p>{@code nomePaciente} vem da porta do módulo de pacientes, não de um JOIN
 * com {@code identidade.pessoas} — ver {@code AgendaServico#montar}.
 */
public record ConsultaResumo(
        long idConsulta,
        long idPaciente,
        String nomePaciente,
        String telefonePaciente,
        long idDentista,
        Instant inicioEm,
        Instant terminoEm,
        String status) {
}
