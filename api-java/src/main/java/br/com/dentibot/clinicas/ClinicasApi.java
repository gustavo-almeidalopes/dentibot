package br.com.dentibot.clinicas;

import java.util.List;
import java.util.Optional;

/**
 * Porta pública do módulo clínicas.
 *
 * <p>Existe por causa de um caso concreto: o orçamento precisa do preço do
 * procedimento, e o preço mora em {@code clinicas.procedimentos}. Um
 * {@code JOIN} de {@code orcamento.itens} com esse schema funcionaria hoje e é
 * exatamente o que o {@code FronteiraDeSchemaTest} reprova — pela mesma razão de
 * sempre: some na revisão, parecendo só uma consulta eficiente.
 */
public interface ClinicasApi {

    List<ProcedimentoResumo> listarProcedimentos(boolean somenteAtivos);

    /** Vazio quando o procedimento não existe NESTE tenant — o RLS decide. */
    Optional<ProcedimentoResumo> buscarProcedimento(long idProcedimento);

    long criarProcedimento(NovoProcedimento novo);

    void atualizarProcedimento(long idProcedimento, NovoProcedimento dados, boolean ativo);
}
