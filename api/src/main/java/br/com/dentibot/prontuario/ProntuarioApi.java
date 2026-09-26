package br.com.dentibot.prontuario;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Porta pública do módulo prontuário — o núcleo regulado (CFO-226/2020, LGPD
 * art. 11).
 *
 * <p>Toda leitura daqui deixa rastro em {@code auditoria.eventos}, na mesma
 * transação. Não é configuração nem feature flag: é o que a norma exige e o que
 * permite responder "quem abriu o prontuário deste paciente".
 */
public interface ProntuarioApi {

    List<EvolucaoResumo> historico(long idPaciente);

    long registrarEvolucao(NovaEvolucao nova);

    /** Adendo de retificação. Não altera o registro anterior — soma a ele. */
    long retificarEvolucao(long idEvolucaoOriginal, String descricao, String motivo);

    List<LancamentoOdontograma> odontograma(long idPaciente);

    long lancarOdontograma(NovoLancamentoOdontograma lancamento);

    /** Alergia, anticoagulante, gestação: o que falar antes de chamar o paciente. */
    AlertasClinicos alertas(long idPaciente);

    /**
     * Das consultas informadas, quais têm evolução registrada. Só ids — nenhum
     * conteúdo clínico sai daqui —, para achar atendimento sem registro (IA-05).
     */
    Set<Long> consultasComEvolucao(Collection<Long> idsConsulta);
}
