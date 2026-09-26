package br.com.dentibot.clinicas;

import java.math.BigDecimal;

/**
 * Um procedimento da tabela de preços da clínica.
 *
 * <p>{@code precoParticular} em {@link BigDecimal}, nunca double: 0.1 + 0.2 em
 * ponto flutuante não é 0.3, e num plano de tratamento de doze parcelas isso
 * vira o centavo que ninguém consegue explicar ao paciente (invariante 6).
 */
public record ProcedimentoResumo(
        long idProcedimento,
        String nomeServico,
        String codigoTuss,
        BigDecimal precoParticular,
        int duracaoMinutos,
        boolean ativo) {
}
