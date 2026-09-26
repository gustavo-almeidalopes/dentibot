package br.com.dentibot.plataforma.erro;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import br.com.dentibot.plataforma.tenant.GuardaDeTransacao.AcessoForaDeTransacaoException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tradução de exceção para resposta, em RFC 7807 (ProblemDetail).
 *
 * <p>Regra que vale para todo handler daqui: <b>a mensagem do banco nunca chega
 * ao cliente</b>. Um erro de constraint carrega nome de tabela, nome de coluna e
 * às vezes o valor que causou o conflito — num sistema de saúde, esse "valor"
 * pode ser um CPF. O detalhe técnico vai para o log, correlacionado; o cliente
 * recebe o que precisa para corrigir a requisição.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);
    private static final URI TIPO_BASE = URI.create("https://dentibot.com.br/erros/");

    @ExceptionHandler(AcessoNegadoException.class)
    public ProblemDetail negado(AcessoNegadoException e) {
        log.info("Acesso negado: {} em {}", e.acao(), e.recurso());
        return problema(HttpStatus.FORBIDDEN, "acesso-negado",
                "Seu perfil não permite esta operação.");
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ProblemDetail indisponivel(ServicoIndisponivelException e) {
        // A frase diz qual variável falta — é para quem opera, e não cita dado de ninguém.
        log.warn("Serviço indisponível: {}", e.getMessage());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "servico-indisponivel", e.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado.");
    }

    /**
     * Override, e não {@code @ExceptionHandler}: a classe base já mapeia esta
     * exceção, e mapear de novo aqui é ambíguo — o contexto não sobe.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "requisicao-invalida",
                "Há campos inválidos na requisição.");
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.put(f.getField(), f.getDefaultMessage()));
        // Nome do campo e a regra violada; nunca o valor recebido — é ele que
        // costuma ser o dado pessoal.
        p.setProperty("campos", campos);
        return ResponseEntity.badRequest().headers(headers).body(p);
    }

    /**
     * O que o Spring MVC lança antes de chegar ao controller: JSON malformado,
     * parâmetro com tipo errado ou ausente, método ou mídia que a rota não
     * aceita, rota inexistente. Antes caía em {@link #inesperado} e virava 500
     * com {@code log.error} — erro do cliente disparando o alerta do servidor.
     *
     * <p>A classe base escolhe o status; aqui só se troca o corpo pelo da casa.
     * O detalhe do Spring é descartado de propósito: o de tipo errado cita o
     * valor recebido, e esse valor pode ser um CPF.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        ProblemDetail p;
        if (status.is5xxServerError()) {
            log.error("Erro do framework", e);
            p = problema(status, "erro-interno", "Erro interno.");
        } else if (status.value() == HttpStatus.NOT_FOUND.value()) {
            p = problema(status, "nao-encontrado", "Recurso não encontrado.");
        } else {
            p = problema(status, "requisicao-invalida",
                    "A requisição não pôde ser atendida: confira o método, os parâmetros e o corpo.");
        }
        return ResponseEntity.status(status).headers(headers).body(p);
    }

    /**
     * Regra de negócio que o Bean Validation não expressa: "dentista exige CRO",
     * "a clínica não pode ficar sem admin". São 400 e não 500 — o cliente
     * corrige a requisição e tenta de novo.
     *
     * <p>A mensagem chega ao cliente, e por isso quem lança é responsável por
     * ela não conter dado pessoal. As de hoje falam de regra, não de valor.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail regraDeNegocio(IllegalArgumentException e) {
        return problema(HttpStatus.BAD_REQUEST, "requisicao-invalida", e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail duplicado(DuplicateKeyException e) {
        log.info("Violação de unicidade", e);
        return problema(HttpStatus.CONFLICT, "registro-duplicado",
                "Já existe um registro com estes dados.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException e) {
        String causa = e.getMostSpecificCause().getMessage();
        log.warn("Violação de integridade", e);

        if (causa != null && causa.contains("ex_dentista_sem_sobreposicao")) {
            return problema(HttpStatus.CONFLICT, "horario-ocupado",
                    "O dentista já tem uma consulta neste horário.");
        }
        return problema(HttpStatus.CONFLICT, "conflito-de-dados",
                "A operação viola uma regra de integridade dos dados.");
    }

    /**
     * SQLState 42501 (insufficient_privilege): o banco recusou a escrita.
     *
     * <p>Chega como {@link BadSqlGrammarException}, não como violação de
     * integridade: o tradutor do Spring classifica pela classe "42", a de
     * gramática. Por isso os ramos de RLS e append-only que moravam em
     * {@link #integridade} nunca rodavam, e as duas recusas respondiam 500.
     *
     * <p>O mesmo SQLState cobre as duas: o RLS e o trigger append-only, que o
     * levanta de propósito. O trigger se distingue pela mensagem, que é nossa
     * ({@code RAISE}) e não muda com o {@code lc_messages} do servidor.
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public ProblemDetail privilegioNoBanco(BadSqlGrammarException e) {
        if (!"42501".equals(e.getSQLException().getSQLState())) {
            return inesperado(e);
        }
        log.warn("Escrita recusada pelo banco", e);
        String causa = e.getSQLException().getMessage();
        if (causa != null && causa.contains("append-only")) {
            return problema(HttpStatus.FORBIDDEN, "registro-imutavel",
                    "Este registro não pode ser alterado nem excluído.");
        }
        // Tentativa de alcançar outro tenant. Responder 404 e não 403 evita
        // confirmar que o recurso existe em alguma outra clínica.
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado.");
    }

    @ExceptionHandler(AcessoForaDeTransacaoException.class)
    public ProblemDetail foraDeTransacao(AcessoForaDeTransacaoException e) {
        // Erro de programação: 500 mesmo, e alto no log, porque significa que um
        // endpoint estaria devolvendo lista vazia em produção.
        log.error("Repositório chamado fora de transação — falta @Transactional", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno",
                "Erro interno.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception e) {
        log.error("Erro não tratado", e);
        // ST-28. Ids e não nomes: o Sentry precisa agrupar por clínica e achar
        // a linha de log, não saber quem é a pessoa. Sem DSN, é no-op.
        ContextoRequisicao ctx = ContextoAtual.obter();
        Sentry.withScope(escopo -> {
            if (ctx.clinicaId() != null) {
                escopo.setTag("clinica", ctx.clinicaId().toString());
            }
            if (ctx.usuarioId() != null) {
                User usuario = new User();
                usuario.setId(ctx.usuarioId().toString());
                escopo.setUser(usuario);
            }
            if (ctx.correlacaoId() != null) {
                escopo.setTag("correlacaoId", ctx.correlacaoId().toString());
            }
            Sentry.captureException(e);
        });
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno.");
    }

    private ProblemDetail problema(HttpStatusCode status, String tipo, String detalhe) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalhe);
        p.setType(TIPO_BASE.resolve(tipo));
        // O correlation-id na resposta é o que permite ao suporte achar a linha
        // de log exata sem pedir print de tela.
        if (ContextoAtual.correlacao() != null) {
            p.setProperty("correlacaoId", ContextoAtual.correlacao().toString());
        }
        return p;
    }
}
