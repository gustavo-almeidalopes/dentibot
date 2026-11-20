package br.com.dentibot.prontuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O vocabulário do odontograma em Java é o do CHECK da V12 — e não uma segunda verdade. */
@DisplayName("Condições do odontograma")
class CondicoesDoOdontogramaTest {

    @Test
    @DisplayName("a lista e o padrão de validação são os do CHECK do banco")
    void mesmaListaDoBanco() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V12__prontuario.sql"),
                StandardCharsets.UTF_8);
        Matcher bloco = Pattern.compile("condicao\\s+TEXT\\s+NOT NULL CHECK \\(condicao IN \\(([^)]*)\\)\\)")
                .matcher(sql);
        assertThat(bloco.find()).as("o CHECK de condicao mudou de forma na V12").isTrue();
        var doBanco = Pattern.compile("'([a-z_]+)'").matcher(bloco.group(1)).results()
                .map(m -> m.group(1)).sorted().toList();

        assertThat(NovoLancamentoOdontograma.CONDICOES.stream().sorted().toList()).isEqualTo(doBanco);
        assertThat(doBanco).allMatch(c -> c.matches(NovoLancamentoOdontograma.PADRAO_CONDICAO));
        assertThat("cárie").doesNotMatch(NovoLancamentoOdontograma.PADRAO_CONDICAO);
    }
}
