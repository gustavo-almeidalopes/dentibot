package br.com.dentibot.pacientes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Comando de criação de paciente.
 *
 * <p>Note o que NÃO está aqui: {@code idClinica}. O tenant vem do token
 * (invariante 5). Se estivesse no corpo, bastaria o cliente trocar o número para
 * cadastrar paciente na clínica de outro — e o RLS recusaria, mas o desenho já
 * estaria errado antes do banco precisar salvar a situação.
 */
public record NovoPaciente(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @Pattern(regexp = "^[0-9]{11}$", message = "CPF deve ter 11 dígitos, sem pontuação")
        String cpf,
        LocalDate dataNascimento,
        @Size(max = 20) String telefoneCelular,
        @Email @Size(max = 254) String email,
        Long idPlanoConvenio,
        @Size(max = 60) String numeroCarteirinha) {
}
