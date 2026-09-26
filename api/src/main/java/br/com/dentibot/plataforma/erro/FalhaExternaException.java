package br.com.dentibot.plataforma.erro;

/**
 * Um serviço de terceiro respondeu mal ou não respondeu (provedor de IA). Vira
 * 502: o erro não é do cliente nem desta API, e o suporte precisa saber disso
 * sem abrir o log.
 */
public class FalhaExternaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaExternaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
