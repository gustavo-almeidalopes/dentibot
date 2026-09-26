package br.com.dentibot.estoque;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NovoFornecedor(
        @NotBlank @Size(max = 144) String razaoSocial,
        @Pattern(regexp = "^[0-9]{14}$", message = "CNPJ deve ter 14 dígitos, sem pontuação")
        String cnpj,
        @Size(max = 20) String telefone,
        @Email @Size(max = 254) String emailVendedor) {
}
