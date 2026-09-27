package br.com.dentibot.comunicacao.interfaces.http;

import br.com.dentibot.comunicacao.Espera;
import br.com.dentibot.comunicacao.application.ComunicacaoServico;
import br.com.dentibot.comunicacao.application.ComunicacaoServico.ConfiguracaoDoCanal;
import br.com.dentibot.comunicacao.application.ComunicacaoServico.Item;
import br.com.dentibot.comunicacao.application.ComunicacaoServico.Tema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comunicacao")
public class ComunicacaoController {

    private final ComunicacaoServico servico;

    public ComunicacaoController(ComunicacaoServico servico) {
        this.servico = servico;
    }

    @GetMapping("/escaladas")
    public List<Item> escaladas() {
        return servico.escaladas();
    }

    @PostMapping("/mensagens/{id}/resolucao")
    public ResponseEntity<Void> resolver(@PathVariable long id) {
        servico.resolver(id);
        return ResponseEntity.noContent().build();
    }

    public record Resposta(@NotBlank @Size(max = 1000) String texto) {
    }

    @PostMapping("/mensagens/{id}/respostas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> responder(@PathVariable long id, @Valid @RequestBody Resposta r) {
        return Map.of("idMensagem", servico.responder(id, r.texto()));
    }

    @GetMapping("/pacientes/{idPaciente}/mensagens")
    public List<Item> doPaciente(@PathVariable long idPaciente) {
        return servico.doPaciente(idPaciente);
    }

    // ─── Lista de espera ─────────────────────────────────────────────────────

    @GetMapping("/espera")
    public List<Espera> espera() {
        return servico.listaDeEspera();
    }

    public record NovaEspera(@NotNull Long idPaciente, Long idDentista,
                             @NotBlank String periodo, @Min(1) @Max(3) int urgencia,
                             @Size(max = 200) String observacao) {
    }

    @PostMapping("/espera")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> entrar(@Valid @RequestBody NovaEspera nova) {
        return Map.of("idEspera", servico.entrarNaEspera(nova.idPaciente(), nova.idDentista(),
                nova.periodo(), nova.urgencia(), nova.observacao()));
    }

    @DeleteMapping("/espera/{id}")
    public ResponseEntity<Void> sair(@PathVariable long id) {
        servico.sairDaEspera(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Canal ───────────────────────────────────────────────────────────────

    @GetMapping("/canal")
    public ConfiguracaoDoCanal canal() {
        return servico.canal();
    }

    public record NovoCanal(@NotBlank @Pattern(regexp = "^[0-9]{5,30}$") String phoneNumberId,
                            boolean ativo, @NotNull @Size(max = 10) List<String> tiposDesligados) {
    }

    @PutMapping("/canal")
    public ConfiguracaoDoCanal salvarCanal(@Valid @RequestBody NovoCanal novo) {
        return servico.salvarCanal(novo.phoneNumberId(), novo.ativo(), novo.tiposDesligados());
    }

    // ─── Orientação (IA-38) ──────────────────────────────────────────────────

    @GetMapping("/orientacoes")
    public List<Tema> temas() {
        return servico.temas();
    }

    public record NovaOrientacao(@NotNull Long idPaciente, @NotBlank String tema) {
    }

    @PostMapping("/orientacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> orientar(@Valid @RequestBody NovaOrientacao nova) {
        return Map.of("idMensagem", servico.enviarOrientacao(nova.idPaciente(), nova.tema()));
    }
}
