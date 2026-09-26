package br.com.dentibot.prontuario;

import java.time.Instant;

/** Anexo listado no prontuário. Sem URL: ela é pedida uma a uma, e expira. */
public record AnexoResumo(
        long idAnexo,
        Long idConsulta,
        String tipo,
        String nomeArquivo,
        String contentType,
        long tamanhoBytes,
        String sha256,
        Instant enviadoEm) {
}
