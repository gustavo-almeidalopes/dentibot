package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.outbox.ConsumidorDeEvento;
import br.com.dentibot.plataforma.outbox.EventoDominio;
import br.com.dentibot.plataforma.outbox.Outbox;
import br.com.dentibot.plataforma.outbox.OutboxWorker;
import br.com.dentibot.plataforma.tenant.ContextoBanco;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Entrega do outbox quando um consumidor falha.
 *
 * <p>O lote inteiro rodava numa transação só. Um erro de SQL num consumidor
 * aborta a transação no Postgres; o {@code reagendar()} seguinte também falha,
 * o lote volta atrás, {@code tentativas} nunca sobe e o mesmo evento volta ao
 * topo da fila a cada ciclo — travando todos os que vêm atrás dele.
 */
@DisplayName("Outbox: entrega evento a evento")
class OutboxEntregaTest extends TesteIntegracao {

    private static final String QUEBRA = "teste.quebra-no-banco";
    private static final String PROMOVE = "teste.promove-clinica";
    private static final String ESPIA = "teste.espia-clinica";
    private static final String FALHA_COM_CPF = "teste.falha-com-cpf";

    /** O que o consumidor espião leu em app.clinica, por evento. */
    private static final Map<UUID, String> CLINICA_VISTA = new ConcurrentHashMap<>();


    @TestConfiguration
    static class Consumidores {

        @Bean
        ConsumidorDeEvento quebraNoBanco(JdbcClient jdbc) {
            return consumidor(QUEBRA, e -> jdbc.sql("SELECT 1 / 0").query(Integer.class).single());
        }

        /** A mensagem de uma violação de unicidade do Postgres traz o valor duplicado. */
        @Bean
        ConsumidorDeEvento falhaComCpf() {
            return consumidor(FALHA_COM_CPF, e -> {
                throw new IllegalStateException("Key (id_clinica, cpf)=(42, 12345678901) already exists");
            });
        }

        /** Faz o que o consumidor de orçamento aprovado faz: promove o tenant do evento. */
        @Bean
        ConsumidorDeEvento promove(ContextoBanco banco) {
            return consumidor(PROMOVE, e -> banco.promoverClinica(e.clinicaId()));
        }

        @Bean
        ConsumidorDeEvento espia(JdbcClient jdbc) {
            return consumidor(ESPIA, e -> CLINICA_VISTA.put(e.eventId(),
                    jdbc.sql("SELECT COALESCE(current_setting('app.clinica', true), '')")
                            .query(String.class).single()));
        }

        private static ConsumidorDeEvento consumidor(String tipo,
                                                     java.util.function.Consumer<EventoDominio> acao) {
            return new ConsumidorDeEvento() {
                @Override
                public String nome() {
                    return tipo;
                }

                @Override
                public boolean interessadoEm(String tipoDeEvento) {
                    return tipo.equals(tipoDeEvento);
                }

                @Override
                public void consumir(EventoDominio evento) {
                    acao.accept(evento);
                }
            };
        }
    }

    @Autowired
    private OnboardingServico onboarding;
    @Autowired
    private Outbox outbox;
    @Autowired
    private OutboxWorker worker;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private TransactionTemplate transacao;

    private long idClinica;

    @BeforeEach
    void clinicaEFilaLimpa() {
        long n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_outbox_" + n, UUID.randomUUID()));
        idClinica = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Entrega LTDA", "Clinica Entrega",
                ZoneId.of("America/Sao_Paulo"), Plano.SOLO,
                "Admin Entrega", "admin.entrega" + n + "@teste.local", "user_outbox_" + n)).idClinica();

        // Eventos que outras classes deixaram pendentes não podem empurrar os
        // deste teste para fora do lote de 50.
        comoWorker();
        transacao.executeWithoutResult(s -> jdbc.sql(
                "UPDATE plataforma.outbox SET publicado_em = NOW() WHERE publicado_em IS NULL").update());
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("consumidor com erro de SQL é reagendado, e o evento seguinte é entregue")
    void falhaDeUmNaoDerrubaOLote() {
        UUID quebrado = gravar(QUEBRA);
        UUID seguinte = gravar(PROMOVE);

        worker.processarAgora();

        assertThat(estado(quebrado))
                .as("o evento que falhou precisa contar a tentativa, senão volta ao topo para sempre")
                .containsEntry("tentativas", 1)
                .containsEntry("publicado", false);
        assertThat(estado(seguinte))
                .as("um consumidor quebrado não pode travar a fila atrás dele")
                .containsEntry("publicado", true);
    }

    @Test
    @DisplayName("o tenant promovido por um evento não vaza para o próximo")
    void tenantNaoVazaEntreEventos() {
        gravar(PROMOVE);
        UUID espiado = gravar(ESPIA);

        worker.processarAgora();

        assertThat(CLINICA_VISTA.get(espiado))
                .as("""
                    app.clinica é is_local: dura até o fim da TRANSAÇÃO. Com o lote numa \
                    transação só, o consumidor seguinte roda com o tenant do evento anterior.""")
                .isEmpty();
    }

    @Test
    @DisplayName("o erro gravado no outbox sai sem CPF")
    void ultimoErroSemPii() {
        UUID evento = gravar(FALHA_COM_CPF);

        worker.processarAgora();

        comoWorker();
        String erro = transacao.execute(s -> jdbc.sql(
                        "SELECT ultimo_erro FROM plataforma.outbox WHERE event_id = CAST(:id AS UUID)")
                .param("id", evento.toString())
                .query(String.class).single());
        assertThat(erro).contains("already exists").doesNotContain("12345678901");
    }

    // ─── auxiliares ──────────────────────────────────────────────────────────

    /** Uma transação por evento: occurred_at distinto dá a ordem de entrega. */
    private UUID gravar(String tipo) {
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, 1L, Papel.ADMIN, UUID.randomUUID()));
        return transacao.execute(s -> outbox.gravar(tipo, Map.of()));
    }

    private Map<String, Object> estado(UUID evento) {
        comoWorker();
        return transacao.execute(s -> jdbc.sql("""
                        SELECT tentativas::int AS tentativas, publicado_em IS NOT NULL AS publicado
                        FROM plataforma.outbox WHERE event_id = CAST(:id AS UUID)
                        """)
                .param("id", evento.toString())
                .query().singleRow());
    }

    private static void comoWorker() {
        ContextoAtual.definir(ContextoRequisicao.deWorker(UUID.randomUUID()));
    }
}
