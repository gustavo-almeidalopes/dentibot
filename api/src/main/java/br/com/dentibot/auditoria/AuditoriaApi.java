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

    /**
     * Dado que saiu do sistema — portabilidade (LGPD art. 18) e afins. A trilha
     * guarda o resumo do que saiu (o hash, o tamanho), nunca o conteúdo.
     */
    void registrarExportacao(String recurso, String idRecurso, Object resumo);

    void registrarCriacao(String recurso, String idRecurso, Object dadosPosteriores);

    void registrarAlteracao(String recurso, String idRecurso,
                            Object dadosAnteriores, Object dadosPosteriores);

    void registrarExclusao(String recurso, String idRecurso, Object dadosAnteriores);

    /** Login, logout, falha de login. Escrito mesmo quando não há usuário resolvido. */
    void registrarAutenticacao(String acao, Long idClinica, Long idUsuario,
                               String ipOrigem, String userAgent);

    /**
     * Consulta a trilha da própria clínica.
     *
     * <p>Ler a trilha é, ele mesmo, um ato auditável — e é registrado. Sem isso,
     * a pergunta "quem andou consultando quem acessou o prontuário de fulano?"
     * não teria resposta, e ela é exatamente o tipo de pergunta que uma
     * investigação faz.
     */
    java.util.List<EventoAuditoria> consultar(FiltroDeTrilha filtro);

    /**
     * Quem acessou o dado deste paciente desde {@code desde} (IA-52). Sem
     * permissão de matriz: quem chama é o módulo LGPD, depois de provar que é
     * o próprio titular pelo link. Mais recentes primeiro.
     */
    java.util.List<EventoAuditoria> acessosAoPaciente(long idPaciente, java.time.Instant desde);
}
