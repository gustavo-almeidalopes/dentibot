package br.com.dentibot.orcamento;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Um item de orçamento aprovado, visto fora do orçamento: é a unidade do
 * "plano de tratamento" que o copiloto acompanha — o que ficou pendente (IA-35,
 * IA-06) e o que foi feito (IA-05).
 */
public record ItemDePlano(
        long idOrcamento,
        long idPaciente,
        long idDentista,
        long idItem,
        long idProcedimento,
        String nomeProcedimento,
        Integer dente,
        BigDecimal valorCobrado,
        String statusExecucao,
        Instant aprovadoEm,
        Instant executadoEm,
        Long idConsulta) {
}
