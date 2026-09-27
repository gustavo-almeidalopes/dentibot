package br.com.dentibot.comunicacao.interfaces.http;

import br.com.dentibot.comunicacao.application.Conversa;
import br.com.dentibot.comunicacao.application.Conversa.Recebida;
import br.com.dentibot.comunicacao.application.Conversa.StatusRecebido;
import br.com.dentibot.comunicacao.infrastructure.ClienteWhatsApp;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Webhook da Meta. Público na cadeia de segurança — a autenticação é a
 * assinatura HMAC, conferida sobre o corpo CRU e ANTES do parse: JSON de quem
 * não assinou não chega nem ao Jackson.
 *
 * <p>Erro no processamento vira 500 de propósito: a Meta reenvia, e o
 * {@code id_externo} único torna o reenvio inofensivo para o que já foi tratado.
 */
@RestController
@RequestMapping("/api/v1/webhooks/whatsapp")
public class WebhookWhatsAppController {

    private final ClienteWhatsApp whatsapp;
    private final Conversa conversa;
    private final ObjectMapper json;

    public WebhookWhatsAppController(ClienteWhatsApp whatsapp, Conversa conversa, ObjectMapper json) {
        this.whatsapp = whatsapp;
        this.conversa = conversa;
        this.json = json;
    }

    /** O aperto de mão do cadastro: devolve o desafio só a quem sabe o token. */
    @GetMapping
    public ResponseEntity<String> verificar(
            @RequestParam(name = "hub.mode", required = false) String modo,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String desafio) {
        if (!"subscribe".equals(modo) || !whatsapp.verificacaoValida(token) || desafio == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(desafio);
    }

    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestBody byte[] corpo,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura) {
        if (!whatsapp.assinaturaValida(corpo, assinatura)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        JsonNode raiz = json.readTree(corpo);
        for (JsonNode entrada : raiz.path("entry")) {
            for (JsonNode mudanca : entrada.path("changes")) {
                JsonNode valor = mudanca.path("value");
                String numero = valor.path("metadata").path("phone_number_id").asString("");
                for (JsonNode s : valor.path("statuses")) {
                    conversa.status(new StatusRecebido(numero, s.path("id").asString(""),
                            s.path("status").asString(""), instante(s)));
                }
                for (JsonNode m : valor.path("messages")) {
                    String texto = texto(m);
                    conversa.receber(new Recebida(numero, m.path("id").asString(""),
                            m.path("from").asString(""),
                            texto == null ? "[" + m.path("type").asString("mídia") + "]" : texto,
                            texto == null));
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    /** Texto, botão ou resposta de lista; null para o que não é texto. */
    private static String texto(JsonNode m) {
        String t = switch (m.path("type").asString("")) {
            case "text" -> m.path("text").path("body").asString("");
            case "button" -> m.path("button").path("text").asString("");
            case "interactive" -> {
                JsonNode i = m.path("interactive");
                yield i.has("button_reply") ? i.path("button_reply").path("title").asString("")
                        : i.path("list_reply").path("title").asString("");
            }
            default -> null;
        };
        if (t == null || t.isBlank()) {
            return null;
        }
        return t.length() > 4000 ? t.substring(0, 4000) : t;
    }

    private static Instant instante(JsonNode n) {
        long segundos = n.path("timestamp").asLong(0);
        return segundos > 0 ? Instant.ofEpochSecond(segundos) : Instant.now();
    }
}
