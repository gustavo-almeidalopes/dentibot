package br.com.dentibot.plataforma.seguranca;

/**
 * O que se protege. Granularidade escolhida para caber numa tabela que um
 * humano consegue ler inteira — se a lista crescer a ponto de precisar de busca,
 * a matriz deixou de ser auditável e virou configuração.
 */
public enum Recurso {
    /** Consulta, bloqueio, disponibilidade. */
    AGENDA,
    /** Cadastro do paciente: nome, contato, status. */
    PACIENTE,
    /** Anamnese, evolução, odontograma, anexo. Dado de saúde, LGPD art. 11. */
    PRONTUARIO,
    /** Orçamento e plano de tratamento. */
    ORCAMENTO,
    /** Recebível, ledger, cobrança, despesa, comissão, NFS-e. */
    FINANCEIRO,
    /** Produto, fornecedor, movimentação. */
    ESTOQUE,
    /** Configurações da clínica, procedimentos, convênios. */
    CONFIGURACAO,
    /** Usuários e dentistas da clínica. */
    EQUIPE,
    /** Termos, consentimentos, solicitações de titular. */
    LGPD,
    /** Trilha de auditoria da própria clínica. */
    AUDITORIA,
    /** Assinatura do SaaS, faturas, plano. */
    BILLING
}
