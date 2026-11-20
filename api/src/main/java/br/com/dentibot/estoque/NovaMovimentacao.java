package br.com.dentibot.estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O sinal de {@code quantidade} é derivado do tipo pelo serviço; só
 * {@code ajuste} usa o sinal enviado. Deixar o cliente mandar o sinal
 * convidaria a uma "saída" positiva, que o CHECK {@code ck_sinal_coerente}
 * recusaria com erro de banco no lugar de uma regra explicada.
 */
public record NovaMovimentacao(
        @NotNull Long idProduto,
        @NotBlank String tipo,
        @NotNull BigDecimal quantidade,
        @Size(max = 60) String lote,
        LocalDate validade,
        BigDecimal custoUnitario,
        @Size(max = 300) String observacao) {
}
