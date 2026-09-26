package br.com.dentibot.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.ia.infrastructure.ClienteAnthropic;
import br.com.dentibot.plataforma.erro.FalhaExternaException;
import br.com.dentibot.plataforma.erro.ServicoIndisponivelException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** O contrato HTTP com a Anthropic, contra um servidor local do JDK. */
@DisplayName("Cliente da API da Anthropic")
class ClienteAnthropicTest {

    private final ObjectMapper json = JsonMapper.builder().build();
    private HttpServer servidor;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> chaveRecebida = new AtomicReference<>();
    private final AtomicReference<String> versaoRecebida = new AtomicReference<>();
    private volatile int status = 200;

    @BeforeEach
    void subir() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/messages", troca -> {
            chaveRecebida.set(troca.getRequestHeaders().getFirst("x-api-key"));
            versaoRecebida.set(troca.getRequestHeaders().getFirst("anthropic-version"));
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resposta = """
                    {"model":"claude-sonnet-5","content":[{"type":"text","text":"ola "},
                     {"type":"text","text":"mundo"}],"usage":{"input_tokens":12,"output_tokens":7}}
                    """.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(status, resposta.length);
            troca.getResponseBody().write(resposta);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void descer() {
        servidor.stop(0);
    }

    private ClienteAnthropic cliente(String chave) {
        return new ClienteAnthropic(json, chave, "claude-sonnet-5",
                "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @Test
    @DisplayName("manda chave, versão, modelo, sistema e entrada; lê texto e tokens")
    void contrato() {
        ClienteAnthropic.Resultado r = cliente("chave-teste").chamar("instrucoes", "entrada", 300);

        assertThat(chaveRecebida.get()).isEqualTo("chave-teste");
        assertThat(versaoRecebida.get()).isEqualTo("2023-06-01");
        assertThat(corpoRecebido.get()).contains("\"model\":\"claude-sonnet-5\"",
                "\"system\":\"instrucoes\"", "\"content\":\"entrada\"", "\"max_tokens\":300");
        assertThat(r.texto()).isEqualTo("ola mundo");
        assertThat(r.tokensEntrada()).isEqualTo(12);
        assertThat(r.tokensSaida()).isEqualTo(7);
    }

    @Test
    @DisplayName("resposta de erro vira 502, sem ecoar o corpo")
    void erroDoProvedor() {
        status = 529;
        assertThatThrownBy(() -> cliente("chave-teste").chamar("s", "e", 10))
                .isInstanceOf(FalhaExternaException.class)
                .hasMessageContaining("HTTP 529")
                .hasMessageNotContaining("ola");
    }

    @Test
    @DisplayName("sem chave, 503 — e nenhuma chamada sai")
    void semChave() {
        assertThatThrownBy(() -> cliente("").chamar("s", "e", 10))
                .isInstanceOf(ServicoIndisponivelException.class);
        assertThat(corpoRecebido.get()).isNull();
    }
}
