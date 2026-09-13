package br.com.dentibot.financeiro;

import java.time.LocalDate;

/** Recorte da listagem de recebíveis. Nulo em qualquer campo significa "sem filtro". */
public record FiltroFinanceiro(LocalDate de, LocalDate ate, String status, Long idPaciente,
                               long apos, int limite) {
}
