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
 *                           replayado contra esta API. Lista vazia desliga a
 *                           checagem — aceitável só em teste, onde não há
 *                           browser nenhum do outro lado.
 */
@ConfigurationProperties(prefix = "dentibot.clerk")
public record PropriedadesClerk(String emissor, List<String> partesAutorizadas) {

    public PropriedadesClerk {
        partesAutorizadas = partesAutorizadas == null ? List.of() : List.copyOf(partesAutorizadas);
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
