package br.com.dentibot.pacientes.interfaces.http;

import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacienteResumo;
import br.com.dentibot.pacientes.PacientesApi;
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

    private static final int LIMITE_MAXIMO = 200;

    private final PacientesApi pacientes;

    public PacienteController(PacientesApi pacientes) {
        this.pacientes = pacientes;
    }

    /**
     * Paginação por keyset ({@code apos}), não por página numerada: com OFFSET o
     * banco lê e descarta as N primeiras linhas a cada requisição, e a página 50
     * custa 50 vezes a primeira.
     */
    @GetMapping
    public List<PacienteResumo> listar(
            @RequestParam(defaultValue = "50") int limite,
            @RequestParam(defaultValue = "0") long apos) {
        return pacientes.listarResumos(Math.min(limite, LIMITE_MAXIMO), apos);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> criar(@Valid @RequestBody NovoPaciente novo) {
        long id = pacientes.criar(novo);
        return ResponseEntity.created(URI.create("/api/v1/pacientes/" + id))
                .body(Map.of("idPaciente", id));
    }
}
