package br.com.dentibot.plataforma.config;

import br.com.dentibot.plataforma.seguranca.ChavesJwt;
import br.com.dentibot.plataforma.seguranca.PropriedadesJwt;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropriedadesJwt.class)
public class SegurancaConfig {

    @Bean
    public ChavesJwt chavesJwt(PropriedadesJwt props, Environment env) {
        boolean dev = env.matchesProfiles("dev", "test", "local");
        return ChavesJwt.de(props, dev);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(ChavesJwt chaves) {
        RSAKey chave = new RSAKey.Builder(chaves.publica())
                .privateKey(chaves.privada())
                .keyID(chaves.idChave())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(chave));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwks) {
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public JwtDecoder jwtDecoder(ChavesJwt chaves) {
        return NimbusJwtDecoder.withPublicKey(chaves.publica()).build();
    }
}
