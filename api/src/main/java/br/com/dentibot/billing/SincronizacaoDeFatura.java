package br.com.dentibot.billing;

import java.time.Instant;
import java.time.LocalDate;

public record SincronizacaoDeFatura(
        long idClinica,
        String idExterno,
        int valorCentavos,
        String status,
        LocalDate competencia,
        LocalDate venceEm,
        Instant pagaEm,
        String urlFatura) {
}
