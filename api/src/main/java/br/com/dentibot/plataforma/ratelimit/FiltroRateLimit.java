package br.com.dentibot.plataforma.ratelimit;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Aplica o limite por identidade, depois da autenticação — a ordem importa:
 * antes dela não se sabe de qual clínica é a requisição, e limitar por IP
 * colocaria a clínica inteira atrás de um NAT no mesmo balde.
 *
 * <p>{@code /api/v1/auth/*} é a exceção: ali ainda não há identidade, o limite é
 * por IP, e é bem mais apertado — é o endpoint onde se tenta senha.
 *
 * <p>Os limites são configuração, não constante: em desenvolvimento roda-se o
 * fluxo de cadastro e login dezenas de vezes seguidas, e um número fixo no
 * código forçaria ou a recompilar ou a desligar o filtro — e filtro de segurança
 * desligado em dev tem a mania de continuar desligado.
 */
@Component
public class FiltroRateLimit extends OncePerRequestFilter {

    private final Limitador limitador;
    private final int limiteAuth;
    private final int limiteGeral;
    private final Duration janela;

    public FiltroRateLimit(
            Limitador limitador,
            @Value("${dentibot.ratelimit.auth-por-minuto:10}") int limiteAuth,
            @Value("${dentibot.ratelimit.geral-por-minuto:300}") int limiteGeral,
            @Value("${dentibot.ratelimit.janela:PT1M}") Duration janela) {
        this.limitador = limitador;
        this.limiteAuth = limiteAuth;
        this.limiteGeral = limiteGeral;
        this.janela = janela;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String caminho = req.getRequestURI();
        Limitador.Resultado resultado = caminho.startsWith("/api/v1/auth/")
                ? limitador.consumir("rate:auth:" + ipDe(req), limiteAuth, janela)
                : limitador.consumir(chaveDeIdentidade(req), limiteGeral, janela);

        res.setHeader("X-RateLimit-Remaining", String.valueOf(resultado.restante()));

        if (!resultado.permitido()) {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("Retry-After", String.valueOf(resultado.esperar().toSeconds()));
            res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("""
                    {"type":"https://dentibot.com.br/erros/limite-excedido",\
                    "title":"Too Many Requests","status":429,\
                    "detail":"Muitas requisições. Tente novamente em instantes."}""");
            return;
        }
        chain.doFilter(req, res);
    }

    private String chaveDeIdentidade(HttpServletRequest req) {
        ContextoRequisicao ctx = ContextoAtual.obter();
        if (ctx.clinicaId() != null) {
            return "rate:clinica:" + ctx.clinicaId() + ":usuario:" + ctx.usuarioId();
        }
        if (ctx.staffId() != null) {
            return "rate:staff:" + ctx.staffId();
        }
        return "rate:ip:" + ipDe(req);
    }

    /**
     * Atrás do Cloudflare, {@code getRemoteAddr()} devolveria o IP do proxy. O
     * {@code forward-headers-strategy: framework} do application.yml faz o Spring
     * tratar X-Forwarded-For, e só então getRemoteAddr é o IP do cliente. Ler o
     * cabeçalho na mão aqui seria confiar em algo que qualquer cliente forja
     * quando a requisição NÃO vem do proxy.
     */
    private String ipDe(HttpServletRequest req) {
        return req.getRemoteAddr();
    }
}
