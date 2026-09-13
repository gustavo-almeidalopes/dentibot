package br.com.dentibot.billing;

import java.time.Instant;

/**
 * O que um webhook do provedor informa sobre a assinatura.
 *
 * <p>Não é um comando ("ative"), é um relato ("o provedor diz que está ativa").
 * A diferença importa: este módulo nunca decide o estado, só o espelha.
 */
public record SincronizacaoDeAssinatura(
        long idClinica,
        String idAssinaturaExterna,
        String idClienteExterno,
        String plano,
        String status,
        Instant periodoInicio,
        Instant periodoFim,
        boolean cancelarNoFim) {
}
