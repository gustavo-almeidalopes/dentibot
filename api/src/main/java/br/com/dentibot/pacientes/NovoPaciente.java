package br.com.dentibot.pacientes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Comando de criação de paciente — a ficha inteira.
 *
 * <p>Note o que NÃO está aqui: {@code idClinica}. O tenant vem do token
 * (invariante 5). Se estivesse no corpo, bastaria o cliente trocar o número para
 * cadastrar paciente na clínica de outro — e o RLS recusaria, mas o desenho já
 * estaria errado antes do banco precisar salvar a situação.
 *
 * <p>Os campos cresceram junto com o formulário: antes eram quatro (nome, CPF,
 * telefone, e-mail) e a triagem de saúde não tinha onde cair. Data de
 * nascimento, alergia e condição sistêmica não são "campos extras" — são o que
 * decide dose de anestésico e necessidade de profilaxia antibiótica.
 */
public record NovoPaciente(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @Pattern(regexp = "^[0-9]{11}$", message = "CPF deve ter 11 dígitos, sem pontuação")
        String cpf,
        @Size(max = 20) String rg,
        @Past(message = "data de nascimento no futuro") LocalDate dataNascimento,
        @Size(max = 20) String telefoneCelular,
        @Email @Size(max = 254) String email,
        @Size(max = 80) String profissao,
        @Size(max = 150) String responsavelLegal,

        @Pattern(regexp = "^[0-9]{8}$", message = "CEP deve ter 8 dígitos, sem pontuação")
        String cep,
        @Size(max = 255) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 100) String complemento,
        @Size(max = 100) String bairro,
        @Size(max = 100) String cidade,
        @Pattern(regexp = "^[A-Z]{2}$", message = "UF com duas letras maiúsculas") String uf,

        @Valid Anamnese anamnese,

        Long idPlanoConvenio,
        @Size(max = 60) String numeroCarteirinha) {

    /**
     * O mínimo que a recepção consegue salvar com o paciente ainda no balcão:
     * nome e um telefone para confirmar a consulta. O resto da ficha se completa
     * na primeira consulta — negar o cadastro por causa do CEP seria trocar um
     * paciente por um campo.
     */
    public static NovoPaciente basico(String nomeCompleto, String telefoneCelular) {
        return new NovoPaciente(nomeCompleto, null, null, null, telefoneCelular, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Triagem de saúde inicial.
     *
     * <p>Os booleanos são {@code Boolean} e não {@code boolean} porque "não
     * respondeu" não é "não". Um {@code false} onde a pergunta ficou em branco
     * diria ao dentista que o paciente NEGOU ser alérgico — que é a única das
     * três respostas capaz de machucar alguém.
     */
    public record Anamnese(
            Boolean emTratamentoMedico,
            @Size(max = 255) String medicamentoContinuo,
            @Size(max = 255) String alergia,
            Boolean condicaoSistemica,
            /** Nulo para quem a pergunta não se aplica. */
            Boolean gravidez,
            @Pattern(regexp = "^(dor|estetica|limpeza|rotina)$") String motivoConsulta,
            Boolean sensibilidade,
            Boolean sangramentoGengival) {
    }
}
