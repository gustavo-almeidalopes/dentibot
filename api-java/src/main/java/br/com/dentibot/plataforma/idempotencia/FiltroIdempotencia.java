package br.com.dentibot.plataforma.idempotencia;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * {@code Idempotency-Key} nos endpoints que criam dinheiro, consulta ou mensagem.
 *
 * <p>O caso real: o dedo escorrega, o celular manda duas vezes, e o paciente
 * recebe duas cobranças ou dois agendamentos. A segunda chamada com a mesma
 * chave e o mesmo corpo devolve a PRIMEIRA resposta — não um erro, porque do
 * ponto de vista do cliente a operação deu certo e ele só não ouviu a resposta.
 *
 * <p>Mesma chave com corpo diferente é 422: é bug do cliente, e devolver 200 com
 * o resultado de outro pedido seria bem pior que recusar.
 */
@Component
public class FiltroIdempotencia extends OncePerRequestFilter {

    public static final String CABECALHO = "Idempotency-Key";

    /** Caminhos onde a chave é OBRIGATÓRIA. */
    private static final Set<String> PREFIXOS_OBRIGATORIOS = Set.of(
            "/api/v1/consultas",
            "/api/v1/cobrancas",
            "/api/v1/mensagens",
            "/api/v1/orcamentos");

    private final ArmazemDeIdempotencia armazem;

    public FiltroIdempotencia(ArmazemDeIdempotencia armazem) {
        this.armazem = armazem;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        if (!"POST".equals(req.getMethod())) {
            return true;
        }
        String caminho = req.getRequestURI();
        return PREFIXOS_OBRIGATORIOS.stream().noneMatch(caminho::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        if (ContextoAtual.obter().clinicaId() == null) {
            // Sem tenant não há como escopar a chave; a autorização já vai barrar.
            chain.doFilter(req, res);
            return;
        }

        String chave = req.getHeader(CABECALHO);
        if (chave == null || chave.isBlank()) {
            responder(res, HttpStatus.BAD_REQUEST, """
                    {"type":"https://dentibot.com.br/erros/idempotency-key-ausente",\
                    "title":"Bad Request","status":400,\
                    "detail":"O cabeçalho Idempotency-Key é obrigatório neste endpoint."}""");
            return;
        }

        // O corpo de uma requisição é stream de passagem única. O wrapper o
        // guarda para que o hash saia aqui e o controller ainda consiga ler.
        RequisicaoComCorpoRelido requisicao = new RequisicaoComCorpoRelido(req);
        String hash = sha256(requisicao.corpo());

        Optional<ArmazemDeIdempotencia.Registro> existente = armazem.buscar(chave);
        if (existente.isPresent()) {
            ArmazemDeIdempotencia.Registro reg = existente.get();
            if (!reg.requestHash().equals(hash)) {
                responder(res, HttpStatus.UNPROCESSABLE_ENTITY, """
                        {"type":"https://dentibot.com.br/erros/idempotency-key-reutilizada",\
                        "title":"Unprocessable Entity","status":422,\
                        "detail":"Esta Idempotency-Key já foi usada com outro conteúdo."}""");
                return;
            }
            if (reg.concluida()) {
                res.setStatus(reg.statusHttp());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                res.setHeader("Idempotency-Replayed", "true");
                res.getWriter().write(reg.resposta() == null ? "" : reg.resposta());
                return;
            }
            // Ainda em andamento: a primeira chamada não terminou. 409 com
            // Retry-After é melhor que processar de novo em paralelo.
            res.setHeader("Retry-After", "2");
            responder(res, HttpStatus.CONFLICT, """
                    {"type":"https://dentibot.com.br/erros/requisicao-em-andamento",\
                    "title":"Conflict","status":409,\
                    "detail":"Uma requisição com esta chave ainda está sendo processada."}""");
            return;
        }

        if (!armazem.reservar(chave, req.getRequestURI(), hash)) {
            // Corrida: outra thread reservou entre o buscar e o reservar.
            res.setHeader("Retry-After", "2");
            responder(res, HttpStatus.CONFLICT, """
                    {"type":"https://dentibot.com.br/erros/requisicao-em-andamento",\
                    "title":"Conflict","status":409,\
                    "detail":"Uma requisição com esta chave ainda está sendo processada."}""");
            return;
        }

        ContentCachingResponseWrapper resposta = new ContentCachingResponseWrapper(res);
        boolean concluiu = false;
        try {
            chain.doFilter(requisicao, resposta);
            int status = resposta.getStatus();
            String corpoResposta = new String(resposta.getContentAsByteArray(), StandardCharsets.UTF_8);
            // Só guarda resultado de sucesso: erro 5xx precisa poder ser
            // reprocessado com a mesma chave depois que a causa for corrigida.
            if (status < 500) {
                armazem.concluir(chave, status, corpoResposta);
                concluiu = true;
            }
            resposta.copyBodyToResponse();
        } finally {
            if (!concluiu) {
                armazem.liberar(chave);
            }
        }
    }

    private void responder(HttpServletResponse res, HttpStatus status, String corpo)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.getWriter().write(corpo);
    }

    private static String sha256(byte[] dados) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(dados));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
