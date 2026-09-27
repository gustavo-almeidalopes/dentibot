package br.com.dentibot.lgpd.infrastructure;

import br.com.dentibot.lgpd.SolicitacaoTitular;
import br.com.dentibot.lgpd.Termo;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class LgpdRepositorio {

    /**
     * Linha crua de consentimento: as finalidades saem do banco como TEXTO de
     * JSONB. Quem as converte é o serviço, que tem o ObjectMapper —
     * repositório não conhece Jackson.
     */
    public record LinhaConsentimento(long idConsentimento, long idPaciente, long idTermo,
                                     String tipoDoTermo, String versaoDoTermo,
                                     String finalidadesJson, java.time.Instant aceitoEm,
                                     String ipOrigem, java.time.Instant revogadoEm) {
    }

    private final JdbcClient jdbc;

    public LgpdRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ─── Termos ──────────────────────────────────────────────────────────────

    public List<Termo> listarTermos(boolean somenteAtivos) {
        return jdbc.sql("""
                        SELECT id_termo, tipo, versao, texto_integral,
                               ativo_desde, ativo, created_at
                        FROM lgpd.termos
                        WHERE (:somenteAtivos = FALSE OR ativo)
                        ORDER BY tipo, ativo_desde DESC
                        """)
                .param("somenteAtivos", somenteAtivos)
                .query(LgpdRepositorio::mapearTermo)
                .list();
    }

    public Optional<Termo> buscarTermo(long idTermo) {
        return jdbc.sql("""
                        SELECT id_termo, tipo, versao, texto_integral,
                               ativo_desde, ativo, created_at
                        FROM lgpd.termos WHERE id_termo = :id
                        """)
                .param("id", idTermo)
                .query(LgpdRepositorio::mapearTermo)
                .optional();
    }

    public long inserirTermo(String tipo, String versao, String texto, LocalDate ativoDesde) {
        return jdbc.sql("""
                        INSERT INTO lgpd.termos
                            (id_clinica, tipo, versao, texto_integral, ativo_desde)
                        VALUES (:clinica, :tipo, :versao, :texto, :desde)
                        RETURNING id_termo
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("tipo", tipo)
                .param("versao", versao)
                .param("texto", texto)
                .param("desde", ativoDesde)
                .query(Long.class)
                .single();
    }

    /**
     * Único UPDATE permitido na tabela — a V16 revoga UPDATE geral e concede
     * {@code UPDATE (ativo)} por coluna. Tentar mexer no texto aqui não
     * "passaria despercebido": o Postgres recusa.
     */
    public int desativarTermo(long idTermo) {
        return jdbc.sql("UPDATE lgpd.termos SET ativo = FALSE WHERE id_termo = :id AND ativo")
                .param("id", idTermo)
                .update();
    }

    /** Desativa as versões anteriores do MESMO tipo ao publicar uma nova. */
    public int desativarOutrasVersoes(String tipo, long idNovoTermo) {
        return jdbc.sql("""
                        UPDATE lgpd.termos SET ativo = FALSE
                        WHERE tipo = :tipo AND id_termo <> :novo AND ativo
                        """)
                .param("tipo", tipo)
                .param("novo", idNovoTermo)
                .update();
    }

    // ─── Consentimentos ──────────────────────────────────────────────────────

    /**
     * O JOIN é com {@code lgpd.termos} — mesmo schema, mesmo módulo. Tipo e
     * versão vêm junto porque um consentimento sem eles não diz a que a pessoa
     * consentiu.
     */
    public List<LinhaConsentimento> consentimentosDoPaciente(long idPaciente) {
        return jdbc.sql("""
                        SELECT c.id_consentimento, c.id_paciente, c.id_termo,
                               t.tipo, t.versao,
                               c.finalidades_aceitas::text AS finalidades,
                               c.aceito_em, host(c.ip_origem) AS ip, c.revogado_em
                        FROM lgpd.consentimentos c
                        JOIN lgpd.termos t ON t.id_termo = c.id_termo
                        WHERE c.id_paciente = :paciente
                        ORDER BY c.aceito_em DESC
                        """)
                .param("paciente", idPaciente)
                .query(LgpdRepositorio::mapearConsentimento)
                .list();
    }

    public long inserirConsentimento(long idPaciente, long idTermo, String finalidadesJson,
                                     String ipOrigem, String userAgent) {
        return jdbc.sql("""
                        INSERT INTO lgpd.consentimentos
                            (id_clinica, id_paciente, id_termo, finalidades_aceitas,
                             aceito_em, ip_origem, user_agent)
                        VALUES (:clinica, :paciente, :termo, CAST(:finalidades AS JSONB),
                                NOW(), CAST(:ip AS INET), :ua)
                        RETURNING id_consentimento
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("termo", idTermo)
                .param("finalidades", finalidadesJson)
                .param("ip", ipOrigem)
                .param("ua", userAgent)
                .query(Long.class)
                .single();
    }

    /**
     * Revogar MARCA, não apaga — e a condição {@code revogado_em IS NULL} deixa
     * a primeira revogação valer. Sobrescrever a data numa segunda chamada
     * moveria a fronteira do período em que o tratamento era lícito.
     */
    public int revogarConsentimento(long idConsentimento) {
        return jdbc.sql("""
                        UPDATE lgpd.consentimentos SET revogado_em = NOW()
                        WHERE id_consentimento = :id AND revogado_em IS NULL
                        """)
                .param("id", idConsentimento)
                .update();
    }

    // ─── Preferências por finalidade ─────────────────────────────────────────

    public record LinhaPreferencia(String finalidade, boolean permitido, Instant em, String origem) {
    }

    /** A mais recente de cada finalidade — o estado atual. */
    public List<LinhaPreferencia> preferenciasAtuais(long idPaciente) {
        return jdbc.sql("""
                        SELECT DISTINCT ON (finalidade) finalidade, permitido, created_at, origem
                        FROM lgpd.preferencias
                        WHERE id_paciente = :paciente
                        ORDER BY finalidade, id_preferencia DESC
                        """)
                .param("paciente", idPaciente)
                .query((rs, n) -> new LinhaPreferencia(rs.getString("finalidade"),
                        rs.getBoolean("permitido"), rs.getTimestamp("created_at").toInstant(),
                        rs.getString("origem")))
                .list();
    }

    public void inserirPreferencia(long idPaciente, String finalidade, boolean permitido,
                                   String origem, Long idUsuario) {
        jdbc.sql("""
                        INSERT INTO lgpd.preferencias
                            (id_clinica, id_paciente, finalidade, permitido, origem, id_usuario)
                        VALUES (:clinica, :paciente, :finalidade, :permitido, :origem, :usuario)
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("finalidade", finalidade)
                .param("permitido", permitido)
                .param("origem", origem)
                .param("usuario", idUsuario)
                .update();
    }

    // ─── Link do titular ─────────────────────────────────────────────────────

    public void inserirAcessoDoTitular(long idPaciente, String tokenSha256, Instant expiraEm,
                                       long criadoPor) {
        jdbc.sql("""
                        INSERT INTO lgpd.acessos_titular
                            (id_clinica, id_paciente, token_sha256, expira_em, criado_por)
                        VALUES (:clinica, :paciente, :token, :expira, :usuario)
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("token", tokenSha256)
                .param("expira", Timestamp.from(expiraEm))
                .param("usuario", criadoPor)
                .update();
    }

    /** Link novo derruba os anteriores: um paciente, um link vivo. */
    public int revogarAcessosDoTitular(long idPaciente) {
        return jdbc.sql("""
                        UPDATE lgpd.acessos_titular SET revogado_em = now()
                        WHERE id_paciente = :paciente AND revogado_em IS NULL
                        """)
                .param("paciente", idPaciente)
                .update();
    }

    public record Titular(long idClinica, long idPaciente) {
    }

    /** Pela função SECURITY DEFINER da V25: roda sem tenant, recebe só o hash. */
    public Optional<Titular> titularDoToken(String tokenSha256) {
        return jdbc.sql("SELECT * FROM lgpd.titular_do_token(:token)")
                .param("token", tokenSha256)
                .query((rs, n) -> new Titular(rs.getLong("id_clinica"), rs.getLong("id_paciente")))
                .optional();
    }

    public long inserirOposicao(long idPaciente, String detalhe, LocalDate prazo) {
        return jdbc.sql("""
                        INSERT INTO lgpd.solicitacoes_titular
                            (id_clinica, id_paciente, direito, prazo_resposta_em, detalhe)
                        VALUES (:clinica, :paciente, 'oposicao', :prazo, :detalhe)
                        RETURNING id_solicitacao
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("prazo", prazo)
                .param("detalhe", detalhe)
                .query(Long.class)
                .single();
    }

    // ─── Solicitações do titular ─────────────────────────────────────────────

    public List<SolicitacaoTitular> listarSolicitacoes(String status) {
        return jdbc.sql("""
                        SELECT id_solicitacao, id_paciente, direito, aberta_em,
                               prazo_resposta_em, status, justificativa_recusa,
                               respondida_em,
                               detalhe,
                               (respondida_em IS NULL AND prazo_resposta_em < CURRENT_DATE)
                                   AS vencida
                        FROM lgpd.solicitacoes_titular
                        WHERE (CAST(:status AS TEXT) IS NULL OR status = :status)
                        ORDER BY vencida DESC, prazo_resposta_em
                        LIMIT 500
                        """)
                .param("status", status)
                .query(LgpdRepositorio::mapearSolicitacao)
                .list();
    }

    public long inserirSolicitacao(long idPaciente, String direito, LocalDate prazo) {
        return jdbc.sql("""
                        INSERT INTO lgpd.solicitacoes_titular
                            (id_clinica, id_paciente, direito, prazo_resposta_em)
                        VALUES (:clinica, :paciente, :direito, :prazo)
                        RETURNING id_solicitacao
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("direito", direito)
                .param("prazo", prazo)
                .query(Long.class)
                .single();
    }

    public int responder(long idSolicitacao, String status, String justificativa, String chave) {
        return jdbc.sql("""
                        UPDATE lgpd.solicitacoes_titular
                        SET status = :status,
                            justificativa_recusa = :justificativa,
                            chave_objeto_resposta = :chave,
                            respondida_em = NOW()
                        WHERE id_solicitacao = :id AND respondida_em IS NULL
                        """)
                .param("id", idSolicitacao)
                .param("status", status)
                .param("justificativa", justificativa)
                .param("chave", chave)
                .update();
    }

    // ─── mapeadores ──────────────────────────────────────────────────────────

    private static Termo mapearTermo(ResultSet rs, int linha) throws SQLException {
        return new Termo(
                rs.getLong("id_termo"),
                rs.getString("tipo"),
                rs.getString("versao"),
                rs.getString("texto_integral"),
                rs.getDate("ativo_desde").toLocalDate(),
                rs.getBoolean("ativo"),
                rs.getTimestamp("created_at").toInstant());
    }

    private static LinhaConsentimento mapearConsentimento(ResultSet rs, int linha)
            throws SQLException {
        return new LinhaConsentimento(
                rs.getLong("id_consentimento"),
                rs.getLong("id_paciente"),
                rs.getLong("id_termo"),
                rs.getString("tipo"),
                rs.getString("versao"),
                rs.getString("finalidades"),
                rs.getTimestamp("aceito_em").toInstant(),
                rs.getString("ip"),
                rs.getTimestamp("revogado_em") == null
                        ? null : rs.getTimestamp("revogado_em").toInstant());
    }

    private static SolicitacaoTitular mapearSolicitacao(ResultSet rs, int linha)
            throws SQLException {
        return new SolicitacaoTitular(
                rs.getLong("id_solicitacao"),
                rs.getLong("id_paciente"),
                rs.getString("direito"),
                rs.getTimestamp("aberta_em").toInstant(),
                rs.getDate("prazo_resposta_em").toLocalDate(),
                rs.getString("status"),
                rs.getString("justificativa_recusa"),
                rs.getTimestamp("respondida_em") == null
                        ? null : rs.getTimestamp("respondida_em").toInstant(),
                rs.getBoolean("vencida"),
                rs.getString("detalhe"));
    }
}
