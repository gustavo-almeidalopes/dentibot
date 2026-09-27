package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * O que a operação enxerga da API: métricas para o Prometheus (ST-25) e as duas
 * perguntas separadas que o orquestrador faz (ST-26) — "o processo está vivo?"
 * e "pode receber tráfego?".
 *
 * <p>Métrica não é pública: volume por rota e latência por endpoint contam a
 * topologia e o movimento de cada clínica a quem pedir.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "dentibot.metricas.senha=senha-do-scrape")
@DisplayName("Operação: métricas e probes")
class OperacaoTest extends TesteIntegracao {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    int porta;

    static HttpResponse<String> get(int porta, String caminho, String basic) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(
                URI.create("http://localhost:" + porta + caminho)).GET();
        if (basic != null) {
            req.header("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(basic.getBytes(StandardCharsets.UTF_8)));
        }
        return HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("o Prometheus lê as métricas com a credencial dele")
    void scrapeAutenticado() throws Exception {
        HttpResponse<String> r = get(porta, "/actuator/prometheus", "prometheus:senha-do-scrape");

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("jvm_memory_used_bytes");
    }

    @Test
    @DisplayName("sem credencial, ou com a errada, não há métrica")
    void scrapeAnonimoRecusado() throws Exception {
        HttpResponse<String> anonimo = get(porta, "/actuator/prometheus", null);
        assertThat(anonimo.statusCode())
                .as("%s %s", anonimo.headers().map(), anonimo.body())
                .isEqualTo(401);
        assertThat(get(porta, "/actuator/prometheus", "prometheus:chute").statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("liveness e readiness respondem sem autenticação e sem detalhe interno")
    void probes() throws Exception {
        for (String probe : new String[] {"/actuator/health/liveness", "/actuator/health/readiness"}) {
            HttpResponse<String> r = get(porta, probe, null);
            assertThat(r.statusCode()).as(probe).isEqualTo(200);
            assertThat(r.body()).as(probe).contains("\"UP\"").doesNotContain("components");
        }
    }
}
