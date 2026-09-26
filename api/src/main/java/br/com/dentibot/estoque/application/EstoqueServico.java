package br.com.dentibot.estoque.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.estoque.EstoqueApi;
import br.com.dentibot.estoque.FornecedorResumo;
import br.com.dentibot.estoque.LoteVencendo;
import br.com.dentibot.estoque.MovimentacaoResumo;
import br.com.dentibot.estoque.NovaMovimentacao;
import br.com.dentibot.estoque.NovoFornecedor;
import br.com.dentibot.estoque.NovoProduto;
import br.com.dentibot.estoque.PosicaoProduto;
import br.com.dentibot.estoque.ProdutoResumo;
import br.com.dentibot.estoque.infrastructure.EstoqueRepositorio;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoqueServico implements EstoqueApi {

    /** Espelha o CHECK de {@code estoque.movimentacoes.tipo}. */
    private static final Set<String> TIPOS = Set.of("entrada", "saida", "ajuste", "perda", "vencimento");

    /** Os que consomem estoque, e por isso entram no ledger com sinal negativo. */
    private static final Set<String> TIPOS_DE_BAIXA = Set.of("saida", "perda", "vencimento");

    private final EstoqueRepositorio estoque;
    private final AvaliadorDePermissao permissoes;
    private final AuditoriaApi auditoria;

    public EstoqueServico(EstoqueRepositorio estoque, AvaliadorDePermissao permissoes,
                          AuditoriaApi auditoria) {
        this.estoque = estoque;
        this.permissoes = permissoes;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosicaoProduto> posicao(boolean somenteAbaixoDoPontoPedido) {
        permissoes.exigir(Recurso.ESTOQUE, Acao.LER);
        return estoque.posicao(somenteAbaixoDoPontoPedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdutoResumo> listarProdutos() {
        permissoes.exigir(Recurso.ESTOQUE, Acao.LER);
        return estoque.listarProdutos();
    }

    @Override
    @Transactional
    public long criarProduto(NovoProduto novo) {
        permissoes.exigir(Recurso.ESTOQUE, Acao.CRIAR);
        long id = estoque.inserirProduto(novo.nomeProduto(), novo.unidadeMedida(),
                novo.pontoPedido(), novo.controlaLote(), novo.idFornecedorPadrao());
        auditoria.registrarCriacao("estoque.produto", String.valueOf(id), novo);
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FornecedorResumo> listarFornecedores() {
        permissoes.exigir(Recurso.ESTOQUE, Acao.LER);
        return estoque.listarFornecedores();
    }

    @Override
    @Transactional
    public long criarFornecedor(NovoFornecedor novo) {
        permissoes.exigir(Recurso.ESTOQUE, Acao.CRIAR);
        long id = estoque.inserirFornecedor(novo.razaoSocial(), novo.cnpj(),
                novo.telefone(), novo.emailVendedor());
        auditoria.registrarCriacao("estoque.fornecedor", String.valueOf(id), novo);
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoResumo> extrato(long idProduto, int limite) {
        permissoes.exigir(Recurso.ESTOQUE, Acao.LER);
        return estoque.extrato(idProduto, limite);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteVencendo> lotesVencendo(int emDias) {
        permissoes.exigir(Recurso.ESTOQUE, Acao.LER);
        return estoque.lotesVencendo(emDias);
    }

    /**
     * Movimentar é CRIAR, não ALTERAR: a tabela é append-only e uma correção é
     * uma linha de ajuste, nunca a edição da anterior. Por isso um auxiliar
     * (que tem ESTOQUE-CRIAR) registra consumo, e não existe caminho para
     * ninguém reescrever o extrato.
     */
    @Override
    @Transactional
    public long movimentar(NovaMovimentacao nova) {
        permissoes.exigir(Recurso.ESTOQUE, Acao.CRIAR);

        String tipo = nova.tipo().toLowerCase(java.util.Locale.ROOT);
        if (!TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de movimentação inválido: " + nova.tipo());
        }
        if (nova.quantidade() == null || nova.quantidade().signum() == 0) {
            throw new IllegalArgumentException("Quantidade não pode ser zero.");
        }
        if ("ajuste".equals(tipo)) {
            // Ajuste é o único tipo que aceita os dois sinais — é o que corrige
            // uma contagem para cima ou para baixo. Por isso ele passa como
            // veio, e os outros têm o sinal derivado.
            return gravar(tipo, nova.quantidade(), nova);
        }

        BigDecimal magnitude = nova.quantidade().abs();
        BigDecimal comSinal = TIPOS_DE_BAIXA.contains(tipo) ? magnitude.negate() : magnitude;

        if (TIPOS_DE_BAIXA.contains(tipo)) {
            exigirSaldo(nova, magnitude);
        }
        if ("entrada".equals(tipo)
                && estoque.produtoControlaLote(nova.idProduto())
                && (nova.lote() == null || nova.lote().isBlank())) {
            // Sem lote na entrada, o aviso de vencimento não tem como existir —
            // e um produto marcado como controla_lote foi marcado assim
            // justamente porque vence.
            throw new IllegalArgumentException(
                    "Este produto controla lote: informe o lote na entrada.");
        }
        return gravar(tipo, comSinal, nova);
    }

    /**
     * Recusa a baixa que deixaria o saldo negativo.
     *
     * <p>Estoque negativo não é um número errado que dá para corrigir depois: é
     * a clínica achando que tem anestésico e descobrindo no meio do atendimento
     * que não tem. A conferência é por lote quando há lote, porque é o lote que
     * sai da prateleira.
     */
    private void exigirSaldo(NovaMovimentacao nova, BigDecimal magnitude) {
        BigDecimal saldo = estoque.saldoDoLote(nova.idProduto(), nova.lote());
        if (saldo.compareTo(magnitude) < 0) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente: disponível %s, pedido %s.".formatted(saldo, magnitude));
        }
    }

    private long gravar(String tipo, BigDecimal quantidade, NovaMovimentacao nova) {
        long id = estoque.inserirMovimentacao(
                nova.idProduto(), tipo, quantidade, nova.lote(), nova.validade(),
                nova.custoUnitario(), nova.observacao(),
                ContextoAtual.obter().usuarioId());

        auditoria.registrarCriacao("estoque.movimentacao", String.valueOf(id),
                Map.of("idProduto", nova.idProduto(), "tipo", tipo, "quantidade", quantidade));
        return id;
    }
}
