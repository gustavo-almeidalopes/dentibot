package br.com.dentibot.pacientes;

/**
 * O que a recepção pode ver de um paciente: identificar e contatar.
 *
 * <p>Contrapartida de {@code PacienteClinico}. A camada 5 diz que
 * recepcionista vê "só nome e horário" — e a forma correta de implementar isso é
 * a consulta NÃO TRAZER o resto, nunca trazer tudo e apagar campo depois. Campo
 * que não veio do banco não vaza em log, em resposta de erro nem em telemetria.
 */
public record PacienteResumo(
        long idPaciente,
        long idPessoa,
        String nomeCompleto,
        String telefoneCelular,
        String status) {
}
