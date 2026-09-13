package br.com.dentibot.financeiro.infrastructure;

import br.com.dentibot.financeiro.ComissaoResumo;
import br.com.dentibot.financeiro.DespesaResumo;
import br.com.dentibot.financeiro.FormaPagamento;
import br.com.dentibot.financeiro.LancamentoResumo;
import br.com.dentibot.financeiro.RecebivelResumo;
import br.com.dentibot.financeiro.RegraComissao;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class FinanceiroRepositorio {

    private final JdbcClient jdbc;

    public FinanceiroRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ─── Recebíveis ──────────────────────────────────────────────────────────

    /**
     * O JOIN é com a própria view do módulo ({@code vw_saldo_recebivel}), não
     * com outro schema. Saldo não é coluna: é SUM sobre o ledger, e a view é a
     * definição única desse cálculo.
     */
    private static final String SQL_RECEBIVEL = """
            SELECT cr.id_recebivel, cr.id_orcamento, cr.id_paciente,
                   cr.parcela_numero, cr.parcela_total, cr.valor_parcela,
                   v.valor_liquidado, v.saldo_devedor,
                   cr.vencimento_em, cr.status
            FROM financeiro.contas_receber cr
            JOIN financeiro.vw_saldo_recebivel v ON v.id_recebivel = cr.id_recebivel
            """;

    public List<RecebivelResumo> listarRecebiveis(LocalDate de, LocalDate ate, String status,
                                                  Long idPaciente, long apos, int limite) {
        return jdbc.sql(SQL_RECEBIVEL + """
                        WHERE (CAST(:de AS DATE) IS NULL OR cr.vencimento_em >= :de)
                          AND (CAST(:ate AS DATE) IS NULL OR cr.vencimento_em <= :ate)
                          AND (CAST(:status AS TEXT) IS NULL OR cr.status = :status)
                          AND (CAST(:paciente AS BIGINT) IS NULL OR cr.id_paciente = :paciente)
                          AND cr.id_recebivel > :apos
                        ORDER BY cr.id_recebivel
                        LIMIT :limite
                        """)
                .param("de", de)
                .param("ate", ate)
                .param("status", status)
                .param("paciente", idPaciente)
                .param("apos", apos)
                .param("limite", limite)
                .query(FinanceiroRepositorio::mapearRecebivel)
                .list();
    }

    public Optional<RecebivelResumo> buscarRecebivel(long idRecebivel) {
        return jdbc.sql(SQL_RECEBIVEL + " WHERE cr.id_recebivel = :id")
                .param("id", idRecebivel)
                .query(FinanceiroRepositorio::mapearRecebivel)
                .optional();
    }

    public long inserirRecebivel(Long idOrcamento, Long idPaciente, Long idConvenio,
                                 int parcelaNumero, int parcelaTotal, BigDecimal valor,
                                 LocalDate vencimento) {
        return jdbc.sql("""
                        INSERT INTO financeiro.contas_receber
                            (id_clinica, id_orcamento, id_paciente, id_convenio,
                             parcela_numero, parcela_total, valor_parcela, vencimento_em)
                        VALUES (:clinica, :orcamento, :paciente, :convenio,
                                :numero, :total, :valor, :vencimento)
                        RETURNING id_recebivel
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("orcamento", idOrcamento)
                .param("paciente", idPaciente)
                .param("convenio", idConvenio)
                .param("numero", parcelaNumero)
                .param("total", parcelaTotal)
                .param("valor", valor)
                .param("vencimento", vencimento)
                .query(Long.class)
                .single();
    }

    public boolean existeRecebivelDoOrcamento(long idOrcamento) {
        return jdbc.sql("""
                        SELECT 1 FROM financeiro.contas_receber
                        WHERE id_orcamento = :id LIMIT 1
                        """)
                .param("id", idOrcamento)
                .query(Integer.class)
                .optional().isPresent();
    }

    /**
     * Sincroniza o status do recebível com o saldo — derivado, nunca digitado.
     *
     * <p>A ordem dos CASE importa: "paga" ganha de "vencida". Uma parcela
     * quitada depois do vencimento está paga, e mostrá-la como vencida colocaria
     * o paciente numa régua de cobrança que ele já não deve.
     */
    public void sincronizarStatus(long idRecebivel) {
        jdbc.sql("""
                        UPDATE financeiro.contas_receber cr
                        SET status = CASE
                                WHEN cr.status = 'cancelada'      THEN 'cancelada'
                                WHEN v.saldo_devedor <= 0         THEN 'paga'
                                WHEN v.valor_liquidado > 0        THEN 'parcial'
                                WHEN cr.vencimento_em < CURRENT_DATE THEN 'vencida'
                                ELSE 'aberta'
                            END
                        FROM financeiro.vw_saldo_recebivel v
                        WHERE v.id_recebivel = cr.id_recebivel
                          AND cr.id_recebivel = :id
                        """)
                .param("id", idRecebivel)
                .update();
    }

    /** Marca como vencido o que passou da data e ainda tem saldo. Roda em lote. */
    public int marcarVencidos() {
        return jdbc.sql("""
                        UPDATE financeiro.contas_receber cr
                        SET status = 'vencida'
                        FROM financeiro.vw_saldo_recebivel v
                        WHERE v.id_recebivel = cr.id_recebivel
                          AND cr.status IN ('aberta', 'parcial')
                          AND cr.vencimento_em < CURRENT_DATE
                          AND v.saldo_devedor > 0
                        """)
                .update();
    }

    // ─── Ledger ──────────────────────────────────────────────────────────────

    public List<LancamentoResumo> extrato(long idRecebivel) {
        return jdbc.sql("""
                        SELECT id_lancamento, id_recebivel, tipo, valor, id_forma,
                               descricao, estorna_lancamento, registrado_por, ocorrido_em
                        FROM financeiro.lancamentos
                        WHERE id_recebivel = :id
                        ORDER BY ocorrido_em, id_lancamento
                        """)
                .param("id", idRecebivel)
                .query(FinanceiroRepositorio::mapearLancamento)
                .list();
    }

    public Optional<LancamentoResumo> buscarLancamento(long idLancamento) {
        return jdbc.sql("""
                        SELECT id_lancamento, id_recebivel, tipo, valor, id_forma,
                               descricao, estorna_lancamento, registrado_por, ocorrido_em
                        FROM financeiro.lancamentos
                        WHERE id_lancamento = :id
                        """)
                .param("id", idLancamento)
                .query(FinanceiroRepositorio::mapearLancamento)
                .optional();
    }

    /** Já existe estorno para este lançamento? Estornar duas vezes credita em dobro. */
    public boolean jaEstornado(long idLancamento) {
        return jdbc.sql("""
                        SELECT 1 FROM financeiro.lancamentos
                        WHERE estorna_lancamento = :id LIMIT 1
                        """)
                .param("id", idLancamento)
                .query(Integer.class)
                .optional().isPresent();
    }

    public long inserirLancamento(Long idRecebivel, String tipo, BigDecimal valorComSinal,
                                  Long idForma, String referenciaExterna, String descricao,
                                  Long estornaLancamento, Long registradoPor, UUID correlacao) {
        return jdbc.sql("""
                        INSERT INTO financeiro.lancamentos
                            (id_clinica, id_recebivel, tipo, valor, id_forma,
                             referencia_externa, descricao, estorna_lancamento,
                             registrado_por, correlacao_id)
                        VALUES (:clinica, :recebivel, :tipo, :valor, :forma,
                                :referencia, :descricao, :estorna,
                                :registrador, CAST(:correlacao AS UUID))
                        RETURNING id_lancamento
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("recebivel", idRecebivel)
                .param("tipo", tipo)
                .param("valor", valorComSinal)
                .param("forma", idForma)
                .param("referencia", referenciaExterna)
                .param("descricao", descricao)
                .param("estorna", estornaLancamento)
                .param("registrador", registradoPor)
                .param("correlacao", correlacao == null ? null : correlacao.toString())
                .query(Long.class)
                .single();
    }

    // ─── Contas a pagar ──────────────────────────────────────────────────────

    public List<DespesaResumo> listarDespesas(LocalDate de, LocalDate ate, String status) {
        return jdbc.sql("""
                        SELECT id_despesa, descricao, categoria, valor_documento,
                               vencimento_em, pago_em, status
                        FROM financeiro.contas_pagar
                        WHERE (CAST(:de AS DATE) IS NULL OR vencimento_em >= :de)
                          AND (CAST(:ate AS DATE) IS NULL OR vencimento_em <= :ate)
                          AND (CAST(:status AS TEXT) IS NULL OR status = :status)
                        ORDER BY vencimento_em
                        LIMIT 500
                        """)
                .param("de", de)
                .param("ate", ate)
                .param("status", status)
                .query((rs, n) -> new DespesaResumo(
                        rs.getLong("id_despesa"),
                        rs.getString("descricao"),
                        rs.getString("categoria"),
                        rs.getBigDecimal("valor_documento"),
                        rs.getDate("vencimento_em").toLocalDate(),
                        rs.getDate("pago_em") == null ? null : rs.getDate("pago_em").toLocalDate(),
                        rs.getString("status")))
                .list();
    }

    public long inserirDespesa(String descricao, String categoria, BigDecimal valor,
                               LocalDate vencimento, String observacoes) {
        return jdbc.sql("""
                        INSERT INTO financeiro.contas_pagar
                            (id_clinica, descricao, categoria, valor_documento,
                             vencimento_em, observacoes)
                        VALUES (:clinica, :descricao, :categoria, :valor, :vencimento, :obs)
                        RETURNING id_despesa
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("descricao", descricao)
                .param("categoria", categoria)
                .param("valor", valor)
                .param("vencimento", vencimento)
                .param("obs", observacoes)
                .query(Long.class)
                .single();
    }

    /**
     * A condição {@code status <> 'paga'} no WHERE, e não em Java: dois cliques
     * simultâneos em "pagar" não podem gravar duas datas de pagamento. O segundo
     * afeta zero linhas e quem chamou sabe pelo retorno.
     */
    public int pagarDespesa(long idDespesa, LocalDate pagoEm) {
        return jdbc.sql("""
                        UPDATE financeiro.contas_pagar
                        SET status = 'paga', pago_em = :data
                        WHERE id_despesa = :id AND status <> 'paga'
                        """)
                .param("id", idDespesa)
                .param("data", pagoEm)
                .update();
    }

    // ─── Formas de pagamento ─────────────────────────────────────────────────

    public List<FormaPagamento> listarFormas() {
        return jdbc.sql("""
                        SELECT id_forma, nome, tipo, taxa_percentual, dias_liquidacao, ativo
                        FROM financeiro.formas_pagamento
                        ORDER BY nome
                        """)
                .query((rs, n) -> new FormaPagamento(
                        rs.getLong("id_forma"),
                        rs.getString("nome"),
                        rs.getString("tipo"),
                        rs.getBigDecimal("taxa_percentual"),
                        rs.getInt("dias_liquidacao"),
                        rs.getBoolean("ativo")))
                .list();
    }

    public long inserirForma(String nome, String tipo, BigDecimal taxa, Integer diasLiquidacao) {
        return jdbc.sql("""
                        INSERT INTO financeiro.formas_pagamento
                            (id_clinica, nome, tipo, taxa_percentual, dias_liquidacao)
                        VALUES (:clinica, :nome, :tipo, :taxa, :dias)
                        RETURNING id_forma
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("nome", nome)
                .param("tipo", tipo)
                .param("taxa", taxa == null ? BigDecimal.ZERO : taxa)
                .param("dias", diasLiquidacao == null ? 0 : diasLiquidacao)
                .query(Long.class)
                .single();
    }

    // ─── Comissões ───────────────────────────────────────────────────────────

    public List<RegraComissao> listarRegras() {
        return jdbc.sql("""
                        SELECT id_regra, id_dentista, id_procedimento, base_calculo,
                               percentual, ativo
                        FROM financeiro.regras_comissao
                        ORDER BY id_dentista, id_procedimento NULLS FIRST
                        """)
                .query((rs, n) -> {
                    long bruto = rs.getLong("id_procedimento");
                    Long procedimento = rs.wasNull() ? null : bruto;
                    return new RegraComissao(
                            rs.getLong("id_regra"),
                            rs.getLong("id_dentista"),
                            procedimento,
                            rs.getString("base_calculo"),
                            rs.getBigDecimal("percentual"),
                            rs.getBoolean("ativo"));
                })
                .list();
    }

    public long inserirRegra(long idDentista, Long idProcedimento, String baseCalculo,
                             BigDecimal percentual) {
        return jdbc.sql("""
                        INSERT INTO financeiro.regras_comissao
                            (id_clinica, id_dentista, id_procedimento, base_calculo, percentual)
                        VALUES (:clinica, :dentista, :procedimento, :base, :percentual)
                        RETURNING id_regra
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("dentista", idDentista)
                .param("procedimento", idProcedimento)
                .param("base", baseCalculo == null ? "liquido" : baseCalculo)
                .param("percentual", percentual)
                .query(Long.class)
                .single();
    }

    public List<ComissaoResumo> listarComissoes(Long idDentista, String status) {
        return jdbc.sql("""
                        SELECT id_comissao, id_dentista, id_item, id_recebivel, valor_base,
                               percentual_aplicado, valor_comissao, status,
                               previsao_liberacao_em, pago_em
                        FROM financeiro.comissoes
                        WHERE (CAST(:dentista AS BIGINT) IS NULL OR id_dentista = :dentista)
                          AND (CAST(:status AS TEXT) IS NULL OR status = :status)
                        ORDER BY id_comissao DESC
                        LIMIT 500
                        """)
                .param("dentista", idDentista)
                .param("status", status)
                .query((rs, n) -> {
                    long bruto = rs.getLong("id_recebivel");
                    Long recebivel = rs.wasNull() ? null : bruto;
                    return new ComissaoResumo(
                            rs.getLong("id_comissao"),
                            rs.getLong("id_dentista"),
                            rs.getLong("id_item"),
                            recebivel,
                            rs.getBigDecimal("valor_base"),
                            rs.getBigDecimal("percentual_aplicado"),
                            rs.getBigDecimal("valor_comissao"),
                            rs.getString("status"),
                            rs.getDate("previsao_liberacao_em") == null
                                    ? null : rs.getDate("previsao_liberacao_em").toLocalDate(),
                            rs.getTimestamp("pago_em") == null
                                    ? null : rs.getTimestamp("pago_em").toInstant());
                })
                .list();
    }

    /**
     * A regra que se aplica a um item: a específica do procedimento vence a
     * genérica do dentista. {@code NULLS LAST} no ORDER BY é o que faz isso —
     * sem ele, a regra "vale para todos" ganharia da negociada para aquele
     * procedimento, e o dentista receberia o percentual errado em silêncio.
     */
    public Optional<RegraComissao> regraAplicavel(long idDentista, long idProcedimento) {
        return jdbc.sql("""
                        SELECT id_regra, id_dentista, id_procedimento, base_calculo,
                               percentual, ativo
                        FROM financeiro.regras_comissao
                        WHERE ativo
                          AND id_dentista = :dentista
                          AND (id_procedimento = :procedimento OR id_procedimento IS NULL)
                        ORDER BY id_procedimento NULLS LAST
                        LIMIT 1
                        """)
                .param("dentista", idDentista)
                .param("procedimento", idProcedimento)
                .query((rs, n) -> {
                    long bruto = rs.getLong("id_procedimento");
                    Long procedimento = rs.wasNull() ? null : bruto;
                    return new RegraComissao(
                            rs.getLong("id_regra"),
                            rs.getLong("id_dentista"),
                            procedimento,
                            rs.getString("base_calculo"),
                            rs.getBigDecimal("percentual"),
                            rs.getBoolean("ativo"));
                })
                .optional();
    }

    public boolean existeComissaoDoItem(long idItem) {
        return jdbc.sql("SELECT 1 FROM financeiro.comissoes WHERE id_item = :id LIMIT 1")
                .param("id", idItem)
                .query(Integer.class)
                .optional().isPresent();
    }

    public long inserirComissao(long idDentista, long idItem, Long idRecebivel,
                                BigDecimal valorBase, BigDecimal percentual,
                                BigDecimal valorComissao, LocalDate previsao) {
        return jdbc.sql("""
                        INSERT INTO financeiro.comissoes
                            (id_clinica, id_dentista, id_item, id_recebivel, valor_base,
                             percentual_aplicado, valor_comissao, previsao_liberacao_em)
                        VALUES (:clinica, :dentista, :item, :recebivel, :base,
                                :percentual, :valor, :previsao)
                        RETURNING id_comissao
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("dentista", idDentista)
                .param("item", idItem)
                .param("recebivel", idRecebivel)
                .param("base", valorBase)
                .param("percentual", percentual)
                .param("valor", valorComissao)
                .param("previsao", previsao)
                .query(Long.class)
                .single();
    }

    public int liberarComissao(long idComissao) {
        return jdbc.sql("""
                        UPDATE financeiro.comissoes
                        SET status = 'liberada'
                        WHERE id_comissao = :id AND status = 'prevista'
                        """)
                .param("id", idComissao)
                .update();
    }

    // ─── Painel ──────────────────────────────────────────────────────────────

    /**
     * O que ENTROU no período, pelo ledger.
     *
     * <p>{@code tipo <> 'cobranca'} porque 'cobranca' é o registro da emissão,
     * não de dinheiro recebido — somá-la contaria o faturamento duas vezes.
     */
    public BigDecimal recebidoEntre(LocalDate de, LocalDate ate) {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(valor), 0)
                        FROM financeiro.lancamentos
                        WHERE tipo <> 'cobranca'
                          AND ocorrido_em >= :de
                          AND ocorrido_em < (CAST(:ate AS DATE) + 1)
                        """)
                .param("de", de)
                .param("ate", ate)
                .query(BigDecimal.class)
                .single();
    }

    public BigDecimal saldoAReceber(boolean somenteVencido) {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(v.saldo_devedor), 0)
                        FROM financeiro.contas_receber cr
                        JOIN financeiro.vw_saldo_recebivel v ON v.id_recebivel = cr.id_recebivel
                        WHERE cr.status NOT IN ('cancelada', 'paga')
                          AND v.saldo_devedor > 0
                          AND (:somenteVencido = FALSE OR cr.vencimento_em < CURRENT_DATE)
                        """)
                .param("somenteVencido", somenteVencido)
                .query(BigDecimal.class)
                .single();
    }

    public BigDecimal totalAPagar() {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(valor_documento), 0)
                        FROM financeiro.contas_pagar
                        WHERE status IN ('aberta', 'vencida')
                        """)
                .query(BigDecimal.class)
                .single();
    }

    public BigDecimal comissoesPrevistas() {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(valor_comissao), 0)
                        FROM financeiro.comissoes
                        WHERE status IN ('prevista', 'liberada')
                        """)
                .query(BigDecimal.class)
                .single();
    }

    // ─── mapeadores ──────────────────────────────────────────────────────────

    private static RecebivelResumo mapearRecebivel(ResultSet rs, int linha) throws SQLException {
        long orcamentoBruto = rs.getLong("id_orcamento");
        Long orcamento = rs.wasNull() ? null : orcamentoBruto;
        long pacienteBruto = rs.getLong("id_paciente");
        Long paciente = rs.wasNull() ? null : pacienteBruto;
        return new RecebivelResumo(
                rs.getLong("id_recebivel"),
                orcamento,
                paciente,
                rs.getInt("parcela_numero"),
                rs.getInt("parcela_total"),
                rs.getBigDecimal("valor_parcela"),
                rs.getBigDecimal("valor_liquidado"),
                rs.getBigDecimal("saldo_devedor"),
                rs.getDate("vencimento_em").toLocalDate(),
                rs.getString("status"));
    }

    private static LancamentoResumo mapearLancamento(ResultSet rs, int linha) throws SQLException {
        long recebivelBruto = rs.getLong("id_recebivel");
        Long recebivel = rs.wasNull() ? null : recebivelBruto;
        long formaBruta = rs.getLong("id_forma");
        Long forma = rs.wasNull() ? null : formaBruta;
        long estornaBruto = rs.getLong("estorna_lancamento");
        Long estorna = rs.wasNull() ? null : estornaBruto;
        long registradorBruto = rs.getLong("registrado_por");
        Long registrador = rs.wasNull() ? null : registradorBruto;
        return new LancamentoResumo(
                rs.getLong("id_lancamento"),
                recebivel,
                rs.getString("tipo"),
                rs.getBigDecimal("valor"),
                forma,
                rs.getString("descricao"),
                estorna,
                registrador,
                rs.getTimestamp("ocorrido_em").toInstant());
    }
}
