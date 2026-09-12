package br.com.dentibot.identidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.identidade.application.AutenticacaoServico;
import br.com.dentibot.identidade.application.AutenticacaoServico.Credenciais;
import br.com.dentibot.identidade.application.AutenticacaoServico.CredenciaisInvalidasException;
import br.com.dentibot.identidade.application.AutenticacaoServico.ParDeTokens;
import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.seguranca.ServicoDeToken;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Login e refresh rotativo.
 *
 * <p>O primeiro teste aqui existe por um bug concreto encontrado em
 * desenvolvimento: {@code identidade.resolver_clinica_por_email} era
 * SECURITY DEFINER e pertencia ao migrador — e SECURITY DEFINER <b>não</b>
 * contorna RLS, só troca quem a função é. Com {@code FORCE ROW LEVEL SECURITY},
 * um role que nenhuma política nomeia enxerga zero linhas. A função devolvia
 * NULL, o login respondia "credenciais inválidas" para a senha certa, e nada
 * aparecia em log: o sistema estava 100% quebrado e 100% silencioso.
 */
@DisplayName("Autenticação")
class AutenticacaoTest extends TesteIntegracao {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);

    @Autowired
    private OnboardingServico onboarding;
    @Autowired
    private AutenticacaoServico autenticacao;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private ServicoDeToken tokens;
    @Autowired
    private TransactionTemplate transacao;

    private String email;
    private String senha;
    private long idClinica;
    private long idUsuarioAdmin;

    @BeforeEach
    void criarClinicaComAdmin() {
        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
        long n = SEQ.incrementAndGet();
        email = "admin.auth" + n + "@teste.local";
        senha = "senha-de-teste-bem-longa";

        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Auth LTDA", "Clinica Auth",
                ZoneId.of("America/Sao_Paulo"), Plano.SOLO,
                "Admin Auth", email, senha));
        idClinica = criada.idClinica();
        idUsuarioAdmin = criada.idUsuarioAdmin();
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("a travessia de RLS do login enxerga o usuário recém-criado")
    void resolverClinicaAtravessaORls() {
        // Sem contexto de tenant, que é a situação real no início do login.
        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));

        Optional<Long> resolvida = transacao.execute(s -> usuarios.resolverClinicaPorEmail(email));

        assertThat(resolvida)
                .as("""
                    A função SECURITY DEFINER precisa de um dono com política de RLS \
                    própria. Sem isso ela devolve NULL e o login recusa a senha certa, \
                    sem erro nenhum em lugar nenhum.""")
                .contains(idClinica);
    }

    @Test
    @DisplayName("senha correta devolve access e refresh")
    void loginComSenhaCorreta() {
        ParDeTokens par = autenticacao.autenticar(
                new Credenciais(email, senha, "127.0.0.1", "teste"));

        assertThat(par.access()).isNotBlank();
        assertThat(par.refresh()).isNotBlank();
        assertThat(par.accessExpiraEm()).isAfter(java.time.Instant.now());
    }

    @Test
    @DisplayName("o token carrega a clínica e o usuário nas posições certas")
    void tokenCarregaContextoCorreto() {
        ParDeTokens par = autenticacao.autenticar(
                new Credenciais(email, senha, "127.0.0.1", "teste"));

        ServicoDeToken.AccessLido lido = tokens.lerAccess(par.access(), UUID.randomUUID());

        // Trocar a ordem de (clinica, usuario) na construção do contexto daria a
        // cada usuário o tenant de número igual ao seu id — vazamento silencioso
        // e sistemático, que só aparece quando os dois números divergem.
        assertThat(lido.contexto().clinicaId()).isEqualTo(idClinica);
        assertThat(lido.contexto().usuarioId()).isEqualTo(idUsuarioAdmin);
        assertThat(lido.contexto().papel()).isNotNull();
        assertThat(lido.contexto().staffPapel())
                .as("token de usuário de clínica nunca carrega papel de staff")
                .isNull();
    }

    @Test
    @DisplayName("senha errada é recusada")
    void senhaErrada() {
        assertThatThrownBy(() -> autenticacao.autenticar(
                new Credenciais(email, "senha-errada-porem-longa", "127.0.0.1", "teste")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("e-mail inexistente é recusado com a MESMA exceção da senha errada")
    void emailInexistenteNaoSeDistingue() {
        // Distinguir os dois casos entrega a lista de usuários a quem testar.
        assertThatThrownBy(() -> autenticacao.autenticar(
                new Credenciais("ninguem@lugar.local", senha, "127.0.0.1", "teste")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("refresh rotaciona: o token antigo deixa de valer")
    void refreshRotaciona() {
        ParDeTokens primeiro = autenticacao.autenticar(
                new Credenciais(email, senha, "127.0.0.1", "teste"));

        ParDeTokens segundo = autenticacao.renovar(primeiro.refresh(), "127.0.0.1", "teste");
        assertThat(segundo.refresh()).isNotEqualTo(primeiro.refresh());

        // O primeiro refresh foi queimado na rotação.
        assertThatThrownBy(() -> autenticacao.renovar(primeiro.refresh(), "127.0.0.1", "teste"))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("reuso de refresh derruba a família inteira")
    void reusoDerrubaFamilia() {
        ParDeTokens primeiro = autenticacao.autenticar(
                new Credenciais(email, senha, "127.0.0.1", "teste"));
        ParDeTokens segundo = autenticacao.renovar(primeiro.refresh(), "127.0.0.1", "teste");

        // Alguém apresenta o token já rotacionado: ou é o usuário com um token
        // velho, ou é cópia roubada. Não dá para distinguir, e a resposta segura
        // para os dois é a mesma.
        assertThatThrownBy(() -> autenticacao.renovar(primeiro.refresh(), "127.0.0.1", "teste"))
                .isInstanceOf(CredenciaisInvalidasException.class);

        // E o token BOM também morre — é isso que "derrubar a família" significa.
        assertThatThrownBy(() -> autenticacao.renovar(segundo.refresh(), "127.0.0.1", "teste"))
                .as("depois de detectado reuso, a sessão inteira cai e exige login novo")
                .isInstanceOf(CredenciaisInvalidasException.class);
    }
}
