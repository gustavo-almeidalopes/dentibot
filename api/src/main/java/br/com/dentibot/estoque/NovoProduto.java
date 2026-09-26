package br.com.dentibot.estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record NovoProduto(
        @NotBlank @Size(max = 120) String nomeProduto,
        @NotBlank @Size(max = 30) String unidadeMedida,
        @PositiveOrZero BigDecimal pontoPedido,
        boolean controlaLote,
        Long idFornecedorPadrao) {
}
