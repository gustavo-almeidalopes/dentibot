package br.com.dentibot.identidade.infrastructure;

import br.com.dentibot.identidade.domain.Sessao;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SessaoRepositorio {

    private final JdbcClient jdbc;

    public SessaoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void criar(long idClinica, long idUsuario, UUID jti, UUID familia,
                      String refreshHash, String userAgent, String ip, Instant expiraEm) {
        jdbc.sql("""
                        INSERT INTO identidade.sessoes
                            (id_clinica, id_usuario, jti, familia, refresh_hash,
                             user_agent, ip_address, expira_em)
                        VALUES (:clinica, :usuario, CAST(:jti AS UUID), CAST(:familia AS UUID),
                                :hash, :ua, CAST(:ip AS INET), :expira)
                        """)
                .param("clinica", idClinica)
                .param("usuario", idUsuario)
                .param("jti", jti.toString())
                .param("familia", familia.toString())
                .param("hash", refreshHash)
                .param("ua", userAgent == null ? null
                        : userAgent.substring(0, Math.min(userAgent.length(), 300)))
                .param("ip", ip)
                .param("expira", java.sql.Timestamp.from(expiraEm))
                .update();
    }

    public Optional<Sessao> buscarPorJti(UUID jti) {
        return jdbc.sql("""
                        SELECT id_sessao, id_clinica, id_usuario, jti, familia, refresh_hash,
                               expira_em, revogada_em
                        FROM identidade.sessoes
                        WHERE jti = CAST(:jti AS UUID)
                        """)
                .param("jti", jti.toString())
                .query((rs, n) -> new Sessao(
                        rs.getLong("id_sessao"),
                        rs.getLong("id_clinica"),
                        rs.getLong("id_usuario"),
                        UUID.fromString(rs.getString("jti")),
                        UUID.fromString(rs.getString("familia")),
                        rs.getString("refresh_hash"),
                        rs.getTimestamp("expira_em").toInstant(),
                        rs.getTimestamp("revogada_em") == null
                                ? null : rs.getTimestamp("revogada_em").toInstant()))
                .optional();
    }

    public void revogar(UUID jti, String motivo) {
        jdbc.sql("""
                        UPDATE identidade.sessoes
                        SET revogada_em = NOW(), motivo_revogacao = :motivo
                        WHERE jti = CAST(:jti AS UUID) AND revogada_em IS NULL
                        """)
                .param("jti", jti.toString())
                .param("motivo", motivo)
                .update();
    }

    /**
     * Revoga a FAMÍLIA inteira. Chamada quando um refresh já rotacionado
     * reaparece: ou o usuário legítimo está com um token velho, ou alguém
     * copiou o token. Não dá para distinguir os dois casos — e a única resposta
     * segura é derrubar as duas pontas e obrigar login novo.
     */
    public int revogarFamilia(UUID familia, String motivo) {
        return jdbc.sql("""
                        UPDATE identidade.sessoes
                        SET revogada_em = NOW(), motivo_revogacao = :motivo
                        WHERE familia = CAST(:familia AS UUID) AND revogada_em IS NULL
                        """)
                .param("familia", familia.toString())
                .param("motivo", motivo)
                .update();
    }

    public void revogarTodasDoUsuario(long idUsuario, String motivo) {
        jdbc.sql("""
                        UPDATE identidade.sessoes
                        SET revogada_em = NOW(), motivo_revogacao = :motivo
                        WHERE id_usuario = :usuario AND revogada_em IS NULL
                        """)
                .param("usuario", idUsuario)
                .param("motivo", motivo)
                .update();
    }
}
