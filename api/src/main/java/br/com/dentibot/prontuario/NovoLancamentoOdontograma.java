package br.com.dentibot.prontuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NovoLancamentoOdontograma(
        @NotNull @Positive Long idPaciente,
        /* O CHECK de faixa FDI vive no banco (V12): validar aqui também seria
           uma segunda fonte de verdade para a mesma regra. Aqui só o tipo. */
        @NotNull Integer dente,
        @Pattern(regexp = "^[VLMDOIP]$") String face,
        @NotBlank String condicao,
        @Size(max = 300) String observacao) {
}
