package br.com.dentibot.plataforma.tenant;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Promove o tenant da transação em curso.
 *
 * <p>Existe para três fluxos, e nenhum outro deve chamá-la:
 *
 * <ul>
 *   <li><b>resolução de acesso</b> — o token do Clerk chegou e ainda não há
 *       tenant. A clínica é resolvida por função SECURITY DEFINER e o contexto
 *       é promovido antes de ler o usuário;</li>
 *   <li><b>onboarding</b> — a clínica acabou de nascer dentro desta transação, e
 *       as linhas seguintes (configurações, usuário admin, trial) precisam do
 *       tenant que ainda não existia quando a transação começou;</li>
 *   <li><b>consumidor de evento</b> — o worker do outbox roda sem tenant, e a
 *       clínica vem do envelope do evento, escrito pelo servidor.</li>
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

    /**
     * Roda {@code trabalho} dentro da clínica, como admin dela, e devolve o
     * contexto anterior ao terminar.
     *
     * <p>Duas promoções, porque há dois consumidores do tenant: o Postgres, para
     * o RLS aceitar as linhas, e o {@code ContextoAtual}, de onde os
     * repositórios leem o {@code id_clinica} que gravam. Promover só um deles
     * falha em silêncio — leitura vazia sob RLS, ou escrita que estoura em
     * {@code clinicaObrigatoria()}.
     *
     * <p>A restauração no {@code finally} é o que impede a thread do pool de
     * levar o tenant desta clínica para a próxima requisição: a versão em
     * memória do que o {@code is_local = true} já garante no banco.
     */
    public <T> T comoClinica(long idClinica, UUID correlacao, Supplier<T> trabalho) {
        promoverClinica(idClinica);
        ContextoRequisicao anterior = ContextoAtual.obter();
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, 0L, Papel.ADMIN, correlacao));
        try {
            return trabalho.get();
        } finally {
            ContextoAtual.definir(anterior);
        }
    }

    private static void exigirTransacao() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "promover contexto exige transação ativa: set_config com is_local=true "
                            + "fora de transação não sobrevive ao próximo statement.");
        }
    }
}
