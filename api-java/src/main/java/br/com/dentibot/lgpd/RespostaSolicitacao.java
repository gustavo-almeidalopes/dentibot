package br.com.dentibot.lgpd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param justificativa obrigatória quando a resposta é recusa ou atendimento
 *                      parcial — o CHECK da V16 recusa a linha sem ela, e a
 *                      recusa sem motivo é o que o art. 18 §4º proíbe.
 */
public record RespostaSolicitacao(
        @NotBlank String status,
        @Size(max = 4000) String justificativa,
        @Size(max = 500) String chaveObjetoResposta) {
}
