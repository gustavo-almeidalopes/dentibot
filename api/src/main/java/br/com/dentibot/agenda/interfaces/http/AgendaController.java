package br.com.dentibot.agenda.interfaces.http;

import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.ConsultaResumo;
import br.com.dentibot.agenda.NovaConsulta;
import br.com.dentibot.agenda.application.AgendaServico.TransicaoInvalidaException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agenda.
 *
 * <p>{@code POST /consultas} exige {@code Idempotency-Key} — está na lista do
 * {@code FiltroIdempotencia}. Dois toques no botão de agendar não podem virar
 * duas consultas.
 */
@RestController
@RequestMapping("/api/v1/consultas")
public class AgendaController {

    private final AgendaApi agenda;

    public AgendaController(AgendaApi agenda) {
        this.agenda = agenda;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> agendar(@Valid @RequestBody NovaConsulta nova) {
        long id = agenda.agendar(nova);
        return ResponseEntity.created(URI.create("/api/v1/consultas/" + id))
                .body(Map.of("idConsulta", id));
    }

    /**
     * @param idDentista ignorado quando o papel do solicitante só alcança a
     *                   própria agenda — o serviço impõe o filtro.
     */
    @GetMapping
    public List<ConsultaResumo> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) Long idDentista) {
        return agenda.listar(de, ate, idDentista);
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable long id) {
        agenda.confirmar(id);
        return ResponseEntity.noContent().build();
    }

    public record PedidoCancelamento(@NotBlank @Size(max = 255) String motivo) {
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable long id,
                                         @Valid @RequestBody PedidoCancelamento pedido) {
        agenda.cancelar(id, pedido.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/falta")
    public ResponseEntity<Void> falta(@PathVariable long id) {
        agenda.registrarFalta(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable long id) {
        agenda.concluir(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 409 e não 404: a consulta existe, o estado é que não permite a transição.
     * Um 404 aqui mandaria o cliente procurar um recurso que ele já tem.
     */
    @ExceptionHandler(TransicaoInvalidaException.class)
    public ProblemDetail transicaoInvalida(TransicaoInvalidaException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setType(URI.create("https://dentibot.com.br/erros/transicao-invalida"));
        return p;
    }
}
