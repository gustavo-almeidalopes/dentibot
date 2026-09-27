package br.com.dentibot.prontuario;

import java.time.Instant;

/**
 * Uma linha do histórico clínico.
 *
 * @param retificaEvolucao quando preenchido, esta entrada CORRIGE aquela — e as
 *                         duas continuam visíveis. Prontuário se corrige somando,
 *                         nunca sobrescrevendo (CFO-226).
 */
public record EvolucaoResumo(
        long idEvolucao,
        long idPaciente,
        long idDentista,
        Long idConsulta,
        String descricao,
        Long retificaEvolucao,
        String motivoRetificacao,
        Instant registradoEm) {
}
