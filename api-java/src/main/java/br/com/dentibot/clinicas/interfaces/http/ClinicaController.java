package br.com.dentibot.clinicas.interfaces.http;

import br.com.dentibot.clinicas.NovoProcedimento;
import br.com.dentibot.clinicas.ProcedimentoResumo;
import br.com.dentibot.clinicas.application.ClinicaServico;
import br.com.dentibot.clinicas.domain.Clinica;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dados da clínica e tabela de procedimentos. */
@RestController
@RequestMapping("/api/v1/clinica")
public class ClinicaController {

    private final ClinicaServico clinicas;

    public ClinicaController(ClinicaServico clinicas) {
        this.clinicas = clinicas;
    }

    /** A clínica do token. Não recebe id: o tenant vem do contexto (invariante 5). */
    @GetMapping
    public Clinica atual() {
        return clinicas.clinicaAtual();
    }

    @GetMapping("/procedimentos")
    public List<ProcedimentoResumo> procedimentos(
            @RequestParam(defaultValue = "true") boolean somenteAtivos) {
        return clinicas.listarProcedimentos(somenteAtivos);
    }

    @PostMapping("/procedimentos")
    public ResponseEntity<Map<String, Long>> criar(@Valid @RequestBody NovoProcedimento novo) {
        long id = clinicas.criarProcedimento(novo);
        return ResponseEntity.created(URI.create("/api/v1/clinica/procedimentos/" + id))
                .body(Map.of("idProcedimento", id));
    }

    public record PedidoAtualizacao(@Valid NovoProcedimento dados, boolean ativo) {
    }

    @PutMapping("/procedimentos/{id}")
    public ResponseEntity<Void> atualizar(@PathVariable long id,
                                          @Valid @RequestBody PedidoAtualizacao pedido) {
        clinicas.atualizarProcedimento(id, pedido.dados(), pedido.ativo());
        return ResponseEntity.noContent().build();
    }
}
