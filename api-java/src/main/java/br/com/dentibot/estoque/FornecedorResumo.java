package br.com.dentibot.estoque;

public record FornecedorResumo(
        long idFornecedor,
        String razaoSocial,
        String cnpj,
        String telefone,
        String emailVendedor,
        boolean ativo) {
}
