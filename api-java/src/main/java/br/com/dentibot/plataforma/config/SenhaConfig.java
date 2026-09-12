package br.com.dentibot.plataforma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class SenhaConfig {

    /**
     * Argon2id com os parâmetros recomendados pelo OWASP: m=19 MiB, t=2, p=1.
     *
     * <p>Substitui o {@code crypt(senha, gen_salt('bf'))} da V1, que produzia
     * bcrypt com custo 6 — o padrão do pgcrypto, abaixo do mínimo recomendado —
     * e ainda por cima decidia política de senha dentro de uma procedure
     * SECURITY DEFINER no banco.
     *
     * <p>Argon2 do Spring Security precisa do BouncyCastle em runtime; a
     * dependência está no pom por causa disto, e por mais nada.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }
}
