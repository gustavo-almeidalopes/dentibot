package br.com.dentibot.plataforma.seguranca;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.correlacao.FiltroCorrelacao;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Traduz o {@code Authorization: Bearer} em {@link ContextoRequisicao}.
 *
 * <p>Duas perguntas, duas fontes, nesta ordem:
 *
 * <ol>
 *   <li><b>Quem é você?</b> — responde o Clerk, pela assinatura do token. Esta
 *       aplicação não emite credencial nenhuma desde a V18.</li>
 *   <li><b>O que você é aqui?</b> — responde este banco, pelo
 *       {@link ProvedorDeIdentidade}. Tenant e papel vêm daqui e de nenhum outro
 *       lugar (invariante 5). Um {@code id_clinica} no corpo da requisição é
 *       ignorado; se alguém tentar usá-lo assim mesmo, o {@code WITH CHECK} do
 *       RLS recusa a escrita.</li>
 * </ol>
 *
 * <p>Token inválido não derruba a requisição aqui: o contexto fica anônimo e
 * quem decide o 401 é a cadeia de autorização. Assim o endpoint público
 * continua público mesmo que o cliente mande um token velho.
 */
@Component
public class FiltroAutenticacao extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroAutenticacao.class);
    private static final String PREFIXO = "Bearer ";

    private final JwtDecoder decoder;
    private final ProvedorDeIdentidade identidades;

    public FiltroAutenticacao(JwtDecoder decoder, ProvedorDeIdentidade identidades) {
        this.decoder = decoder;
        this.identidades = identidades;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        UUID correlacao = FiltroCorrelacao.daRequisicao(req);
        ContextoAtual.definir(resolver(req, correlacao));
        popularSpringSecurity(ContextoAtual.obter());
        try {
            chain.doFilter(req, res);
        } finally {
            ContextoAtual.limpar();
            SecurityContextHolder.clearContext();
        }
    }

    private ContextoRequisicao resolver(HttpServletRequest req, UUID correlacao) {
        String cabecalho = req.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            return ContextoRequisicao.anonimo(correlacao);
        }
        try {
            Jwt jwt = decoder.decode(cabecalho.substring(PREFIXO.length()).trim());
            String sujeito = jwt.getSubject();
            if (sujeito == null || sujeito.isBlank()) {
                return ContextoRequisicao.anonimo(correlacao);
            }
            // Sem conta correspondente o contexto continua sem tenant — e
            // portanto sem acesso a linha nenhuma. O sujeito é preservado só
            // para o cadastro de clínica, que é o fluxo que existe justamente
            // para criar essa conta.
            return identidades.resolver(sujeito, emailVerificado(jwt), correlacao)
                    .orElseGet(() -> ContextoRequisicao.semConta(sujeito, correlacao));
        } catch (Exception e) {
            // Largo de propósito, e a versão anterior só pegava JwtException: um
            // token com claim de forma inesperada estourava NumberFormatException
            // ou NullPointerException, que escapavam daqui e viravam 500. Token
            // malformado é 401, nunca erro do servidor — e o cliente que manda
            // lixo não deve conseguir distinguir um do outro pelo status.
            // Nunca logar o token. O motivo basta para diagnosticar.
            log.debug("Token rejeitado: {}", e.toString());
            return ContextoRequisicao.anonimo(correlacao);
        }
    }

    /**
     * O e-mail do token, e só quando o Clerk o declara verificado.
     *
     * <p>Os dois claims dependem de estarem no template de sessão do Clerk —
     * ver {@code DENTIBOT_CLERK_ISSUER} no {@code .env.example}. Ausentes, isto
     * devolve nulo e o vínculo por e-mail simplesmente não acontece: quem já tem
     * {@code sub} gravado continua entrando, e quem foi cadastrado pela tela de
     * equipe e ainda não tem fica sem conseguir — que é o modo de falhar certo.
     * O modo errado seria aceitar o e-mail sem a verificação.
     */
    private static String emailVerificado(Jwt jwt) {
        return Boolean.TRUE.equals(jwt.getClaim("email_verified"))
                ? jwt.getClaimAsString("email")
                : null;
    }

    /**
     * O Spring Security precisa de um Authentication para os matchers de
     * {@code authorizeHttpRequests} funcionarem. As authorities aqui são
     * grosseiras de propósito — a decisão fina é do
     * {@link AvaliadorDePermissao}, e duplicar a matriz em authorities criaria a
     * segunda fonte de verdade que a invariante 3 proíbe.
     */
    private void popularSpringSecurity(ContextoRequisicao ctx) {
        if (!ctx.autenticado()) {
            SecurityContextHolder.clearContext();
            return;
        }
        String authority = ctx.staffPapel() != null
                ? "STAFF_" + ctx.staffPapel().name()
                : "PAPEL_" + ctx.papel().name();
        String principal = ctx.staffPapel() != null
                ? "staff:" + ctx.staffId()
                : "usuario:" + ctx.usuarioId();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority(authority))));
    }
}
