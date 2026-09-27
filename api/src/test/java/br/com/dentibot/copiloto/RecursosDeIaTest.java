package br.com.dentibot.copiloto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.NovaConsulta;
import br.com.dentibot.clinicas.ClinicasApi;
import br.com.dentibot.clinicas.NovoProcedimento;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.copiloto.application.CopilotoServico;
import br.com.dentibot.ia.infrastructure.ClienteAnthropic;
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
import br.com.dentibot.plataforma.erro.FalhaExternaException;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import br.com.dentibot.prontuario.LancamentoSugerido;
import br.com.dentibot.prontuario.RascunhoDeNota;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Doc 03-C: nota a partir do ditado (IA-01) e plano em duas linguagens (IA-04). */
@DisplayName("Recursos de IA generativa (Doc 03-C)")
class RecursosDeIaTest extends TesteIntegracao {

    @MockitoBean private ClienteAnthropic cliente;
    @Autowired private CopilotoServico copiloto;
    @Autowired private OnboardingServico onboarding;
    @Autowired private IdentidadeApi identidade;
    @Autowired private PacientesApi pacientes;
    @Autowired private AgendaApi agenda;
    @Autowired private ClinicasApi clinicas;
    @Autowired private OrcamentoApi orcamentos;

    private long idClinica;
    private long idAdmin;
    private long usuarioDentista;
    private long idPaciente;
    private long idOrcamento;

    @BeforeEach
    void clinica() {
        long n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_iac_" + n, UUID.randomUUID()));
        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica IAC LTDA", "Clinica IAC",
                ZoneId.of("America/Sao_Paulo"), Plano.CLINICA,
                "Admin IAC", "admin.iac" + n + "@teste.local", "user_iac_" + n));
        idClinica = criada.idClinica();
        idAdmin = criada.idUsuarioAdmin();
        como(Papel.ADMIN, idAdmin);
        usuarioDentista = identidade.admitirMembro(new NovoMembro(
                "Dra. IAC", "dra.iac" + n + "@teste.local", Papel.DENTISTA, "I" + n, "SP", null));
        long dentista = identidade.dentistaDoUsuario(usuarioDentista).orElseThrow();
        idPaciente = pacientes.criar(NovoPaciente.basico("Mariana Albuquerque", "11999990004"));
        Instant inicio = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        agenda.agendar(new NovaConsulta(idPaciente, dentista, null, inicio,
                inicio.plus(30, ChronoUnit.MINUTES), null));
        long procedimento = clinicas.criarProcedimento(
                new NovoProcedimento("Restauração", null, new BigDecimal("200.00"), 30));
        idOrcamento = orcamentos.criar(new NovoOrcamento(idPaciente, dentista,
                LocalDate.now().plusDays(30), null, null));
        orcamentos.adicionarItem(idOrcamento, new NovoItem(procedimento, 36, "O", null));

        given(cliente.configurado()).willReturn(true);
        given(cliente.provedor()).willReturn("anthropic");
        given(cliente.modelo()).willReturn("claude-opus-5");
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("nota: sai sem o nome, com schema e esforço baixo; a regra separa o que não serve")
    void notaClinica() {
        responder("""
                {"evolucao":"[PACIENTE] recebeu restauração em resina no 36.",
                 "lancamentos":[{"dente":36,"face":"O","condicao":"restauracao","observacao":"resina"},
                                {"dente":99,"face":null,"condicao":"carie","observacao":null}],
                 "nao_ancorado":["cor da resina não dita"]}""");
        como(Papel.DENTISTA, usuarioDentista);

        RascunhoDeNota r = copiloto.rascunhoDeNota(idPaciente,
                "Mariana Albuquerque, restauração em resina no 36, face oclusal.");

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(cliente).chamar(anyString(), enviado.capture(), notNull(), eq("low"));
        assertThat(enviado.getValue()).doesNotContain("Mariana", "Albuquerque").contains("[PACIENTE]");
        assertThat(r.lancamentos()).containsExactly(new LancamentoSugerido(36, "O", "restauracao", "resina"));
        assertThat(r.naoConfirmado()).hasSize(2);
        assertThat(r.idChamada()).isPositive();
    }

    @Test
    @DisplayName("nota: recepção não escreve prontuário — nada sai para o provedor")
    void recepcaoNaoDitaNota() {
        como(Papel.RECEPCIONISTA, idAdmin);
        assertThatThrownBy(() -> copiloto.rascunhoDeNota(idPaciente, "qualquer coisa"))
                .isInstanceOf(AcessoNegadoException.class);
        verify(cliente, never()).chamar(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("nota: resposta que não é JSON vira 502 — e a chamada fica na trilha mesmo assim")
    void respostaInvalida() {
        responder("desculpe, não entendi");
        como(Papel.DENTISTA, usuarioDentista);
        assertThatThrownBy(() -> copiloto.rascunhoDeNota(idPaciente, "restauração no 36"))
                .isInstanceOf(FalhaExternaException.class);
    }

    @Test
    @DisplayName("plano: a entrada é o orçamento sem identificação; esforço médio; duas versões")
    void planoEmDuasLinguagens() {
        responder("{\"tecnica\":\"Restauração classe I no 36.\",\"paciente\":\"Vamos tratar a cárie do dente de trás.\"}");

        PlanoExplicado p = copiloto.explicarPlano(idOrcamento);

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(cliente).chamar(anyString(), enviado.capture(), notNull(), eq("medium"));
        assertThat(enviado.getValue()).contains("Restauração — dente 36, face O", "R$")
                .doesNotContain("Mariana");
        assertThat(p.versaoPaciente()).contains("cárie");
        assertThat(p.procedimentos()).containsExactly("Restauração — dente 36, face O");
    }

    private void responder(String texto) {
        given(cliente.chamar(anyString(), anyString(), any(), anyString()))
                .willReturn(new ClienteAnthropic.Resultado(texto, 800, 300, "claude-opus-5"));
    }

    private void como(Papel papel, long idUsuario) {
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idUsuario, papel, UUID.randomUUID()));
    }
}
