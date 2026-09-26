package br.com.dentibot.financeiro;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta pública do módulo financeiro.
 *
 * <p>Dois princípios atravessam a interface inteira:
 *
 * <ol>
 *   <li><b>Não existe "marcar como pago".</b> Recebimento é uma linha no ledger
 *       append-only, e o saldo do recebível é derivado dela. Um setter de status
 *       permitiria um recebível "pago" sem nenhum dinheiro registrado — que é
 *       como o extrato e o relatório passam a discordar.</li>
 *   <li><b>Desfazer é estornar.</b> Nunca apagar, nunca editar. O
 *       {@code trg_ledger_append_only} recusa o UPDATE no banco; aqui a API nem
 *       oferece o método.</li>
 * </ol>
 */
public interface FinanceiroApi {

    List<RecebivelResumo> listarRecebiveis(FiltroFinanceiro filtro);

    RecebivelDetalhado detalharRecebivel(long idRecebivel);

    /** Gera as parcelas de um valor. Usado pela aprovação de orçamento e à mão. */
    List<Long> abrirRecebiveis(NovoRecebivel novo);

    /** Registra recebimento, juros, multa, desconto ou taxa. */
    long registrarLancamento(NovoLancamento novo);

    /** Estorna um lançamento, criando o espelho negativo dele. */
    long estornar(long idLancamento, String motivo);

    List<DespesaResumo> listarDespesas(LocalDate de, LocalDate ate, String status);

    long criarDespesa(NovaDespesa nova);

    void pagarDespesa(long idDespesa, LocalDate pagoEm);

    List<FormaPagamento> listarFormas();

    long criarForma(NovaFormaPagamento nova);

    List<ComissaoResumo> listarComissoes(Long idDentista, String status);

    List<RegraComissao> listarRegras();

    long criarRegra(NovaRegraComissao nova);

    void liberarComissao(long idComissao);

    /** O painel do financeiro num período. */
    ResumoFinanceiro resumo(LocalDate de, LocalDate ate);
}
