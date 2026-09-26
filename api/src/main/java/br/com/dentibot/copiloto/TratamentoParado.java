package br.com.dentibot.copiloto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * IA-35: paciente com plano aprovado, item por fazer e nenhuma consulta
 * marcada. {@code paradoDesde} é o mais recente entre a aprovação do plano e a
 * última consulta realizada — é desde quando nada acontece.
 */
public record TratamentoParado(
        long idPaciente,
        String nomePaciente,
        String telefone,
        int itensEmAberto,
        BigDecimal valorEmAberto,
        List<String> procedimentos,
        Instant paradoDesde,
        long diasParado) {
}
