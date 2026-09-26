package br.com.dentibot.orcamento;

import java.util.List;

/**
 * Porta pública do módulo orçamento.
 *
 * <p>O orçamento é o documento que vira dinheiro: aprovar um gera o recebível no
 * financeiro. Por isso a aprovação é um método próprio e não um
 * {@code atualizar(status)} genérico — a transição tem efeito colateral em outro
 * módulo, e esconder isso atrás de um setter é como ele passa despercebido.
 */
public interface OrcamentoApi {

    List<OrcamentoResumo> listar(Long idPaciente, String status);

    OrcamentoDetalhado detalhar(long idOrcamento);

    long criar(NovoOrcamento novo);

    /** Adiciona um item e recalcula os totais do cabeçalho. */
    long adicionarItem(long idOrcamento, NovoItem item);

    void removerItem(long idOrcamento, long idItem);

    /** Envia ao paciente: sai de rascunho e congela a composição. */
    void enviar(long idOrcamento);

    /**
     * Aprovação do paciente. Publica {@code orcamento.aprovado} no outbox, que é
     * o que o financeiro consome para abrir o recebível.
     */
    void aprovar(long idOrcamento);

    void recusar(long idOrcamento, String motivo);

    /** Marca um item como executado, normalmente ao concluir a consulta. */
    void concluirItem(long idOrcamento, long idItem, Long idConsulta);
}
