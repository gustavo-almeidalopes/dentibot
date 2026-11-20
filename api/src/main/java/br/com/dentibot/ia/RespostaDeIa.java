package br.com.dentibot.ia;

/**
 * O que o modelo devolveu, com o id da chamada na trilha. É esse id que a tela
 * devolve ao confirmar ou descartar a sugestão.
 */
public record RespostaDeIa(long idChamada, String texto) {
}
