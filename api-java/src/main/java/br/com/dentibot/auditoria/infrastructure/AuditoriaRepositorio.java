package br.com.dentibot.auditoria.infrastructure;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuditoriaRepositorio {

    private final JdbcClient jdbc;

    public AuditoriaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
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
