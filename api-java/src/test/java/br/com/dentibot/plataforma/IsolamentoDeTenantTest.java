package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacienteResumo;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.pacientes.infrastructure.PacienteRepositorio;
import br.com.dentibot.plataforma.tenant.GuardaDeTransacao;
import java.time.ZoneId;
import java.util.List;
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
 * O teste que a V1 deste projeto não tinha — e cuja ausência deixou passar 31
 * políticas de RLS que nunca foram criadas, com vazamento entre clínicas
 * comprovável em três linhas de SQL.
 *
 * <p>Cada asserção de NEGAÇÃO vem acompanhada de uma asserção de PERMISSÃO. Sem
 * isso, "A não vê B" fica verde com o banco vazio, que é a forma mais comum de
 * um teste de isolamento mentir.
 */
@DisplayName("Isolamento multi-tenant")
class IsolamentoDeTenantTest extends TesteIntegracao {

    @Autowired
    private OnboardingServico onboarding;

    @Autowired
    private PacientesApi pacientes;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private TransactionTemplate transacao;

    /** Contador para CNPJ e e-mail únicos entre execuções na mesma JVM. */
    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);

    private long clinicaA;
    private long clinicaB;

    @BeforeEach
    void prepararDuasClinicas() {
        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));

        clinicaA = criarClinica("Clinica A").idClinica();
        clinicaB = criarClinica("Clinica B").idClinica();

        comoAdminDe(clinicaA);
        pacientes.criar(novoPaciente("Paciente da Clinica A"));

        comoAdminDe(clinicaB);
        pacientes.criar(novoPaciente("Paciente da Clinica B"));
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("tenant A enxerga o próprio paciente e não enxerga o de B")
    void tenantAveSomenteOsProprios() {
        comoAdminDe(clinicaA);
        List<PacienteResumo> vistos = pacientes.listarResumos(100, 0);

        // PERMISSÃO: a clínica A realmente enxerga o que é dela.
        assertThat(vistos).hasSize(1);
        assertThat(vistos.getFirst().nomeCompleto()).isEqualTo("Paciente da Clinica A");

        // NEGAÇÃO: e nada do que é da B.
        assertThat(vistos).noneMatch(p -> p.nomeCompleto().contains("Clinica B"));
    }

    @Test
    @DisplayName("tenant B enxerga o próprio paciente e não enxerga o de A")
    void tenantBveSomenteOsProprios() {
        comoAdminDe(clinicaB);
        List<PacienteResumo> vistos = pacientes.listarResumos(100, 0);

        assertThat(vistos).hasSize(1);
        assertThat(vistos.getFirst().nomeCompleto()).isEqualTo("Paciente da Clinica B");
    }

    @Test
    @DisplayName("sem tenant no contexto, a consulta devolve zero linhas — falha fechando")
    void semTenantNaoEnxergaNada() {
        // Prova de que existem linhas para serem vistas, se o tenant permitisse.
        comoAdminDe(clinicaA);
        assertThat(pacientes.listarResumos(100, 0)).isNotEmpty();

        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
        assertThat(pacientes.listarResumos(100, 0)).isEmpty();
    }

    @Test
    @DisplayName("com tenant A ativo, escrever na clínica B é recusado pelo banco")
    void escritaCruzadaEhRecusada() {
        comoAdminDe(clinicaA);

        // PERMISSÃO ao lado da negação: dentro do próprio tenant a mesma
        // inserção passa. Sem esta metade, o teste ficaria verde mesmo que o
        // INSERT estivesse falhando por qualquer outro motivo — falta de
        // contexto, coluna errada, tabela inexistente.
        transacao.executeWithoutResult(s -> {
            int linhas = inserirPessoaEm(clinicaA, "Fulano da propria clinica");
            assertThat(linhas).isEqualTo(1);
        });

        // NEGAÇÃO: mesma transação, mesmo tenant ativo, id_clinica da outra.
        assertThatThrownBy(() -> transacao.executeWithoutResult(s ->
                inserirPessoaEm(clinicaB, "Invasor")))
                .hasStackTraceContaining("row-level security");
    }

    @Test
    @DisplayName("repositório fora de transação falha alto em vez de devolver vazio")
    void foraDeTransacaoFalhaAlto() {
        comoAdminDe(clinicaA);

        // Dentro de transação funciona...
        Integer dentroDaTransacao = transacao.execute(s -> pacienteRepositorio.contar());
        assertThat(dentroDaTransacao).isEqualTo(1);

        // ...e fora dela não há set_config, o RLS devolveria zero linhas em
        // silêncio, e silêncio aqui vira "o RLS está atrapalhando, vamos tirar".
        assertThatThrownBy(pacienteRepositorio::contar)
                .isInstanceOf(GuardaDeTransacao.AcessoForaDeTransacaoException.class)
                .hasMessageContaining("@Transactional");
    }

    private int inserirPessoaEm(long idClinica, String nome) {
        return jdbc.sql("""
                        INSERT INTO identidade.pessoas (id_clinica, nome_completo)
                        VALUES (:clinica, :nome)
                        """)
                .param("clinica", idClinica)
                .param("nome", nome)
                .update();
    }

    // ─── auxiliares ──────────────────────────────────────────────────────────

    private OnboardingServico.ClinicaCriada criarClinica(String nome) {
        long n = SEQ.incrementAndGet();
        return onboarding.provisionar(new NovaClinica(
                String.format("%014d", n),
                nome + " LTDA",
                nome,
                ZoneId.of("America/Sao_Paulo"),
                Plano.SOLO,
                "Admin " + nome,
                "admin" + n + "@teste.local",
                "senha-de-teste-muito-longa"));
    }

    private void comoAdminDe(long clinica) {
        ContextoAtual.definir(
                ContextoRequisicao.deClinica(clinica, 1L, Papel.ADMIN, UUID.randomUUID()));
    }

    private NovoPaciente novoPaciente(String nome) {
        return new NovoPaciente(nome, null, null, "11999990000", null, null, null);
    }
}
