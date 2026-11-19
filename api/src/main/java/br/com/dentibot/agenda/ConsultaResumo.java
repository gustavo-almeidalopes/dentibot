package br.com.dentibot.agenda;

import java.time.Instant;
import java.util.List;

/**
 * Linha da agenda como a recepção a vê.
 *
 * <p>{@code nomePaciente} vem da porta do módulo de pacientes, não de um JOIN
 * com {@code identidade.pessoas} — ver {@code AgendaServico#montar}.
 *
 * @param acoes as transições que quem pediu pode fazer agora, pelo status e
 *              pelo alcance dele; é o que os clientes mostram como botão
 */
public record ConsultaResumo(
        long idConsulta,
        long idPaciente,
        String nomePaciente,
        String telefonePaciente,
        long idDentista,
        Instant inicioEm,
        Instant terminoEm,
        String status,
        List<String> acoes) {
}
