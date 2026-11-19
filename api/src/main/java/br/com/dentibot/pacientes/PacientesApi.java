package br.com.dentibot.pacientes;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PacientesApi {

    Map<Long, PacienteResumo> mapaDeResumos(Collection<Long> idsPaciente);

    List<PacienteResumo> listarResumos(int limite, long apos);

    long criar(NovoPaciente novo);

    /**
     * O paciente existe NESTA clínica? A resposta vem do RLS: fora do tenant,
     * a linha simplesmente não está lá.
     */
    boolean existe(long idPaciente);
}
