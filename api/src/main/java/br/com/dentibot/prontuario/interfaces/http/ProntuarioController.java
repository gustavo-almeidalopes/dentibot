package br.com.dentibot.prontuario.interfaces.http;

import br.com.dentibot.prontuario.AnexoResumo;
import br.com.dentibot.prontuario.EnvioDeAnexo;
import br.com.dentibot.prontuario.EvolucaoResumo;
import br.com.dentibot.prontuario.LancamentoOdontograma;
import br.com.dentibot.prontuario.NovaEvolucao;
import br.com.dentibot.prontuario.NovoAnexo;
import br.com.dentibot.prontuario.NovoLancamentoOdontograma;
import br.com.dentibot.prontuario.ProntuarioApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prontuário eletrônico.
 *
 * <p>Todo GET aqui grava uma linha de auditoria de LEITURA, na mesma transação.
 * É a exigência da CFO-226/2020 e da LGPD, e a razão de estes endpoints não
 * poderem ser servidos de cache nem de réplica de leitura.
 */
@RestController
@RequestMapping("/api/v1/pacientes/{idPaciente}/prontuario")
public class ProntuarioController {

    private final ProntuarioApi prontuario;

    public ProntuarioController(ProntuarioApi prontuario) {
        this.prontuario = prontuario;
    }

    @GetMapping("/evolucoes")
    public List<EvolucaoResumo> historico(@PathVariable long idPaciente) {
        return prontuario.historico(idPaciente);
    }

    public record PedidoEvolucao(Long idConsulta,
                                 @NotBlank @Size(max = 20000) String descricao) {
    }

    @PostMapping("/evolucoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> registrar(@PathVariable long idPaciente,
                                       @Valid @RequestBody PedidoEvolucao pedido) {
        long id = prontuario.registrarEvolucao(
                new NovaEvolucao(idPaciente, pedido.idConsulta(), pedido.descricao()));
        return Map.of("idEvolucao", id);
    }

    public record PedidoRetificacao(@NotBlank @Size(max = 20000) String descricao,
                                    @NotBlank @Size(max = 500) String motivo) {
    }

    /**
     * POST e não PUT: retificação CRIA um adendo. O registro original continua
     * como está — prontuário se corrige somando (CFO-226).
     */
    @PostMapping("/evolucoes/{idEvolucao}/retificacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> retificar(@PathVariable long idPaciente,
                                       @PathVariable long idEvolucao,
                                       @Valid @RequestBody PedidoRetificacao pedido) {
        long id = prontuario.retificarEvolucao(idEvolucao, pedido.descricao(), pedido.motivo());
        return Map.of("idAdendo", id);
    }

    @GetMapping("/odontograma")
    public List<LancamentoOdontograma> odontograma(@PathVariable long idPaciente) {
        return prontuario.odontograma(idPaciente);
    }

    public record PedidoLancamento(Integer dente, String face, String condicao,
                                   String observacao) {
    }

    @PostMapping("/odontograma")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> lancar(@PathVariable long idPaciente,
                                    @Valid @RequestBody PedidoLancamento pedido) {
        long id = prontuario.lancarOdontograma(new NovoLancamentoOdontograma(
                idPaciente, pedido.dente(), pedido.face(),
                pedido.condicao(), pedido.observacao()));
        return Map.of("idLancamento", id);
    }

    // ─── anexos (ST-41) ──────────────────────────────────────────────────────

    /** Passo 1: devolve para onde o navegador manda os bytes. */
    @PostMapping("/anexos")
    @ResponseStatus(HttpStatus.CREATED)
    public EnvioDeAnexo iniciarAnexo(@PathVariable long idPaciente, @Valid @RequestBody NovoAnexo novo) {
        return prontuario.iniciarAnexo(idPaciente, novo);
    }

    public record PedidoConfirmacao(@NotBlank @Size(max = 500) String chave, @Valid NovoAnexo anexo) {
    }

    /** Passo 2: confere no bucket e registra no prontuário. */
    @PostMapping("/anexos/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> confirmarAnexo(@PathVariable long idPaciente,
                                           @Valid @RequestBody PedidoConfirmacao pedido) {
        return Map.of("idAnexo", prontuario.confirmarAnexo(idPaciente, pedido.chave(), pedido.anexo()));
    }

    @GetMapping("/anexos")
    public List<AnexoResumo> anexos(@PathVariable long idPaciente) {
        return prontuario.anexos(idPaciente);
    }

    @GetMapping("/anexos/{idAnexo}/url")
    public Map<String, String> urlDoAnexo(@PathVariable long idPaciente, @PathVariable long idAnexo) {
        return Map.of("url", prontuario.urlDoAnexo(idPaciente, idAnexo));
    }
}
