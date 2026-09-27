package br.com.dentibot.plataforma.correlacao;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Primeiro filtro depois do CORS. Dá um identificador à requisição e o mantém
 * no MDC, de onde ele entra em toda linha de log.
 *
 * <p>O mesmo id segue para o evento no outbox, de lá para o worker e para a
 * linha de auditoria. É o que transforma "sumiu a agenda de ontem" numa consulta
 * em vez de um chute.
 *
 * <p>O id do cliente é aceito, mas validado: um cabeçalho de correlação vindo de
 * fora é entrada de usuário como qualquer outra, e entra em log — que é
 * exatamente o vetor de log injection se for ecoado sem validar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroCorrelacao extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Correlation-Id";
    public static final String MDC_CHAVE = "correlacaoId";
    private static final String ATRIBUTO = "dentibot.correlacao";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        UUID correlacao = extrairOuGerar(req.getHeader(CABECALHO));
        req.setAttribute(ATRIBUTO, correlacao);
        res.setHeader(CABECALHO, correlacao.toString());
        MDC.put(MDC_CHAVE, correlacao.toString());
        try {
            chain.doFilter(req, res);
        } finally {
            // Thread de pool: sem o remove, a próxima requisição herda o id da
            // anterior e o rastro fica inutilizável justamente quando importa.
            MDC.remove(MDC_CHAVE);
        }
    }

    public static UUID daRequisicao(HttpServletRequest req) {
        Object valor = req.getAttribute(ATRIBUTO);
        return valor instanceof UUID uuid ? uuid : UUID.randomUUID();
    }

    private static UUID extrairOuGerar(String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(cabecalho);
        } catch (IllegalArgumentException e) {
            // Não é UUID: descarta em silêncio e gera um. Rejeitar a requisição
            // por causa disso seria transformar um detalhe de observabilidade em
            // indisponibilidade.
            return UUID.randomUUID();
        }
    }
}
