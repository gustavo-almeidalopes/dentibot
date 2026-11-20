package br.com.dentibot.prontuario;

import java.util.List;

/**
 * O que a IA organizou a partir do ditado (IA-01) — rascunho, nunca registro.
 *
 * @param idChamada     a linha em ia.chamadas; volta na confirmação ou no descarte
 * @param evolucao      texto proposto para a evolução, editável na tela
 * @param lancamentos   sugestões que passaram na regra (dente FDI, face, condição)
 * @param naoConfirmado o que o modelo não conseguiu ancorar no ditado e o que a
 *                      regra descartou — mostrado à parte, para o dentista decidir
 */
public record RascunhoDeNota(long idChamada, String evolucao, List<LancamentoSugerido> lancamentos,
                             List<String> naoConfirmado) {
}
