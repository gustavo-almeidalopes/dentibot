package br.com.dentibot.financeiro.application;

import br.com.dentibot.financeiro.infrastructure.FinanceiroRepositorio;
import br.com.dentibot.financeiro.RegraComissao;
import br.com.dentibot.orcamento.ItemResumo;
import br.com.dentibot.orcamento.OrcamentoApi;
import br.com.dentibot.orcamento.OrcamentoDetalhado;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.outbox.ConsumidorDeEvento;
import br.com.dentibot.plataforma.outbox.EventoDominio;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import br.com.dentibot.plataforma.tenant.ContextoBanco;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orçamento aprovado vira dinheiro a receber e comissão prevista.
 *
 * <p>Por evento e não por chamada direta: aprovar um orçamento é ato do
 * consultório, e o financeiro reagir a isso é consequência. Se fosse chamada
 * direta, {@code orcamento} passaria a depender de {@code financeiro} e a
 * aprovação falharia quando o financeiro falhasse — o dentista não conseguiria
 * fechar um plano de tratamento porque o cálculo de comissão tem um bug.
 *
 * <p><b>Idempotência é contrato</b>, não zelo: o outbox entrega at-least-once. A
 * guarda é {@code existeRecebivelDoOrcamento} — sem ela, uma reentrega
 * duplicaria a cobrança de um paciente, que é o pior modo de falhar deste
 * sistema inteiro.
 */
@Component
public class ConsumidorDeOrcamentoAprovado implements ConsumidorDeEvento {

    private static final Logger log =
            LoggerFactory.getLogger(ConsumidorDeOrcamentoAprovado.class);

    /** Primeiro vencimento padrão. A clínica reparcela depois se quiser. */
    private static final int DIAS_ATE_O_PRIMEIRO_VENCIMENTO = 30;

    private final FinanceiroRepositorio financeiro;
    private final OrcamentoApi orcamentos;
    private final ContextoBanco contextoBanco;

    public ConsumidorDeOrcamentoAprovado(FinanceiroRepositorio financeiro,
                                         OrcamentoApi orcamentos,
                                         ContextoBanco contextoBanco) {
        this.financeiro = financeiro;
        this.orcamentos = orcamentos;
        this.contextoBanco = contextoBanco;
    }

    @Override
    public String nome() {
        return "financeiro.orcamento-aprovado";
    }

    @Override
    public boolean interessadoEm(String tipoDeEvento) {
        return TiposDeEvento.ORCAMENTO_APROVADO.equals(tipoDeEvento);
    }

    /**
     * O worker roda SEM tenant: {@code ContextoRequisicao.deWorker()} não tem
     * {@code clinicaId}, e sem ele {@code plataforma.clinica_atual()} é nula.
     * Consequência, se nada for promovido aqui: toda leitura sob RLS devolve
     * zero linhas em silêncio — a guarda de idempotência nunca enxerga o
     * recebível que já existe — e a primeira escrita estoura em
     * {@code clinicaObrigatoria()}. O evento é reagendado dez vezes e
     * abandonado, e aprovar orçamento nunca abre recebível.
     *
     * <p>A clínica vem do envelope do evento, que é escrito pelo servidor no
     * mesmo INSERT do outbox — nunca de conteúdo enviado pelo cliente.
     */
    @Override
    @Transactional
    public void consumir(EventoDominio evento) {
        ContextoRequisicao anterior = ContextoAtual.obter();
        contextoBanco.promoverClinica(evento.clinicaId());
        ContextoAtual.definir(ContextoRequisicao.deClinica(
                evento.clinicaId(), 0L, Papel.ADMIN, evento.correlationId()));
        try {
            processar(evento);
        } finally {
            // Sem isto a thread do pool leva o tenant do último evento para o
            // próximo — a versão em memória do vazamento que o is_local=true
            // evita no banco.
            ContextoAtual.definir(anterior);
        }
    }

    private void processar(EventoDominio evento) {
        long idOrcamento = numero(evento, "idOrcamento").longValue();

        if (financeiro.existeRecebivelDoOrcamento(idOrcamento)) {
            log.info("Orçamento {} já tem recebível; reentrega ignorada.", idOrcamento);
            return;
        }

        Long idPaciente = evento.data().get("idPaciente") == null
                ? null : numero(evento, "idPaciente").longValue();
        long idDentista = numero(evento, "idDentista").longValue();
        BigDecimal valorFinal = new BigDecimal(
                String.valueOf(evento.data().get("valorFinal")));

        if (valorFinal.signum() <= 0) {
            log.info("Orçamento {} aprovado com valor zero; nada a receber.", idOrcamento);
            return;
        }

        long idRecebivel = financeiro.inserirRecebivel(
                idOrcamento, idPaciente, null, 1, 1, valorFinal,
                LocalDate.now().plusDays(DIAS_ATE_O_PRIMEIRO_VENCIMENTO));

        log.info("Recebível {} aberto para o orçamento {}.", idRecebivel, idOrcamento);
        preverComissoes(idOrcamento, idDentista, idRecebivel);
    }

    /**
     * Comissão por ITEM, não sobre o total do orçamento.
     *
     * <p>O percentual pode variar por procedimento, e aplicar um percentual só
     * sobre o total dá o número errado sempre que o plano mistura procedimentos
     * com regras diferentes — errado para menos ou para mais, silenciosamente,
     * no contracheque de alguém.
     */
    private void preverComissoes(long idOrcamento, long idDentista, long idRecebivel) {
        OrcamentoDetalhado detalhado = orcamentos.detalhar(idOrcamento);

        for (ItemResumo item : detalhado.itens()) {
            if (financeiro.existeComissaoDoItem(item.idItem())) {
                continue;
            }
            Optional<RegraComissao> regra =
                    financeiro.regraAplicavel(idDentista, item.idProcedimento());
            if (regra.isEmpty()) {
                continue;
            }
            BigDecimal percentual = regra.get().percentual();
            BigDecimal valor = item.valorCobrado()
                    .multiply(percentual)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            financeiro.inserirComissao(idDentista, item.idItem(), idRecebivel,
                    item.valorCobrado(), percentual, valor,
                    LocalDate.now().plusDays(DIAS_ATE_O_PRIMEIRO_VENCIMENTO));
        }
    }

    /**
     * O JSON do evento volta com o número no tipo que o Jackson escolheu
     * (Integer para valores pequenos, Long acima). Ler como Number e converter
     * evita o ClassCastException que aparece só quando os ids crescem — ou
     * seja, em produção, meses depois.
     */
    private static Number numero(EventoDominio evento, String campo) {
        Object valor = evento.data().get(campo);
        if (valor instanceof Number n) {
            return n;
        }
        throw new IllegalStateException(
                "Evento %s sem o campo numérico '%s'.".formatted(evento.eventType(), campo));
    }
}
