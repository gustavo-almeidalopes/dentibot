package br.com.dentibot.financeiro;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ComissaoResumo(
        long idComissao,
        long idDentista,
        long idItem,
        Long idRecebivel,
        BigDecimal valorBase,
        BigDecimal percentualAplicado,
        BigDecimal valorComissao,
        String status,
        LocalDate previsaoLiberacaoEm,
        Instant pagoEm) {
}
