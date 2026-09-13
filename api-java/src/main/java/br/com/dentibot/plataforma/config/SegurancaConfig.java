package br.com.dentibot.plataforma.config;

import br.com.dentibot.plataforma.seguranca.PropriedadesClerk;
import br.com.dentibot.plataforma.seguranca.ValidadorDeParteAutorizada;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * O Clerk é o emissor dos tokens; esta aplicação só valida.
 *
 * <p>Saiu daqui, na V18, todo o material de assinatura próprio: par RSA,
 * {@code JwtEncoder}, {@code JWKSource} e o endpoint de JWKS. Emitir token
 * deixou de ser trabalho deste serviço — e chave de assinatura que não existe
 * não vaza, não expira sem aviso e não precisa de rotação coordenada.
 *
 * <p>O que continua sendo trabalho daqui, e não do Clerk: decidir de qual
 * clínica é o usuário e o que o papel dele alcança. Ver
 * {@link br.com.dentibot.plataforma.seguranca.ProvedorDeIdentidade}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropriedadesClerk.class)
public class SegurancaConfig {

    /**
     * {@code withJwkSetUri} e não uma chave fixa: o Clerk rotaciona a chave de
     * assinatura, e o Nimbus busca o JWKS, cacheia e refaz a busca quando
     * aparece um {@code kid} desconhecido. Fixar a chave pública funcionaria até
     * a primeira rotação, que derrubaria a autenticação inteira num horário
     * escolhido por outra pessoa.
     */
    @Bean
    public JwtDecoder jwtDecoder(PropriedadesClerk clerk) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(clerk.urlDoJwks()).build();

        // createDefaultWithIssuer já cobre assinatura, exp, nbf e iss. O azp é a
        // parte que o default NÃO cobre e que o Clerk documenta como necessária.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                JwtValidators.createDefaultWithIssuer(clerk.emissor()),
                new ValidadorDeParteAutorizada(clerk.partesAutorizadas())));

        return decoder;
    }
}
