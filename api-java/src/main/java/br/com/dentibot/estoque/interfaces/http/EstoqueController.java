package br.com.dentibot.estoque.interfaces.http;

import br.com.dentibot.estoque.EstoqueApi;
import br.com.dentibot.estoque.FornecedorResumo;
import br.com.dentibot.estoque.LoteVencendo;
import br.com.dentibot.estoque.MovimentacaoResumo;
import br.com.dentibot.estoque.NovaMovimentacao;
import br.com.dentibot.estoque.NovoFornecedor;
import br.com.dentibot.estoque.NovoProduto;
import br.com.dentibot.estoque.PosicaoProduto;
import br.com.dentibot.estoque.ProdutoResumo;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Estoque: posição, produtos, fornecedores e movimentação. */
@RestController
@RequestMapping("/api/v1/estoque")
public class EstoqueController {

    private final EstoqueApi estoque;

    public EstoqueController(EstoqueApi estoque) {
        this.estoque = estoque;
    }

    @GetMapping("/posicao")
    public List<PosicaoProduto> posicao(
            @RequestParam(defaultValue = "false") boolean somenteAbaixoDoPontoPedido) {
        return estoque.posicao(somenteAbaixoDoPontoPedido);
    }

    @GetMapping("/produtos")
    public List<ProdutoResumo> produtos() {
        return estoque.listarProdutos();
    }

    @PostMapping("/produtos")
    public ResponseEntity<Map<String, Long>> criarProduto(@Valid @RequestBody NovoProduto novo) {
        long id = estoque.criarProduto(novo);
        return ResponseEntity.created(URI.create("/api/v1/estoque/produtos/" + id))
                .body(Map.of("idProduto", id));
    }

    @GetMapping("/fornecedores")
    public List<FornecedorResumo> fornecedores() {
        return estoque.listarFornecedores();
    }

    @PostMapping("/fornecedores")
    public ResponseEntity<Map<String, Long>> criarFornecedor(
            @Valid @RequestBody NovoFornecedor novo) {
        long id = estoque.criarFornecedor(novo);
        return ResponseEntity.created(URI.create("/api/v1/estoque/fornecedores/" + id))
                .body(Map.of("idFornecedor", id));
    }

    @GetMapping("/produtos/{idProduto}/movimentacoes")
    public List<MovimentacaoResumo> extrato(@PathVariable long idProduto,
                                            @RequestParam(defaultValue = "100") int limite) {
        return estoque.extrato(idProduto, Math.min(Math.max(limite, 1), 200));
    }

    /** Lotes com saldo que vencem dentro da janela. Alimenta o aviso de reposição. */
    @GetMapping("/vencimentos")
    public List<LoteVencendo> vencimentos(@RequestParam(defaultValue = "60") int emDias) {
        return estoque.lotesVencendo(Math.min(Math.max(emDias, 1), 365));
    }

    @PostMapping("/movimentacoes")
    public ResponseEntity<Map<String, Long>> movimentar(
            @Valid @RequestBody NovaMovimentacao nova) {
        long id = estoque.movimentar(nova);
        return ResponseEntity.created(URI.create("/api/v1/estoque/movimentacoes/" + id))
                .body(Map.of("idMovimentacao", id));
    }
}
