package br.com.dentibot.pacientes.interfaces.http;

import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacienteResumo;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.config.Listagem;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pacientes")
public class PacienteController {

    private final PacientesApi pacientes;

    public PacienteController(PacientesApi pacientes) {
        this.pacientes = pacientes;
    }

    @GetMapping
    public List<PacienteResumo> listar(
            @RequestParam(defaultValue = "50") int limite,
            @RequestParam(defaultValue = "0") long apos) {
        return pacientes.listarResumos(Listagem.limite(limite), apos);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> criar(@Valid @RequestBody NovoPaciente novo) {
        long id = pacientes.criar(novo);
        return ResponseEntity.created(URI.create("/api/v1/pacientes/" + id))
                .body(Map.of("idPaciente", id));
    }
}
