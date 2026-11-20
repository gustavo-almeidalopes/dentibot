package br.com.dentibot.prontuario;

import java.time.Instant;
import java.util.Map;

/**
 * Para onde mandar os bytes: URL pré-assinada de PUT e os cabeçalhos que a
 * assinatura cobre — o cliente manda exatamente estes, ou o armazenamento
 * recusa. {@code chave} volta na confirmação.
 */
public record EnvioDeAnexo(String chave, String url, Map<String, String> cabecalhos, Instant expiraEm) {
}
