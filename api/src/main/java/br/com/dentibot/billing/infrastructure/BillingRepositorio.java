package br.com.dentibot.billing.infrastructure;

import br.com.dentibot.billing.Assinatura;
import br.com.dentibot.billing.Fatura;
import br.com.dentibot.billing.Plano;
import br.com.dentibot.billing.SincronizacaoDeAssinatura;
import br.com.dentibot.billing.SincronizacaoDeFatura;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class BillingRepositorio {

    private final JdbcClient jdbc;

    public BillingRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ─── Catálogo ────────────────────────────────────────────────────────────

    /**
     * {@code billing.planos} não tem {@code id_clinica} e por isso não está sob
     * RLS: é catálogo, igual para todo mundo. A app só lê.
     */
    public List<Plano> listarPlanos() {
        return jdbc.sql("""
                        SELECT codigo, nome, preco_mensal_centavos, max_profissionais,
                               max_mensagens_mes, permite_permissao_por_perfil
                        FROM billing.planos
                        ORDER BY preco_mensal_centavos
                        """)
                .query((rs, n) -> new Plano(
                        rs.getString("codigo"),
                        rs.getString("nome"),
                        rs.getInt("preco_mensal_centavos"),
                        rs.getInt("max_profissionais"),
                        rs.getInt("max_mensagens_mes"),
                        rs.getBoolean("permite_permissao_por_perfil")))
                .list();
    }

    public Optional<Plano> buscarPlano(String codigo) {
        return jdbc.sql("""
                        SELECT codigo, nome, preco_mensal_centavos, max_profissionais,
                               max_mensagens_mes, permite_permissao_por_perfil
                        FROM billing.planos WHERE codigo = :codigo
                        """)
                .param("codigo", codigo)
                .query((rs, n) -> new Plano(
                        rs.getString("codigo"),
                        rs.getString("nome"),
                        rs.getInt("preco_mensal_centavos"),
                        rs.getInt("max_profissionais"),
                        rs.getInt("max_mensagens_mes"),
                        rs.getBoolean("permite_permissao_por_perfil")))
                .optional();
    }

    // ─── Assinatura ──────────────────────────────────────────────────────────

    /**
     * A assinatura VIVA. O índice parcial {@code uq_assinatura_viva} garante que
     * existe no máximo uma nesses três estados, então o {@code optional()} aqui
     * nunca pode estourar com "mais de uma linha" — a garantia é do banco, não
     * do LIMIT.
     */
    public Optional<Assinatura> buscarViva() {
        return jdbc.sql("""
                        SELECT id_assinatura, plano, provedor, status, periodo_inicio,
                               periodo_fim, cancelar_no_fim, id_assinatura_externa
                        FROM billing.assinaturas
                        WHERE status IN ('trial', 'ativa', 'inadimplente')
                        """)
                .query(BillingRepositorio::mapearAssinatura)
                .optional();
    }

    public long inserirAssinatura(String plano, String provedor, String status) {
        return jdbc.sql("""
                        INSERT INTO billing.assinaturas (id_clinica, plano, provedor, status)
                        VALUES (:clinica, :plano, :provedor, :status)
                        RETURNING id_assinatura
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("plano", plano)
                .param("provedor", provedor)
                .param("status", status)
                .query(Long.class)
                .single();
    }

    /**
     * UPSERT pelo id externo. É o que torna o webhook idempotente: o Stripe
     * reentrega, e a segunda entrega do mesmo evento não pode criar uma segunda
     * assinatura nem reverter um estado mais novo.
     *
     * <p>Não usa {@code ContextoAtual} porque quem chama é o consumidor do
     * webhook, que age sobre a clínica que o evento nomeia.
     */
    public int sincronizarAssinatura(SincronizacaoDeAssinatura s) {
        return jdbc.sql("""
                        INSERT INTO billing.assinaturas
                            (id_clinica, plano, provedor, id_cliente_externo,
                             id_assinatura_externa, status, periodo_inicio, periodo_fim,
                             cancelar_no_fim)
                        VALUES (:clinica, :plano, 'stripe', :cliente, :externa, :status,
                                COALESCE(:inicio, NOW()), :fim, :cancelar)
                        ON CONFLICT (id_assinatura_externa) DO UPDATE
                            SET plano = EXCLUDED.plano,
                                status = EXCLUDED.status,
                                periodo_inicio = EXCLUDED.periodo_inicio,
                                periodo_fim = EXCLUDED.periodo_fim,
                                cancelar_no_fim = EXCLUDED.cancelar_no_fim,
                                id_cliente_externo = COALESCE(EXCLUDED.id_cliente_externo,
                                                              billing.assinaturas.id_cliente_externo)
                        """)
                .param("clinica", s.idClinica())
                .param("plano", s.plano())
                .param("cliente", s.idClienteExterno())
                .param("externa", s.idAssinaturaExterna())
                .param("status", s.status())
                .param("inicio", s.periodoInicio() == null ? null : Timestamp.from(s.periodoInicio()))
                .param("fim", s.periodoFim() == null ? null : Timestamp.from(s.periodoFim()))
                .param("cancelar", s.cancelarNoFim())
                .update();
    }

    /**
     * Encerra a assinatura em trial ao vincular a do provedor.
     *
     * <p>Sem isto, o índice parcial recusaria a nova: já existe uma viva. O
     * erro apareceria como violação de unicidade dentro de um webhook, que é o
     * pior lugar possível para descobrir isso.
     */
    public int encerrarTrialSemProvedor(long idClinica) {
        return jdbc.sql("""
                        UPDATE billing.assinaturas
                        SET status = 'encerrada'
                        WHERE id_clinica = :clinica
                          AND status = 'trial'
                          AND id_assinatura_externa IS NULL
                        """)
                .param("clinica", idClinica)
                .update();
    }

    // ─── Faturas ─────────────────────────────────────────────────────────────

    public List<Fatura> listarFaturas() {
        return jdbc.sql("""
                        SELECT id_fatura, id_externo, valor_centavos, status, competencia,
                               vence_em, paga_em, url_fatura
                        FROM billing.faturas
                        ORDER BY competencia DESC
                        LIMIT 60
                        """)
                .query(BillingRepositorio::mapearFatura)
                .list();
    }

    /** UPSERT pelo id externo, mesma razão da assinatura. */
    public int sincronizarFatura(SincronizacaoDeFatura f) {
        return jdbc.sql("""
                        INSERT INTO billing.faturas
                            (id_clinica, id_externo, valor_centavos, status, competencia,
                             vence_em, paga_em, url_fatura)
                        VALUES (:clinica, :externo, :valor, :status, :competencia,
                                :vence, :paga, :url)
                        ON CONFLICT (id_externo) DO UPDATE
                            SET status = EXCLUDED.status,
                                valor_centavos = EXCLUDED.valor_centavos,
                                vence_em = EXCLUDED.vence_em,
                                paga_em = EXCLUDED.paga_em,
                                url_fatura = EXCLUDED.url_fatura
                        """)
                .param("clinica", f.idClinica())
                .param("externo", f.idExterno())
                .param("valor", f.valorCentavos())
                .param("status", f.status())
                .param("competencia", f.competencia())
                .param("vence", f.venceEm())
                .param("paga", f.pagaEm() == null ? null : Timestamp.from(f.pagaEm()))
                .param("url", f.urlFatura())
                .update();
    }

    // ─── Uso ─────────────────────────────────────────────────────────────────

    /** Zeros quando o mês ainda não tem linha — mês sem uso não é erro. */
    public int[] usoDoMes(LocalDate competencia) {
        return jdbc.sql("""
                        SELECT mensagens_enviadas, profissionais_ativos
                        FROM billing.uso_mensal
                        WHERE competencia = :competencia
                        """)
                .param("competencia", competencia)
                .query((rs, n) -> new int[] {
                        rs.getInt("mensagens_enviadas"), rs.getInt("profissionais_ativos") })
                .optional()
                .orElse(new int[] { 0, 0 });
    }

    /**
     * Soma ao contador do mês. UPSERT em vez de ler-somar-gravar: duas mensagens
     * disparadas ao mesmo tempo perderiam uma contagem no caminho de leitura, e
     * o limite de plano viraria sugestão.
     */
    public void somarMensagens(long idClinica, LocalDate competencia, int quantas) {
        jdbc.sql("""
                        INSERT INTO billing.uso_mensal
                            (id_clinica, competencia, mensagens_enviadas)
                        VALUES (:clinica, :competencia, :quantas)
                        ON CONFLICT (id_clinica, competencia) DO UPDATE
                            SET mensagens_enviadas =
                                    billing.uso_mensal.mensagens_enviadas + EXCLUDED.mensagens_enviadas,
                                atualizado_em = NOW()
                        """)
                .param("clinica", idClinica)
                .param("competencia", competencia)
                .param("quantas", quantas)
                .update();
    }

    /** Fotografia do número de profissionais — substitui, não soma. */
    public void registrarProfissionais(LocalDate competencia, int quantos) {
        jdbc.sql("""
                        INSERT INTO billing.uso_mensal
                            (id_clinica, competencia, profissionais_ativos)
                        VALUES (:clinica, :competencia, :quantos)
                        ON CONFLICT (id_clinica, competencia) DO UPDATE
                            SET profissionais_ativos = EXCLUDED.profissionais_ativos,
                                atualizado_em = NOW()
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("competencia", competencia)
                .param("quantos", quantos)
                .update();
    }

    private static Assinatura mapearAssinatura(ResultSet rs, int linha) throws SQLException {
        return new Assinatura(
                rs.getLong("id_assinatura"),
                rs.getString("plano"),
                rs.getString("provedor"),
                rs.getString("status"),
                rs.getTimestamp("periodo_inicio").toInstant(),
                rs.getTimestamp("periodo_fim") == null
                        ? null : rs.getTimestamp("periodo_fim").toInstant(),
                rs.getBoolean("cancelar_no_fim"),
                rs.getString("id_assinatura_externa"));
    }

    private static Fatura mapearFatura(ResultSet rs, int linha) throws SQLException {
        return new Fatura(
                rs.getLong("id_fatura"),
                rs.getString("id_externo"),
                rs.getInt("valor_centavos"),
                rs.getString("status"),
                rs.getDate("competencia").toLocalDate(),
                rs.getDate("vence_em") == null ? null : rs.getDate("vence_em").toLocalDate(),
                rs.getTimestamp("paga_em") == null ? null : rs.getTimestamp("paga_em").toInstant(),
                rs.getString("url_fatura"));
    }
}
