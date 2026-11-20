package br.com.dentibot.billing;

import java.time.Instant;

public record Assinatura(
        long idAssinatura,
        String plano,
        String provedor,
        String status,
        Instant periodoInicio,
        Instant periodoFim,
        boolean cancelarNoFim,
        String idAssinaturaExterna) {

    public boolean daAcesso() {
        return "trial".equals(status) || "ativa".equals(status) || "inadimplente".equals(status);
    }
}
