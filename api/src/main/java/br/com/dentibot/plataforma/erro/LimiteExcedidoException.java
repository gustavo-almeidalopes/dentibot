package br.com.dentibot.plataforma.erro;

/** Cota da clínica esgotada (ex.: IA do mês). Vira 429: esperar resolve, insistir não. */
public class LimiteExcedidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LimiteExcedidoException(String mensagem) {
        super(mensagem);
    }
}
