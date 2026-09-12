package br.com.dentibot.identidade;

/**
 * Projeção de exibição de uma pessoa: o que a recepção precisa para montar uma
 * agenda ou uma lista.
 *
 * <p>Deliberadamente sem CPF, sem endereço e sem data de nascimento. A camada 5
 * exige que a permissão por coluna seja resolvida ESCOLHENDO a projeção na
 * consulta, e não carregando o registro completo para limpar campos depois —
 * porque o campo que alguém esquecer de limpar vaza, e porque o dado que nunca
 * saiu do banco não aparece em log, em stack trace nem em telemetria.
 */
public record PessoaResumo(
        long idPessoa,
        String nomeCompleto,
        String telefoneCelular) {
}
