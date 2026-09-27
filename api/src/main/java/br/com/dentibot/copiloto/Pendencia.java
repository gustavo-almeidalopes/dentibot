package br.com.dentibot.copiloto;

import java.time.Instant;

/**
 * IA-05: atendimento que aconteceu e não ficou registrado. Registro clínico é
 * obrigação do profissional (CFO); a regra aqui é determinística e verificável
 * — nenhuma delas "acha", todas apontam o registro que falta.
 */
public record Pendencia(
        String tipo,
        long idPaciente,
        String nomePaciente,
        Long idConsulta,
        Long idItem,
        Instant quando,
        String descricao) {

    public static final String CONSULTA_SEM_EVOLUCAO = "consulta_sem_evolucao";
    public static final String PROCEDIMENTO_SEM_EVOLUCAO = "procedimento_sem_evolucao";
    public static final String PROCEDIMENTO_SEM_CONSULTA = "procedimento_sem_consulta";
}
