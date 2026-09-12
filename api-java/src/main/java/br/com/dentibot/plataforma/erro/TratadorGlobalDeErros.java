package br.com.dentibot.plataforma.erro;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import br.com.dentibot.plataforma.tenant.GuardaDeTransacao.AcessoForaDeTransacaoException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
public class TratadorGlobalDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);
    private static final URI TIPO_BASE = URI.create("https://dentibot.com.br/erros/");

    @ExceptionHandler(AcessoNegadoException.class)
    public ProblemDetail negado(AcessoNegadoException e) {
        log.info("Acesso negado: {} em {}", e.acao(), e.recurso());
        return problema(HttpStatus.FORBIDDEN, "acesso-negado",
                "Seu perfil não permite esta operação.");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "requisicao-invalida",
                "Há campos inválidos na requisição.");
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.put(f.getField(), f.getDefaultMessage()));
        // Nome do campo e a regra violada; nunca o valor recebido — é ele que
        // costuma ser o dado pessoal.
        p.setProperty("campos", campos);
        return p;
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

        if (causa != null && causa.contains("row-level security")) {
            // Tentativa de alcançar outro tenant. Responder 404 e não 403 evita
            // confirmar que o recurso existe em alguma outra clínica.
            return problema(HttpStatus.NOT_FOUND, "nao-encontrado",
                    "Recurso não encontrado.");
        }
        if (causa != null && causa.contains("ex_dentista_sem_sobreposicao")) {
            return problema(HttpStatus.CONFLICT, "horario-ocupado",
                    "O dentista já tem uma consulta neste horário.");
        }
        if (causa != null && causa.contains("append-only")) {
            return problema(HttpStatus.FORBIDDEN, "registro-imutavel",
                    "Este registro não pode ser alterado nem excluído.");
        }
        return problema(HttpStatus.CONFLICT, "conflito-de-dados",
                "A operação viola uma regra de integridade dos dados.");
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
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno.");
    }

    private ProblemDetail problema(HttpStatus status, String tipo, String detalhe) {
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
