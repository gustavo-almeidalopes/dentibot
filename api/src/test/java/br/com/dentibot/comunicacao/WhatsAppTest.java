package br.com.dentibot.comunicacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.ConsultaResumo;
import br.com.dentibot.agenda.NovaConsulta;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.comunicacao.application.ComunicacaoServico;
import br.com.dentibot.comunicacao.application.Despachante;
import br.com.dentibot.comunicacao.application.ProgramadorDeMensagens;
import br.com.dentibot.comunicacao.domain.Textos;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.NovoMembro;
import br.com.dentibot.lgpd.Finalidade;
import br.com.dentibot.lgpd.LgpdApi;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.outbox.EventoDominio;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Doc 03-D de ponta a ponta: evento da agenda → programação → despacho contra
 * uma Meta falsa → webhook assinado de volta → ação na agenda.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("WhatsApp (Doc 03-D)")
class WhatsAppTest extends TesteIntegracao {

    private static final String SEGREDO = "segredo-teste";
    private static final List<String> ENVIADOS = new CopyOnWriteArrayList<>();
    private static final AtomicLong WAMID = new AtomicLong();
    private static final HttpServer META = meta();

    @DynamicPropertySource
    static void whatsapp(DynamicPropertyRegistry registro) {
        registro.add("dentibot.whatsapp.url", () -> "http://127.0.0.1:" + META.getAddress().getPort());
        registro.add("dentibot.whatsapp.token", () -> "token-teste");
        registro.add("dentibot.whatsapp.segredo-do-app", () -> SEGREDO);
        registro.add("dentibot.whatsapp.token-de-verificacao", () -> "verifica-teste");
    }

    private static HttpServer meta() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.createContext("/", troca -> {
                ENVIADOS.add(troca.getRequestHeaders().getFirst("Authorization") + " "
                        + new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] r = ("{\"messages\":[{\"id\":\"wamid." + WAMID.incrementAndGet() + "\"}]}")
                        .getBytes(StandardCharsets.UTF_8);
                troca.sendResponseHeaders(200, r.length);
                troca.getResponseBody().write(r);
                troca.close();
            });
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    static void pararMeta() {
        META.stop(0);
    }

    @LocalServerPort private int porta;
    @Autowired private OnboardingServico onboarding;
    @Autowired private IdentidadeApi identidade;
    @Autowired private PacientesApi pacientes;
    @Autowired private AgendaApi agenda;
    @Autowired private LgpdApi lgpd;
    @Autowired private ComunicacaoServico comunicacao;
    @Autowired private ProgramadorDeMensagens programador;
    @Autowired private Despachante despachante;
    @Autowired private TransactionTemplate transacao;
    @Autowired private JdbcClient jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private long n;
    private long idClinica;
    private long idAdmin;
    private long dentista;
    private String numero;
    private String celular;
    private long idPaciente;

    @BeforeEach
    void clinica() {
        n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_wa_" + n, UUID.randomUUID()));
        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica WA LTDA", "Clinica WA",
                ZoneId.of("America/Sao_Paulo"), Plano.CLINICA,
                "Admin WA", "admin.wa" + n + "@teste.local", "user_wa_" + n));
        idClinica = criada.idClinica();
        idAdmin = criada.idUsuarioAdmin();
        admin();
        long usuario = identidade.admitirMembro(new NovoMembro(
                "Dra. WA", "dra.wa" + n + "@teste.local", Papel.DENTISTA, "W" + n, "SP", null));
        dentista = identidade.dentistaDoUsuario(usuario).orElseThrow();
        numero = "9" + String.format("%09d", n);
        comunicacao.salvarCanal(numero, true, List.of());
        celular = "119" + String.format("%08d", n % 100_000_000);
        idPaciente = pacientes.criar(NovoPaciente.basico("Paciente WA " + n, celular));
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("consulta marcada vira lembrete; fora da janela sai como modelo; reentrega não duplica")
    void lembreteSaiComoModelo() {
        long idConsulta = agendar(idPaciente, daquiATresDias());
        programar(TiposDeEvento.CONSULTA_AGENDADA, idConsulta);
        programar(TiposDeEvento.CONSULTA_AGENDADA, idConsulta);

        assertThat(mensagens()).singleElement()
                .satisfies(m -> assertThat(m.get("tipo")).isEqualTo("lembrete"))
                .satisfies(m -> assertThat((String) m.get("texto")).contains("Clinica WA", "Responda 1"));

        despacharTudo();

        assertThat(mensagens().getFirst().get("status")).isEqualTo("enviada");
        assertThat(mensagens().getFirst().get("id_externo").toString()).startsWith("wamid.");
        assertThat(ENVIADOS.getLast()).startsWith("Bearer token-teste ")
                .contains("\"type\":\"template\"", "\"to\":\"55" + celular + "\"", "dentibot_aviso");
    }

    @Test
    @DisplayName("paciente responde 1: consulta confirmada, resposta como texto dentro da janela")
    void respostaConfirma() throws Exception {
        long idConsulta = agendar(idPaciente, daquiATresDias());
        programar(TiposDeEvento.CONSULTA_AGENDADA, idConsulta);
        despacharTudo();

        String payload = texto("wamid.in." + n, "55" + celular, "1");
        assertThat(webhook(payload, assinar(payload)).statusCode()).isEqualTo(200);
        assertThat(webhook(payload, assinar(payload)).statusCode()).isEqualTo(200);

        assertThat(agenda.buscar(idConsulta).map(ConsultaResumo::status)).contains("confirmada");
        assertThat(mensagens()).filteredOn(m -> m.get("tipo").equals("recebida")).hasSize(1);
        assertThat(mensagens()).filteredOn(m -> m.get("tipo").equals("resposta"))
                .singleElement()
                .satisfies(m -> assertThat((String) m.get("texto")).startsWith("Consulta confirmada"));

        despacharTudo();
        assertThat(ENVIADOS.getLast()).contains("\"type\":\"text\"");
    }

    @Test
    @DisplayName("urgência escala com prioridade; PARAR corta o automático, não a resposta")
    void urgenciaEParar() throws Exception {
        long idConsulta = agendar(idPaciente, daquiATresDias());
        programar(TiposDeEvento.CONSULTA_AGENDADA, idConsulta);

        String urgente = texto("wamid.u." + n, "55" + celular, "Estou com sangramento que não para");
        webhook(urgente, assinar(urgente));
        admin();
        assertThat(comunicacao.escaladas()).singleElement().satisfies(i -> {
            assertThat(i.mensagem().escalada()).isEqualTo("urgente");
            assertThat(i.nomePaciente()).isEqualTo("Paciente WA " + n);
        });
        assertThat(mensagens()).extracting(m -> m.get("texto")).contains(Textos.URGENCIA);

        String parar = texto("wamid.p." + n, "55" + celular, "PARAR");
        webhook(parar, assinar(parar));
        admin();
        assertThat(lgpd.permite(idPaciente, Finalidade.WHATSAPP)).isFalse();

        despacharTudo();
        assertThat(mensagens()).filteredOn(m -> m.get("tipo").equals("lembrete")).singleElement()
                .satisfies(m -> assertThat(m.get("status")).isEqualTo("cancelada"))
                .satisfies(m -> assertThat(m.get("motivo")).isEqualTo("paciente desligou o WhatsApp"));
        assertThat(mensagens()).filteredOn(m -> m.get("tipo").equals("resposta"))
                .allSatisfy(m -> assertThat(m.get("status")).isEqualTo("enviada"));

        comunicacao.resolver(comunicacao.escaladas().getFirst().mensagem().idMensagem());
        assertThat(comunicacao.escaladas()).isEmpty();
    }

    @Test
    @DisplayName("vaga cancelada vai para quem espera; o SIM marca a consulta")
    void vagaParaQuemEspera() throws Exception {
        String celularB = "118" + String.format("%08d", n % 100_000_000);
        long pacienteB = pacientes.criar(NovoPaciente.basico("Espera WA " + n, celularB));
        comunicacao.entrarNaEspera(pacienteB, null, "qualquer", 2, null);

        Instant inicio = daquiATresDias();
        long idConsulta = agendar(idPaciente, inicio);
        agenda.cancelar(idConsulta, "paciente pediu");
        programar(TiposDeEvento.CONSULTA_CANCELADA, idConsulta);
        assertThat(mensagens()).extracting(m -> m.get("tipo")).containsExactly("oferta_vaga");
        despacharTudo();

        String sim = texto("wamid.s." + n, "55" + celularB, "SIM");
        webhook(sim, assinar(sim));
        admin();

        assertThat(agenda.historicoDoPaciente(pacienteB)).singleElement()
                .satisfies(c -> assertThat(c.inicioEm()).isEqualTo(inicio))
                .satisfies(c -> assertThat(c.status()).isEqualTo("agendada"));
        assertThat(comunicacao.listaDeEspera()).isEmpty();
        assertThat(mensagens()).extracting(m -> (String) m.get("texto"))
                .anyMatch(t -> t.startsWith("Horário garantido"));
    }

    @Test
    @DisplayName("orientação lida fica provada; status atrasado não desfaz; sem assinatura é 401")
    void orientacaoLidaEAssinatura() throws Exception {
        comunicacao.enviarOrientacao(idPaciente, "pos_extracao");
        despacharTudo();
        String wamid = mensagens().getFirst().get("id_externo").toString();

        String lida = status(wamid, "read");
        webhook(lida, assinar(lida));
        String atrasado = status(wamid, "delivered");
        webhook(atrasado, assinar(atrasado));
        admin();
        assertThat(mensagens().getFirst()).satisfies(m -> {
            assertThat(m.get("status")).isEqualTo("lida");
            assertThat(m.get("lida_em")).isNotNull();
        });

        assertThat(webhook(lida, null).statusCode()).isEqualTo(401);
        assertThat(webhook(lida, "sha256=" + "0".repeat(64)).statusCode()).isEqualTo(401);

        HttpResponse<String> ok = http.send(HttpRequest.newBuilder(URI.create(base()
                + "?hub.mode=subscribe&hub.verify_token=verifica-teste&hub.challenge=abc123"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.body()).isEqualTo("abc123");
        HttpResponse<String> nao = http.send(HttpRequest.newBuilder(URI.create(base()
                + "?hub.mode=subscribe&hub.verify_token=errado&hub.challenge=abc123"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(nao.statusCode()).isEqualTo(403);
    }

    // ─── apoio ───────────────────────────────────────────────────────────────

    private void admin() {
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idAdmin, Papel.ADMIN,
                UUID.randomUUID()));
    }

    private static Instant daquiATresDias() {
        return Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    }

    private long agendar(long paciente, Instant inicio) {
        return agenda.agendar(new NovaConsulta(paciente, dentista, null, inicio,
                inicio.plus(30, ChronoUnit.MINUTES), null));
    }

    private void programar(String tipo, long idConsulta) {
        programador.consumir(new EventoDominio(UUID.randomUUID(), tipo, 1, Instant.now(),
                idClinica, UUID.randomUUID(), EventoDominio.Ator.sistema(),
                Map.of("consulta_id", idConsulta)));
    }

    /** Traz tudo o que está programado para agora e roda o despachante. */
    private void despacharTudo() {
        admin();
        transacao.executeWithoutResult(s -> jdbc.sql("""
                        UPDATE comunicacao.mensagens SET enviar_em = now() - interval '1 minute'
                        WHERE status = 'agendada'
                        """).update());
        despachante.despachar();
        admin();
    }

    private List<Map<String, Object>> mensagens() {
        admin();
        return transacao.execute(s -> jdbc.sql("""
                        SELECT tipo, status, motivo, texto, id_externo, lida_em
                        FROM comunicacao.mensagens ORDER BY id_mensagem
                        """).query().listOfRows());
    }

    private String base() {
        return "http://127.0.0.1:" + porta + "/api/v1/webhooks/whatsapp";
    }

    private HttpResponse<String> webhook(String corpo, String assinatura) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(base()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo));
        if (assinatura != null) {
            req.header("X-Hub-Signature-256", assinatura);
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String assinar(String corpo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
    }

    private String texto(String wamid, String de, String texto) {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages",
                "value":{"messaging_product":"whatsapp","metadata":{"phone_number_id":"%s"},
                "messages":[{"from":"%s","id":"%s","timestamp":"1790000000","type":"text",
                "text":{"body":"%s"}}]}}]}]}""".formatted(numero, de, wamid, texto);
    }

    private String status(String wamid, String status) {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages",
                "value":{"messaging_product":"whatsapp","metadata":{"phone_number_id":"%s"},
                "statuses":[{"id":"%s","status":"%s","timestamp":"1790000000",
                "recipient_id":"55%s"}]}}]}]}""".formatted(numero, wamid, status, celular);
    }
}
