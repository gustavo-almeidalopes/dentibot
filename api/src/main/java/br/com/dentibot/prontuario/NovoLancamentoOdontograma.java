package br.com.dentibot.prontuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NovoLancamentoOdontograma(
        @NotNull @Positive Long idPaciente,
        /* O CHECK de faixa FDI vive no banco (V12): validar aqui também seria
           uma segunda fonte de verdade para a mesma regra. Aqui só o tipo. */
        @NotNull Integer dente,
        @Pattern(regexp = "^[VLMDOIP]$") String face,
        @NotBlank @Pattern(regexp = PADRAO_CONDICAO) String condicao,
        @Size(max = 300) String observacao) {

    /**
     * O vocabulário do CHECK de {@code prontuario.odontograma_lancamentos} (V12).
     * Existe em Java porque a validação da entrada e a checagem das sugestões da
     * IA (IA-01) precisam dele — e {@code CondicoesDoOdontogramaTest} o confere
     * contra o SQL, para que não vire uma segunda verdade que diverge em silêncio.
     */
    public static final List<String> CONDICOES = List.of("higido", "carie", "restauracao",
            "ausente", "implante", "coroa", "canal", "fratura", "extraido", "selante", "protese");

    public static final String PADRAO_CONDICAO =
            "^(higido|carie|restauracao|ausente|implante|coroa|canal|fratura|extraido|selante|protese)$";
}
