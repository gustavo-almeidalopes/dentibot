package br.com.dentibot.plataforma.config;

import br.com.dentibot.plataforma.seguranca.FiltroAutenticacao;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * A borda, na ordem da camada 3:
 * CORS → Correlation-Id → Autenticação → Autorização → Rate limit → Validação →
 * Idempotência → módulo de negócio.
 *
 * <p>Nota conceitual que quase todo mundo erra: <b>CORS não é comunicação entre
 * serviços</b>. CORS é o navegador em {@code app.dentibot.com.br} falando com
 * {@code api.dentibot.com.br}. Java ↔ Python e agente .NET → Java não passam por
 * CORS: passam por HTTP autenticado ou por evento. Afrouxar CORS não resolve
 * problema de integração interna — só abre a API para qualquer página.
 */
@Configuration(proxyBeanMethods = false)
public class CadeiaDeSeguranca {

    private final List<String> origensPermitidas;

    public CadeiaDeSeguranca(@Value("${dentibot.cors.origens}") String origens) {
        this.origensPermitidas = List.of(origens.split("\\s*,\\s*"));
    }

    @Bean
    public SecurityFilterChain cadeia(HttpSecurity http, FiltroAutenticacao filtroAutenticacao)
            throws Exception {
        return http
                .cors(cors -> cors.configurationSource(fonteCors()))
                // API stateless com Bearer: não há sessão de servidor para o
                // atacante sequestrar, e o cookie de refresh é SameSite=Lax, que
                // impede o POST cross-site. O CSRF token clássico protegeria um
                // formulário com sessão — não é este desenho.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Cadastro de clínica: sem tenant (é ele que o cria), mas
                        // não anônimo — o controller exige token do Clerk válido,
                        // porque o `sub` vira o dono da clínica. Desde a V18 esta
                        // aplicação não emite token nenhum e por isso não publica
                        // mais JWKS: o emissor é o Clerk.
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Webhook de PSP não tem token: a autenticidade vem da
                        // assinatura HMAC do provedor, verificada ANTES do parse.
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        // Deny by default: o que não foi liberado acima exige
                        // autenticação, inclusive rota que ainda não existe.
                        .anyRequest().authenticated())
                .addFilterBefore(filtroAutenticacao, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(c -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)))
                .build();
    }

    @Bean
    public CorsConfigurationSource fonteCors() {
        CorsConfiguration config = new CorsConfiguration();
        // Lista explícita, nunca "*": com allowCredentials o navegador recusa o
        // curinga, e a saída tentadora — refletir o Origin recebido — aceita
        // qualquer site.
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Correlation-Id", "Idempotency-Key"));
        config.setExposedHeaders(List.of("X-Correlation-Id", "Retry-After"));
        // O refresh viaja em cookie HttpOnly.
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", config);
        return fonte;
    }
}
