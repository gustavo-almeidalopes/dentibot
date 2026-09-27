package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * ST-20: o contrato da API é um arquivo versionado, {@code api/openapi.json}, e
 * este teste falha quando o código muda o contrato sem o arquivo mudar junto.
 *
 * <p>É o que torna a quebra visível no PR: mudar um endpoint obriga a
 * regenerar o arquivo, e a mudança aparece no diff, onde o revisor vê — e onde
 * o teste de clientes do web e do app ({@code contrato.test.mjs}) confere cada
 * chamada contra ele. Para regenerar: {@code ./mvnw test
 * -Dtest=ContratoOpenApiTest -Dcontrato.atualizar=true}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "springdoc.api-docs.enabled=true")
@DisplayName("Contrato OpenAPI (ST-20)")
class ContratoOpenApiTest extends TesteIntegracao {

    private static final Path ARQUIVO = Path.of("openapi.json");

    @LocalServerPort private int porta;
    @Autowired private ObjectMapper json;

    @Test
    @DisplayName("o contrato gerado é o contrato versionado")
    void contratoVersionado() throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/v3/api-docs")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(200);

        ObjectNode gerado = (ObjectNode) json.readTree(r.body());
        // A porta é sorteada a cada execução; servidor não é contrato.
        gerado.remove("servers");
        String atual = json.writerWithDefaultPrettyPrinter().writeValueAsString(gerado)
                .replace("\r\n", "\n") + "\n";

        if (Boolean.getBoolean("contrato.atualizar") || !Files.exists(ARQUIVO)) {
            Files.writeString(ARQUIVO, atual, StandardCharsets.UTF_8);
        }
        assertThat(atual)
                .as("A API mudou e api/openapi.json não. Rode com -Dcontrato.atualizar=true, "
                        + "confira o diff e commite junto.")
                .isEqualTo(Files.readString(ARQUIVO, StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }
}
