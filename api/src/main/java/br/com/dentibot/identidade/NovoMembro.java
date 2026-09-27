package br.com.dentibot.identidade;

import br.com.dentibot.plataforma.contexto.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Comando de admissão de um membro da equipe.
 *
 * <p>Note o que NÃO está aqui: senha. Quem cadastra não escolhe credencial de
 * outra pessoa — a pessoa cria a própria conta no Clerk, e o vínculo com esta
 * linha acontece na primeira entrada, por e-mail verificado.
 *
 * <p>CRO só faz sentido para dentista. A validação cruzada mora no serviço, não
 * em anotação: "obrigatório quando papel = DENTISTA" não cabe num @NotBlank.
 */
public record NovoMembro(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull Papel papel,
        @Size(max = 20) String croNumero,
        @Pattern(regexp = "^[A-Z]{2}$", message = "UF do CRO em duas letras maiúsculas")
        String croUf,
        @Size(max = 80) String especialidade) {
}
