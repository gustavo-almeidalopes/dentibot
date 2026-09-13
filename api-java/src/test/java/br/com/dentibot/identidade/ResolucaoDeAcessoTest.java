package br.com.dentibot.identidade;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.identidade.infrastructure.PessoaRepositorio;
import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.seguranca.ProvedorDeIdentidade;
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
 * Resolução de acesso a partir do {@code sub} do Clerk.
 *
 * <p>Substitui o antigo {@code AutenticacaoTest}, que exercitava o login por
 * senha e o refresh rotativo — ambos saíram na V18 junto com o emissor próprio.
 * O que aquele teste cobria e continua valendo está preservado aqui: a
 * travessia de RLS e a ordem dos eixos do contexto. O resto (senha errada,
 * e-mail inexistente indistinguível, rotação de refresh) virou responsabilidade
 * do Clerk e não tem mais código nosso para testar.
 *
 * <p>O primeiro teste existe por um bug concreto, documentado na V3 e repetido
 * na V18: SECURITY DEFINER <b>não</b> contorna RLS, só troca quem a função é.
 * Com {@code FORCE ROW LEVEL SECURITY}, um role que nenhuma política nomeia
 * enxerga zero linhas. A função "funciona", devolve vazio, e a API responde 401
 * para um token perfeitamente válido — sem nada em log, em lugar nenhum.
 */
@DisplayName("Resolução de acesso pelo Clerk")
class ResolucaoDeAcessoTest extends TesteIntegracao {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);

    @Autowired
    private OnboardingServico onboarding;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private PessoaRepositorio pessoas;
    @Autowired
    private ProvedorDeIdentidade identidades;
    @Autowired
    private TransactionTemplate transacao;

    private String emailAdmin;
    private String subAdmin;
    private long idClinica;
    private long idUsuarioAdmin;

    @BeforeEach
    void criarClinicaComAdmin() {
        long n = SEQ.incrementAndGet();
        emailAdmin = "admin.clerk" + n + "@teste.local";
        subAdmin = "user_teste_" + n;

        // O cadastro roda com o sub já autenticado no Clerk e ainda sem clínica:
        // é o único momento em que um sub sem linha correspondente é legítimo.
        ContextoAtual.definir(ContextoRequisicao.semConta(subAdmin, UUID.randomUUID()));

        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Clerk LTDA", "Clinica Clerk",
                ZoneId.of("America/Sao_Paulo"), Plano.SOLO,
                "Admin Clerk", emailAdmin, subAdmin));
        idClinica = criada.idClinica();
        idUsuarioAdmin = criada.idUsuarioAdmin();

        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("a travessia de RLS enxerga o usuário recém-criado")
    void resolverAtravessaORls() {
        var acesso = transacao.execute(s -> usuarios.resolverAcessoPorClerk(subAdmin));

        assertThat(acesso)
                .as("""
                    A função SECURITY DEFINER precisa de um dono com política de RLS \
                    própria. Sem isso ela devolve vazio e a API recusa um token válido, \
                    sem erro nenhum em lugar nenhum.""")
                .isPresent();
        assertThat(acesso.get().idClinica()).isEqualTo(idClinica);
    }

    @Test
    @DisplayName("o contexto recebe clínica e usuário nas posições certas")
    void contextoCarregaOsEixosCorretos() {
        Optional<ContextoRequisicao> ctx = transacao.execute(
                s -> identidades.resolver(subAdmin, null, UUID.randomUUID()));

        assertThat(ctx).isPresent();
        // Trocar os dois daria a cada usuário o tenant de número igual ao seu id
        // — um vazamento silencioso e sistemático, e o motivo de esta asserção
        // conferir os dois campos separadamente em vez de só "resolveu".
        assertThat(ctx.get().clinicaId()).isEqualTo(idClinica);
        assertThat(ctx.get().usuarioId()).isEqualTo(idUsuarioAdmin);
        assertThat(ctx.get().papel()).isEqualTo(Papel.ADMIN);
        assertThat(ctx.get().staffPapel()).isNull();
        assertThat(ctx.get().sujeitoExterno()).isEqualTo(subAdmin);
    }

    @Test
    @DisplayName("sub desconhecido não resolve e não vira acesso")
    void subDesconhecidoNaoResolve() {
        Optional<ContextoRequisicao> ctx = transacao.execute(
                s -> identidades.resolver("user_nao_existe_" + UUID.randomUUID(), null,
                        UUID.randomUUID()));

        assertThat(ctx).isEmpty();
    }

    @Test
    @DisplayName("membro cadastrado pela equipe vincula na primeira entrada, e só uma vez")
    void vinculoPorEmailVerificadoAconteceUmaVezSo() {
        long n = SEQ.incrementAndGet();
        String emailMembro = "dentista.clerk" + n + "@teste.local";

        // A clínica cadastra a pessoa: nasce com e-mail e papel, sem clerk_user_id.
        cadastrarMembro("Dentista Clerk", emailMembro, Papel.DENTISTA);
        String subMembro = "user_membro_" + n;
        Optional<ContextoRequisicao> primeira = transacao.execute(
                s -> identidades.resolver(subMembro, emailMembro, UUID.randomUUID()));

        assertThat(primeira)
                .as("a pessoa que a clínica cadastrou precisa conseguir entrar na primeira vez")
                .isPresent();
        assertThat(primeira.get().papel()).isEqualTo(Papel.DENTISTA);
        assertThat(primeira.get().clinicaId()).isEqualTo(idClinica);

        // A janela fecha: um SEGUNDO sub com o mesmo e-mail verificado não pode
        // herdar a conta. Sem o `clerk_user_id IS NULL` no resolvedor, criar uma
        // conta nova no Clerk com o e-mail de alguém seria tomada de posse.
        Optional<ContextoRequisicao> invasor = transacao.execute(
                s -> identidades.resolver("user_invasor_" + n, emailMembro, UUID.randomUUID()));

        assertThat(invasor)
                .as("""
                    A janela de vínculo é de uso único. Reabri-la deixa qualquer conta \
                    nova do Clerk com o mesmo e-mail assumir um usuário já vinculado.""")
                .isEmpty();
    }

    @Test
    @DisplayName("e-mail não verificado nunca vincula")
    void semEmailVerificadoNaoVincula() {
        long n = SEQ.incrementAndGet();
        String emailMembro = "recepcao.clerk" + n + "@teste.local";

        cadastrarMembro("Recepcao Clerk", emailMembro, Papel.RECEPCIONISTA);
        // emailVerificado nulo é o que o filtro passa quando o claim
        // `email_verified` está ausente ou não é booleano true.
        Optional<ContextoRequisicao> ctx = transacao.execute(
                s -> identidades.resolver("user_sem_verificacao_" + n, null, UUID.randomUUID()));

        assertThat(ctx)
                .as("""
                    Vínculo por e-mail é tomada de posse de uma conta. Aceitar e-mail não \
                    verificado deixaria qualquer um digitar o endereço de um admin e herdar \
                    a clínica dele.""")
                .isEmpty();
    }

    /**
     * Cadastra um membro como a tela de equipe faria: pessoa + usuário, sem
     * {@code clerk_user_id}.
     *
     * <p>O contexto é definido ANTES de abrir a transação, e não dentro dela: o
     * {@code GerenciadorTransacaoComTenant} lê o {@code ContextoAtual} no início
     * da transação para rodar o {@code set_config}. Definido no callback, ele
     * chega depois — o {@code app.clinica} fica vazio e o INSERT bate no RLS.
     */
    private void cadastrarMembro(String nome, String email, Papel papel) {
        ContextoAtual.definir(ContextoRequisicao.deClinica(
                idClinica, idUsuarioAdmin, Papel.ADMIN, UUID.randomUUID()));
        transacao.executeWithoutResult(s ->
                usuarios.inserir(pessoas.inserir(nome, null, null, email), email, papel, null));
        ContextoAtual.definir(ContextoRequisicao.anonimo(UUID.randomUUID()));
    }
}
