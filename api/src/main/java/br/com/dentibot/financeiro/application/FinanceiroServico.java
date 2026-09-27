package br.com.dentibot.financeiro.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.financeiro.ComissaoResumo;
import br.com.dentibot.financeiro.DespesaResumo;
import br.com.dentibot.financeiro.FinanceiroApi;
import br.com.dentibot.financeiro.FiltroFinanceiro;
import br.com.dentibot.financeiro.FormaPagamento;
import br.com.dentibot.financeiro.LancamentoResumo;
import br.com.dentibot.financeiro.NovaDespesa;
import br.com.dentibot.financeiro.NovaFormaPagamento;
import br.com.dentibot.financeiro.NovaRegraComissao;
import br.com.dentibot.financeiro.NovoLancamento;
import br.com.dentibot.financeiro.NovoRecebivel;
import br.com.dentibot.financeiro.RecebivelDetalhado;
import br.com.dentibot.financeiro.RecebivelResumo;
import br.com.dentibot.financeiro.RegraComissao;
import br.com.dentibot.financeiro.ResumoFinanceiro;
import br.com.dentibot.financeiro.infrastructure.FinanceiroRepositorio;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.outbox.Outbox;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceiroServico implements FinanceiroApi {

    /**
     * Os tipos que este endpoint aceita.
     *
     * <p>Ficaram de fora, e não por esquecimento: {@code cobranca} é escrito
     * pela emissão da cobrança, {@code estorno} tem método próprio
     * ({@link #estornar}) porque precisa apontar para o lançamento revertido, e
     * {@code taxa} é custo de adquirente, que pertence à conciliação do PSP e
     * não ao saldo do paciente — lançá-la aqui aumentaria a dívida de quem já
     * pagou.
     */
    private static final Set<String> TIPOS_MANUAIS =
            Set.of("recebimento", "desconto", "juros", "multa", "glosa");

    /**
     * Tipos que AUMENTAM o que o paciente deve.
     *
     * <p>O saldo é {@code valor_parcela - SUM(valor)}, então o sinal aqui é o
     * inverso da intuição: juros e multa entram NEGATIVOS para fazer a dívida
     * subir. Errar este sinal não quebra nada visivelmente — só faz a clínica
     * cobrar a menos, todo mês, sem ninguém notar.
     */
    private static final Set<String> TIPOS_QUE_AUMENTAM_A_DIVIDA = Set.of("juros", "multa");

    private final FinanceiroRepositorio financeiro;
    private final AvaliadorDePermissao permissoes;
    private final AuditoriaApi auditoria;
    private final Outbox outbox;

    public FinanceiroServico(FinanceiroRepositorio financeiro, AvaliadorDePermissao permissoes,
                             AuditoriaApi auditoria, Outbox outbox) {
        this.financeiro = financeiro;
        this.permissoes = permissoes;
        this.auditoria = auditoria;
        this.outbox = outbox;
    }

    // ─── Recebíveis ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RecebivelResumo> listarRecebiveis(FiltroFinanceiro filtro) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.LER);
        return financeiro.listarRecebiveis(filtro.de(), filtro.ate(), filtro.status(),
                filtro.idPaciente(), filtro.apos(), filtro.limite());
    }

    @Override
    @Transactional(readOnly = true)
    public RecebivelDetalhado detalharRecebivel(long idRecebivel) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.LER);
        RecebivelResumo r = financeiro.buscarRecebivel(idRecebivel)
                .orElseThrow(() -> new RecursoNaoEncontradoException("recebivel", idRecebivel));
        return new RecebivelDetalhado(r, financeiro.extrato(idRecebivel));
    }

    /**
     * Divide o total em parcelas sem perder nem inventar centavo.
     *
     * <p>A sobra do arredondamento vai toda na PRIMEIRA parcela. R$ 100,00 em
     * três vezes é 33,34 + 33,33 + 33,33 — nunca 3 × 33,33, que sumiria com um
     * centavo, nem 3 × 33,34, que cobraria dois a mais. Em doze parcelas essa
     * diferença é a conversa que ninguém consegue explicar ao paciente.
     */
    @Override
    @Transactional
    public List<Long> abrirRecebiveis(NovoRecebivel novo) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.CRIAR);

        int parcelas = Math.max(novo.parcelas(), 1);
        BigDecimal total = novo.valorTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.DOWN);
        BigDecimal sobra = total.subtract(base.multiply(BigDecimal.valueOf(parcelas)));

        List<Long> ids = new ArrayList<>(parcelas);
        for (int i = 0; i < parcelas; i++) {
            BigDecimal valor = i == 0 ? base.add(sobra) : base;
            long id = financeiro.inserirRecebivel(
                    novo.idOrcamento(), novo.idPaciente(), novo.idConvenio(),
                    i + 1, parcelas, valor, novo.primeiroVencimento().plusMonths(i));
            ids.add(id);
        }

        auditoria.registrarCriacao("financeiro.recebivel", ids.toString(),
                Map.of("valorTotal", total, "parcelas", parcelas));
        return ids;
    }

    @Override
    @Transactional
    public long registrarLancamento(NovoLancamento novo) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.CRIAR);

        String tipo = novo.tipo().toLowerCase(java.util.Locale.ROOT);
        if (!TIPOS_MANUAIS.contains(tipo)) {
            throw new IllegalArgumentException(
                    "Tipo de lançamento não aceito aqui: %s. Aceitos: %s."
                            .formatted(novo.tipo(), String.join(", ", TIPOS_MANUAIS)));
        }
        financeiro.buscarRecebivel(novo.idRecebivel())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "recebivel", novo.idRecebivel()));

        BigDecimal valor = novo.valor().abs().setScale(2, RoundingMode.HALF_UP);
        BigDecimal comSinal = TIPOS_QUE_AUMENTAM_A_DIVIDA.contains(tipo) ? valor.negate() : valor;

        long id = financeiro.inserirLancamento(novo.idRecebivel(), tipo, comSinal,
                novo.idForma(), novo.referenciaExterna(), novo.descricao(), null,
                ContextoAtual.obter().usuarioId(), ContextoAtual.correlacao());

        // Status derivado do saldo, nunca digitado: um recebível "pago" sem
        // dinheiro no ledger é como o extrato e o relatório passam a discordar.
        financeiro.sincronizarStatus(novo.idRecebivel());

        auditoria.registrarCriacao("financeiro.lancamento", String.valueOf(id),
                Map.of("idRecebivel", novo.idRecebivel(), "tipo", tipo, "valor", comSinal));

        if ("recebimento".equals(tipo)) {
            RecebivelResumo depois = financeiro.buscarRecebivel(novo.idRecebivel()).orElseThrow();
            if (depois.saldoDevedor().signum() <= 0) {
                outbox.gravar(TiposDeEvento.COBRANCA_PAGA,
                        Map.of("idRecebivel", novo.idRecebivel(),
                                "valorParcela", depois.valorParcela()));
            }
        }
        return id;
    }

    /**
     * Estorna criando o espelho negativo. Nunca apaga, nunca edita — o
     * {@code trg_ledger_append_only} recusaria, e a API nem oferece o caminho.
     */
    @Override
    @Transactional
    public long estornar(long idLancamento, String motivo) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.ALTERAR);

        LancamentoResumo original = financeiro.buscarLancamento(idLancamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("lancamento", idLancamento));

        if (financeiro.jaEstornado(idLancamento)) {
            // Sem esta guarda, dois cliques em "estornar" creditam o valor duas
            // vezes e o paciente fica com saldo a favor que ninguém lhe deve.
            throw new IllegalArgumentException("Este lançamento já foi estornado.");
        }
        if ("estorno".equals(original.tipo())) {
            throw new IllegalArgumentException(
                    "Não se estorna um estorno. Registre um novo lançamento.");
        }

        long id = financeiro.inserirLancamento(
                original.idRecebivel(), "estorno", original.valor().negate(),
                original.idForma(), null, "Estorno: " + motivo, idLancamento,
                ContextoAtual.obter().usuarioId(), ContextoAtual.correlacao());

        if (original.idRecebivel() != null) {
            financeiro.sincronizarStatus(original.idRecebivel());
        }
        auditoria.registrarCriacao("financeiro.estorno", String.valueOf(id),
                Map.of("estorna", idLancamento, "motivo", motivo));
        outbox.gravar(TiposDeEvento.COBRANCA_ESTORNADA,
                Map.of("idLancamento", idLancamento, "idEstorno", id));
        return id;
    }

    // ─── Contas a pagar ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DespesaResumo> listarDespesas(LocalDate de, LocalDate ate, String status) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.LER);
        return financeiro.listarDespesas(de, ate, status);
    }

    @Override
    @Transactional
    public long criarDespesa(NovaDespesa nova) {
        // ALTERAR e não CRIAR: pela matriz da camada 5, a recepcionista tem
        // FINANCEIRO-CRIAR para registrar recebimento no balcão, e despesa não
        // é dela — é do perfil financeiro.
        permissoes.exigir(Recurso.FINANCEIRO, Acao.ALTERAR);
        long id = financeiro.inserirDespesa(nova.descricao(), nova.categoria(),
                nova.valorDocumento(), nova.vencimentoEm(), nova.observacoes());
        auditoria.registrarCriacao("financeiro.despesa", String.valueOf(id), nova);
        return id;
    }

    @Override
    @Transactional
    public void pagarDespesa(long idDespesa, LocalDate pagoEm) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.ALTERAR);
        LocalDate data = pagoEm == null ? LocalDate.now() : pagoEm;
        if (financeiro.pagarDespesa(idDespesa, data) == 0) {
            throw new IllegalArgumentException("Despesa inexistente ou já paga.");
        }
        auditoria.registrarAlteracao("financeiro.despesa", String.valueOf(idDespesa),
                Map.of("status", "aberta"), Map.of("status", "paga", "pagoEm", data.toString()));
    }

    // ─── Formas de pagamento ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<FormaPagamento> listarFormas() {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.LER);
        return financeiro.listarFormas();
    }

    @Override
    @Transactional
    public long criarForma(NovaFormaPagamento nova) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.ALTERAR);
        long id = financeiro.inserirForma(nova.nome(), nova.tipo(),
                nova.taxaPercentual(), nova.diasLiquidacao());
        auditoria.registrarCriacao("financeiro.forma_pagamento", String.valueOf(id), nova);
        return id;
    }

    // ─── Comissões ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ComissaoResumo> listarComissoes(Long idDentista, String status) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.LER);
        return financeiro.listarComissoes(idDentista, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegraComissao> listarRegras() {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.LER);
        return financeiro.listarRegras();
    }

    @Override
    @Transactional
    public long criarRegra(NovaRegraComissao nova) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.ALTERAR);
        long id = financeiro.inserirRegra(nova.idDentista(), nova.idProcedimento(),
                nova.baseCalculo(), nova.percentual());
        auditoria.registrarCriacao("financeiro.regra_comissao", String.valueOf(id), nova);
        return id;
    }

    @Override
    @Transactional
    public void liberarComissao(long idComissao) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.ALTERAR);
        if (financeiro.liberarComissao(idComissao) == 0) {
            throw new IllegalArgumentException("Comissão inexistente ou já liberada.");
        }
        auditoria.registrarAlteracao("financeiro.comissao", String.valueOf(idComissao),
                Map.of("status", "prevista"), Map.of("status", "liberada"));
    }

    // ─── Painel ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResumoFinanceiro resumo(LocalDate de, LocalDate ate) {
        permissoes.exigir(Recurso.FINANCEIRO, Acao.LER);
        return new ResumoFinanceiro(
                financeiro.recebidoEntre(de, ate),
                financeiro.saldoAReceber(false),
                financeiro.saldoAReceber(true),
                financeiro.totalAPagar(),
                financeiro.comissoesPrevistas());
    }
}
