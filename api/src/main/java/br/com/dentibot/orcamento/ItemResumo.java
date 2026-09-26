package br.com.dentibot.orcamento;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Um item do plano de tratamento.
 *
 * <p>{@code nomeProcedimento} vem da porta de {@code clinicas}, não de JOIN:
 * preço e nome moram lá. {@code valorCobrado} é o preço CONGELADO no momento em
 * que o item entrou — mudar a tabela de preços depois não pode alterar um
 * orçamento que o paciente já viu.
 */
public record ItemResumo(
        long idItem,
        long idProcedimento,
        String nomeProcedimento,
        Integer dente,
        String face,
        BigDecimal valorCobrado,
        String statusExecucao,
        Instant executadoEm,
        Long idConsulta) {
}
