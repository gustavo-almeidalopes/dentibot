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
import br.com.dentibot.plataforma.contexto.Papel;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
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

    private static final String TOKEN_ADMIN = "token-do-admin";

    @TestConfiguration
    static class RotasDeTeste {

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

        /**
         * Escrita numa rota pública que não confere nada: quem decide se ela
         * roda é só a cadeia. Nas rotas de verdade o 403 da autorização
         * esconderia o do CSRF — toda escrita delas já exige Bearer ou prova
         * própria.
         */
        @Bean
        RouterFunction<ServerResponse> escritaPublica() {
            return RouterFunctions.route()
                    .POST("/api/v1/auth/teste/escrita", req -> ServerResponse.ok().body("gravado"))
                    .build();
        }
    }

    @MockitoBean
    private JwtDecoder decoder;
    @Autowired
    private OnboardingServico onboarding;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private TransactionTemplate transacao;

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

        assertThat(r.statusCode()).as("sem token é 401, não 403").isEqualTo(401);
    }

    @Test
    @DisplayName("resposta da API proíbe ser embutida e não vaza referer")
    void cabecalhosDeSeguranca() throws Exception {
        HttpResponse<String> r = enviar(HttpRequest.newBuilder(url("/actuator/health")));

        assertThat(r.headers().firstValue("Content-Security-Policy"))
                .hasValue("default-src 'none'; frame-ancestors 'none'");
        assertThat(r.headers().firstValue("Referrer-Policy")).hasValue("no-referrer");
    }

    @Test
    @DisplayName("escrita sem Bearer para no CSRF, mesmo com o cookie que o navegador anexaria")
    void escritaSemBearerParaNoCsrf() throws Exception {
        HttpResponse<String> r = escrever("Cookie", "__session=do-navegador");

        assertThat(r.statusCode())
                .as("""
                    É o POST que outro site monta: o navegador anexa o cookie sozinho, nunca o \
                    Authorization. Com o CSRF desligado, a rota pública gravava.""")
                .isEqualTo(403);
        assertThat(r.headers().allValues("Set-Cookie"))
                .as("recusa de CSRF não abre sessão: numa API sem estado, seria memória por POST anônimo")
                .noneMatch(c -> c.startsWith("JSESSIONID="));
    }

    @Test
    @DisplayName("com Bearer, a escrita não depende de token CSRF")
    void bearerDispensaCsrf() throws Exception {
        HttpResponse<String> r = escrever("Authorization", "Bearer " + TOKEN_ADMIN);

        assertThat(r.statusCode())
                .as("o navegador não anexa Authorization sozinho: quem o manda é página que o CORS liberou")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("o link do titular chega ao serviço sem Bearer e sem token CSRF")
    void titularDispensaCsrf() throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(url("/api/v1/titular/painel"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"token\":\"link-que-nao-existe\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(r.statusCode())
                .as("404 é o serviço dizendo que o link não existe; 403 seria o CSRF barrando o paciente")
                .isEqualTo(404);
    }

    @Test
    @DisplayName("o e-mail do admin cadastrado é o verificado no Clerk, não o do corpo")
    void emailDoAdminVemDoToken() throws Exception {
        long n = SEQ.incrementAndGet();
        String token = "token-cadastro-" + n;
        willReturn(Jwt.withTokenValue(token).header("alg", "RS256")
                .subject("user_novo_" + n)
                .claim("email", "dono" + n + "@clinica.local")
                .claim("email_verified", true)
                .build()).given(decoder).decode(token);

        HttpResponse<String> r = cadastrar(token, n, "dra.fulana" + n + "@clinicareal.com.br");

        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(emailDoAdmin(r.body()))
                .as("""
                    Com o e-mail vindo do corpo, qualquer conta do Clerk ocupa o endereço de \
                    outra pessoa: o UNIQUE é global, e a dona verdadeira toma 409 depois.""")
                .isEqualTo("dono" + n + "@clinica.local");
    }

    @Test
    @DisplayName("sem e-mail verificado no token, não há cadastro")
    void cadastroExigeEmailVerificado() throws Exception {
        long n = SEQ.incrementAndGet();
        String token = "token-sem-email-" + n;
        willReturn(Jwt.withTokenValue(token).header("alg", "RS256")
                .subject("user_sem_email_" + n)
                .claim("email", "nao.verificado" + n + "@clinica.local")
                .claim("email_verified", false)
                .build()).given(decoder).decode(token);

        HttpResponse<String> r = cadastrar(token, n, "qualquer" + n + "@clinica.local");

        assertThat(r.statusCode()).isEqualTo(400);
    }

    // ─── Aceite do cadastro: prova do consentimento (LGPD art. 8º §2º) ──────

    private static final String ACEITE_CLINICA = """
            {"versao":"2026-11-20","termosEPrivacidade":true,"representacao":true,"novidades":false}""";

    @Test
    @DisplayName("cadastro de clínica sem aceite dos termos não passa")
    void cadastroExigeAceite() throws Exception {
        long n = SEQ.incrementAndGet();
        String sub = "user_sem_aceite_" + n;
        String token = tokenVerificado(sub);

        assertThat(postar("/api/v1/auth/cadastro", token, corpoClinica(n, "x@x.local", null)).statusCode())
                .as("sem a linha do aceite não há como provar que a clínica aceitou os termos")
                .isEqualTo(400);
        assertThat(postar("/api/v1/auth/cadastro", token, corpoClinica(n, "x@x.local", """
                {"versao":"2026-11-20","termosEPrivacidade":false,"representacao":true}""")).statusCode())
                .isEqualTo(400);
        assertThat(aceitesDe(sub)).isZero();
    }

    @Test
    @DisplayName("cadastro de clínica grava o aceite com o IP da requisição")
    void cadastroGravaAceite() throws Exception {
        long n = SEQ.incrementAndGet();
        String sub = "user_com_aceite_" + n;

        HttpResponse<String> r = postar("/api/v1/auth/cadastro", tokenVerificado(sub),
                corpoClinica(n, "x@x.local", ACEITE_CLINICA));

        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(comoSuperusuario("""
                SELECT origem || '|' || versao_termos || '|' || representacao || '|' || host(ip_origem)
                FROM lgpd.aceites_cadastro WHERE clerk_user_id = ?""", sub))
                .isEqualTo("clinica|2026-11-20|true|127.0.0.1");
    }

    @Test
    @DisplayName("pré-cadastro com triagem de saúde exige o consentimento específico")
    void triagemExigeConsentimentoDeSaude() throws Exception {
        long n = SEQ.incrementAndGet();
        String sub = "user_paciente_" + n;
        String token = tokenVerificado(sub);
        String ficha = """
                {"nomeCompleto":"Paciente Teste","dataNascimento":"1990-01-01",
                 "anamnese":{"alergia":"penicilina"}}""";

        HttpResponse<String> sem = postar("/api/v1/auth/pre-cadastro", token, """
                {"ficha":%s,"aceites":{"versao":"2026-11-20","termosEPrivacidade":true,
                 "dadosDeSaude":false}}""".formatted(ficha));
        assertThat(sem.statusCode()).as("dado de saúde sem consentimento específico: art. 11 I").isEqualTo(400);
        assertThat(aceitesDe(sub)).as("a recusa não deixa meio cadastro para trás").isZero();

        HttpResponse<String> com = postar("/api/v1/auth/pre-cadastro", token, """
                {"ficha":%s,"aceites":{"versao":"2026-11-20","termosEPrivacidade":true,
                 "dadosDeSaude":true}}""".formatted(ficha));
        assertThat(com.statusCode()).isEqualTo(201);
        assertThat(comoSuperusuario("""
                SELECT origem || '|' || dados_de_saude FROM lgpd.aceites_cadastro
                WHERE clerk_user_id = ?""", sub)).isEqualTo("paciente|true");
    }

    @Test
    @DisplayName("pré-cadastro de menor exige o consentimento do responsável")
    void menorExigeResponsavel() throws Exception {
        long n = SEQ.incrementAndGet();
        String nascimento = LocalDate.now().minusYears(10).toString();

        HttpResponse<String> r = postar("/api/v1/auth/pre-cadastro", tokenVerificado("user_menor_" + n), """
                {"ficha":{"nomeCompleto":"Crianca Teste","dataNascimento":"%s",
                 "responsavelLegal":"Mae Teste"},
                 "aceites":{"versao":"2026-11-20","termosEPrivacidade":true}}""".formatted(nascimento));

        assertThat(r.statusCode()).as("art. 14 §1º").isEqualTo(400);
    }

    private String tokenVerificado(String sub) {
        String token = "token-" + sub;
        willReturn(Jwt.withTokenValue(token).header("alg", "RS256")
                .subject(sub)
                .claim("email", sub + "@clinica.local")
                .claim("email_verified", true)
                .build()).given(decoder).decode(token);
        return token;
    }

    private static String corpoClinica(long n, String emailNoCorpo, String aceites) {
        return """
                {"cnpj":"%014d","razaoSocial":"Clinica Nova LTDA","nomeFantasia":"Clinica Nova",
                 "nomeAdmin":"Dona","emailAdmin":"%s"%s}""".formatted(
                n, emailNoCorpo, aceites == null ? "" : ",\"aceites\":" + aceites);
    }

    /* A aplicação só tem INSERT em lgpd.aceites_cadastro (V26); conferir o que
       foi gravado exige outro papel — e é bom que exija. */
    private static String comoSuperusuario(String sql, String parametro) throws Exception {
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "postgres", "postgres");
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static long aceitesDe(String sub) throws Exception {
        return Long.parseLong(comoSuperusuario(
                "SELECT count(*) FROM lgpd.aceites_cadastro WHERE clerk_user_id = ?", sub));
    }

    private HttpResponse<String> postar(String caminho, String token, String corpo) throws Exception {
        return http.send(HttpRequest.newBuilder(url(caminho))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(corpo)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> cadastrar(String token, long n, String emailNoCorpo) throws Exception {
        return postar("/api/v1/auth/cadastro", token, corpoClinica(n, emailNoCorpo, ACEITE_CLINICA));
    }

    private String emailDoAdmin(String respostaDoCadastro) {
        long idClinica = Long.parseLong(respostaDoCadastro.replaceAll(".*\"idClinica\":(\\d+).*", "$1"));
        long idUsuario = Long.parseLong(respostaDoCadastro.replaceAll(".*\"idUsuarioAdmin\":(\\d+).*", "$1"));
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idUsuario, Papel.ADMIN, UUID.randomUUID()));
        return transacao.execute(s -> jdbc.sql("SELECT email FROM identidade.usuarios WHERE id_usuario = :id")
                .param("id", idUsuario).query(String.class).single());
    }

    private URI url(String caminho) {
        return URI.create("http://localhost:" + porta + caminho);
    }

    private HttpResponse<String> enviar(HttpRequest.Builder req) throws Exception {
        return http.send(req.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> escrever(String cabecalho, String valor) throws Exception {
        return http.send(HttpRequest.newBuilder(url("/api/v1/auth/teste/escrita"))
                        .header(cabecalho, valor)
                        .POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
