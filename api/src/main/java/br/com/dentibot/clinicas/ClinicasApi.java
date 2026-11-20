package br.com.dentibot.clinicas;

import java.util.List;
import java.util.Optional;

/**
 * O orçamento precisa do preço, que mora em {@code clinicas.procedimentos}: é
 * por aqui que ele o lê. Um {@code JOIN} de {@code orcamento.itens} com esse
 * schema é o que o {@code FronteiraDeSchemaTest} reprova.
 */
public interface ClinicasApi {

    List<ProcedimentoResumo> listarProcedimentos(boolean somenteAtivos);

    /** Vazio quando o procedimento não existe NESTE tenant — o RLS decide. */
    Optional<ProcedimentoResumo> buscarProcedimento(long idProcedimento);

    long criarProcedimento(NovoProcedimento novo);

    void atualizarProcedimento(long idProcedimento, NovoProcedimento dados, boolean ativo);

    /**
     * Nome e fuso da clínica corrente, para falar com o paciente: a mensagem
     * diz "às 14h" no fuso da clínica, nunca no do servidor. Sem permissão: é
     * o que a clínica assina, não configuração.
     */
    Identificacao identificacao();

    record Identificacao(String nome, java.time.ZoneId fuso) {
    }
}
