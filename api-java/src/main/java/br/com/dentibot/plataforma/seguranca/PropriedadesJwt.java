package br.com.dentibot.plataforma.seguranca;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param chavePrivadaPem RSA PKCS#8 em PEM, via variável de ambiente. Vazio só é
 *                        aceito nos perfis de desenvolvimento e teste, onde um
 *                        par efêmero é gerado no boot — ver {@link ChavesJwt}.
 * @param validadeAccess  curta de propósito: a blocklist de JTI só precisa
 *                        cobrir a janela entre a revogação e a expiração natural.
 */
@ConfigurationProperties(prefix = "dentibot.jwt")
public record PropriedadesJwt(
        String chavePrivadaPem,
        String chavePublicaPem,
        String emissor,
        Duration validadeAccess,
        Duration validadeRefresh) {

    public PropriedadesJwt {
        if (emissor == null || emissor.isBlank()) {
            emissor = "https://api.dentibot.com.br";
        }
        if (validadeAccess == null) {
            validadeAccess = Duration.ofMinutes(15);
        }
        if (validadeRefresh == null) {
            validadeRefresh = Duration.ofDays(14);
        }
    }

    public boolean temChaveConfigurada() {
        return chavePrivadaPem != null && !chavePrivadaPem.isBlank();
    }
}
