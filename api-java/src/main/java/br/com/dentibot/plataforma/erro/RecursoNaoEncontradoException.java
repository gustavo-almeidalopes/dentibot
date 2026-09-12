package br.com.dentibot.plataforma.erro;

/**
 * O recurso não existe DENTRO DO TENANT ATUAL.
 *
 * <p>Deliberadamente não distingue "nunca existiu" de "existe em outra clínica".
 * Responder 403 no segundo caso confirmaria que aquele id existe em algum lugar
 * — e num sistema de saúde, "o paciente 4815 existe" já é informação sobre uma
 * pessoa. 404 nos dois casos não entrega nada.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s %s não encontrado.".formatted(recurso, id));
    }
}
