package br.com.dentibot.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.ia.infrastructure.ClienteAnthropic;
import br.com.dentibot.plataforma.erro.FalhaExternaException;
import br.com.dentibot.plataforma.erro.ServicoIndisponivelException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O contrato com a API da Anthropic, pelo SDK, contra um servidor local do JDK. */
@DisplayName("Cliente da API da Anthropic")
class ClienteAnthropicTest {

    private static final String MENSAGEM = """
            {"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5",
             "content":[{"type":"text","text":"{\\"ok\\":"},{"type":"text","text":"true}"}],
             "stop_reason":"%s","stop_sequence":null,%s
             "usage":{"input_tokens":12,"output_tokens":7}}
            """;

    private HttpServer servidor;
    private final AtomicReference<String> corpo = new AtomicReference<>();
    private final AtomicReference<String> chave = new AtomicReference<>();
    private final AtomicReference<String> beta = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String resposta = MENSAGEM.formatted("end_turn", "");

    @BeforeEach
    void subir() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/messages", troca -> {
            chave.set(troca.getRequestHeaders().getFirst("x-api-key"));
            beta.set(troca.getRequestHeaders().getFirst("anthropic-beta"));
            corpo.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = (status == 200 ? resposta
                    : "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"x\"}}")
                    .getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            troca.getResponseBody().write(bytes);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void descer() {
        servidor.stop(0);
    }

    private ClienteAnthropic cliente(String chaveDeApi) {
        return new ClienteAnthropic(chaveDeApi, "claude-opus-5",
                "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @Test
    @DisplayName("manda chave, modelo, sistema, schema, esforço e fallback; lê texto e tokens")
    void contrato() {
        Map<String, Object> esquema = Map.of("type", "object", "additionalProperties", false,
                "required", List.of("ok"), "properties", Map.of("ok", Map.of("type", "boolean")));

        ClienteAnthropic.Resultado r = cliente("chave-teste").chamar("instrucoes", "entrada", esquema, "low");

        assertThat(chave.get()).isEqualTo("chave-teste");
        assertThat(beta.get()).contains("server-side-fallback-2026-07-01");
        assertThat(corpo.get()).contains("\"model\":\"claude-opus-5\"", "\"system\":\"instrucoes\"",
                "\"max_tokens\":16000", "\"effort\":\"low\"", "\"json_schema\"",
                "\"fallbacks\":\"default\"");
        assertThat(r.texto()).isEqualTo("{\"ok\":true}");
        assertThat(r.tokensEntrada()).isEqualTo(12);
        assertThat(r.tokensSaida()).isEqualTo(7);
    }

    @Test
    @DisplayName("recusa do modelo vira falha — nunca texto que alguém leria como resposta")
    void recusa() {
        resposta = MENSAGEM.formatted("refusal",
                "\"stop_details\":{\"type\":\"refusal\",\"category\":null,\"explanation\":null},");
        assertThatThrownBy(() -> cliente("chave-teste").chamar("s", "e", null, "low"))
                .isInstanceOf(FalhaExternaException.class)
                .hasMessageContaining("recusou");
    }

    @Test
    @DisplayName("erro HTTP vira 502, sem ecoar o corpo do provedor")
    void erroDoProvedor() {
        status = 400;
        assertThatThrownBy(() -> cliente("chave-teste").chamar("s", "e", null, "low"))
                .isInstanceOf(FalhaExternaException.class)
                .hasMessageContaining("HTTP 400");
    }

    @Test
    @DisplayName("sem chave, 503 — e nenhuma chamada sai")
    void semChave() {
        assertThatThrownBy(() -> cliente("").chamar("s", "e", null, "low"))
                .isInstanceOf(ServicoIndisponivelException.class);
        assertThat(corpo.get()).isNull();
    }
}
