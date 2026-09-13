package br.com.dentibot.lgpd;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Um pedido do titular sob o art. 18.
 *
 * <p>{@code prazoRespostaEm} é calculado na abertura e não é editável. É o que
 * transforma "a gente responde" em algo que a tela consegue cobrar e a
 * auditoria consegue medir.
 */
public record SolicitacaoTitular(
        long idSolicitacao,
        long idPaciente,
        String direito,
        Instant abertaEm,
        LocalDate prazoRespostaEm,
        String status,
        String justificativaRecusa,
        Instant respondidaEm,
        boolean vencida) {
}
