package br.com.dentibot.prontuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Sem {@code idDentista}: quem assina a evolução é o profissional autenticado,
 * resolvido do token. Aceitar o autor por parâmetro permitiria registrar ato
 * clínico em nome de outro — o oposto do que a CFO-226 exige ao pedir
 * identificação do responsável.
 */
public record NovaEvolucao(
        @NotNull @Positive Long idPaciente,
        Long idConsulta,
        @NotBlank @Size(max = 20000) String descricao) {
}
