package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * O que um atacante vê da API pela internet, com Tomcat de verdade.
 *
 * <p>Porta real e não MockMvc: o tratamento de {@code X-Forwarded-For} com
 * {@code forward-headers-strategy: native} mora numa valve do Tomcat, que o
 * MockMvc não executa.
 *
 * <p>{@code internal-proxies} aponta para um endereço que não é o do teste: do
 * ponto de vista da API, este cliente é alguém da internet chamando a origem
 * direto, e não o proxy.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.tomcat.remoteip.internal-proxies=192\\.0\\.2\\.1")
@DisplayName("Borda pública")
class BordaPublicaTest extends TesteIntegracao {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);
    private static final String TOKEN_ADMIN = "token-do-admin";

    @TestConfiguration
    static class EcoDoIp {

        /**
         * Sob /auth porque é a rota pública — a mesma do rate limit por IP.
         * RouterFunction e não @RestController: o component scan da aplicação
         * também varre as classes de teste, e registraria o controller em todo
         * contexto.
         */
        @Bean
        RouterFunction<ServerResponse> ecoDoIp() {
            return RouterFunctions.route()
                    .GET("/api/v1/auth/teste/ip",
                            req -> ServerResponse.ok().body(req.servletRequest().getRemoteAddr()))
                    .build();
        }
    }

    @MockitoBean
    private JwtDecoder decoder;
    @Autowired
    private OnboardingServico onboarding;

    @Value("${local.server.port}")
    private int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void adminComToken() {
        long n = SEQ.incrementAndGet();
        String sub = "user_borda_" + n;
        ContextoAtual.definir(ContextoRequisicao.semConta(sub, UUID.randomUUID()));
        onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Borda LTDA", "Clinica Borda",
                ZoneId.of("America/Sao_Paulo"), Plano.SOLO,
                "Admin Borda", "admin.borda" + n + "@teste.local", sub));
        ContextoAtual.limpar();

        willThrow(new JwtException("token inválido")).given(decoder).decode(anyString());
        willReturn(Jwt.withTokenValue(TOKEN_ADMIN).header("alg", "RS256").subject(sub).build())
                .given(decoder).decode(TOKEN_ADMIN);
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("X-Forwarded-For de quem não é o proxy não muda o IP visto pela API")
    void xffForjadoEhIgnorado() throws Exception {
        HttpResponse<String> r = enviar(HttpRequest.newBuilder(url("/api/v1/auth/teste/ip"))
                .header("X-Forwarded-For", "203.0.113.9"));

        assertThat(r.body())
                .as("""
                    Com o IP forjável, cada requisição cai num balde novo do rate limit de \
                    /auth, e o ipOrigem da trilha de autenticação é o que o atacante escolher.""")
                .isNotEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("usuário de clínica não vê os detalhes internos do health")
    void healthSemDetalhes() throws Exception {
        HttpResponse<String> r = enviar(HttpRequest.newBuilder(url("/actuator/health"))
                .header("Authorization", "Bearer " + TOKEN_ADMIN));

        assertThat(r.body())
                .as("show-details when-authorized sem papel configurado vale para qualquer autenticado")
                .contains("\"status\"")
                .doesNotContain("components");
    }

    @Test
    @DisplayName("rota de webhook que não existe não nasce pública")
    void webhookNaoEhPublico() throws Exception {
        HttpResponse<String> r = enviar(HttpRequest.newBuilder(url("/api/v1/webhooks/psp")));

        assertThat(r.statusCode()).isIn(401, 403);
    }

    @Test
    @DisplayName("resposta da API proíbe ser embutida e não vaza referer")
    void cabecalhosDeSeguranca() throws Exception {
        HttpResponse<String> r = enviar(HttpRequest.newBuilder(url("/actuator/health")));

        assertThat(r.headers().firstValue("Content-Security-Policy"))
                .hasValue("default-src 'none'; frame-ancestors 'none'");
        assertThat(r.headers().firstValue("Referrer-Policy")).hasValue("no-referrer");
    }

    private URI url(String caminho) {
        return URI.create("http://localhost:" + porta + caminho);
    }

    private HttpResponse<String> enviar(HttpRequest.Builder req) throws Exception {
        return http.send(req.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
