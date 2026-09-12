package br.com.dentibot.auditoria;

/**
 * Porta pública do módulo auditoria.
 *
 * <p>Todos os métodos são {@code REQUIRED}: participam da transação de quem
 * chamou, de propósito. Auditoria em transação separada significa que ela
 * sobrevive a um rollback do fato auditado — registrando uma leitura que não
 * aconteceu — ou que se perde quando o commit dela falha. As duas são piores que
 * não ter auditoria, porque parecem que têm.
 */
public interface AuditoriaApi {

    /**
     * Registra LEITURA de dado clínico.
     *
     * <p>Não é opcional e não é exagero: a CFO-226/2020 e a LGPD exigem saber
     * quem abriu o prontuário de quem. É a única parte da auditoria que custa
     * escrita numa operação de leitura, e é a que mais importa numa investigação.
     */
    void registrarLeitura(String recurso, String idRecurso);

    void registrarCriacao(String recurso, String idRecurso, Object dadosPosteriores);

    void registrarAlteracao(String recurso, String idRecurso,
                            Object dadosAnteriores, Object dadosPosteriores);

    void registrarExclusao(String recurso, String idRecurso, Object dadosAnteriores);

    /** Login, logout, falha de login. Escrito mesmo quando não há usuário resolvido. */
    void registrarAutenticacao(String acao, Long idClinica, Long idUsuario,
                               String ipOrigem, String userAgent);
}
