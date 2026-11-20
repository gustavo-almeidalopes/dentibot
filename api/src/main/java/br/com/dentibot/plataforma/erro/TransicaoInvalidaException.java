package br.com.dentibot.plataforma.erro;

/**
 * O recurso existe, mas o estado atual dele não permite a mudança pedida.
 * Responde 409, e não 404: um 404 mandaria o cliente procurar um recurso que ele
 * já tem.
 */
public class TransicaoInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
