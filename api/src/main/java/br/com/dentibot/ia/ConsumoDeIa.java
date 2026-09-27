package br.com.dentibot.ia;

import java.math.BigDecimal;

/** IA-59: o que a clínica gastou com IA, e o que disso virou registro. */
public record ConsumoDeIa(String recurso, long chamadas, long erros, BigDecimal custoUsd,
                          long aceitas, long descartadas) {
}
