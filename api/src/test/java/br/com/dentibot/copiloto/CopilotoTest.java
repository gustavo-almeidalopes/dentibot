package br.com.dentibot.copiloto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.ConsultaResumo;
import br.com.dentibot.agenda.NovaConsulta;
import br.com.dentibot.clinicas.ClinicasApi;
import br.com.dentibot.clinicas.NovoProcedimento;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.copiloto.application.CopilotoServico;
import br.com.dentibot.estoque.EstoqueApi;
import br.com.dentibot.estoque.NovaMovimentacao;
import br.com.dentibot.estoque.NovoProduto;
import br.com.dentibot.estoque.SugestaoCompra;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.NovoMembro;
import br.com.dentibot.orcamento.NovoItem;
import br.com.dentibot.orcamento.NovoOrcamento;
import br.com.dentibot.orcamento.OrcamentoApi;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import br.com.dentibot.prontuario.NovaEvolucao;
import br.com.dentibot.prontuario.ProntuarioApi;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
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
 * O copiloto (Doc 03-A) contra banco de verdade e três papéis. O que importa
 * provar não é só a conta — é que cada papel vê o resumo que a matriz permite,
 * sem regra de permissão nova escrita no copiloto.
 */
@DisplayName("Copiloto sobre o dado existente (Doc 03-A)")
class CopilotoTest extends TesteIntegracao {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);

    @Autowired private OnboardingServico onboarding;
    @Autowired private IdentidadeApi identidade;
    @Autowired private PacientesApi pacientes;
    @Autowired private AgendaApi agenda;
    @Autowired private ClinicasApi clinicas;
    @Autowired private OrcamentoApi orcamentos;
    @Autowired private ProntuarioApi prontuario;
    @Autowired private EstoqueApi estoque;
    @Autowired private CopilotoServico copiloto;
    @Autowired private JdbcClient jdbc;
    @Autowired private TransactionTemplate transacao;

    private long idClinica;
    private long idAdmin;
    private long usuarioDentista;
    private long dentista;
    private long idPaciente;

    @BeforeEach
    void clinicaComPacienteEmTratamento() {
        long n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_copiloto_" + n, UUID.randomUUID()));
        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Copiloto LTDA", "Clinica Copiloto",
                ZoneId.of("America/Sao_Paulo"), Plano.CLINICA,
                "Admin Copiloto", "admin.copiloto" + n + "@teste.local", "user_copiloto_" + n));
        idClinica = criada.idClinica();
        idAdmin = criada.idUsuarioAdmin();

        como(Papel.ADMIN, idAdmin);
        usuarioDentista = identidade.admitirMembro(new NovoMembro(
                "Dra. Copiloto", "dra.copiloto" + n + "@teste.local", Papel.DENTISTA, "C" + n, "SP", null));
        dentista = identidade.dentistaDoUsuario(usuarioDentista).orElseThrow();
        idPaciente = pacientes.criar(new NovoPaciente("Paciente Copiloto", null, null, null,
                "11999990001", null, null, null, null, null, null, null, null, null, null,
                new NovoPaciente.Anamnese(false, "Marevan 5mg", "dipirona", true, null, "dor",
                        null, null),
                null, null));

        // Plano aprovado com uma restauração ainda por fazer.
        long procedimento = clinicas.criarProcedimento(
                new NovoProcedimento("Restauração", null, new BigDecimal("200.00"), 30));
        long orcamento = orcamentos.criar(new NovoOrcamento(idPaciente, dentista,
                LocalDate.now().plusDays(30), null, null));
        orcamentos.adicionarItem(orcamento, new NovoItem(procedimento, 36, "O", null));
        orcamentos.enviar(orcamento);
        orcamentos.aprovar(orcamento);

        // Histórico: duas faltas e uma consulta realizada, no passado.
        agenda.registrarFalta(agendar(-20));
        agenda.registrarFalta(agendar(-15));
        agenda.concluir(agendar(-10));
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    // ─── IA-06 / IA-27 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("dentista vê alertas falados, plano em aberto e agenda — e não o financeiro")
    void resumoDoDentista() {
        como(Papel.DENTISTA, usuarioDentista);

        ResumoDoPaciente r = copiloto.resumo(idPaciente);

        assertThat(r.alertas().permitido()).isTrue();
        assertThat(r.alertas().dados().avisos()).containsExactly(
                "Alergia: dipirona.",
                "Possível anticoagulante em uso: Marevan 5mg.",
                "Condição sistêmica declarada.");
        assertThat(r.planoEmAberto().dados()).singleElement()
                .satisfies(i -> assertThat(i.nomeProcedimento()).isEqualTo("Restauração"));
        assertThat(r.agenda().dados().ultimaRealizada()).isNotNull();
        assertThat(r.financeiro().permitido()).as("dentista não lê financeiro").isFalse();
    }

    @Test
    @DisplayName("recepção recebe o resumo sem a parte clínica, sem erro")
    void resumoDaRecepcao() {
        como(Papel.RECEPCIONISTA, idAdmin);

        ResumoDoPaciente r = copiloto.resumo(idPaciente);

        assertThat(r.alertas().permitido()).isFalse();
        assertThat(r.alertas().dados()).isNull();
        assertThat(r.ultimaEvolucao().permitido()).isFalse();
        assertThat(r.financeiro().permitido()).isTrue();
        assertThat(r.planoEmAberto().permitido()).isTrue();
    }

    // ─── IA-15 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("a agenda diz quantas das últimas consultas encerradas foram falta")
    void historicoDeFaltasNaAgenda() {
        como(Papel.RECEPCIONISTA, idAdmin);
        long amanha = agendar(1);

        ConsultaResumo linha = agenda.listar(Instant.now(), Instant.now().plus(2, ChronoUnit.DAYS), null)
                .stream().filter(c -> c.idConsulta() == amanha).findFirst().orElseThrow();

        assertThat(linha.faltasRecentes()).isEqualTo(2);
        assertThat(linha.consultasRecentes()).isEqualTo(3);
    }

    // ─── IA-35 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("plano aprovado sem consulta marcada entra no radar; com consulta marcada, sai")
    void radarDeAbandono() {
        como(Papel.RECEPCIONISTA, idAdmin);
        assertThat(copiloto.tratamentosParados(0)).singleElement().satisfies(t -> {
            assertThat(t.idPaciente()).isEqualTo(idPaciente);
            assertThat(t.valorEmAberto()).isEqualByComparingTo("200.00");
            assertThat(t.procedimentos()).containsExactly("Restauração (dente 36)");
        });

        agendar(7);

        assertThat(copiloto.tratamentosParados(0)).isEmpty();
    }

    // ─── IA-05 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("consulta realizada sem evolução é apontada até a evolução existir")
    void pendenciaDeRegistro() {
        como(Papel.DENTISTA, usuarioDentista);
        assertThat(copiloto.pendencias(30))
                .extracting(Pendencia::tipo).containsExactly(Pendencia.CONSULTA_SEM_EVOLUCAO);
        long idConsulta = copiloto.pendencias(30).get(0).idConsulta();

        prontuario.registrarEvolucao(new NovaEvolucao(idPaciente, idConsulta, "Profilaxia."));

        assertThat(copiloto.pendencias(30)).isEmpty();
    }

    @Test
    @DisplayName("pendência de registro clínico é pergunta clínica: recepção não vê")
    void recepcaoNaoVePendencias() {
        como(Papel.RECEPCIONISTA, idAdmin);
        assertThatThrownBy(() -> copiloto.pendencias(30)).isInstanceOf(AcessoNegadoException.class);
    }

    // ─── IA-43 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sugestão de compra cobre o consumo da cobertura e repõe o ponto de pedido")
    void sugestaoDeCompra() {
        long produto = estoque.criarProduto(new NovoProduto("Resina A2", "un",
                new BigDecimal("80"), false, null));
        estoque.movimentar(new NovaMovimentacao(produto, "entrada", new BigDecimal("100"),
                null, null, null, null));
        estoque.movimentar(new NovaMovimentacao(produto, "saida", new BigDecimal("30"),
                null, null, null, null));

        // 30 em 90 dias = 1/3 por dia; 30 dias = 10; saldo 70, ponto 80 → 10 + 80 - 70 = 20.
        assertThat(estoque.sugestaoDeCompra(30)).singleElement().satisfies(s -> {
            assertThat(s.idProduto()).isEqualTo(produto);
            assertThat(s.quantidadeSugerida()).isEqualByComparingTo("20");
        });
    }

    // ─── IA-47 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("portabilidade: JSON com o que a clínica tem, SHA-256 dos bytes e rastro na auditoria")
    void exportacao() throws Exception {
        Exportacao e = copiloto.exportar(idPaciente);

        String json = new String(e.conteudo(), StandardCharsets.UTF_8);
        assertThat(json).contains("dentibot.portabilidade.v1", "Paciente Copiloto", "dipirona",
                "Restauração", "faltou");
        assertThat(e.sha256()).isEqualTo(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(e.conteudo())));
        String hashNaTrilha = transacao.execute(s -> jdbc.sql("""
                        SELECT dados_posteriores->>'sha256' FROM auditoria.eventos
                        WHERE acao = 'exportacao' AND recurso = 'paciente' AND id_recurso = :id
                        """)
                .param("id", String.valueOf(idPaciente))
                .query(String.class).single());
        assertThat(hashNaTrilha).isEqualTo(e.sha256());
    }

    @Test
    @DisplayName("portabilidade é do admin: dentista não exporta")
    void soAdminExporta() {
        como(Papel.DENTISTA, usuarioDentista);
        assertThatThrownBy(() -> copiloto.exportar(idPaciente))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // ─── apoio ───────────────────────────────────────────────────────────────

    private void como(Papel papel, long idUsuario) {
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idUsuario, papel, UUID.randomUUID()));
    }

    /** Consulta de 30 min, {@code dias} a partir de hoje, como admin. */
    private long agendar(int dias) {
        ContextoRequisicao antes = ContextoAtual.obter();
        como(Papel.ADMIN, idAdmin);
        Instant inicio = Instant.now().plus(dias, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        long id = agenda.agendar(new NovaConsulta(idPaciente, dentista, null, inicio,
                inicio.plus(30, ChronoUnit.MINUTES), null));
        ContextoAtual.definir(antes);
        return id;
    }
}
