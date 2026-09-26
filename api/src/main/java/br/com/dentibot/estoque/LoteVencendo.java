package br.com.dentibot.estoque;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lote com saldo positivo e validade próxima.
 *
 * <p>O saldo é por lote, não por produto: um produto pode ter um lote vencendo e
 * outro novo, e avisar sobre o produto inteiro esconderia justamente qual caixa
 * precisa sair da prateleira.
 */
public record LoteVencendo(
        long idProduto,
        String nomeProduto,
        String lote,
        LocalDate validade,
        BigDecimal saldo) {
}
