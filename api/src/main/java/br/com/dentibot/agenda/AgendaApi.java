package br.com.dentibot.agenda;

import java.time.Instant;
import java.util.List;

public interface AgendaApi {

    long agendar(NovaConsulta nova);

    List<ConsultaResumo> listar(Instant de, Instant ate, Long idDentista);

    /**
     * @param acao   uma das palavras de {@link ConsultaResumo#acoes()}
     * @param motivo exigido só para {@code cancelar}; ignorado nas outras
     */
    void transicionar(long idConsulta, String acao, String motivo);

    /**
     * O paciente já teve consulta com este dentista?
     *
     * <p>É a tradução operacional de "o dentista vê o prontuário dos SEUS
     * pacientes" (camada 5). Mora aqui, e não no módulo de prontuário, porque a
     * resposta está em {@code agenda.consultas} — ler a tabela de outro módulo
     * direto é a fronteira sumindo em silêncio, mesmo sem JOIN.
     */
    boolean pacienteAtendidoPor(long idPaciente, long idDentista);
}
