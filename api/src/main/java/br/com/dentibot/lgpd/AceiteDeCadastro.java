package br.com.dentibot.lgpd;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O que a pessoa marcou no cadastro, e de qual versão dos termos.
 *
 * <p>Sem hora e sem IP de propósito: os dois vêm do servidor. Hora ou IP
 * mandados pelo cliente seriam os que o cliente escolheu.
 *
 * @param dadosDeSaude     só no cadastro de paciente; {@code null} na clínica.
 * @param responsavelLegal só quando o paciente é menor de idade.
 * @param representacao    só no cadastro de clínica.
 */
public record AceiteDeCadastro(
        @NotBlank @Size(max = 10) String versao,
        @NotNull @AssertTrue(message = "é preciso aceitar os termos de uso e a política de privacidade")
        Boolean termosEPrivacidade,
        Boolean dadosDeSaude,
        Boolean responsavelLegal,
        Boolean representacao,
        Boolean novidades) {
}
