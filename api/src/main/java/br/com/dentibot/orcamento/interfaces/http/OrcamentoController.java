package br.com.dentibot.orcamento.interfaces.http;

import br.com.dentibot.orcamento.NovoItem;
import br.com.dentibot.orcamento.NovoOrcamento;
import br.com.dentibot.orcamento.OrcamentoApi;
import br.com.dentibot.orcamento.OrcamentoDetalhado;
import br.com.dentibot.orcamento.OrcamentoResumo;
import br.com.dentibot.orcamento.application.OrcamentoServico.TransicaoInvalidaException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Orçamento e plano de tratamento.
 *
 * <p>{@code POST /orcamentos} exige {@code Idempotency-Key} — está na lista do
 * {@code FiltroIdempotencia}. Dois cliques em "criar orçamento" não podem virar
 * dois planos de tratamento para o mesmo paciente.
 */
@RestController
@RequestMapping("/api/v1/orcamentos")
public class OrcamentoController {

    private final OrcamentoApi orcamentos;

    public OrcamentoController(OrcamentoApi orcamentos) {
        this.orcamentos = orcamentos;
    }

    @GetMapping
    public List<OrcamentoResumo> listar(@RequestParam(required = false) Long idPaciente,
                                        @RequestParam(required = false) String status) {
        return orcamentos.listar(idPaciente,
                status == null || status.isBlank() ? null : status);
    }

    @GetMapping("/{id}")
    public OrcamentoDetalhado detalhar(@PathVariable long id) {
        return orcamentos.detalhar(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> criar(@Valid @RequestBody NovoOrcamento novo) {
        long id = orcamentos.criar(novo);
        return ResponseEntity.created(URI.create("/api/v1/orcamentos/" + id))
                .body(Map.of("idOrcamento", id));
    }

    @PostMapping("/{id}/itens")
    public ResponseEntity<Map<String, Long>> adicionarItem(@PathVariable long id,
                                                           @Valid @RequestBody NovoItem item) {
        long idItem = orcamentos.adicionarItem(id, item);
        return ResponseEntity.created(URI.create("/api/v1/orcamentos/" + id + "/itens/" + idItem))
                .body(Map.of("idItem", idItem));
    }

    @DeleteMapping("/{id}/itens/{idItem}")
    public ResponseEntity<Void> removerItem(@PathVariable long id, @PathVariable long idItem) {
        orcamentos.removerItem(id, idItem);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enviar")
    public ResponseEntity<Void> enviar(@PathVariable long id) {
        orcamentos.enviar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<Void> aprovar(@PathVariable long id) {
        orcamentos.aprovar(id);
        return ResponseEntity.noContent().build();
    }

    public record PedidoRecusa(@NotBlank @Size(max = 500) String motivo) {
    }

    @PostMapping("/{id}/recusar")
    public ResponseEntity<Void> recusar(@PathVariable long id,
                                        @Valid @RequestBody PedidoRecusa pedido) {
        orcamentos.recusar(id, pedido.motivo());
        return ResponseEntity.noContent().build();
    }

    public record PedidoConclusao(Long idConsulta) {
    }

    @PostMapping("/{id}/itens/{idItem}/concluir")
    public ResponseEntity<Void> concluirItem(@PathVariable long id, @PathVariable long idItem,
                                             @RequestBody(required = false) PedidoConclusao pedido) {
        orcamentos.concluirItem(id, idItem, pedido == null ? null : pedido.idConsulta());
        return ResponseEntity.noContent().build();
    }

    /**
     * 409 e não 404: o orçamento existe, o estado é que não permite a transição.
     * Um 404 aqui mandaria o cliente procurar um recurso que ele já tem.
     */
    @ExceptionHandler(TransicaoInvalidaException.class)
    public ProblemDetail transicaoInvalida(TransicaoInvalidaException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setType(URI.create("https://dentibot.com.br/erros/transicao-invalida"));
        return p;
    }
}
