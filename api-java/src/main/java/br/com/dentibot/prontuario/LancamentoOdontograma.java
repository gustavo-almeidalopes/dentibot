package br.com.dentibot.prontuario;

import java.time.Instant;

/**
 * Estado de um dente (ou de uma face) num instante.
 *
 * <p>O odontograma é append-only: o estado ATUAL é o último lançamento por
 * (dente, face), e o histórico sai de graça.
 *
 * @param dente notação FDI (ISO 3950)
 * @param face  V, L, M, D, O, I, P — nulo quando a condição é do dente inteiro
 */
public record LancamentoOdontograma(
        long idLancamento,
        int dente,
        String face,
        String condicao,
        String observacao,
        long idDentista,
        Instant registradoEm) {
}
