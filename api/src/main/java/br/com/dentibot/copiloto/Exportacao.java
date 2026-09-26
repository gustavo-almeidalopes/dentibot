package br.com.dentibot.copiloto;

/**
 * IA-47: o arquivo da portabilidade e o SHA-256 dos bytes dele. O hash vai no
 * cabeçalho da resposta e na trilha de auditoria — quem recebe confere com
 * {@code sha256sum}, e a clínica prova depois o que entregou.
 */
public record Exportacao(byte[] conteudo, String sha256, String nomeArquivo) {
}
