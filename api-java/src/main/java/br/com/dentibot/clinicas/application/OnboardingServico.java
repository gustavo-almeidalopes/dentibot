package br.com.dentibot.clinicas.application;

import br.com.dentibot.billing.BillingApi;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.clinicas.infrastructure.ClinicaRepositorio;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.tenant.ContextoBanco;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nascimento de uma clínica. É o único fluxo do sistema que começa sem tenant e
 * termina com um.
 *
 * <p>Tudo numa transação só: se o usuário admin falhar (e-mail duplicado, por
 * exemplo), a clínica não fica órfã no banco sem ninguém que consiga entrar
 * nela.
 *
 * <p>Desde a V18 o fluxo é o inverso do que era: a conta no Clerk vem PRIMEIRO,
 * e o cadastro acontece com o requisitante já autenticado. É por isso que a
 * clínica nasce com o `sub` do admin já gravado, e não com uma senha escolhida
 * por um formulário que este serviço nunca deveria ter visto.
 */
@Service
public class OnboardingServico {

    private final ClinicaRepositorio clinicas;
    private final IdentidadeApi identidade;
    private final BillingApi billing;
    private final ContextoBanco contextoBanco;

    public OnboardingServico(ClinicaRepositorio clinicas, IdentidadeApi identidade,
                             BillingApi billing, ContextoBanco contextoBanco) {
        this.clinicas = clinicas;
        this.identidade = identidade;
        this.billing = billing;
        this.contextoBanco = contextoBanco;
    }

    public record NovaClinica(
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            ZoneId timezone,
            Plano plano,
            String nomeAdmin,
            String emailAdmin,
            /** O `sub` do Clerk de quem está cadastrando. Nunca uma senha. */
            String clerkUserIdAdmin) {
    }

    public record ClinicaCriada(long idClinica, long idUsuarioAdmin) {
    }

    @Transactional
    public ClinicaCriada provisionar(NovaClinica cmd) {
        long idClinica = clinicas.provisionar(
                cmd.cnpj(), cmd.razaoSocial(), cmd.nomeFantasia(), cmd.timezone(), cmd.plano());

        // A partir daqui as inserções são DENTRO do tenant recém-criado. Duas
        // promoções, porque há dois consumidores do contexto:
        //   · o Postgres, para o RLS aceitar as próximas linhas;
        //   · o ContextoAtual, porque os repositórios leem id_clinica dele para
        //     preencher a coluna (invariante 5: vem do contexto, não do corpo).
        contextoBanco.promoverClinica(idClinica);
        ContextoRequisicao anterior = ContextoAtual.obter();
        ContextoAtual.definir(ContextoRequisicao.deClinica(
                idClinica, 0L, Papel.ADMIN, anterior.correlacaoId()));

        try {
            clinicas.criarConfiguracoesPadrao(idClinica);
            long idAdmin = identidade.criarUsuario(
                    cmd.nomeAdmin(), cmd.emailAdmin(), Papel.ADMIN, cmd.clerkUserIdAdmin());

            // O trial nasce na MESMA transação. Uma clínica sem assinatura é um
            // estado que nenhuma tela sabe representar — e seria criado
            // justamente para quem acabou de se cadastrar.
            billing.iniciarTrial(cmd.plano().valorBanco());
            return new ClinicaCriada(idClinica, idAdmin);
        } finally {
            // Restaura para não deixar a thread do pool com o tenant da última
            // clínica provisionada — a versão em memória do bug que o
            // set_config(..., true) evita no banco.
            ContextoAtual.definir(anterior);
        }
    }
}
