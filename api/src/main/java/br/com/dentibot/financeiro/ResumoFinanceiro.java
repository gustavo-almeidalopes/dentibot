package br.com.dentibot.financeiro;

import java.math.BigDecimal;

/**
 * O painel do financeiro num período.
 *
 * <p>{@code recebidoNoPeriodo} vem do ledger — o que entrou de fato.
 * {@code aReceber} e {@code vencido} vêm dos recebíveis em aberto. São perguntas
 * diferentes e a tela mostra as duas separadas: confundi-las é como uma clínica
 * acredita ter faturado o que ainda não recebeu.
 */
public record ResumoFinanceiro(
        BigDecimal recebidoNoPeriodo,
        BigDecimal aReceber,
        BigDecimal vencido,
        BigDecimal aPagar,
        BigDecimal comissoesPrevistas) {
}
