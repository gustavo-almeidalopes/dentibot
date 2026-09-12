package br.com.dentibot.plataforma.seguranca;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revogação de access token antes da expiração natural (logout, troca de senha,
 * desativação de usuário).
 *
 * <p>Duas camadas, com papéis diferentes:
 *
 * <ul>
 *   <li><b>Redis</b> é o caminho rápido — consultado em toda requisição
 *       autenticada, com TTL igual ao {@code exp} do token;</li>
 *   <li><b>Postgres</b> é a verdade durável. Redis nunca é fonte de verdade
 *       (camada 11).</li>
 * </ul>
 *
 * <p>Com o Redis fora do ar, cai para o Postgres: fica mais lento, continua
 * correto. O contrário — confiar só no Redis — significaria que um flush do
 * cache ressuscita todos os tokens revogados.
 */
@Component
public class BlocklistDeToken {

    private static final Logger log = LoggerFactory.getLogger(BlocklistDeToken.class);
    private static final String PREFIXO = "revogado:jti:";

    private final StringRedisTemplate redis;
    private final JdbcClient jdbc;

    public BlocklistDeToken(StringRedisTemplate redis, JdbcClient jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revogar(UUID jti, Instant expiraEm) {
        jdbc.sql("""
                        INSERT INTO plataforma.tokens_revogados (jti, expira_em)
                        VALUES (CAST(:jti AS UUID), :expira)
                        ON CONFLICT (jti) DO NOTHING
                        """)
                .param("jti", jti.toString())
                .param("expira", java.sql.Timestamp.from(expiraEm))
                .update();

        try {
            Duration ttl = Duration.between(Instant.now(), expiraEm);
            if (!ttl.isNegative() && !ttl.isZero()) {
                redis.opsForValue().set(PREFIXO + jti, "1", ttl);
            }
        } catch (RuntimeException e) {
            // O Postgres já registrou; o Redis é só velocidade.
            log.warn("Redis indisponível ao revogar jti — seguindo só com o banco: {}",
                    e.getMessage());
        }
    }

    public boolean revogado(UUID jti) {
        try {
            Boolean noRedis = redis.hasKey(PREFIXO + jti);
            if (Boolean.TRUE.equals(noRedis)) {
                return true;
            }
        } catch (RuntimeException e) {
            log.debug("Redis indisponível na checagem de blocklist: {}", e.getMessage());
        }
        return revogadoNoBanco(jti);
    }

    /**
     * Transação própria: a checagem acontece no filtro de autenticação, antes de
     * qualquer transação de negócio existir.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean revogadoNoBanco(UUID jti) {
        try {
            Integer achou = jdbc.sql("""
                            SELECT 1 FROM plataforma.tokens_revogados
                            WHERE jti = CAST(:jti AS UUID) AND expira_em > NOW()
                            """)
                    .param("jti", jti.toString())
                    .query(Integer.class)
                    .optional()
                    .orElse(null);
            return achou != null;
        } catch (DataAccessException e) {
            // Banco fora significa que o sistema está fora de qualquer forma.
            // Falhar fechando aqui: token tratado como revogado.
            log.error("Falha ao consultar blocklist — tratando token como revogado.", e);
            return true;
        }
    }
}
