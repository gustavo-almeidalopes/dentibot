package br.com.dentibot.identidade.domain;

import br.com.dentibot.plataforma.contexto.Papel;
import java.time.Instant;

/**
 * O que o fluxo de login precisa saber sobre um usuário. Interno ao módulo: o
 * hash da senha nunca atravessa a fronteira de {@code identidade}.
 */
public record CredenciaisUsuario(
        long idUsuario,
        long idClinica,
        String senhaHash,
        Papel papel,
        String status,
        String metodo2fa,
        String totpSecret,
        int falhasConsecutivas,
        Instant bloqueadoAte) {

    public boolean ativo() {
        return "ativo".equals(status);
    }

    public boolean bloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(Instant.now());
    }

    public boolean exige2fa() {
        return !"nenhum".equals(metodo2fa);
    }

    /**
     * Evita que o hash apareça em log por acidente — {@code toString()} de record
     * imprime todos os campos, e um {@code log.info("usuario={}", cred)}
     * distraído colocaria o hash no Better Stack (invariante 11).
     */
    @Override
    public String toString() {
        return "CredenciaisUsuario[idUsuario=%d, idClinica=%d, papel=%s, status=%s]"
                .formatted(idUsuario, idClinica, papel, status);
    }
}
