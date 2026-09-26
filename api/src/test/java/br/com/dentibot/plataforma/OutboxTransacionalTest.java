package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.NovaConsulta;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A propriedade que justifica o outbox existir: evento e escrita de negócio
 * commitam JUNTOS, ou não commitam (invariante 8).
 *
 * <p>O caso que este teste cobre é o que mais dói em produção: a transação falha
 * DEPOIS de o evento já ter sido produzido. Com broker externo, o e-mail de
 * confirmação já saiu e o paciente aparece para uma consulta que não existe.
 */
@DisplayName("Outbox transacional")
class OutboxTransacionalTest extends TesteIntegracao {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);

    @Autowired
    private OnboardingServico onboarding;
    @Autowired
    private PacientesApi pacientes;
    @Autowired
    private AgendaApi agenda;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private TransactionTemplate transacao;

    private long idClinica;
    private long idPaciente;
    private long idDentista;

    @BeforeEach
    void prepararClinica() {
        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
        long n = SEQ.incrementAndGet();

        idClinica = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Outbox LTDA", "Clinica Outbox",
                ZoneId.of("America/Sao_Paulo"), Plano.SOLO,
                "Admin", "admin.outbox" + n + "@teste.local",
                "senha-de-teste-muito-longa")).idClinica();

        comoAdmin();
        idPaciente = pacientes.criar(NovoPaciente.basico("Paciente Outbox", "11999990000"));
        idDentista = criarDentista();
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("agendar grava consulta E evento na mesma transação")
    void eventoNasceJuntoComAConsulta() {
        comoAdmin();
        Instant inicio = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);

        long idConsulta = agenda.agendar(new NovaConsulta(
                idPaciente, idDentista, null, inicio, inicio.plus(30, ChronoUnit.MINUTES), null));

        assertThat(contarConsultas()).isEqualTo(1);
        assertThat(contarEventos(TiposDeEvento.CONSULTA_AGENDADA)).isEqualTo(1);
        assertThat(idConsulta).isPositive();
    }

    @Test
    @DisplayName("rollback depois do agendamento leva o evento junto")
    void rollbackNaoDeixaEventoOrfao() {
        comoAdmin();
        Instant inicio = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);

        assertThatThrownBy(() -> transacao.executeWithoutResult(status -> {
            agenda.agendar(new NovaConsulta(
                    idPaciente, idDentista, null, inicio, inicio.plus(30, ChronoUnit.MINUTES), null));
            // O que dá errado depois do "sucesso": cobrança recusada, validação
            // de regra de negócio, queda do banco. Aqui, explicitamente.
            throw new IllegalStateException("falha simulada depois de agendar");
        })).hasMessageContaining("falha simulada");

        // As DUAS coisas sumiram. É esta simetria que o broker externo não dá:
        // lá o evento já teria saído e o paciente receberia confirmação de um
        // horário que ninguém marcou.
        assertThat(contarConsultas())
                .as("a consulta não deve existir após o rollback")
                .isZero();
        assertThat(contarEventos(TiposDeEvento.CONSULTA_AGENDADA))
                .as("o evento não deve existir após o rollback")
                .isZero();
    }

    @Test
    @DisplayName("gravar evento fora de transação é recusado")
    void eventoForaDeTransacaoFalha() {
        comoAdmin();
        // Sem transação o evento não tem a que se atar, e a garantia do outbox
        // deixa de existir — melhor explodir que gravar algo sem sentido.
        assertThatThrownBy(() -> outboxSemTransacao())
                .hasMessageContaining("fora de transação");
    }

    private void outboxSemTransacao() {
        // Chamada direta ao repositório do outbox sem @Transactional ao redor.
        jdbcForaDeTransacaoGravaEvento();
    }

    private void jdbcForaDeTransacaoGravaEvento() {
        br.com.dentibot.plataforma.outbox.Outbox outbox =
                new br.com.dentibot.plataforma.outbox.Outbox(jdbc,
                        new tools.jackson.databind.ObjectMapper());
        outbox.gravar(TiposDeEvento.PACIENTE_CRIADO, java.util.Map.of("paciente_id", idPaciente));
    }

    // ─── auxiliares ──────────────────────────────────────────────────────────

    private void comoAdmin() {
        ContextoAtual.definir(
                ContextoRequisicao.deClinica(idClinica, 1L, Papel.ADMIN, UUID.randomUUID()));
    }

    private Integer contarConsultas() {
        return transacao.execute(s -> jdbc
                .sql("SELECT count(*) FROM agenda.consultas")
                .query(Integer.class).single());
    }

    private Integer contarEventos(String tipo) {
        return transacao.execute(s -> jdbc
                .sql("SELECT count(*) FROM plataforma.outbox WHERE event_type = :tipo")
                .param("tipo", tipo)
                .query(Integer.class).single());
    }

    private long criarDentista() {
        return transacao.execute(s -> {
            Long idPessoa = jdbc.sql("""
                            INSERT INTO identidade.pessoas (id_clinica, nome_completo)
                            VALUES (:clinica, 'Dra. Teste') RETURNING id_pessoa
                            """)
                    .param("clinica", idClinica).query(Long.class).single();
            return jdbc.sql("""
                            INSERT INTO identidade.dentistas
                                (id_clinica, id_pessoa, cro_numero, cro_uf)
                            VALUES (:clinica, :pessoa, :cro, 'SP') RETURNING id_dentista
                            """)
                    .param("clinica", idClinica)
                    .param("pessoa", idPessoa)
                    .param("cro", String.valueOf(SEQ.incrementAndGet()))
                    .query(Long.class).single();
        });
    }
}
