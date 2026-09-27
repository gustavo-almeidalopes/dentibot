package br.com.dentibot.plataforma.seguranca;

/**
 * Quanto do recurso o papel alcança, DENTRO do tenant.
 *
 * <p>Existe porque duas linhas da matriz da camada 5 não são "sim ou não", e
 * sim "sim, mas só o seu": o dentista vê a própria agenda e o prontuário dos
 * seus pacientes. Devolver um booleano obrigaria cada chamador a redescobrir
 * essa nuance — e é assim que nasce a segunda checagem de permissão que a
 * invariante 3 proíbe.
 */
public enum Alcance {
    /** Todas as linhas do tenant. */
    TODOS,
    /** Só as linhas ligadas ao próprio usuário (dentista dono da agenda/paciente). */
    PROPRIOS,
    /** Nenhuma. Equivale a negado. */
    NENHUM;

    public boolean permite() {
        return this != NENHUM;
    }
}
