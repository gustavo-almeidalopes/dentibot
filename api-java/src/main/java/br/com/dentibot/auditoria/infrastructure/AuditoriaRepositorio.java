package br.com.dentibot.auditoria.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuditoriaRepositorio {

    private final JdbcClient jdbc;

    public AuditoriaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Linha crua: o nome do usuário é de identidade, e o serviço o busca. */
    public record LinhaEvento(long idEvento, java.time.Instant ocorridoEm, Long idUsuario,
                              String staffPapel, String acao, String recurso, String idRecurso,
                              String ipOrigem, String correlacaoId) {
    }

    /**
     * Consulta com faixa de data obrigatória: a tabela é particionada por
     * {@code ocorrido_em}, e sem o intervalo no WHERE o planner lê todas as
     * partições. Os demais filtros são opcionais e resolvidos com
     * {@code (:x IS NULL OR coluna = :x)} — um plano só, sem montar SQL por
     * string, que é como injeção entra.
     */
    public List<LinhaEvento> consultar(java.time.Instant de, java.time.Instant ate,
                                       String acao, String recurso, Long idUsuario,
                                       long apos, int limite) {
        return jdbc.sql("""
                        SELECT id_evento, ocorrido_em, id_usuario, staff_papel,
                               acao, recurso, id_recurso,
                               host(ip_origem) AS ip_origem, correlacao_id
                        FROM auditoria.eventos
                        WHERE ocorrido_em >= :de
                          AND ocorrido_em <  :ate
                          AND (CAST(:acao AS TEXT) IS NULL OR acao = :acao)
                          AND (CAST(:recurso AS TEXT) IS NULL OR recurso = :recurso)
                          AND (CAST(:usuario AS BIGINT) IS NULL OR id_usuario = :usuario)
                          AND id_evento > :apos
                        ORDER BY id_evento
                        LIMIT :limite
                        """)
                .param("de", java.sql.Timestamp.from(de))
                .param("ate", java.sql.Timestamp.from(ate))
                .param("acao", acao)
                .param("recurso", recurso)
                .param("usuario", idUsuario)
                .param("apos", apos)
                .param("limite", limite)
                .query((rs, n) -> {
                    long bruto = rs.getLong("id_usuario");
                    Long usuario = rs.wasNull() ? null : bruto;
                    return new LinhaEvento(
                            rs.getLong("id_evento"),
                            rs.getTimestamp("ocorrido_em").toInstant(),
                            usuario,
                            rs.getString("staff_papel"),
                            rs.getString("acao"),
                            rs.getString("recurso"),
                            rs.getString("id_recurso"),
                            rs.getString("ip_origem"),
                            rs.getString("correlacao_id"));
                })
                .list();
    }

    public void inserir(long idClinica, Long idUsuario, String staffPapel,
                        String acao, String recurso, String idRecurso,
                        String dadosAnteriores, String dadosPosteriores, UUID correlacao) {
        jdbc.sql("""
                        INSERT INTO auditoria.eventos
                            (id_clinica, id_usuario, staff_papel, acao, recurso, id_recurso,
                             dados_anteriores, dados_posteriores, correlacao_id)
                        VALUES (:clinica, :usuario, :staff, :acao, :recurso, :idRecurso,
                                CAST(:antes AS JSONB), CAST(:depois AS JSONB),
                                CAST(:correlacao AS UUID))
                        """)
                .param("clinica", idClinica)
                .param("usuario", idUsuario)
                .param("staff", staffPapel)
                .param("acao", acao)
                .param("recurso", recurso)
                .param("idRecurso", idRecurso)
                .param("antes", dadosAnteriores)
                .param("depois", dadosPosteriores)
                .param("correlacao", correlacao == null ? null : correlacao.toString())
                .update();
    }

    /**
     * Login e falha de login acontecem antes de haver contexto completo — às
     * vezes antes de existir usuário resolvido. Por isso a clínica vem por
     * parâmetro em vez de sair do contexto.
     */
    public void inserirAutenticacao(String acao, Long idClinica, Long idUsuario,
                                    String ipOrigem, String userAgent, UUID correlacao) {
        if (idClinica == null) {
            // Sem clínica resolvida (e-mail inexistente) não há linha de
            // auditoria de tenant possível: o RLS recusaria, e forçar a escrita
            // exigiria abrir a tabela. Fica no log da aplicação, sem o e-mail.
            return;
        }
        jdbc.sql("""
                        INSERT INTO auditoria.eventos
                            (id_clinica, id_usuario, acao, recurso, id_recurso,
                             ip_origem, user_agent, correlacao_id)
                        VALUES (:clinica, :usuario, :acao, 'autenticacao', NULL,
                                CAST(:ip AS INET), :ua, CAST(:correlacao AS UUID))
                        """)
                .param("clinica", idClinica)
                .param("usuario", idUsuario)
                .param("acao", acao)
                .param("ip", ipOrigem)
                .param("ua", userAgent == null ? null
                        : userAgent.substring(0, Math.min(userAgent.length(), 500)))
                .param("correlacao", correlacao == null ? null : correlacao.toString())
                .update();
    }
}
