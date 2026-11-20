package br.com.dentibot.copiloto;

import java.math.BigDecimal;

/** O que o paciente deve à clínica, e quanto disso já venceu. */
public record SituacaoFinanceira(BigDecimal emAberto, BigDecimal vencido, int parcelasEmAberto) {
}
