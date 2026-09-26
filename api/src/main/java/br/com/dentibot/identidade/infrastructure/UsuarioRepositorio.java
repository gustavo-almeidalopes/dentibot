package br.com.dentibot.identidade.infrastructure;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.Papel;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Usuários da clínica (eixo A).
 *
 * <p>Saiu daqui, na V18: tudo que atendia senha. {@code senha_hash},
 * {@code bloqueio_login}, contagem de falha e desbloqueio por decurso de prazo
 * eram a implementação de um login que este serviço não faz mais — o Clerk faz,
 * junto com 2FA, proteção contra força bruta e recuperação de conta. Código de
 * autenticação que continua existindo sem ser chamado é pior que código
 * ausente: parece uma defesa ativa numa revisão.
 */
@Repository
public class UsuarioRepositorio {

    private final JdbcClient jdbc;

    public UsuarioRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** O que a resolução por {@code sub} do Clerk devolve. */
    public record AcessoDeClinica(long idUsuario, long idClinica, Papel papel, String status) {
    }

    /** Idem, para o eixo B. Staff não tem clínica, por definição. */
    public record AcessoDeStaff(long idStaff, String papel, String status) {
    }

    /** Linha crua de usuário, sem o nome: o nome é de {@code identidade.pessoas}. */
    public record LinhaUsuario(long idUsuario, long idPessoa, String email, Papel papel,
                               String status, boolean vinculado) {
    }

    /**
     * Resolve a conta a partir do {@code sub} do token do Clerk, ANTES de existir
     * contexto de tenant.
     *
     * <p>Chama uma função SECURITY DEFINER da V18 — a travessia do RLS tem
     * sujeito próprio ({@code dentibot_autenticador}) e privilégio por coluna,
     * pelo mesmo motivo documentado na V3: a tabela tem RLS e, no momento desta
     * consulta, ninguém sabe ainda de qual clínica é a linha.
     *
     * <p>Roda a cada requisição autenticada: uma ida a mais ao banco, por índice
     * único, dentro de uma transação de leitura.
     */
    // ponytail: uma consulta por requisição; cachear em Redis por ~60s com
    // invalidação no UPDATE de papel/status se o perfil de carga cobrar.
    public Optional<AcessoDeClinica> resolverAcessoPorClerk(String sub) {
        return jdbc.sql("SELECT * FROM identidade.resolver_acesso_por_clerk(:sub)")
                .param("sub", sub)
                .query(UsuarioRepositorio::mapearAcesso)
                .optional();
    }

    /**
     * A conta que a clínica cadastrou e que ainda espera o primeiro login —
     * e-mail bate, {@code clerk_user_id} ainda nulo. A função filtra por isso;
     * ver a V18 para por que a segunda metade da condição é a que importa.
     */
    public Optional<AcessoDeClinica> resolverAcessoPendentePorEmail(String email) {
        return jdbc.sql("SELECT * FROM identidade.resolver_acesso_pendente_por_email(:email)")
                .param("email", email.toLowerCase())
                .query(UsuarioRepositorio::mapearAcesso)
                .optional();
    }

    /**
     * O eixo B mora em {@code identidade.staff_plataforma} — outra tabela, mesmo
     * schema, mesmo módulo. Fica neste repositório em vez de ganhar uma classe
     * para uma consulta só: a pergunta é a mesma ("quem é este sub?"), e o que
     * difere é a resposta.
     */
    public Optional<AcessoDeStaff> resolverStaffPorClerk(String sub) {
        return jdbc.sql("SELECT * FROM identidade.resolver_staff_por_clerk(:sub)")
                .param("sub", sub)
                .query((rs, n) -> new AcessoDeStaff(
                        rs.getLong("id_staff"),
                        rs.getString("papel"),
                        rs.getString("status")))
                .optional();
    }

    /**
     * Vincula a conta do Clerk ao usuário. Escrita normal da app, sob RLS: quem
     * chama precisa ter promovido o tenant antes, senão o UPDATE encontra zero
     * linhas e não avisa.
     */
    public void vincularClerk(long idUsuario, String sub) {
        jdbc.sql("""
                        UPDATE identidade.usuarios
                        SET clerk_user_id = :sub
                        WHERE id_usuario = :id
                        """)
                .param("id", idUsuario)
                .param("sub", sub)
                .update();
    }

    /**
     * @param clerkUserId o {@code sub} de quem já tem conta no Clerk (o admin
     *                    que acabou de cadastrar a clínica), ou nulo para quem a
     *                    clínica cadastrou pela tela de equipe e ainda vai
     *                    entrar pela primeira vez.
     */
    public long inserir(long idPessoa, String email, Papel papel, String clerkUserId) {
        return jdbc.sql("""
                        INSERT INTO identidade.usuarios
                            (id_clinica, id_pessoa, email, papel, clerk_user_id)
                        VALUES (:clinica, :pessoa, :email, :papel, :clerk)
                        RETURNING id_usuario
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("pessoa", idPessoa)
                .param("email", email.toLowerCase())
                .param("papel", papel.valorBanco())
                .param("clerk", clerkUserId)
                .query(Long.class)
                .single();
    }

    /** Listagem da tela de equipe. O RLS já limita ao tenant. */
    public List<LinhaUsuario> listar() {
        return jdbc.sql("""
                        SELECT id_usuario, id_pessoa, email, papel, status,
                               clerk_user_id IS NOT NULL AS vinculado
                        FROM identidade.usuarios
                        ORDER BY id_usuario
                        """)
                .query((rs, n) -> new LinhaUsuario(
                        rs.getLong("id_usuario"),
                        rs.getLong("id_pessoa"),
                        rs.getString("email"),
                        Papel.de(rs.getString("papel")),
                        rs.getString("status"),
                        rs.getBoolean("vinculado")))
                .list();
    }

    public Optional<LinhaUsuario> buscar(long idUsuario) {
        return jdbc.sql("""
                        SELECT id_usuario, id_pessoa, email, papel, status,
                               clerk_user_id IS NOT NULL AS vinculado
                        FROM identidade.usuarios
                        WHERE id_usuario = :id
                        """)
                .param("id", idUsuario)
                .query((rs, n) -> new LinhaUsuario(
                        rs.getLong("id_usuario"),
                        rs.getLong("id_pessoa"),
                        rs.getString("email"),
                        Papel.de(rs.getString("papel")),
                        rs.getString("status"),
                        rs.getBoolean("vinculado")))
                .optional();
    }

    /**
     * Muda papel e status. Os dois num UPDATE só porque é uma tela só — e porque
     * separar em dois abriria a janela em que o usuário está com o papel novo e
     * o status velho.
     */
    public int atualizar(long idUsuario, Papel papel, String status) {
        return jdbc.sql("""
                        UPDATE identidade.usuarios
                        SET papel = :papel, status = :status
                        WHERE id_usuario = :id
                        """)
                .param("id", idUsuario)
                .param("papel", papel.valorBanco())
                .param("status", status)
                .update();
    }

    /** Quantos admins ativos restam. Guarda contra a clínica ficar sem dono. */
    public int contarAdminsAtivos() {
        return jdbc.sql("""
                        SELECT count(*) FROM identidade.usuarios
                        WHERE papel = 'admin' AND status = 'ativo'
                        """)
                .query(Integer.class)
                .single();
    }

    public int contarProfissionaisAtivos() {
        return jdbc.sql("""
                        SELECT count(*) FROM identidade.usuarios
                        WHERE status = 'ativo' AND papel IN ('admin','dentista')
                        """)
                .query(Integer.class)
                .single();
    }

    private static AcessoDeClinica mapearAcesso(java.sql.ResultSet rs, int linha)
            throws java.sql.SQLException {
        return new AcessoDeClinica(
                rs.getLong("id_usuario"),
                rs.getLong("id_clinica"),
                Papel.de(rs.getString("papel")),
                rs.getString("status"));
    }
}
