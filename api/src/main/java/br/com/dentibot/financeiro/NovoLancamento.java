package br.com.dentibot.financeiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * @param valor sempre POSITIVO. O sinal é derivado do tipo pelo serviço:
 *              recebimento e desconto abatem o saldo, juros e multa aumentam.
 *              Deixar o cliente mandar o sinal é como um "recebimento" negativo
 *              entra no ledger e o relatório de faturamento passa a mentir.
 */
public record NovoLancamento(
        @NotNull Long idRecebivel,
        @NotBlank String tipo,
        @NotNull @Positive BigDecimal valor,
        Long idForma,
        @Size(max = 120) String referenciaExterna,
        @Size(max = 300) String descricao) {
}
