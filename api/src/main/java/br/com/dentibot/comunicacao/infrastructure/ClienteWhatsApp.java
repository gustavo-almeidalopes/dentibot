package br.com.dentibot.comunicacao.infrastructure;

import br.com.dentibot.plataforma.erro.FalhaExternaException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * WhatsApp Cloud API da Meta, pelo {@code HttpClient} do JDK — duas chamadas
 * não justificam SDK.
 *
 * <p>Dentro da janela de 24 horas depois da última mensagem do paciente, texto
 * livre. Fora dela, a Meta só entrega modelo aprovado; o texto vai como o único
 * parâmetro do modelo {@code dentibot.whatsapp.modelo}, que a clínica cadastra
 * no Business Manager ("Mensagem da sua clínica: {{1}}").
 *
 * <p>Sem token, {@link #configurado()} é falso e nada sai: o despachante nem
 * trava a fila. Sem segredo do app, nenhum webhook é aceito — assinatura que
 * não dá para conferir é assinatura inválida.
 */
@Component
public class ClienteWhatsApp {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json;
    private final String token;
    private final String segredoDoApp;
    private final String tokenDeVerificacao;
    private final String url;
    private final String modelo;
    private final String idioma;

    public ClienteWhatsApp(ObjectMapper json,
                           @Value("${dentibot.whatsapp.token:}") String token,
                           @Value("${dentibot.whatsapp.segredo-do-app:}") String segredoDoApp,
                           @Value("${dentibot.whatsapp.token-de-verificacao:}") String tokenDeVerificacao,
                           @Value("${dentibot.whatsapp.url:https://graph.facebook.com/v23.0}") String url,
                           @Value("${dentibot.whatsapp.modelo:dentibot_aviso}") String modelo,
                           @Value("${dentibot.whatsapp.idioma:pt_BR}") String idioma) {
        this.json = json;
        this.token = token;
        this.segredoDoApp = segredoDoApp;
        this.tokenDeVerificacao = tokenDeVerificacao;
        this.url = url;
        this.modelo = modelo;
        this.idioma = idioma;
    }

    public boolean configurado() {
        return !token.isBlank();
    }

    /** Devolve o wamid, que amarra entrega, leitura e resposta a esta mensagem. */
    public String enviar(String phoneNumberId, String para, String texto, boolean dentroDaJanela) {
        Map<String, Object> corpo = dentroDaJanela
                ? Map.of("messaging_product", "whatsapp", "to", para, "type", "text",
                        "text", Map.of("preview_url", false, "body", texto))
                : Map.of("messaging_product", "whatsapp", "to", para, "type", "template",
                        "template", Map.of("name", modelo,
                                "language", Map.of("code", idioma),
                                "components", List.of(Map.of("type", "body",
                                        "parameters", List.of(Map.of("type", "text",
                                                "text", texto.replaceAll("\\s+", " ")))))));
        HttpRequest pedido = HttpRequest.newBuilder(
                        URI.create(url + "/" + phoneNumberId + "/messages"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                .build();
        HttpResponse<String> resposta;
        try {
            resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new FalhaExternaException("WhatsApp fora do ar: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaExternaException("Envio ao WhatsApp interrompido.", e);
        }
        JsonNode raiz;
        try {
            raiz = json.readTree(resposta.body());
        } catch (RuntimeException e) {
            throw new FalhaExternaException("WhatsApp respondeu HTTP " + resposta.statusCode()
                    + " sem JSON.", null);
        }
        if (resposta.statusCode() >= 400) {
            // A mensagem de erro da Meta, nunca o corpo: ele ecoa o telefone.
            throw new FalhaExternaException("WhatsApp recusou: " + raiz.path("error").path("message")
                    .asString("HTTP " + resposta.statusCode()), null);
        }
        return raiz.path("messages").path(0).path("id").asString("");
    }

    /** X-Hub-Signature-256: HMAC-SHA256 do corpo CRU com o segredo do app. */
    public boolean assinaturaValida(byte[] corpo, String cabecalho) {
        if (segredoDoApp.isBlank() || cabecalho == null || !cabecalho.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredoDoApp.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] recebido = HexFormat.of().parseHex(cabecalho.substring("sha256=".length()));
            return MessageDigest.isEqual(mac.doFinal(corpo), recebido);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    /** O aperto de mão do cadastro do webhook na Meta. */
    public boolean verificacaoValida(String recebido) {
        return !tokenDeVerificacao.isBlank() && recebido != null
                && MessageDigest.isEqual(tokenDeVerificacao.getBytes(StandardCharsets.UTF_8),
                        recebido.getBytes(StandardCharsets.UTF_8));
    }
}
