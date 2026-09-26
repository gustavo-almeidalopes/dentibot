package br.com.dentibot.financeiro;

import java.math.BigDecimal;
import java.time.Instant;

/** Uma linha do ledger. Append-only: o que está aqui não muda mais. */
public record LancamentoResumo(
        long idLancamento,
        Long idRecebivel,
        String tipo,
        BigDecimal valor,
        Long idForma,
        String descricao,
        Long estornaLancamento,
        Long registradoPor,
        Instant ocorridoEm) {
}
