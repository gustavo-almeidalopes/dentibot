package br.com.dentibot.lgpd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.lgpd.application.LgpdServico;
import br.com.dentibot.lgpd.application.TitularServico;
import br.com.dentibot.lgpd.application.TitularServico.Painel;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.prontuario.ProntuarioApi;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Doc 03-E: o paciente, pelo link, vê quem acessou e decide finalidade por finalidade. */
@DisplayName("Titular no controle (IA-52, IA-53)")
class TitularTest extends TesteIntegracao {

    @Autowired private OnboardingServico onboarding;
    @Autowired private PacientesApi pacientes;
    @Autowired private ProntuarioApi prontuario;
    @Autowired private LgpdServico lgpd;
    @Autowired private TitularServico titular;

    private long idClinica;
    private long idAdmin;
    private long idPaciente;

    @BeforeEach
    void clinica() {
        long n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_tit_" + n, UUID.randomUUID()));
        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Titular LTDA", "Clinica Titular",
                ZoneId.of("America/Sao_Paulo"), Plano.CLINICA,
                "Admin Tit", "admin.tit" + n + "@teste.local", "user_tit_" + n));
        idClinica = criada.idClinica();
        idAdmin = criada.idUsuarioAdmin();
        admin();
        idPaciente = pacientes.criar(NovoPaciente.basico("Titular " + n, "11977770000"));
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("o extrato mostra quem abriu o prontuário, com nome e papel")
    void extrato() {
        prontuario.historico(idPaciente);
        String token = lgpd.gerarLink(idPaciente).token();

        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
        Painel painel = titular.painel(token);

        assertThat(painel.clinica()).isEqualTo("Clinica Titular");
        assertThat(painel.acessos()).anySatisfy(a -> {
            assertThat(a.recurso()).isEqualTo("prontuario.evolucao");
            assertThat(a.quem()).isEqualTo("Admin Tit (admin)");
        });
        assertThat(painel.preferencias()).extracting(Preferencia::finalidade)
                .containsExactly("whatsapp", "imagem_ensino", "dados_anonimizados");
    }

    @Test
    @DisplayName("desligar vale na hora, e a história fica: padrão, depois a escolha do titular")
    void preferencias() {
        String token = lgpd.gerarLink(idPaciente).token();
        assertThat(lgpd.permite(idPaciente, Finalidade.WHATSAPP)).isTrue();
        assertThat(lgpd.permite(idPaciente, Finalidade.IMAGEM_ENSINO)).isFalse();

        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
        titular.alterar(token, Finalidade.WHATSAPP, false);
        titular.alterar(token, Finalidade.IMAGEM_ENSINO, true);

        admin();
        assertThat(lgpd.permite(idPaciente, Finalidade.WHATSAPP)).isFalse();
        assertThat(lgpd.permite(idPaciente, Finalidade.IMAGEM_ENSINO)).isTrue();
        assertThat(lgpd.preferencias(idPaciente))
                .filteredOn(p -> p.finalidade().equals("whatsapp")).singleElement()
                .satisfies(p -> assertThat(p.origem()).isEqualTo("titular"));
    }

    @Test
    @DisplayName("contestar um acesso abre oposição com prazo; acesso de outro não se contesta")
    void oposicao() {
        prontuario.historico(idPaciente);
        String token = lgpd.gerarLink(idPaciente).token();

        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
        long idEvento = titular.painel(token).acessos().stream()
                .filter(a -> a.recurso().equals("prontuario.evolucao"))
                .findFirst().orElseThrow().idEvento();
        titular.opor(token, idEvento, "Não reconheço este acesso.");
        assertThatThrownBy(() -> titular.opor(token, 999_999_999L, "x"))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        admin();
        assertThat(lgpd.listarSolicitacoes(null)).singleElement().satisfies(s -> {
            assertThat(s.direito()).isEqualTo("oposicao");
            assertThat(s.detalhe()).contains("Admin Tit", "Não reconheço este acesso.");
        });
    }

    @Test
    @DisplayName("link novo derruba o velho; token inventado é 404")
    void linkRevogado() {
        String velho = lgpd.gerarLink(idPaciente).token();
        String novo = lgpd.gerarLink(idPaciente).token();

        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
        assertThat(titular.painel(novo).clinica()).isEqualTo("Clinica Titular");
        assertThatThrownBy(() -> titular.painel(velho))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> titular.painel("inventado"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private void admin() {
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idAdmin, Papel.ADMIN,
                UUID.randomUUID()));
    }
}
