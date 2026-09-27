package br.com.dentibot.plataforma.erro;

/**
 * Uma dependência externa que esta instância não tem configurada — bucket de
 * anexos, provedor de IA. Vira 503 com a frase dizendo o que falta, e não 500:
 * não é defeito, é ambiente incompleto, e quem opera precisa saber qual.
 */
public class ServicoIndisponivelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
