package br.com.dentibot.orcamento.infrastructure;

import br.com.dentibot.orcamento.OrcamentoResumo;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrcamentoRepositorio {

    /** Linha crua de item: sem o nome do procedimento, que é de outro módulo. */
    public record LinhaItem(long idItem, long idProcedimento, Integer dente, String face,
                            BigDecimal valorCobrado, String statusExecucao,
                            java.time.Instant executadoEm, Long idConsulta) {
    }

    private final JdbcClient jdbc;

    public OrcamentoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final String COLUNAS = """
            SELECT id_orcamento, id_paciente, id_dentista, status,
                   valor_bruto, valor_desconto, valor_final,
                   validade_em, aprovado_em, created_at
            FROM orcamento.orcamentos
            """;

    public List<OrcamentoResumo> listar(Long idPaciente, String status) {
        return jdbc.sql(COLUNAS + """
                        WHERE (CAST(:paciente AS BIGINT) IS NULL OR id_paciente = :paciente)
                          AND (CAST(:status AS TEXT) IS NULL OR status = :status)
                        ORDER BY created_at DESC
                        LIMIT 200
                        """)
                .param("paciente", idPaciente)
                .param("status", status)
                .query(OrcamentoRepositorio::mapear)
                .list();
    }

    public Optional<OrcamentoResumo> buscar(long idOrcamento) {
        return jdbc.sql(COLUNAS + " WHERE id_orcamento = :id")
                .param("id", idOrcamento)
                .query(OrcamentoRepositorio::mapear)
                .optional();
    }

    public String observacoes(long idOrcamento) {
        return jdbc.sql("SELECT observacoes FROM orcamento.orcamentos WHERE id_orcamento = :id")
                .param("id", idOrcamento)
                .query(String.class)
                .optional().orElse(null);
    }

    public long inserir(long idPaciente, long idDentista, LocalDate validadeEm,
                        String observacoes) {
        return jdbc.sql("""
                        INSERT INTO orcamento.orcamentos
                            (id_clinica, id_paciente, id_dentista, validade_em, observacoes)
                        VALUES (:clinica, :paciente, :dentista, :validade, :observacoes)
                        RETURNING id_orcamento
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("dentista", idDentista)
                .param("validade", validadeEm)
                .param("observacoes", observacoes)
                .query(Long.class)
                .single();
    }

    // ─── Itens ───────────────────────────────────────────────────────────────

    public List<LinhaItem> itens(long idOrcamento) {
        return jdbc.sql("""
                        SELECT id_item, id_procedimento, dente, face, valor_cobrado,
                               status_execucao, executado_em, id_consulta
                        FROM orcamento.itens
                        WHERE id_orcamento = :id
                        ORDER BY id_item
                        """)
                .param("id", idOrcamento)
                .query((rs, n) -> {
                    int dente = rs.getInt("dente");
                    Integer denteOuNulo = rs.wasNull() ? null : dente;
                    long consulta = rs.getLong("id_consulta");
                    Long consultaOuNula = rs.wasNull() ? null : consulta;
                    return new LinhaItem(
                            rs.getLong("id_item"),
                            rs.getLong("id_procedimento"),
                            denteOuNulo,
                            rs.getString("face"),
                            rs.getBigDecimal("valor_cobrado"),
                            rs.getString("status_execucao"),
                            rs.getTimestamp("executado_em") == null
                                    ? null : rs.getTimestamp("executado_em").toInstant(),
                            consultaOuNula);
                })
                .list();
    }

    public long inserirItem(long idOrcamento, long idProcedimento, Integer dente, String face,
                            BigDecimal valorCobrado) {
        return jdbc.sql("""
                        INSERT INTO orcamento.itens
                            (id_clinica, id_orcamento, id_procedimento, dente, face, valor_cobrado)
                        VALUES (:clinica, :orcamento, :procedimento, :dente, :face, :valor)
                        RETURNING id_item
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("orcamento", idOrcamento)
                .param("procedimento", idProcedimento)
                .param("dente", dente)
                .param("face", face)
                .param("valor", valorCobrado)
                .query(Long.class)
                .single();
    }

    public int removerItem(long idOrcamento, long idItem) {
        return jdbc.sql("""
                        DELETE FROM orcamento.itens
                        WHERE id_item = :item AND id_orcamento = :orcamento
                        """)
                .param("item", idItem)
                .param("orcamento", idOrcamento)
                .update();
    }

    public int concluirItem(long idItem, long idOrcamento, Long idConsulta) {
        return jdbc.sql("""
                        UPDATE orcamento.itens
                        SET status_execucao = 'concluido',
                            executado_em = NOW(),
                            id_consulta = COALESCE(:consulta, id_consulta)
                        WHERE id_item = :item AND id_orcamento = :orcamento
                          AND status_execucao <> 'cancelado'
                        """)
                .param("item", idItem)
                .param("orcamento", idOrcamento)
                .param("consulta", idConsulta)
                .update();
    }

    // ─── Totais e transições ─────────────────────────────────────────────────

    /**
     * Recalcula o cabeçalho a partir dos itens, no banco.
     *
     * <p>Somar em Java e mandar o total pronto abriria a janela em que dois
     * pedidos concorrentes leem o mesmo bruto e gravam totais diferentes. Aqui o
     * {@code SUM} e o {@code UPDATE} são um statement só, e o
     * {@code ck_total_coerente} confere a aritmética antes de aceitar a linha.
     *
     * <p>O {@code LEAST} protege o outro CHECK: um desconto maior que o novo
     * bruto (porque itens saíram) recusaria o UPDATE com erro de constraint em
     * vez de simplesmente limitar o desconto ao que existe.
     */
    public void recalcularTotais(long idOrcamento) {
        jdbc.sql("""
                        UPDATE orcamento.orcamentos o
                        SET valor_bruto = t.bruto,
                            valor_desconto = LEAST(o.valor_desconto, t.bruto),
                            valor_final = t.bruto - LEAST(o.valor_desconto, t.bruto)
                        FROM (
                            SELECT COALESCE(SUM(valor_cobrado), 0) AS bruto
                            FROM orcamento.itens
                            WHERE id_orcamento = :id AND status_execucao <> 'cancelado'
                        ) t
                        WHERE o.id_orcamento = :id
                        """)
                .param("id", idOrcamento)
                .update();
    }

    public void definirDesconto(long idOrcamento, BigDecimal desconto) {
        jdbc.sql("""
                        UPDATE orcamento.orcamentos
                        SET valor_desconto = LEAST(:desconto, valor_bruto),
                            valor_final = valor_bruto - LEAST(:desconto, valor_bruto)
                        WHERE id_orcamento = :id
                        """)
                .param("id", idOrcamento)
                .param("desconto", desconto)
                .update();
    }

    /**
     * Transição condicionada ao status atual, no próprio UPDATE.
     *
     * <p>Ler o status, decidir em Java e gravar deixaria a janela clássica: dois
     * pedidos de aprovação simultâneos passariam os dois pela checagem e o
     * financeiro abriria dois recebíveis para o mesmo orçamento. Com a condição
     * no WHERE, o segundo afeta zero linhas e quem chamou sabe disso pelo
     * retorno.
     */
    public int transicionar(long idOrcamento, String de, String para, boolean marcarAprovacao) {
        return jdbc.sql("""
                        UPDATE orcamento.orcamentos
                        SET status = :para,
                            aprovado_em = CASE WHEN :marcar THEN NOW() ELSE aprovado_em END
                        WHERE id_orcamento = :id AND status = :de
                        """)
                .param("id", idOrcamento)
                .param("de", de)
                .param("para", para)
                .param("marcar", marcarAprovacao)
                .update();
    }

    public int registrarRecusa(long idOrcamento, String motivo) {
        return jdbc.sql("""
                        UPDATE orcamento.orcamentos
                        SET status = 'recusado',
                            observacoes = COALESCE(observacoes || E'\\n', '') || :motivo
                        WHERE id_orcamento = :id AND status IN ('enviado', 'rascunho')
                        """)
                .param("id", idOrcamento)
                .param("motivo", "Recusado: " + motivo)
                .update();
    }

    private static OrcamentoResumo mapear(java.sql.ResultSet rs, int linha)
            throws java.sql.SQLException {
        return new OrcamentoResumo(
                rs.getLong("id_orcamento"),
                rs.getLong("id_paciente"),
                rs.getLong("id_dentista"),
                rs.getString("status"),
                rs.getBigDecimal("valor_bruto"),
                rs.getBigDecimal("valor_desconto"),
                rs.getBigDecimal("valor_final"),
                rs.getDate("validade_em") == null ? null : rs.getDate("validade_em").toLocalDate(),
                rs.getTimestamp("aprovado_em") == null
                        ? null : rs.getTimestamp("aprovado_em").toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }
}
