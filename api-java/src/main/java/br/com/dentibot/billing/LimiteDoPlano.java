package br.com.dentibot.billing;

/**
 * Resposta de uma checagem de limite.
 *
 * @param cabe    se a operação pode seguir
 * @param usado   quantos já existem
 * @param limite  quantos o plano permite
 * @param motivo  texto pronto para a interface quando não cabe — escrito aqui
 *                porque quem sabe qual limite estourou é este módulo, e montar a
 *                frase na tela duplicaria a regra no front.
 */
public record LimiteDoPlano(boolean cabe, int usado, int limite, String motivo) {

    public static LimiteDoPlano liberado(int usado, int limite) {
        return new LimiteDoPlano(true, usado, limite, null);
    }
}
