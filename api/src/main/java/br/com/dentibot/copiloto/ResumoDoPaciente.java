package br.com.dentibot.copiloto;

import br.com.dentibot.agenda.SituacaoNaAgenda;
import br.com.dentibot.orcamento.ItemDePlano;
import br.com.dentibot.prontuario.AlertasClinicos;
import br.com.dentibot.prontuario.EvolucaoResumo;
import java.util.List;

/**
 * IA-06: meia tela antes de chamar o paciente — alertas, última conduta, o que
 * falta do plano, a situação financeira e a próxima consulta.
 */
public record ResumoDoPaciente(
        long idPaciente,
        String nomePaciente,
        Secao<AlertasClinicos> alertas,
        Secao<EvolucaoResumo> ultimaEvolucao,
        Secao<List<ItemDePlano>> planoEmAberto,
        Secao<SituacaoFinanceira> financeiro,
        Secao<SituacaoNaAgenda> agenda) {
}
