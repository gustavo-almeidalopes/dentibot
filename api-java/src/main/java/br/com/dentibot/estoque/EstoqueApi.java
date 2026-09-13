package br.com.dentibot.estoque;

import java.util.List;

/**
 * Porta pública do módulo estoque.
 *
 * <p>Não existe "dar baixa alterando a quantidade": posição é SUM sobre
 * movimentações append-only, como o ledger financeiro. Por isso a porta só
 * oferece registrar movimentação e consultar posição — não há setter de saldo,
 * porque não há saldo armazenado que possa discordar do extrato.
 */
public interface EstoqueApi {

    List<PosicaoProduto> posicao(boolean somenteAbaixoDoPontoPedido);

    List<ProdutoResumo> listarProdutos();

    long criarProduto(NovoProduto novo);

    List<FornecedorResumo> listarFornecedores();

    long criarFornecedor(NovoFornecedor novo);

    List<MovimentacaoResumo> extrato(long idProduto, int limite);

    /** Lotes que vencem dentro da janela e ainda têm saldo. */
    List<LoteVencendo> lotesVencendo(int emDias);

    long movimentar(NovaMovimentacao nova);
}
