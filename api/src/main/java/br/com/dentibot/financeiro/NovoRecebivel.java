package br.com.dentibot.financeiro;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abre um recebível, possivelmente parcelado.
 *
 * <p>O valor total é dividido pelo serviço, que joga a diferença de
 * arredondamento na PRIMEIRA parcela. R$ 100,00 em três vezes vira 33,34 +
 * 33,33 + 33,33 — nunca 3 x 33,33, que perderia um centavo, nem 3 x 33,34, que
 * cobraria dois a mais. Num plano de tratamento de doze parcelas essa diferença
 * é a conversa que ninguém consegue explicar ao paciente.
 */
public record NovoRecebivel(
        Long idOrcamento,
        Long idPaciente,
        Long idConvenio,
        @NotNull @Positive BigDecimal valorTotal,
        @Min(1) @Max(120) int parcelas,
        @NotNull LocalDate primeiroVencimento) {
}
