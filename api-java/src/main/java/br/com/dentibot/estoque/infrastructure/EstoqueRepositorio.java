package br.com.dentibot.estoque.infrastructure;

import br.com.dentibot.estoque.FornecedorResumo;
import br.com.dentibot.estoque.LoteVencendo;
import br.com.dentibot.estoque.MovimentacaoResumo;
import br.com.dentibot.estoque.PosicaoProduto;
import br.com.dentibot.estoque.ProdutoResumo;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EstoqueRepositorio {

    private final JdbcClient jdbc;

    public EstoqueRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ─── Posição ─────────────────────────────────────────────────────────────

    /**
     * Lê a view, não as tabelas: {@code vw_posicao} já é o SUM das movimentações
     * por produto, e repetir esse GROUP BY aqui criaria uma segunda definição de
     * "quantidade atual" — que diverge da primeira no dia em que alguém mudar
     * uma das duas.
     */
    public List<PosicaoProduto> posicao(boolean somenteAbaixo) {
        return jdbc.sql("""
                        SELECT id_produto, nome_produto, unidade_medida, ponto_pedido,
                               quantidade_atual, abaixo_do_ponto_pedido
                        FROM estoque.vw_posicao
                        WHERE (:somenteAbaixo = FALSE OR abaixo_do_ponto_pedido)
                        ORDER BY abaixo_do_ponto_pedido DESC, nome_produto
                        """)
                .param("somenteAbaixo", somenteAbaixo)
                .query((rs, n) -> new PosicaoProduto(
                        rs.getLong("id_produto"),
                        rs.getString("nome_produto"),
                        rs.getString("unidade_medida"),
                        rs.getBigDecimal("ponto_pedido"),
                        rs.getBigDecimal("quantidade_atual"),
                        rs.getBoolean("abaixo_do_ponto_pedido")))
                .list();
    }

    // ─── Produtos ────────────────────────────────────────────────────────────

    public List<ProdutoResumo> listarProdutos() {
        return jdbc.sql("""
                        SELECT id_produto, nome_produto, unidade_medida, ponto_pedido,
                               controla_lote, id_fornecedor_padrao, ativo
                        FROM estoque.produtos
                        ORDER BY nome_produto
                        """)
                .query((rs, n) -> {
                    long bruto = rs.getLong("id_fornecedor_padrao");
                    Long fornecedor = rs.wasNull() ? null : bruto;
                    return new ProdutoResumo(
                            rs.getLong("id_produto"),
                            rs.getString("nome_produto"),
                            rs.getString("unidade_medida"),
                            rs.getBigDecimal("ponto_pedido"),
                            rs.getBoolean("controla_lote"),
                            fornecedor,
                            rs.getBoolean("ativo"));
                })
                .list();
    }

    public long inserirProduto(String nome, String unidade, BigDecimal pontoPedido,
                               boolean controlaLote, Long idFornecedorPadrao) {
        return jdbc.sql("""
                        INSERT INTO estoque.produtos
                            (id_clinica, nome_produto, unidade_medida, ponto_pedido,
                             controla_lote, id_fornecedor_padrao)
                        VALUES (:clinica, :nome, :unidade, :ponto, :lote, :fornecedor)
                        RETURNING id_produto
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("nome", nome)
                .param("unidade", unidade)
                .param("ponto", pontoPedido == null ? BigDecimal.ZERO : pontoPedido)
                .param("lote", controlaLote)
                .param("fornecedor", idFornecedorPadrao)
                .query(Long.class)
                .single();
    }

    public boolean produtoControlaLote(long idProduto) {
        return Boolean.TRUE.equals(jdbc.sql("""
                        SELECT controla_lote FROM estoque.produtos WHERE id_produto = :id
                        """)
                .param("id", idProduto)
                .query(Boolean.class)
                .optional().orElse(null));
    }

    // ─── Fornecedores ────────────────────────────────────────────────────────

    public List<FornecedorResumo> listarFornecedores() {
        return jdbc.sql("""
                        SELECT id_fornecedor, razao_social, cnpj, telefone,
                               email_vendedor, ativo
                        FROM estoque.fornecedores
                        ORDER BY razao_social
                        """)
                .query((rs, n) -> new FornecedorResumo(
                        rs.getLong("id_fornecedor"),
                        rs.getString("razao_social"),
                        rs.getString("cnpj"),
                        rs.getString("telefone"),
                        rs.getString("email_vendedor"),
                        rs.getBoolean("ativo")))
                .list();
    }

    public long inserirFornecedor(String razaoSocial, String cnpj, String telefone,
                                  String emailVendedor) {
        return jdbc.sql("""
                        INSERT INTO estoque.fornecedores
                            (id_clinica, razao_social, cnpj, telefone, email_vendedor)
                        VALUES (:clinica, :razao, :cnpj, :telefone, :email)
                        RETURNING id_fornecedor
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("razao", razaoSocial)
                .param("cnpj", cnpj)
                .param("telefone", telefone)
                .param("email", emailVendedor)
                .query(Long.class)
                .single();
    }

    // ─── Movimentações ───────────────────────────────────────────────────────

    public List<MovimentacaoResumo> extrato(long idProduto, int limite) {
        return jdbc.sql("""
                        SELECT id_movimentacao, id_produto, tipo, quantidade, lote, validade,
                               custo_unitario, observacao, id_usuario, movimentado_em
                        FROM estoque.movimentacoes
                        WHERE id_produto = :produto
                        ORDER BY movimentado_em DESC
                        LIMIT :limite
                        """)
                .param("produto", idProduto)
                .param("limite", limite)
                .query((rs, n) -> {
                    long bruto = rs.getLong("id_usuario");
                    Long usuario = rs.wasNull() ? null : bruto;
                    LocalDate validade = rs.getDate("validade") == null
                            ? null : rs.getDate("validade").toLocalDate();
                    return new MovimentacaoResumo(
                            rs.getLong("id_movimentacao"),
                            rs.getLong("id_produto"),
                            rs.getString("tipo"),
                            rs.getBigDecimal("quantidade"),
                            rs.getString("lote"),
                            validade,
                            rs.getBigDecimal("custo_unitario"),
                            rs.getString("observacao"),
                            usuario,
                            rs.getTimestamp("movimentado_em").toInstant());
                })
                .list();
    }

    /**
     * Saldo por LOTE, e só o que ainda tem saldo positivo.
     *
     * <p>O {@code HAVING SUM(...) > 0} é a parte que importa: sem ele, um lote
     * já inteiramente consumido continuaria aparecendo no aviso de vencimento
     * para sempre, e um alerta que não some é um alerta que se aprende a
     * ignorar.
     */
    public List<LoteVencendo> lotesVencendo(int emDias) {
        return jdbc.sql("""
                        SELECT m.id_produto, p.nome_produto, m.lote, m.validade,
                               SUM(m.quantidade) AS saldo
                        FROM estoque.movimentacoes m
                        JOIN estoque.produtos p ON p.id_produto = m.id_produto
                        WHERE m.validade IS NOT NULL
                          AND m.validade <= CURRENT_DATE + CAST(:dias AS INT)
                        GROUP BY m.id_produto, p.nome_produto, m.lote, m.validade
                        HAVING SUM(m.quantidade) > 0
                        ORDER BY m.validade
                        """)
                .param("dias", emDias)
                .query((rs, n) -> new LoteVencendo(
                        rs.getLong("id_produto"),
                        rs.getString("nome_produto"),
                        rs.getString("lote"),
                        rs.getDate("validade").toLocalDate(),
                        rs.getBigDecimal("saldo")))
                .list();
    }

    public BigDecimal saldoDoLote(long idProduto, String lote) {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(quantidade), 0)
                        FROM estoque.movimentacoes
                        WHERE id_produto = :produto
                          AND lote IS NOT DISTINCT FROM CAST(:lote AS VARCHAR)
                        """)
                .param("produto", idProduto)
                .param("lote", lote)
                .query(BigDecimal.class)
                .single();
    }

    public long inserirMovimentacao(long idProduto, String tipo, BigDecimal quantidadeComSinal,
                                    String lote, LocalDate validade, BigDecimal custoUnitario,
                                    String observacao, Long idUsuario) {
        return jdbc.sql("""
                        INSERT INTO estoque.movimentacoes
                            (id_clinica, id_produto, id_usuario, tipo, quantidade,
                             lote, validade, custo_unitario, observacao)
                        VALUES (:clinica, :produto, :usuario, :tipo, :quantidade,
                                :lote, :validade, :custo, :observacao)
                        RETURNING id_movimentacao
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("produto", idProduto)
                .param("usuario", idUsuario)
                .param("tipo", tipo)
                .param("quantidade", quantidadeComSinal)
                .param("lote", lote)
                .param("validade", validade)
                .param("custo", custoUnitario)
                .param("observacao", observacao)
                .query(Long.class)
                .single();
    }
}
