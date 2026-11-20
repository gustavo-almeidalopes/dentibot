package br.com.dentibot.ia.infrastructure;

import br.com.dentibot.plataforma.erro.FalhaExternaException;
import br.com.dentibot.plataforma.erro.ServicoIndisponivelException;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A Claude, pelo SDK oficial da Anthropic. Só o gateway chama isto, e nenhum
 * texto chega aqui sem ter passado pela redação de PII do {@code GatewayDeIa}.
 *
 * <p>Três escolhas que vêm da orientação da Anthropic para os modelos atuais:
 * <ul>
 *   <li>saída estruturada ({@code output_config.format}): o JSON que o recurso
 *       espera é garantido pelo schema, não pedido no prompt;</li>
 *   <li>{@code max_tokens} folgado: com raciocínio adaptativo, pensar também
 *       consome o teto, e resposta cortada é JSON inválido;</li>
 *   <li>fallback no servidor para recusa do classificador de segurança — sem
 *       ele, a chamada recusada simplesmente para. Recusa que sobra é tratada
 *       como falha, nunca como texto.</li>
 * </ul>
 */
@Component
public class ClienteAnthropic implements DisposableBean {

    public record Resultado(String texto, int tokensEntrada, int tokensSaida, String modelo) {
    }

    private static final long MAX_TOKENS = 16_000L;
    private static final String BETA_FALLBACK = "server-side-fallback-2026-07-01";

    private final AnthropicClient cliente;
    private final String modelo;

    public ClienteAnthropic(@Value("${dentibot.ia.chave:}") String chave,
                            @Value("${dentibot.ia.modelo:claude-opus-5}") String modelo,
                            @Value("${dentibot.ia.url:https://api.anthropic.com}") String base) {
        this.modelo = modelo;
        this.cliente = chave.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(chave)
                .baseUrl(base)
                // O dentista espera na tela: mais de 90 s e o recurso parece
                // quebrado. As duas novas tentativas do SDK (429, 5xx, rede)
                // ficam dentro desse orçamento na prática.
                .timeout(Duration.ofSeconds(90))
                .maxRetries(2)
                .build();
    }

    public boolean configurado() {
        return cliente != null;
    }

    public String provedor() {
        return "anthropic";
    }

    public String modelo() {
        return modelo;
    }

    public Resultado chamar(String instrucoes, String entrada, Map<String, Object> esquema,
                            String esforco) {
        if (cliente == null) {
            throw new ServicoIndisponivelException(
                    "Provedor de IA não configurado nesta instância (DENTIBOT_IA_CHAVE).");
        }
        OutputConfig.Builder saida = OutputConfig.builder().effort(OutputConfig.Effort.of(esforco));
        if (esquema != null) {
            saida.format(JsonOutputFormat.builder()
                    .schema(JsonOutputFormat.Schema.builder()
                            .additionalProperties(esquema.entrySet().stream().collect(
                                    Collectors.toMap(Map.Entry::getKey, e -> JsonValue.from(e.getValue()))))
                            .build())
                    .build());
        }
        MessageCreateParams params = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(MAX_TOKENS)
                .system(instrucoes)
                .addUserMessage(entrada)
                .outputConfig(saida.build())
                .putAdditionalHeader("anthropic-beta", BETA_FALLBACK)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();

        Message resposta;
        try {
            resposta = cliente.messages().create(params);
        } catch (AnthropicServiceException e) {
            // A mensagem do provedor não vai adiante: pode ecoar a entrada.
            throw new FalhaExternaException(
                    "O provedor de IA recusou a chamada (HTTP " + e.statusCode() + ").", e);
        } catch (AnthropicException e) {
            throw new FalhaExternaException("O provedor de IA não respondeu.", e);
        }

        StopReason motivo = resposta.stopReason().orElse(null);
        if (StopReason.REFUSAL.equals(motivo)) {
            String categoria = resposta.stopDetails()
                    .flatMap(d -> d.category().map(Object::toString)).orElse("sem categoria");
            throw new FalhaExternaException(
                    "O modelo recusou esta solicitação (" + categoria + "). Escreva o texto à mão.", null);
        }
        if (StopReason.MAX_TOKENS.equals(motivo)) {
            throw new FalhaExternaException("A resposta do modelo foi cortada no limite de tokens.", null);
        }
        String texto = resposta.content().stream()
                .flatMap(bloco -> bloco.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());
        return new Resultado(texto, Math.toIntExact(resposta.usage().inputTokens()),
                Math.toIntExact(resposta.usage().outputTokens()), resposta.model().asString());
    }

    @Override
    public void destroy() {
        if (cliente != null) {
            cliente.close();
        }
    }
}
