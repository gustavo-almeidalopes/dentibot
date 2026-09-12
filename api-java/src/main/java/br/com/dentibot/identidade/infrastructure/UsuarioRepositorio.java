package br.com.dentibot.identidade.infrastructure;

import br.com.dentibot.identidade.domain.CredenciaisUsuario;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.Papel;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioRepositorio {

    private final JdbcClient jdbc;

    public UsuarioRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Resolve a clínica de um e-mail ANTES de existir contexto de tenant.
     *
     * <p>Chama a função SECURITY DEFINER da V3 — a única travessia legítima do
     * RLS no sistema. Devolve vazio quando o e-mail não existe, e quem chama
     * responde sempre "credenciais inválidas": distinguir "e-mail não existe" de
     * "senha errada" entrega a lista de usuários a quem tentar.
     */
    public Optional<Long> resolverClinicaPorEmail(String email) {
        return jdbc.sql("SELECT identidade.resolver_clinica_por_email(:email)")
                .param("email", email.toLowerCase())
                .query(Long.class)
                .optional();
    }

    /**
     * O LEFT JOIN é com {@code identidade.bloqueio_login} — mesmo schema, mesmo
     * módulo. Juntar schemas de módulos diferentes é o que a camada 7 proíbe;
     * dentro do próprio módulo, JOIN é só SQL.
     */
    private static final String SQL_CREDENCIAIS = """
            SELECT u.id_usuario, u.id_clinica, u.senha_hash, u.papel, u.status,
                   u.metodo_2fa, u.totp_secret,
                   COALESCE(b.falhas_consecutivas, 0) AS falhas,
                   b.bloqueado_ate
            FROM identidade.usuarios u
            LEFT JOIN identidade.bloqueio_login b ON b.id_usuario = u.id_usuario
            """;

    /** Exige contexto de tenant já promovido. */
    public Optional<CredenciaisUsuario> buscarCredenciaisPorEmail(String email) {
        return jdbc.sql(SQL_CREDENCIAIS + " WHERE u.email = :email")
                .param("email", email.toLowerCase())
                .query(this::mapear)
                .optional();
    }

    /** Usado na renovação de token, quando o e-mail não viaja no refresh. */
    public Optional<CredenciaisUsuario> buscarCredenciaisPorId(long idUsuario) {
        return jdbc.sql(SQL_CREDENCIAIS + " WHERE u.id_usuario = :id")
                .param("id", idUsuario)
                .query(this::mapear)
                .optional();
    }

    private CredenciaisUsuario mapear(java.sql.ResultSet rs, int linha) throws java.sql.SQLException {
        return new CredenciaisUsuario(
                rs.getLong("id_usuario"),
                rs.getLong("id_clinica"),
                rs.getString("senha_hash"),
                Papel.de(rs.getString("papel")),
                rs.getString("status"),
                rs.getString("metodo_2fa"),
                rs.getString("totp_secret"),
                rs.getInt("falhas"),
                rs.getTimestamp("bloqueado_ate") == null
                        ? null : rs.getTimestamp("bloqueado_ate").toInstant());
    }

    public long inserir(long idPessoa, String email, String senhaHash, Papel papel) {
        return jdbc.sql("""
                        INSERT INTO identidade.usuarios
                            (id_clinica, id_pessoa, email, senha_hash, papel)
                        VALUES (:clinica, :pessoa, :email, :hash, :papel)
                        RETURNING id_usuario
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("pessoa", idPessoa)
                .param("email", email.toLowerCase())
                .param("hash", senhaHash)
                .param("papel", papel.valorBanco())
                .query(Long.class)
                .single();
    }

    public void registrarLoginBemSucedido(long idUsuario) {
        jdbc.sql("UPDATE identidade.usuarios SET ultimo_login_em = NOW() WHERE id_usuario = :id")
                .param("id", idUsuario)
                .update();
        jdbc.sql("""
                        UPDATE identidade.bloqueio_login
                        SET falhas_consecutivas = 0, bloqueado_ate = NULL, ultima_falha_em = NULL
                        WHERE id_usuario = :id
                        """)
                .param("id", idUsuario)
                .update();
    }

    /**
     * Conta a falha e bloqueia ao atingir o limite. Um UPSERT só: duas
     * requisições simultâneas com senha errada não podem perder uma contagem,
     * senão o limite de tentativas vira sugestão.
     */
    public int registrarFalhaLogin(long idUsuario, int limite, java.time.Duration bloqueio) {
        Integer falhas = jdbc.sql("""
                        INSERT INTO identidade.bloqueio_login
                            (id_usuario, id_clinica, falhas_consecutivas, ultima_falha_em)
                        VALUES (:id, :clinica, 1, NOW())
                        ON CONFLICT (id_usuario) DO UPDATE
                            SET falhas_consecutivas = identidade.bloqueio_login.falhas_consecutivas + 1,
                                ultima_falha_em = NOW()
                        RETURNING falhas_consecutivas
                        """)
                .param("id", idUsuario)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .query(Integer.class)
                .single();

        if (falhas >= limite) {
            jdbc.sql("""
                            UPDATE identidade.bloqueio_login
                            SET bloqueado_ate = NOW() + CAST(:janela AS INTERVAL)
                            WHERE id_usuario = :id
                            """)
                    .param("id", idUsuario)
                    .param("janela", bloqueio.toMinutes() + " minutes")
                    .update();
            jdbc.sql("UPDATE identidade.usuarios SET status = 'bloqueado' WHERE id_usuario = :id")
                    .param("id", idUsuario)
                    .update();
        }
        return falhas;
    }

    /** Desbloqueio por decurso de prazo, avaliado na hora do login. */
    public void desbloquearSeExpirado(long idUsuario) {
        jdbc.sql("""
                        UPDATE identidade.usuarios u
                        SET status = 'ativo'
                        FROM identidade.bloqueio_login b
                        WHERE u.id_usuario = b.id_usuario
                          AND u.id_usuario = :id
                          AND u.status = 'bloqueado'
                          AND b.bloqueado_ate IS NOT NULL
                          AND b.bloqueado_ate < NOW()
                        """)
                .param("id", idUsuario)
                .update();
    }

    public int contarProfissionaisAtivos() {
        return jdbc.sql("""
                        SELECT count(*) FROM identidade.usuarios
                        WHERE status = 'ativo' AND papel IN ('admin','dentista')
                        """)
                .query(Integer.class)
                .single();
    }
}
