package br.com.dentibot.prontuario;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * O que o cliente declara antes de enviar um anexo (ST-41) — e repete ao
 * confirmar. O SHA-256 é calculado na origem: a URL de envio amarra esse hash,
 * e o armazenamento recusa bytes que não batem com ele.
 */
public record NovoAnexo(
        @NotNull @Pattern(regexp = "^(radiografia|foto_intraoral|documento|laudo|modelo_3d)$")
        String tipo,
        @NotBlank @Size(max = 255) String nomeArquivo,
        @NotBlank @Size(max = 120) String contentType,
        // 50 MB: radiografia panorâmica e modelo 3D cabem; vídeo não é anexo.
        @NotNull @Positive @Max(52_428_800) Long tamanhoBytes,
        @NotNull @Pattern(regexp = "^[0-9a-f]{64}$") String sha256,
        Long idConsulta) {
}
