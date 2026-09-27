package br.com.dentibot.agenda;

import java.time.Instant;

/**
 * Linha da agenda como a recepção a vê.
 *
 * <p>{@code nomePaciente} vem da porta do módulo de pacientes, não de um JOIN
 * com {@code identidade.pessoas} — ver {@code AgendaServico#montar}.
 *
 * <p>{@code faltasRecentes} de {@code consultasRecentes} (IA-15): das últimas
 * consultas encerradas do paciente — realizadas ou faltas, até dez —, quantas
 * foram falta. Contagem e não probabilidade: "faltou 3 de 5" a recepção
 * confere e explica; "62% de risco" ninguém sabe de onde veio. E sai só do
 * histórico da própria pessoa — nada de bairro, idade ou renda, que
 * transformariam otimização de agenda em discriminação.
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
        int faltasRecentes,
        int consultasRecentes) {
}
