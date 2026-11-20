package br.com.dentibot.prontuario;

/**
 * Uma sugestão de lançamento no odontograma vinda do ditado (IA-01). Não é
 * lançamento: a tela mostra, o dentista confirma um a um pelo fluxo normal.
 */
public record LancamentoSugerido(int dente, String face, String condicao, String observacao) {
}
