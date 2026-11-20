package br.com.dentibot.lgpd;

import java.time.Instant;

/**
 * O estado atual de uma finalidade. {@code alteradaEm} nulo = ninguém mexeu, e
 * {@code permitido} é o padrão de {@link Finalidade}.
 */
public record Preferencia(String finalidade, String descricao, boolean permitido,
                          Instant alteradaEm, String origem) {
}
