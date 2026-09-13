package br.com.dentibot.financeiro.interfaces.http;

import br.com.dentibot.financeiro.ComissaoResumo;
import br.com.dentibot.financeiro.DespesaResumo;
import br.com.dentibot.financeiro.FinanceiroApi;
import br.com.dentibot.financeiro.FiltroFinanceiro;
import br.com.dentibot.financeiro.FormaPagamento;
import br.com.dentibot.financeiro.NovaDespesa;
import br.com.dentibot.financeiro.NovaFormaPagamento;
import br.com.dentibot.financeiro.NovaRegraComissao;
import br.com.dentibot.financeiro.NovoLancamento;
import br.com.dentibot.financeiro.NovoRecebivel;
import br.com.dentibot.financeiro.RecebivelDetalhado;
import br.com.dentibot.financeiro.RecebivelResumo;
import br.com.dentibot.financeiro.RegraComissao;
import br.com.dentibot.financeiro.ResumoFinanceiro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Financeiro: recebíveis, ledger, despesas, formas de pagamento e comissões.
 *
 * <p>Não existe {@code PUT /recebiveis/{id}} nem "marcar como pago": o status é
 * derivado do saldo, e o saldo é SUM sobre um ledger append-only. Para desfazer,
 * {@code POST /lancamentos/{id}/estornar}.
 */
@RestController
@RequestMapping("/api/v1/financeiro")
public class FinanceiroController {

    private static final int LIMITE_MAXIMO = 200;

    private final FinanceiroApi financeiro;

    public FinanceiroController(FinanceiroApi financeiro) {
        this.financeiro = financeiro;
    }

    // ─── Painel ──────────────────────────────────────────────────────────────

    @GetMapping("/resumo")
    public ResumoFinanceiro resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate fim = ate == null ? LocalDate.now() : ate;
        LocalDate inicio = de == null ? fim.withDayOfMonth(1) : de;
        return financeiro.resumo(inicio, fim);
    }

    // ─── Recebíveis ──────────────────────────────────────────────────────────

    @GetMapping("/recebiveis")
    public List<RecebivelResumo> recebiveis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long idPaciente,
            @RequestParam(defaultValue = "0") long apos,
            @RequestParam(defaultValue = "100") int limite) {
        return financeiro.listarRecebiveis(new FiltroFinanceiro(
                de, ate, vazioViraNulo(status), idPaciente, apos,
                Math.min(Math.max(limite, 1), LIMITE_MAXIMO)));
    }

    @GetMapping("/recebiveis/{id}")
    public RecebivelDetalhado detalhar(@PathVariable long id) {
        return financeiro.detalharRecebivel(id);
    }

    @PostMapping("/recebiveis")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, List<Long>> abrir(@Valid @RequestBody NovoRecebivel novo) {
        return Map.of("idsRecebivel", financeiro.abrirRecebiveis(novo));
    }

    // ─── Ledger ──────────────────────────────────────────────────────────────

    @PostMapping("/lancamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> lancar(@Valid @RequestBody NovoLancamento novo) {
        return Map.of("idLancamento", financeiro.registrarLancamento(novo));
    }

    public record PedidoEstorno(@NotBlank @Size(max = 280) String motivo) {
    }

    @PostMapping("/lancamentos/{id}/estornar")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> estornar(@PathVariable long id,
                                      @Valid @RequestBody PedidoEstorno pedido) {
        return Map.of("idEstorno", financeiro.estornar(id, pedido.motivo()));
    }

    // ─── Contas a pagar ──────────────────────────────────────────────────────

    @GetMapping("/despesas")
    public List<DespesaResumo> despesas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String status) {
        return financeiro.listarDespesas(de, ate, vazioViraNulo(status));
    }

    @PostMapping("/despesas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> criarDespesa(@Valid @RequestBody NovaDespesa nova) {
        return Map.of("idDespesa", financeiro.criarDespesa(nova));
    }

    public record PedidoPagamento(LocalDate pagoEm) {
    }

    @PostMapping("/despesas/{id}/pagar")
    public ResponseEntity<Void> pagar(@PathVariable long id,
                                      @RequestBody(required = false) PedidoPagamento pedido) {
        financeiro.pagarDespesa(id, pedido == null ? null : pedido.pagoEm());
        return ResponseEntity.noContent().build();
    }

    // ─── Formas de pagamento ─────────────────────────────────────────────────

    @GetMapping("/formas")
    public List<FormaPagamento> formas() {
        return financeiro.listarFormas();
    }

    @PostMapping("/formas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> criarForma(@Valid @RequestBody NovaFormaPagamento nova) {
        return Map.of("idForma", financeiro.criarForma(nova));
    }

    // ─── Comissões ───────────────────────────────────────────────────────────

    @GetMapping("/comissoes")
    public List<ComissaoResumo> comissoes(@RequestParam(required = false) Long idDentista,
                                          @RequestParam(required = false) String status) {
        return financeiro.listarComissoes(idDentista, vazioViraNulo(status));
    }

    @PostMapping("/comissoes/{id}/liberar")
    public ResponseEntity<Void> liberar(@PathVariable long id) {
        financeiro.liberarComissao(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/regras-comissao")
    public List<RegraComissao> regras() {
        return financeiro.listarRegras();
    }

    @PostMapping("/regras-comissao")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> criarRegra(@Valid @RequestBody NovaRegraComissao nova) {
        return Map.of("idRegra", financeiro.criarRegra(nova));
    }

    private static String vazioViraNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
