package br.com.dentibot.identidade.interfaces.http;

import br.com.dentibot.identidade.application.AutenticacaoServico;
import br.com.dentibot.identidade.application.AutenticacaoServico.CredenciaisInvalidasException;
import br.com.dentibot.identidade.application.AutenticacaoServico.ParDeTokens;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticação.
 *
 * <p>O access token volta no CORPO e é guardado só em memória pelo SPA. O
 * refresh volta em cookie {@code HttpOnly; Secure; SameSite=Lax}, que JavaScript
 * não lê.
 *
 * <p>Por que não {@code localStorage}: qualquer XSS na aplicação lê
 * {@code localStorage} inteiro e leva a sessão junto. Com o refresh em cookie
 * HttpOnly, um XSS consegue no máximo usar o access token pelos minutos que
 * faltam para ele expirar — e não consegue renovar sozinho para sempre.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {

    private static final String COOKIE_REFRESH = "dentibot_refresh";
    /** Escopo estreito: o cookie só é enviado para os endpoints que o usam. */
    private static final String CAMINHO_COOKIE = "/api/v1/auth";

    private final AutenticacaoServico autenticacao;
    private final boolean cookieSeguro;

    public AutenticacaoController(
            AutenticacaoServico autenticacao,
            @org.springframework.beans.factory.annotation.Value("${dentibot.cookie-seguro:true}")
            boolean cookieSeguro) {
        this.autenticacao = autenticacao;
        this.cookieSeguro = cookieSeguro;
    }

    public record PedidoLogin(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 200) String senha) {
    }

    public record RespostaLogin(String accessToken, Instant expiraEm, String tokenType) {
    }

    @PostMapping("/login")
    public ResponseEntity<RespostaLogin> login(@Valid @RequestBody PedidoLogin pedido,
                                               HttpServletRequest req) {
        ParDeTokens par = autenticacao.autenticar(new AutenticacaoServico.Credenciais(
                pedido.email(), pedido.senha(), req.getRemoteAddr(),
                req.getHeader(HttpHeaders.USER_AGENT)));
        return respostaComCookie(par);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RespostaLogin> refresh(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refresh,
            HttpServletRequest req) {
        if (refresh == null || refresh.isBlank()) {
            throw new CredenciaisInvalidasException();
        }
        ParDeTokens par = autenticacao.renovar(refresh, req.getRemoteAddr(),
                req.getHeader(HttpHeaders.USER_AGENT));
        return respostaComCookie(par);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ContextoRequisicao ctx = ContextoAtual.obter();
        if (ctx.usuarioId() != null) {
            // Revoga as sessões do usuário. O access token corrente expira
            // sozinho em minutos; a blocklist cobre a janela quando o jti é
            // conhecido.
            autenticacao.sairDeTodasAsSessoes(ctx.usuarioId());
        }
        // Cookie com Max-Age zero: manda o navegador apagar.
        ResponseCookie limpar = ResponseCookie.from(COOKIE_REFRESH, "")
                .httpOnly(true).secure(cookieSeguro).sameSite("Lax")
                .path(CAMINHO_COOKIE).maxAge(0).build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, limpar.toString())
                .build();
    }

    public record Identidade(Long usuarioId, Long clinicaId, String papel, String staffPapel) {
    }

    @GetMapping("/me")
    public Identidade me() {
        ContextoRequisicao ctx = ContextoAtual.obter();
        if (!ctx.autenticado()) {
            throw new CredenciaisInvalidasException();
        }
        return new Identidade(
                ctx.usuarioId(), ctx.clinicaId(),
                ctx.papel() == null ? null : ctx.papel().valorBanco(),
                ctx.staffPapel() == null ? null : ctx.staffPapel().valorBanco());
    }

    private ResponseEntity<RespostaLogin> respostaComCookie(ParDeTokens par) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_REFRESH, par.refresh())
                .httpOnly(true)
                .secure(cookieSeguro)
                // Lax e não None: bloqueia o POST cross-site, que é o que
                // substitui o token CSRF neste desenho sem sessão de servidor.
                .sameSite("Lax")
                .path(CAMINHO_COOKIE)
                .maxAge(Duration.ofDays(14))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new RespostaLogin(par.access(), par.accessExpiraEm(), "Bearer"));
    }

    /**
     * Uma resposta só para qualquer falha de credencial. Sem distinguir
     * "usuário não existe" de "senha errada" de "conta bloqueada" — as três
     * confirmariam a existência da conta a quem está testando.
     */
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<org.springframework.http.ProblemDetail> invalidas() {
        var problema = org.springframework.http.ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        problema.setType(java.net.URI.create("https://dentibot.com.br/erros/credenciais-invalidas"));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema);
    }
}
