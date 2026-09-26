package br.com.dentibot.identidade.application;

import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio;
import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio.AcessoDeClinica;
import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio.AcessoDeStaff;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.StaffPapel;
import br.com.dentibot.plataforma.seguranca.ProvedorDeIdentidade;
import br.com.dentibot.plataforma.tenant.ContextoBanco;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Quem é este {@code sub} do Clerk, aqui dentro?"
 *
 * <p>Os dois eixos da camada 5 são consultados em ordem e são exclusivos: um
 * {@code sub} é de um usuário de clínica OU de um staff da plataforma. Se
 * aparecesse nos dois, o staff herdaria acesso clínico de um tenant — que é
 * exatamente o que a separação existe para impedir —, então o caso é tratado
 * como erro e não como precedência.
 */
@Service
public class ResolvedorDeAcessoClerk implements ProvedorDeIdentidade {

    private static final Logger log = LoggerFactory.getLogger(ResolvedorDeAcessoClerk.class);
    private static final String ATIVO = "ativo";

    private final UsuarioRepositorio usuarios;
    private final ContextoBanco contextoBanco;

    public ResolvedorDeAcessoClerk(UsuarioRepositorio usuarios, ContextoBanco contextoBanco) {
        this.usuarios = usuarios;
        this.contextoBanco = contextoBanco;
    }

    /**
     * {@code @Transactional} não é decoração: o {@link
     * br.com.dentibot.plataforma.tenant.GuardaDeTransacao} recusa acesso a
     * repositório fora de transação, e com razão — sem transação não há contexto
     * de tenant e o RLS devolveria vazio em silêncio.
     */
    @Override
    @Transactional
    public Optional<ContextoRequisicao> resolver(String sujeito, String emailVerificado,
                                                 UUID correlacao) {
        Optional<AcessoDeStaff> staff = usuarios.resolverStaffPorClerk(sujeito);
        // A linkagem NÃO pode rodar quando o sub já resolveu no eixo staff. O
        // sub de um staff nunca está em identidade.usuarios, então sem esta
        // condição ela tentava vincular a cada requisição dele — e bastava um
        // admin qualquer cadastrar o e-mail de um staff na tela de equipe para
        // capturar aquele sub, travando a conta nos dois eixos para sempre.
        Optional<AcessoDeClinica> clinica = usuarios.resolverAcessoPorClerk(sujeito)
                .or(() -> staff.isPresent()
                        ? Optional.empty()
                        : vincularPorEmail(sujeito, emailVerificado));

        if (staff.isPresent() && clinica.isPresent()) {
            // Fail closed. Nenhum dos dois vale: promover um dos eixos aqui
            // seria escolher em silêncio, e a escolha errada dá a um staff o
            // dado clínico de um tenant.
            log.error("sub do Clerk presente nos dois eixos de autorização — acesso negado. "
                    + "Corrija o vínculo duplicado em identidade.");
            return Optional.empty();
        }

        if (staff.isPresent()) {
            AcessoDeStaff s = staff.get();
            // Staff inativo não "cai" para o eixo A: ele não tem eixo A. Parar
            // aqui é o ponto.
            return ATIVO.equals(s.status())
                    ? Optional.of(ContextoRequisicao.deStaff(
                            s.idStaff(), StaffPapel.de(s.papel()), correlacao, sujeito))
                    : Optional.empty();
        }
        if (clinica.isPresent()) {
            AcessoDeClinica c = clinica.get();
            return ATIVO.equals(c.status())
                    ? Optional.of(ContextoRequisicao.deClinica(
                            c.idClinica(), c.idUsuario(), c.papel(), correlacao, sujeito))
                    : Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Vínculo na primeira entrada de quem a clínica cadastrou na tela de equipe.
     *
     * <p>O usuário nasce ali com e-mail e papel e sem {@code clerk_user_id} —
     * ninguém pode criar conta do Clerk no lugar de outra pessoa. Quando essa
     * pessoa entra pela primeira vez, o {@code sub} dela é gravado na linha que
     * já a esperava.
     *
     * <p>O e-mail precisa vir verificado pelo Clerk. Sem essa condição, bastaria
     * cadastrar uma conta nova com o e-mail de um admin de outra clínica para
     * herdar a conta dele — o vínculo por e-mail é, literalmente, uma tomada de
     * posse.
     */
    private Optional<AcessoDeClinica> vincularPorEmail(String sujeito, String emailVerificado) {
        if (emailVerificado == null || emailVerificado.isBlank()) {
            return Optional.empty();
        }
        Optional<AcessoDeClinica> pendente = usuarios.resolverAcessoPendentePorEmail(emailVerificado);
        if (pendente.isEmpty()) {
            return Optional.empty();
        }
        AcessoDeClinica acesso = pendente.get();
        if (!ATIVO.equals(acesso.status())) {
            // Vincular a uma conta bloqueada não daria acesso nenhum e queimaria
            // a janela de uso único: a coluna deixaria de ser nula e a pessoa
            // nunca mais conseguiria se vincular depois de reativada.
            return Optional.empty();
        }

        // O UPDATE é escrita da app e cai sob RLS: sem promover o tenant, a
        // linha que a função SECURITY DEFINER acabou de enxergar fica invisível
        // aqui e o UPDATE afeta zero linhas, em silêncio.
        contextoBanco.promoverClinica(acesso.idClinica());
        usuarios.vincularClerk(acesso.idUsuario(), sujeito);
        log.info("Conta do Clerk vinculada ao usuario={} da clinica={}",
                acesso.idUsuario(), acesso.idClinica());
        return pendente;
    }
}
