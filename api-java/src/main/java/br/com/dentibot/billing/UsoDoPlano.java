package br.com.dentibot.billing;

import java.time.LocalDate;

/**
 * O consumo do mês contra o que o plano permite.
 *
 * <p>Traz o limite junto com o usado de propósito: uma tela que mostra só "412
 * mensagens" não diz nada; "412 de 500" diz que falta pouco.
 */
public record UsoDoPlano(
        LocalDate competencia,
        String plano,
        int mensagensEnviadas,
        int maxMensagensMes,
        int profissionaisAtivos,
        int maxProfissionais) {
}
