package br.com.dentibot.billing;

/**
 * Um plano do catálogo.
 *
 * <p>Valores em CENTAVOS e inteiros, não {@code BigDecimal} como o financeiro:
 * é o que o Stripe usa, e converter na fronteira duas vezes é onde o centavo se
 * perde. O financeiro guarda dinheiro de paciente em NUMERIC porque lá ele é
 * somado, parcelado e conciliado; aqui ele só é repassado.
 */
public record Plano(
        String codigo,
        String nome,
        int precoMensalCentavos,
        int maxProfissionais,
        int maxMensagensMes,
        boolean permitePermissaoPorPerfil) {
}
