package br.com.dentibot.financeiro;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @param idProcedimento nulo faz a regra valer para todos os procedimentos. A
 *                       regra específica vence a genérica no cálculo.
 */
public record NovaRegraComissao(
        @NotNull Long idDentista,
        Long idProcedimento,
        String baseCalculo,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal percentual) {
}
