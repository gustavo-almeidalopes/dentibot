package br.com.dentibot.comunicacao;

import java.time.Instant;

/** Quem espera vaga (IA-18). {@code nomePaciente} vem da porta de pacientes, na tela. */
public record Espera(
        long idEspera,
        long idPaciente,
        String nomePaciente,
        Long idDentista,
        String periodo,
        int urgencia,
        String observacao,
        Instant desde) {

    public Espera comNome(String nome) {
        return new Espera(idEspera, idPaciente, nome, idDentista, periodo, urgencia, observacao, desde);
    }
}
