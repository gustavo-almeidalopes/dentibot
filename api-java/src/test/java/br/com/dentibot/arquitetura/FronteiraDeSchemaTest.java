package br.com.dentibot.arquitetura;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O irmão SQL do {@link FronteiraDeModulosTest}.
 *
 * <p>O ArchUnit pega import cruzado entre módulos em Java. Ele NÃO pega o que
 * acontece dentro de uma string: um {@code JOIN} — ou mesmo um {@code SELECT}
 * simples — contra a tabela de outro módulo passa por qualquer análise de
 * bytecode. E é justamente a forma mais tentadora de furar a fronteira, porque
 * na revisão parece só uma consulta eficiente.
 *
 * <p>Este teste varre o código-fonte e reprova quando o SQL de um módulo cita o
 * schema de outro módulo de domínio.
 *
 * <p>{@code plataforma} e {@code auditoria} ficam de fora da regra: são
 * infraestrutura compartilhada (outbox, idempotência, trilha de auditoria) que
 * todo módulo legitimamente escreve — e sempre pela classe dedicada, nunca por
 * JOIN.
 */
@DisplayName("Fronteira de schema entre módulos")
class FronteiraDeSchemaTest {

    private static final Path RAIZ = Path.of("src/main/java/br/com/dentibot");

    /** Schemas de domínio: cada um pertence a exatamente um módulo. */
    private static final Set<String> SCHEMAS_DE_DOMINIO = Set.of(
            "clinicas", "identidade", "pacientes", "agenda", "prontuario",
            "orcamento", "financeiro", "billing", "estoque", "lgpd");

    /** Infraestrutura compartilhada: qualquer módulo pode escrever. */
    private static final Set<String> SCHEMAS_COMPARTILHADOS = Set.of("plataforma", "auditoria");

    private static final Pattern REFERENCIA_A_SCHEMA =
            Pattern.compile("\\b([a-z_]+)\\.[a-z_]+\\b");

    @Test
    @DisplayName("o SQL de um módulo não cita o schema de outro módulo")
    void sqlNaoAtravessaModulo() throws IOException {
        List<String> violacoes = new ArrayList<>();

        try (Stream<Path> arquivos = Files.walk(RAIZ)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                String modulo = moduloDoArquivo(arquivo);
                if (modulo == null || SCHEMAS_COMPARTILHADOS.contains(modulo)) {
                    continue;
                }
                String fonte = Files.readString(arquivo, StandardCharsets.UTF_8);
                for (String sql : extrairBlocosSql(fonte)) {
                    for (String schema : schemasCitados(sql)) {
                        if (SCHEMAS_DE_DOMINIO.contains(schema) && !schema.equals(modulo)) {
                            violacoes.add("%s (módulo '%s') consulta o schema '%s'"
                                    .formatted(RAIZ.relativize(arquivo), modulo, schema));
                        }
                    }
                }
            }
        }

        assertThat(violacoes.stream().distinct().toList())
                .as("""
                    SQL atravessando a fronteira de módulo. Use a porta pública do outro \
                    módulo (br.com.dentibot.<modulo>.<Modulo>Api) ou um evento. O JOIN \
                    funciona hoje e é exatamente o que impede extrair o módulo depois.""")
                .isEmpty();
    }

    /** Só os blocos de texto que realmente parecem SQL. */
    private static List<String> extrairBlocosSql(String fonte) {
        List<String> blocos = new ArrayList<>();
        Matcher m = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL).matcher(fonte);
        while (m.find()) {
            String bloco = m.group(1);
            String maiusculo = bloco.toUpperCase(java.util.Locale.ROOT);
            if (maiusculo.contains("SELECT ") || maiusculo.contains("INSERT ")
                    || maiusculo.contains("UPDATE ") || maiusculo.contains("DELETE ")) {
                blocos.add(bloco);
            }
        }
        // SQL em string simples também conta.
        Matcher simples = Pattern.compile("\"((?:SELECT|INSERT|UPDATE|DELETE)[^\"]*)\"",
                Pattern.CASE_INSENSITIVE).matcher(fonte);
        while (simples.find()) {
            blocos.add(simples.group(1));
        }
        return blocos;
    }

    private static Set<String> schemasCitados(String sql) {
        Set<String> schemas = new java.util.HashSet<>();
        Matcher m = REFERENCIA_A_SCHEMA.matcher(sql);
        while (m.find()) {
            schemas.add(m.group(1));
        }
        return schemas;
    }

    private static String moduloDoArquivo(Path arquivo) {
        Path relativo = RAIZ.relativize(arquivo);
        return relativo.getNameCount() < 2 ? null : relativo.getName(0).toString();
    }

    @Test
    @DisplayName("a varredura realmente encontra SQL — senão o teste acima é vácuo")
    void aVarreduraNaoEstaVazia() throws IOException {
        int blocos = 0;
        try (Stream<Path> arquivos = Files.walk(RAIZ)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                blocos += extrairBlocosSql(
                        Files.readString(arquivo, StandardCharsets.UTF_8)).size();
            }
        }
        // Sem esta asserção, um erro no extrator faria o teste de fronteira
        // passar para sempre sem olhar nada — a armadilha do teste que fica
        // verde com o conjunto vazio.
        assertThat(blocos)
                .as("o extrator de SQL precisa estar encontrando consultas de verdade")
                .isGreaterThan(20);
    }
}
