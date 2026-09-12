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
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Traduz o {@code Authorization: Bearer} em {@link ContextoRequisicao}.
 *
 * <p>Daqui em diante, tenant e papel vêm do token e de nenhum outro lugar
 * (invariante 5). Um {@code id_clinica} no corpo da requisição é ignorado; se
 * alguém tentar usá-lo assim mesmo, o {@code WITH CHECK} do RLS recusa a
 * escrita.
 *
 * <p>Token inválido não derruba a requisição aqui: o contexto fica anônimo e
 * quem decide o 401 é a cadeia de autorização. Assim o endpoint público
 * continua público mesmo que o cliente mande um token velho.
 */
@Component
public class FiltroAutenticacao extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroAutenticacao.class);
    private static final String PREFIXO = "Bearer ";

    private final ServicoDeToken tokens;
    private final BlocklistDeToken blocklist;

    public FiltroAutenticacao(ServicoDeToken tokens, BlocklistDeToken blocklist) {
        this.tokens = tokens;
        this.blocklist = blocklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        UUID correlacao = FiltroCorrelacao.daRequisicao(req);
        ContextoRequisicao contexto = ContextoRequisicao.anonimo(correlacao);

        String cabecalho = req.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            String token = cabecalho.substring(PREFIXO.length()).trim();
            try {
                ServicoDeToken.AccessLido lido = tokens.lerAccess(token, correlacao);
                // Logout e troca de senha revogam antes da expiração natural.
                // A blocklist só precisa cobrir a janela até o exp do token.
                contexto = blocklist.revogado(lido.jti())
                        ? ContextoRequisicao.anonimo(correlacao)
                        : lido.contexto();
            } catch (JwtException e) {
                // Nunca logar o token. O motivo basta para diagnosticar.
                log.debug("Token rejeitado: {}", e.getMessage());
            }
        }

        ContextoAtual.definir(contexto);
        popularSpringSecurity(contexto);
        try {
            chain.doFilter(req, res);
        } finally {
            ContextoAtual.limpar();
            SecurityContextHolder.clearContext();
        }
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
