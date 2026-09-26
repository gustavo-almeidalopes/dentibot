package br.com.dentibot.orcamento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * @param valorCobrado nulo usa o preço de tabela do procedimento. Preenchido, é
 *                     um preço negociado — e o serviço registra os dois na
 *                     auditoria, porque desconto no item é a forma mais comum de
 *                     furar a tabela sem ninguém ver.
 * @param dente        notação FDI (11–48); nulo para procedimento que não é por
 *                     dente, como profilaxia.
 */
public record NovoItem(
        @NotNull Long idProcedimento,
        @Min(11) @Max(48) Integer dente,
        @Pattern(regexp = "^[VLMDOIP]$", message = "Face deve ser uma de V, L, M, D, O, I, P")
        String face,
        @PositiveOrZero BigDecimal valorCobrado) {
}
