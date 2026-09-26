package br.com.dentibot.prontuario;

import java.util.List;

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
}
