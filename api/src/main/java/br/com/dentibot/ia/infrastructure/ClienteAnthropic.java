package br.com.dentibot.ia.infrastructure;

import br.com.dentibot.plataforma.erro.FalhaExternaException;
import br.com.dentibot.plataforma.erro.ServicoIndisponivelException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * A API de Messages da Anthropic, por HTTP puro. Sem SDK: é um POST, e o que
 * importa controlar aqui — timeout, o que sai, o que se lê de volta — fica à
 * vista em trinta linhas.
 *
 * <p>Só o gateway chama isto. Nenhum texto chega aqui sem ter passado pela
 * redação de PII do {@code GatewayDeIa}.
 */
@Component
public class ClienteAnthropic {

    public record Resultado(String texto, int tokensEntrada, int tokensSaida, String modelo) {
    }

    private static final String VERSAO_DA_API = "2023-06-01";

    private final ObjectMapper json;
    private final String chave;
    private final String modelo;
    private final URI base;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ClienteAnthropic(ObjectMapper json,
                            @Value("${dentibot.ia.chave:}") String chave,
                            @Value("${dentibot.ia.modelo:claude-sonnet-5}") String modelo,
                            @Value("${dentibot.ia.url:https://api.anthropic.com}") String base) {
        this.json = json;
        this.chave = chave;
        this.modelo = modelo;
        this.base = URI.create(base);
    }

    public boolean configurado() {
        return !chave.isBlank();
    }

    public String provedor() {
        return "anthropic";
    }

    public String modelo() {
        return modelo;
    }

    public Resultado chamar(String instrucoes, String entrada, int maxTokens) {
        if (!configurado()) {
            throw new ServicoIndisponivelException(
                    "Provedor de IA não configurado nesta instância (DENTIBOT_IA_CHAVE).");
        }
        byte[] corpo = json.writeValueAsBytes(Map.of(
                "model", modelo,
                "max_tokens", maxTokens,
                "system", instrucoes,
                "messages", List.of(Map.of("role", "user", "content", entrada))));
        HttpRequest pedido = HttpRequest.newBuilder(base.resolve("/v1/messages"))
                // Nota clínica cabe em segundos; um minuto é o teto antes de o
                // dentista desistir e o recurso parecer quebrado.
                .timeout(Duration.ofSeconds(60))
                .header("x-api-key", chave)
                .header("anthropic-version", VERSAO_DA_API)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(corpo))
                .build();

        HttpResponse<String> resposta;
        try {
            resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new FalhaExternaException("O provedor de IA não respondeu.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaExternaException("A chamada ao provedor de IA foi interrompida.", e);
        }
        if (resposta.statusCode() != 200) {
            // O corpo do erro não vai para a mensagem: pode ecoar a entrada.
            throw new FalhaExternaException(
                    "O provedor de IA recusou a chamada (HTTP " + resposta.statusCode() + ").", null);
        }

        JsonNode raiz = json.readTree(resposta.body());
        StringBuilder texto = new StringBuilder();
        for (JsonNode bloco : raiz.path("content")) {
            if ("text".equals(bloco.path("type").asString())) {
                texto.append(bloco.path("text").asString());
            }
        }
        JsonNode uso = raiz.path("usage");
        return new Resultado(texto.toString(), uso.path("input_tokens").asInt(),
                uso.path("output_tokens").asInt(), raiz.path("model").asString(modelo));
    }
}
