package br.com.dentibot.plataforma.config;

/**
 * Parâmetros de listagem que todo controller recebe do mesmo jeito.
 *
 * <p>{@link #LIMITE_MAXIMO} é espelhado em {@code web/src/api.js}
 * ({@code LIMITE_LISTA}) e conferido por {@code web/src/api.test.mjs}: é com ele
 * que a tela sabe que uma lista de 200 provavelmente não é a lista inteira.
 */
public final class Listagem {

    public static final int LIMITE_MAXIMO = 200;

    private Listagem() {
    }

    /** O limite pedido, dentro de {@code [1, LIMITE_MAXIMO]}. */
    public static int limite(int pedido) {
        return Math.min(Math.max(pedido, 1), LIMITE_MAXIMO);
    }

    /** Um filtro em branco vindo do formulário é "sem filtro", não "igual a ''". */
    public static String vazioViraNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
