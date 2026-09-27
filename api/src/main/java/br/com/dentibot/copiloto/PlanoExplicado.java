package br.com.dentibot.copiloto;

import java.util.List;

/**
 * IA-04: o mesmo plano em duas linguagens — rascunho para o dentista revisar.
 *
 * @param procedimentos o que está no orçamento, na tela ao lado do texto, para
 *                      conferir que a IA não inventou nem esqueceu etapa
 */
public record PlanoExplicado(long idChamada, String versaoTecnica, String versaoPaciente,
                             List<String> procedimentos) {
}
