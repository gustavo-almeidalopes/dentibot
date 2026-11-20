package br.com.dentibot.copiloto.interfaces.http;

import br.com.dentibot.copiloto.Exportacao;
import br.com.dentibot.copiloto.Pendencia;
import br.com.dentibot.copiloto.PlanoExplicado;
import br.com.dentibot.copiloto.ResumoDoPaciente;
import br.com.dentibot.copiloto.TratamentoParado;
import br.com.dentibot.copiloto.application.CopilotoServico;
import br.com.dentibot.prontuario.RascunhoDeNota;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CopilotoController {

    /** Cabeçalho com o SHA-256 do arquivo de portabilidade. */
    public static final String CABECALHO_SHA256 = "X-Conteudo-Sha256";

    private final CopilotoServico copiloto;

    public CopilotoController(CopilotoServico copiloto) {
        this.copiloto = copiloto;
    }

    @GetMapping("/pacientes/{idPaciente}/resumo")
    public ResumoDoPaciente resumo(@PathVariable long idPaciente) {
        return copiloto.resumo(idPaciente);
    }

    @GetMapping("/copiloto/tratamentos-parados")
    public List<TratamentoParado> tratamentosParados(@RequestParam(defaultValue = "30") int dias) {
        return copiloto.tratamentosParados(dias);
    }

    @GetMapping("/copiloto/pendencias")
    public List<Pendencia> pendencias(@RequestParam(defaultValue = "30") int dias) {
        return copiloto.pendencias(Math.min(dias, 365));
    }

    public record PedidoDeNota(@NotNull Long idPaciente, @NotBlank @Size(max = 8000) String ditado) {
    }

    /**
     * IA-01: rascunho de evolução a partir do ditado. Sob /copiloto e não sob
     * /pacientes: POST em /pacientes passa pelo armazém de idempotência, que
     * guardaria a resposta — texto clínico — em cache.
     */
    @PostMapping("/copiloto/nota-clinica")
    public RascunhoDeNota notaClinica(@Valid @RequestBody PedidoDeNota pedido) {
        return copiloto.rascunhoDeNota(pedido.idPaciente(), pedido.ditado());
    }

    public record PedidoDePlano(@NotNull Long idOrcamento) {
    }

    /** IA-04: o plano do orçamento em duas linguagens, para o dentista revisar. */
    @PostMapping("/copiloto/plano-explicado")
    public PlanoExplicado planoExplicado(@Valid @RequestBody PedidoDePlano pedido) {
        return copiloto.explicarPlano(pedido.idOrcamento());
    }

    /**
     * GET e não POST: a rota de POST de pacientes exige Idempotency-Key, e o
     * armazém de idempotência guardaria a resposta — o prontuário inteiro do
     * paciente — em cache. Aqui nada fica guardado além da linha de auditoria.
     */
    @GetMapping("/pacientes/{idPaciente}/exportacao")
    public ResponseEntity<byte[]> exportar(@PathVariable long idPaciente) {
        Exportacao e = copiloto.exportar(idPaciente);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(e.nomeArquivo()).build().toString())
                .header(CABECALHO_SHA256, e.sha256())
                .body(e.conteudo());
    }
}
