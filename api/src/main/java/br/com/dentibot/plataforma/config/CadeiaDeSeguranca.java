package br.com.dentibot.plataforma.config;

import br.com.dentibot.plataforma.seguranca.FiltroAutenticacao;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
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

    /**
     * {@code /actuator/prometheus} (ST-25) tem cadeia própria, antes da principal:
     * quem raspa é o Prometheus, que não tem token do Clerk — tem uma credencial
     * Basic que só serve para isto.
     *
     * <p>Sem {@code dentibot.metricas.senha} o endpoint nega todo mundo. Uma
     * senha padrão no código seria a senha de todas as instâncias que esquecerem
     * de configurar a variável.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain metricas(HttpSecurity http,
                                        @Value("${dentibot.metricas.senha:}") String senha)
            throws Exception {
        http.securityMatcher("/actuator/prometheus")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .logout(l -> l.disable());
        if (senha.isBlank()) {
            return http.authorizeHttpRequests(a -> a.anyRequest().denyAll())
                    .httpBasic(b -> b.disable())
                    .build();
        }
        PasswordEncoder codificador = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // setStatus e não sendError, que é o que o BasicAuthenticationEntryPoint
        // faz: sendError dispara um error dispatch para /error, que não é desta
        // cadeia — cai na principal, que exige Bearer e troca o 401 por 403.
        AuthenticationEntryPoint desafio = (req, res, e) -> {
            res.setHeader("WWW-Authenticate", "Basic realm=\"metricas\", charset=\"UTF-8\"");
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
        };
        return http.authorizeHttpRequests(a -> a.anyRequest().hasRole("METRICAS"))
                .httpBasic(b -> b.authenticationEntryPoint(desafio))
                .exceptionHandling(e -> e.authenticationEntryPoint(desafio))
                .userDetailsService(new InMemoryUserDetailsManager(User.withUsername("prometheus")
                        .password(codificador.encode(senha))
                        .roles("METRICAS")
                        .build()))
                .build();
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
                        // Webhook um a um, nunca /webhooks/**: liberado de
                        // antemão, o próximo controller criado ali nasceria
                        // público. Este confere a assinatura HMAC da Meta antes
                        // do parse (WebhookWhatsAppController).
                        .requestMatchers("/api/v1/webhooks/whatsapp").permitAll()
                        // Painel do titular: a credencial é o link, conferida
                        // pelo hash na V25 — não há conta de paciente.
                        .requestMatchers(HttpMethod.POST, "/api/v1/titular/**").permitAll()
                        // Contrato OpenAPI (ST-20): público quando ligado, e
                        // desligado em produção — aí a rota nem existe.
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**").permitAll()
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
                        // A API só devolve JSON: nenhum recurso a carregar, nenhuma
                        // página que possa ser embutida, nenhum referer a vazar.
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
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
        config.setExposedHeaders(List.of("X-Correlation-Id", "Retry-After",
                "Content-Disposition", "X-Conteudo-Sha256"));
        // O refresh viaja em cookie HttpOnly.
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", config);
        return fonte;
    }
}
