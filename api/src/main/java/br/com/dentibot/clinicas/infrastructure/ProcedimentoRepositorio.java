package br.com.dentibot.clinicas.infrastructure;

import br.com.dentibot.clinicas.NovoProcedimento;
import br.com.dentibot.clinicas.ProcedimentoResumo;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProcedimentoRepositorio {

    /* O mesmo DEFAULT de clinicas.procedimentos.duracao_minutos (V8). Ele vale
       aqui também porque o UPDATE com null precisa voltar ao padrão, e um
       INSERT com null explícito não aciona o DEFAULT da coluna. */
    private static final int DURACAO_PADRAO_MIN = 30;

    private final JdbcClient jdbc;

    public ProcedimentoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final String COLUNAS = """
            SELECT id_procedimento, nome_servico, codigo_tuss,
                   preco_particular, duracao_minutos, ativo
            FROM clinicas.procedimentos
            """;

    public List<ProcedimentoResumo> listar(boolean somenteAtivos) {
        return jdbc.sql(COLUNAS + """
                        WHERE (:somenteAtivos = FALSE OR ativo)
                        ORDER BY nome_servico
                        """)
                .param("somenteAtivos", somenteAtivos)
                .query(ProcedimentoRepositorio::mapear)
                .list();
    }

    public Optional<ProcedimentoResumo> buscar(long idProcedimento) {
        return jdbc.sql(COLUNAS + " WHERE id_procedimento = :id")
                .param("id", idProcedimento)
                .query(ProcedimentoRepositorio::mapear)
                .optional();
    }

    public long inserir(NovoProcedimento novo) {
        return jdbc.sql("""
                        INSERT INTO clinicas.procedimentos
                            (id_clinica, nome_servico, codigo_tuss, preco_particular, duracao_minutos)
                        VALUES (:clinica, :nome, :tuss, :preco, :duracao)
                        RETURNING id_procedimento
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("nome", novo.nomeServico())
                .param("tuss", novo.codigoTuss())
                .param("preco", novo.precoParticular())
                .param("duracao", novo.duracaoMinutos() == null ? DURACAO_PADRAO_MIN : novo.duracaoMinutos())
                .query(Long.class)
                .single();
    }

    public int atualizar(long idProcedimento, NovoProcedimento dados, boolean ativo) {
        return jdbc.sql("""
                        UPDATE clinicas.procedimentos
                        SET nome_servico = :nome, codigo_tuss = :tuss,
                            preco_particular = :preco, duracao_minutos = :duracao, ativo = :ativo
                        WHERE id_procedimento = :id
                        """)
                .param("id", idProcedimento)
                .param("nome", dados.nomeServico())
                .param("tuss", dados.codigoTuss())
                .param("preco", dados.precoParticular())
                .param("duracao", dados.duracaoMinutos() == null ? DURACAO_PADRAO_MIN : dados.duracaoMinutos())
                .param("ativo", ativo)
                .update();
    }

    private static ProcedimentoResumo mapear(java.sql.ResultSet rs, int linha)
            throws java.sql.SQLException {
        return new ProcedimentoResumo(
                rs.getLong("id_procedimento"),
                rs.getString("nome_servico"),
                rs.getString("codigo_tuss"),
                rs.getBigDecimal("preco_particular"),
                rs.getInt("duracao_minutos"),
                rs.getBoolean("ativo"));
    }
}
