package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * O log que sai do processo não carrega CPF, e-mail nem telefone.
 *
 * <p>Passa pelo Logback configurado de verdade, e não pelo {@code ScrubberDePii}
 * direto: o {@code TratadorGlobalDeErros} loga a exceção de unicidade inteira,
 * cujo Detail do Postgres é {@code Key (id_clinica, cpf)=(42, 12345678901)}, e
 * um conversor correto que ninguém liga não protege nada.
 */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Log sem dado pessoal")
class LogSemPiiTest extends TesteIntegracao {

    @Test
    @DisplayName("mensagem e exceção logadas saem sem CPF nem e-mail")
    void logSaiLimpo(CapturedOutput saida) {
        LoggerFactory.getLogger("br.com.dentibot.teste").warn(
                "Paciente {} duplicado", "12345678901",
                new IllegalStateException(
                        "Detail: Key (id_clinica, cpf)=(42, 98765432100) already exists; fulana@clinica.com.br"));

        assertThat(saida.getAll())
                .contains("duplicado")
                .contains("already exists")
                .doesNotContain("12345678901")
                .doesNotContain("98765432100")
                .doesNotContain("fulana@clinica.com.br");
    }
}
