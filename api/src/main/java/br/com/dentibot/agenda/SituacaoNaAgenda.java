package br.com.dentibot.agenda;

import java.time.Instant;

/**
 * Onde o paciente está na agenda: a última consulta que aconteceu e a próxima
 * que está marcada. Qualquer um dos dois pode ser nulo.
 */
public record SituacaoNaAgenda(long idPaciente, Instant ultimaRealizada, Instant proximaMarcada) {
}
