package br.com.dentibot.identidade.domain;

import java.time.Instant;
import java.util.UUID;

public record Sessao(
        long idSessao,
        long idClinica,
        long idUsuario,
        UUID jti,
        UUID familia,
        String refreshHash,
        Instant expiraEm,
        Instant revogadaEm) {

    public boolean valida() {
        return revogadaEm == null && expiraEm.isAfter(Instant.now());
    }

    /**
     * Um refresh já revogado sendo apresentado é o sinal de reuso: ou o usuário
     * guardou um token velho, ou alguém copiou o dele. Não dá para distinguir,
     * e a resposta correta para os dois casos é a mesma — derrubar a família.
     */
    public boolean indicaReuso() {
        return revogadaEm != null;
    }

    @Override
    public String toString() {
        return "Sessao[id=%d, clinica=%d, usuario=%d, valida=%s]"
                .formatted(idSessao, idClinica, idUsuario, valida());
    }
}
