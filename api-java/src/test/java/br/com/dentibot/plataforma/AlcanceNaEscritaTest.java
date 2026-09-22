package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.NovaConsulta;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.NovoMembro;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A matriz aplicada nos serviços, e não só declarada.
 *
 * <p>O {@code MatrizDePermissaoTest} confere a tabela; este confere que os
 * serviços perguntam a ela e respeitam o {@link
 * br.com.dentibot.plataforma.seguranca.Alcance} devolvido. Os dois defeitos que
 * motivaram esta classe passavam no teste da matriz: o de pacientes não
 * chamava {@code exigir()}, e o da agenda chamava e jogava o alcance fora.
 */
@DisplayName("Alcance aplicado nas escritas")
class AlcanceNaEscritaTest extends TesteIntegracao {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);

    @Autowired
    private OnboardingServico onboarding;
    @Autowired
    private IdentidadeApi identidade;
    @Autowired
    private PacientesApi pacientes;
    @Autowired
    private AgendaApi agenda;

    private long idClinica;
    private long idAdmin;
    private long idPaciente;
    private long usuarioDentistaA;
    private long dentistaA;
    private long usuarioDentistaB;
    private long dentistaB;

    @BeforeEach
    void clinicaComDoisDentistas() {
        long n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_alcance_" + n, UUID.randomUUID()));
        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Alcance LTDA", "Clinica Alcance",
                ZoneId.of("America/Sao_Paulo"), Plano.CLINICA,
                "Admin Alcance", "admin.alcance" + n + "@teste.local", "user_alcance_" + n));
        idClinica = criada.idClinica();
        idAdmin = criada.idUsuarioAdmin();

        como(Papel.ADMIN, idAdmin);
        idPaciente = pacientes.criar(NovoPaciente.basico("Paciente Alcance", "11999990000"));
        usuarioDentistaA = identidade.admitirMembro(new NovoMembro(
                "Dra. A", "dra.a" + n + "@teste.local", Papel.DENTISTA, "A" + n, "SP", null));
        usuarioDentistaB = identidade.admitirMembro(new NovoMembro(
                "Dr. B", "dr.b" + n + "@teste.local", Papel.DENTISTA, "B" + n, "SP", null));
        dentistaA = identidade.dentistaDoUsuario(usuarioDentistaA).orElseThrow();
        dentistaB = identidade.dentistaDoUsuario(usuarioDentistaB).orElseThrow();
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    // ─── pacientes ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("financeiro e auxiliar só leem paciente: cadastrar é negado")
    void somenteLeituraNaoCadastraPaciente() {
        for (Papel papel : new Papel[] {Papel.FINANCEIRO, Papel.AUXILIAR}) {
            como(papel, idAdmin);
            assertThatThrownBy(() -> pacientes.criar(NovoPaciente.basico("Intruso", "11988880000")))
                    .as("%s cadastrando paciente", papel)
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }

    @Test
    @DisplayName("recepcionista cadastra e lista paciente — é o trabalho dela")
    void recepcionistaCadastraPaciente() {
        como(Papel.RECEPCIONISTA, idAdmin);
        long id = pacientes.criar(NovoPaciente.basico("Paciente Balcao", "11977770000"));

        assertThat(pacientes.listarResumos(100, 0))
                .extracting(p -> p.idPaciente())
                .contains(id);
    }

    @Test
    @DisplayName("sem papel na clínica, listar pacientes é negado")
    void semPapelNaoListaPaciente() {
        ContextoAtual.definir(ContextoRequisicao.semConta("user_sem_papel", UUID.randomUUID()));
        assertThatThrownBy(() -> pacientes.listarResumos(10, 0))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // ─── agenda ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("dentista não agenda em nome de um colega")
    void dentistaNaoAgendaParaOutro() {
        como(Papel.DENTISTA, usuarioDentistaA);
        Instant inicio = horario(1);

        assertThatThrownBy(() -> agenda.agendar(new NovaConsulta(
                idPaciente, dentistaB, null, inicio, inicio.plus(30, ChronoUnit.MINUTES), null)))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("dentista não mexe na consulta de um colega — nenhuma das transições")
    void dentistaNaoTransicionaConsultaDeOutro() {
        Map<String, LongConsumer> transicoes = Map.of(
                "confirmar", agenda::confirmar,
                "cancelar", id -> agenda.cancelar(id, "tentativa"),
                "registrarFalta", agenda::registrarFalta,
                "concluir", agenda::concluir);

        int dia = 2;
        for (var t : transicoes.entrySet()) {
            como(Papel.ADMIN, idAdmin);
            Instant inicio = horario(dia++);
            long daColega = agenda.agendar(new NovaConsulta(
                    idPaciente, dentistaB, null, inicio, inicio.plus(30, ChronoUnit.MINUTES), null));

            como(Papel.DENTISTA, usuarioDentistaA);
            // 404 e não 403, como no orçamento: confirmar que a consulta existe
            // já diz algo sobre o paciente de outra pessoa.
            assertThatThrownBy(() -> t.getValue().accept(daColega))
                    .as("dentista A em %s na consulta do dentista B", t.getKey())
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Test
    @DisplayName("dentista agenda e cancela a própria consulta")
    void dentistaMexeNaPropria() {
        como(Papel.DENTISTA, usuarioDentistaA);
        Instant inicio = horario(10);

        long propria = agenda.agendar(new NovaConsulta(
                idPaciente, dentistaA, null, inicio, inicio.plus(30, ChronoUnit.MINUTES), null));
        agenda.cancelar(propria, "paciente desmarcou");

        assertThat(agenda.listar(inicio.minus(1, ChronoUnit.HOURS), inicio.plus(1, ChronoUnit.HOURS), null))
                .singleElement()
                .satisfies(c -> assertThat(c.status()).isEqualTo("cancelada"));
    }

    // ─── auxiliares ──────────────────────────────────────────────────────────

    private void como(Papel papel, long idUsuario) {
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idUsuario, papel, UUID.randomUUID()));
    }

    private static Instant horario(int diasAFrente) {
        return Instant.now().plus(diasAFrente, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    }
}
