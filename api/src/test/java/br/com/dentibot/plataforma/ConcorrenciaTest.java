package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.financeiro.FinanceiroApi;
import br.com.dentibot.financeiro.NovoLancamento;
import br.com.dentibot.financeiro.NovoRecebivel;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.NovoMembro;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checagens do tipo "consulta, depois grava" sob duas requisições ao mesmo
 * tempo.
 *
 * <p>O banco roda em READ COMMITTED: a segunda requisição não enxerga o que a
 * primeira gravou e ainda não commitou. Qualquer guarda que só lê antes de
 * escrever passa nas duas. Os testes forçam exatamente essa janela — a
 * primeira operação segura o commit enquanto a segunda chega ao banco — em vez
 * de disparar threads e torcer para colidirem.
 */
@DisplayName("Concorrência nas guardas de negócio")
class ConcorrenciaTest extends TesteIntegracao {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);

    @Autowired
    private OnboardingServico onboarding;
    @Autowired
    private FinanceiroApi financeiro;
    @Autowired
    private IdentidadeApi identidade;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private TransactionTemplate transacao;

    private long idClinica;
    private long idAdmin;

    @BeforeEach
    void clinica() {
        long n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_concorrencia_" + n, UUID.randomUUID()));
        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Concorrencia LTDA", "Clinica Concorrencia",
                ZoneId.of("America/Sao_Paulo"), Plano.CLINICA,
                "Admin Concorrencia", "admin.concorrencia" + n + "@teste.local",
                "user_concorrencia_" + n));
        idClinica = criada.idClinica();
        idAdmin = criada.idUsuarioAdmin();
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idAdmin, Papel.ADMIN, UUID.randomUUID()));
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("dois estornos simultâneos do mesmo lançamento: só um entra")
    void estornoDuplo() throws Exception {
        long idRecebivel = financeiro.abrirRecebiveis(new NovoRecebivel(
                null, null, null, new BigDecimal("300.00"), 1, LocalDate.now().plusDays(30))).getFirst();
        long idLancamento = financeiro.registrarLancamento(new NovoLancamento(
                idRecebivel, "recebimento", new BigDecimal("300.00"), null, null, "balcão"));

        Throwable segunda = segundaEnquantoAPrimeiraSegura(
                () -> financeiro.estornar(idLancamento, "primeiro clique"),
                () -> financeiro.estornar(idLancamento, "segundo clique"));

        assertThat(contar("SELECT count(*) FROM financeiro.lancamentos WHERE estorna_lancamento = "
                + idLancamento))
                .as("estornar duas vezes credita o valor em dobro ao paciente")
                .isEqualTo(1);
        assertThat(segunda).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("dois admins se rebaixando ao mesmo tempo: a clínica fica com um")
    void ultimoAdmin() throws Exception {
        long n = SEQ.incrementAndGet();
        long outroAdmin = identidade.admitirMembro(new NovoMembro(
                "Outro Admin", "outro.admin" + n + "@teste.local", Papel.ADMIN, null, null, null));

        Throwable segunda = segundaEnquantoAPrimeiraSegura(
                () -> identidade.atualizarMembro(idAdmin, Papel.RECEPCIONISTA, "ativo"),
                () -> identidade.atualizarMembro(outroAdmin, Papel.RECEPCIONISTA, "ativo"));

        assertThat(contar("SELECT count(*) FROM identidade.usuarios WHERE papel = 'admin' AND status = 'ativo'"))
                .as("a guarda do último admin lê a contagem antes de escrever")
                .isEqualTo(1);
        assertThat(segunda)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem administrador");
    }

    /**
     * Roda {@code primeira} numa transação que só commita depois que
     * {@code segunda} já está no banco, em outra thread.
     *
     * <p>Sem proteção, a segunda termina dentro dessa janela — não enxerga a
     * escrita da primeira — e as duas commitam. Com índice único ou trava, ela
     * espera o commit da primeira e é recusada.
     *
     * @return o que a segunda lançou, ou {@code null} se passou
     */
    private Throwable segundaEnquantoAPrimeiraSegura(Runnable primeira, Runnable segunda)
            throws Exception {
        ContextoRequisicao ctx = ContextoAtual.obter();
        CountDownLatch primeiraEscreveu = new CountDownLatch(1);

        CompletableFuture<Throwable> outra = CompletableFuture.supplyAsync(() -> {
            ContextoAtual.definir(ctx);
            try {
                primeiraEscreveu.await();
                segunda.run();
                return null;
            } catch (Throwable t) {
                return t;
            } finally {
                ContextoAtual.limpar();
            }
        });

        transacao.executeWithoutResult(s -> {
            primeira.run();
            primeiraEscreveu.countDown();
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return outra.get(15, TimeUnit.SECONDS);
    }

    private Integer contar(String sql) {
        return transacao.execute(s -> jdbc.sql(sql).query(Integer.class).single());
    }
}
