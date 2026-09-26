package br.com.dentibot.plataforma.tenant;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.jdbc.datasource.ConnectionHolder;

/**
 * Injeta o contexto da requisição no Postgres, no início de toda transação.
 *
 * <p>Esta classe é a razão de o RLS funcionar. Se ela não rodar, nenhuma
 * política encontra tenant, e toda consulta devolve zero linhas — o sistema
 * para, em vez de vazar. Fail closed é o modo de falha correto aqui.
 *
 * <p>Três armadilhas, todas endereçadas:
 *
 * <ol>
 *   <li><b>{@code SET LOCAL} não aceita bind.</b> É comando utilitário, e a
 *       saída tentadora — concatenar o id na string — troca um problema de
 *       tenancy por um de injeção de SQL. {@code set_config()} é forma de
 *       função e aceita parâmetro.</li>
 *   <li><b>O terceiro argumento {@code true} é {@code is_local}.</b> Sem ele o
 *       valor sobrevive ao fim da transação; com pool de conexões, a próxima
 *       requisição herda o tenant da anterior, em silêncio. É o pior bug
 *       possível neste sistema, e é uma letra de diferença.</li>
 *   <li><b>Precisa ser DEPOIS do begin.</b> Fora de transação, um
 *       {@code set_config} local vale só para o statement e some. Por isso o
 *       gancho é {@code doBegin}, após {@code super}.</li>
 * </ol>
 */
public class GerenciadorTransacaoComTenant extends JdbcTransactionManager {

    private static final String SQL = """
            SELECT set_config('app.clinica',    ?, true),
                   set_config('app.usuario',    ?, true),
                   set_config('app.staff',      ?, true),
                   set_config('app.correlacao', ?, true),
                   set_config('app.worker',     ?, true)
            """;

    public GerenciadorTransacaoComTenant(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);

        ConnectionHolder holder =
                (ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
        if (holder == null) {
            throw new CannotCreateTransactionException(
                    "Transação iniciada sem conexão ligada — não há onde aplicar o contexto de tenant.");
        }
        aplicarContexto(holder.getConnection(), ContextoAtual.obter());
    }

    private void aplicarContexto(Connection conexao, ContextoRequisicao ctx) {
        try (PreparedStatement ps = conexao.prepareStatement(SQL)) {
            // String vazia, não NULL: as funções de contexto usam
            // NULLIF(current_setting(...), '') e traduzem vazio para NULL, que
            // não casa com nada e portanto filtra tudo.
            ps.setString(1, texto(ctx.clinicaId()));
            ps.setString(2, texto(ctx.usuarioId()));
            ps.setString(3, ctx.staffPapel() == null ? "" : ctx.staffPapel().valorBanco());
            ps.setString(4, texto(ctx.correlacaoId()));
            ps.setString(5, ctx.modoWorker() ? "true" : "false");
            ps.execute();
        } catch (SQLException e) {
            throw new CannotCreateTransactionException(
                    "Falha ao aplicar o contexto de tenant na transação.", e);
        }
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }
}
