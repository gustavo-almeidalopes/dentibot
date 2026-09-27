package br.com.dentibot.plataforma.tenant;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Recusa acesso a repositório fora de transação.
 *
 * <p>O contexto de tenant é aplicado por {@link GerenciadorTransacaoComTenant} no
 * início da transação. Uma consulta em autocommit não tem contexto — e o RLS,
 * corretamente, devolve zero linhas. O problema é que zero linhas é um resultado
 * plausível: a tela abre vazia, exceção nenhuma aparece, e o próximo passo de
 * alguém com pressa é suspeitar do RLS e desligá-lo.
 *
 * <p>Trocar o silêncio por uma exceção com o conserto escrito nela custa este
 * arquivo, e fecha a porta de saída errada.
 *
 * <p>Nota sobre uma tentativa anterior, que ficou no histórico: sobrescrever
 * {@code JdbcTemplate.execute(PreparedStatementCreator, PreparedStatementCallback)}
 * NÃO funciona. O {@code query()} do Spring chama uma sobrecarga interna de três
 * argumentos e passa ao largo da pública. O teste que deveria falhar passou, e
 * só apareceu porque havia um teste cobrando o guard — motivo para o guard ter
 * teste próprio.
 */
@Aspect
@Component
public class GuardaDeTransacao {

    @Before("within(@org.springframework.stereotype.Repository *)")
    public void exigirTransacao() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new AcessoForaDeTransacaoException();
        }
    }

    /** Erro de programação, não condição de runtime: falta um {@code @Transactional}. */
    public static class AcessoForaDeTransacaoException extends IllegalStateException {
        public AcessoForaDeTransacaoException() {
            super("""
                  Acesso a repositório fora de transação. O contexto de tenant (app.clinica) \
                  é aplicado no início da transação; sem ela o RLS não encontra tenant e a \
                  consulta devolveria zero linhas em silêncio. Anote o método de serviço com \
                  @Transactional — inclusive para leitura: @Transactional(readOnly = true).""");
        }
    }
}
