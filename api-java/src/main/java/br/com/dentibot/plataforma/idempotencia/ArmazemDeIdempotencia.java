package br.com.dentibot.plataforma.idempotencia;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estado durável do {@code Idempotency-Key}.
 *
 * <p>Cada método abre transação PRÓPRIA ({@code REQUIRES_NEW}), e isso é o ponto
 * central: a reserva da chave precisa estar COMMITADA antes de o handler rodar.
 * Se ela participasse da transação de negócio, dois toques simultâneos no botão
 * não enxergariam a reserva um do outro — que é exatamente o cenário que a
 * idempotência existe para cobrir.
 */
@Component
public class ArmazemDeIdempotencia {

    private final JdbcClient jdbc;

    public ArmazemDeIdempotencia(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record Registro(String estado, String requestHash, Integer statusHttp, String resposta) {
        public boolean concluida() {
            return "concluida".equals(estado);
        }
    }

    /**
     * @return {@code true} se esta chamada reservou a chave (é a primeira);
     *         {@code false} se já existia — e aí {@link #buscar} diz o que fazer.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reservar(String chave, String endpoint, String requestHash) {
        try {
            int linhas = jdbc.sql("""
                            INSERT INTO plataforma.idempotencia
                                (id_clinica, chave, endpoint, request_hash)
                            VALUES (:clinica, :chave, :endpoint, :hash)
                            ON CONFLICT (id_clinica, chave) DO NOTHING
                            """)
                    .param("clinica", ContextoAtual.clinicaObrigatoria())
                    .param("chave", chave)
                    .param("endpoint", endpoint)
                    .param("hash", requestHash)
                    .update();
            return linhas == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Registro> buscar(String chave) {
        return jdbc.sql("""
                        SELECT estado, request_hash, status_http, resposta::text AS resposta
                        FROM plataforma.idempotencia
                        WHERE id_clinica = :clinica AND chave = :chave
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("chave", chave)
                .query((rs, n) -> new Registro(
                        rs.getString("estado"),
                        rs.getString("request_hash"),
                        (Integer) rs.getObject("status_http"),
                        rs.getString("resposta")))
                .optional();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void concluir(String chave, int statusHttp, String corpo) {
        jdbc.sql("""
                        UPDATE plataforma.idempotencia
                        SET estado = 'concluida', status_http = :status,
                            resposta = CAST(:corpo AS JSONB), concluido_em = NOW()
                        WHERE id_clinica = :clinica AND chave = :chave
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("chave", chave)
                .param("status", statusHttp)
                .param("corpo", corpo == null || corpo.isBlank() ? "null" : corpo)
                .update();
    }

    /**
     * O handler falhou: a reserva sai para o cliente poder tentar de novo com a
     * mesma chave. Deixar a reserva pendurada transformaria um erro transitório
     * em "esta chave está travada por 24 horas".
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void liberar(String chave) {
        jdbc.sql("""
                        DELETE FROM plataforma.idempotencia
                        WHERE id_clinica = :clinica AND chave = :chave AND estado = 'em_andamento'
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("chave", chave)
                .update();
    }
}
