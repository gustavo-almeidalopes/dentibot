package br.com.dentibot.identidade;

import java.time.LocalDate;

/**
 * Uma pessoa física da clínica, como a ficha a coleta.
 *
 * <p>Substituiu quatro parâmetros soltos ({@code nome, cpf, telefone, email})
 * em {@link IdentidadeApi#criarPessoa}. O motivo não é estético: a ficha do
 * paciente tem quinze campos, e quinze parâmetros posicionais do mesmo tipo
 * {@code String} é a assinatura em que trocar cidade por bairro compila.
 *
 * <p>Endereço e documentos ficam aqui, e não em {@code pacientes}, porque a
 * mesma pessoa pode ser paciente e responsável por outro — o dado pessoal mora
 * num lugar só (V3).
 */
public record DadosPessoais(
        String nomeCompleto,
        String cpf,
        String rg,
        LocalDate dataNascimento,
        String telefoneCelular,
        String email,
        String profissao,
        String responsavelLegal,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf) {

    /**
     * O que a tela de equipe sabe de um membro na hora de criá-lo: nome e
     * e-mail. O resto a própria pessoa completa depois, no perfil.
     */
    public static DadosPessoais basico(String nomeCompleto, String email) {
        return new DadosPessoais(nomeCompleto, null, null, null, null, email,
                null, null, null, null, null, null, null, null, null);
    }
}
