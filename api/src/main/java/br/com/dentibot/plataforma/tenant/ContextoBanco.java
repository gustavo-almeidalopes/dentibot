package br.com.dentibot.plataforma.tenant;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Promove o tenant da transação em curso.
 *
 * <p>Existe para exatamente dois fluxos, e nenhum outro deve chamá-la:
 *
 * <ul>
 *   <li><b>login</b> — o cliente mandou e-mail e senha, ainda não há tenant. A
 *       clínica é resolvida por função SECURITY DEFINER e o contexto é promovido
 *       antes de ler o usuário;</li>
 *   <li><b>onboarding</b> — a clínica acabou de nascer dentro desta transação, e
 *       as linhas seguintes (configurações, primeira pessoa, usuário admin)
 *       precisam do tenant que ainda não existia quando a transação começou.</li>
 * </ul>
 *
 * <p>É uma classe com nome próprio, e não um {@code set_config} solto no meio de
 * um serviço, justamente para que qualquer uso novo apareça numa busca por
 * referências e precise ser justificado.
 */
@Component
public class ContextoBanco {

    private final JdbcClient jdbc;

    public ContextoBanco(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void promoverClinica(long idClinica) {
        exigirTransacao();
        // is_local = true: some no fim da transação, como todo o resto do
        // contexto. Bind de parâmetro, nunca concatenação.
        jdbc.sql("SELECT set_config('app.clinica', :valor, true)")
                .param("valor", String.valueOf(idClinica))
                .query(String.class)
                .single();
    }

    public void promoverUsuario(long idUsuario) {
        exigirTransacao();
        jdbc.sql("SELECT set_config('app.usuario', :valor, true)")
                .param("valor", String.valueOf(idUsuario))
                .query(String.class)
                .single();
    }

    private static void exigirTransacao() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "promover contexto exige transação ativa: set_config com is_local=true "
                            + "fora de transação não sobrevive ao próximo statement.");
        }
    }
}
