package br.com.dentibot.ia;

import java.math.BigDecimal;
import java.util.List;

/**
 * Por clínica: o que está desligado, a cota do mês, quanto já foi, e se esta
 * instância tem provedor configurado.
 */
public record ConfiguracaoDeIa(List<String> recursosDisponiveis, List<String> recursosDesligados,
                               BigDecimal cotaMensalUsd, BigDecimal gastoNoMesUsd,
                               boolean provedorConfigurado, String modelo) {
}
