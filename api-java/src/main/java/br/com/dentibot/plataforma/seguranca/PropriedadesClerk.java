package br.com.dentibot.plataforma.seguranca;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do Clerk como emissor dos tokens.
 *
 * @param emissor            a Frontend API da instância, exatamente como aparece
 *                           no claim {@code iss} — por exemplo
 *                           {@code https://clean-mule-42.clerk.accounts.dev}. É
 *                           daqui que sai a URL do JWKS.
 * @param partesAutorizadas  origens que o claim {@code azp} pode conter. O Clerk
 *                           põe nele a origem que pediu o token; conferir isso
 *                           impede que um token emitido para outro site seja
 *                           replayado contra esta API. Obrigatória: lista vazia
 *                           derruba a subida, em vez de desligar a checagem.
 */
@ConfigurationProperties(prefix = "dentibot.clerk")
public record PropriedadesClerk(String emissor, List<String> partesAutorizadas) {

    public PropriedadesClerk {
        partesAutorizadas = partesAutorizadas == null ? List.of()
                : partesAutorizadas.stream().filter(p -> !p.isBlank()).toList();
        if (partesAutorizadas.isEmpty()) {
            // DENTIBOT_CLERK_ORIGINS="" desligava a checagem do azp em silêncio:
            // o validador aceita qualquer origem quando a lista vem vazia, e um
            // token emitido para outro site da mesma instância passava.
            throw new IllegalStateException(
                    "DENTIBOT_CLERK_ORIGINS vazia: sem origens autorizadas, a checagem do azp "
                            + "aceitaria token emitido para qualquer site.");
        }
    }

    /**
     * O Clerk publica o JWKS no caminho padrão do OIDC sob a Frontend API. O
     * Spring busca, cacheia e reage à rotação de chave sozinho — nada disto
     * precisa de código nosso.
     */
    public String urlDoJwks() {
        if (emissor == null || emissor.isBlank()) {
            throw new IllegalStateException("""
                    DENTIBOT_CLERK_ISSUER não configurada. Sem o emissor não há JWKS, e sem \
                    JWKS a API não consegue validar nenhum token — toda rota autenticada \
                    responderia 401. O valor é a Frontend API da instância, visível no \
                    dashboard do Clerk em API Keys → Show JWT public key.""");
        }
        return emissor.replaceAll("/+$", "") + "/.well-known/jwks.json";
    }
}
