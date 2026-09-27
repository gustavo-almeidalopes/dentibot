package br.com.dentibot.prontuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import br.com.dentibot.prontuario.infrastructure.ArmazemDeAnexos;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Anexo clínico de ponta a ponta (ST-41), com bucket de verdade: a API assina,
 * o "navegador" (HttpClient) envia direto ao MinIO, e a confirmação só registra
 * o que o bucket prova ter recebido.
 */
@DisplayName("Anexos clínicos no bucket (ST-41)")
class AnexosTest extends TesteIntegracao {

    /** quay.io e não Docker Hub: é o registro que a MinIO mantém aberto (ver o compose). */
    static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("quay.io/minio/minio:latest").asCompatibleSubstituteFor("minio/minio"));

    static {
        MINIO.start();
    }

    @DynamicPropertySource
    static void bucket(DynamicPropertyRegistry registro) {
        registro.add("dentibot.anexos.endpoint", MINIO::getS3URL);
        registro.add("dentibot.anexos.regiao", () -> "us-east-1");
        registro.add("dentibot.anexos.chave-acesso", MINIO::getUserName);
        registro.add("dentibot.anexos.segredo", MINIO::getPassword);
    }

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired private OnboardingServico onboarding;
    @Autowired private PacientesApi pacientes;
    @Autowired private ProntuarioApi prontuario;
    @Autowired private ArmazemDeAnexos armazem;

    private long idClinica;
    private long idAdmin;
    private long idPaciente;

    @BeforeEach
    void clinicaComPaciente() {
        armazem.criarBucketSeFaltar();
        long n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_anexo_" + n, UUID.randomUUID()));
        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica Anexo LTDA", "Clinica Anexo",
                ZoneId.of("America/Sao_Paulo"), Plano.CLINICA,
                "Admin Anexo", "admin.anexo" + n + "@teste.local", "user_anexo_" + n));
        idClinica = criada.idClinica();
        idAdmin = criada.idUsuarioAdmin();
        como(Papel.ADMIN);
        idPaciente = pacientes.criar(NovoPaciente.basico("Paciente Anexo", "11999990002"));
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("assina, envia direto ao bucket, confirma e lê de volta os mesmos bytes")
    void fluxoCompleto() throws Exception {
        byte[] bytes = "radiografia panoramica".getBytes(StandardCharsets.UTF_8);
        NovoAnexo declarado = declarar(bytes);

        EnvioDeAnexo envio = prontuario.iniciarAnexo(idPaciente, declarado);
        assertThat(enviar(envio, bytes)).isEqualTo(200);
        long id = prontuario.confirmarAnexo(idPaciente, envio.chave(), declarado);

        assertThat(prontuario.anexos(idPaciente)).singleElement().satisfies(a -> {
            assertThat(a.idAnexo()).isEqualTo(id);
            assertThat(a.sha256()).isEqualTo(declarado.sha256());
        });
        HttpResponse<byte[]> lido = HTTP.send(
                HttpRequest.newBuilder(URI.create(prontuario.urlDoAnexo(idPaciente, id))).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(lido.body()).isEqualTo(bytes);
    }

    @Test
    @DisplayName("bytes diferentes dos declarados: o bucket recusa, e não há o que confirmar")
    void bytesTrocados() throws Exception {
        NovoAnexo declarado = declarar("o arquivo declarado".getBytes(StandardCharsets.UTF_8));
        EnvioDeAnexo envio = prontuario.iniciarAnexo(idPaciente, declarado);

        byte[] outros = "outro arquivo, mesmo tamanh".getBytes(StandardCharsets.UTF_8);
        assertThat(enviar(envio, outros)).isNotEqualTo(200);
        assertThatThrownBy(() -> prontuario.confirmarAnexo(idPaciente, envio.chave(), declarado))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(prontuario.anexos(idPaciente)).isEmpty();
    }

    @Test
    @DisplayName("a chave de um paciente não confirma anexo no prontuário de outro")
    void chaveDeOutroPaciente() throws Exception {
        byte[] bytes = "foto intraoral".getBytes(StandardCharsets.UTF_8);
        NovoAnexo declarado = declarar(bytes);
        EnvioDeAnexo envio = prontuario.iniciarAnexo(idPaciente, declarado);
        assertThat(enviar(envio, bytes)).isEqualTo(200);
        long outro = pacientes.criar(NovoPaciente.basico("Outro Paciente", "11999990003"));

        assertThatThrownBy(() -> prontuario.confirmarAnexo(outro, envio.chave(), declarado))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("recepção não anexa nem lista anexo clínico")
    void recepcaoNaoAlcanca() {
        como(Papel.RECEPCIONISTA);
        NovoAnexo declarado = declarar("x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> prontuario.iniciarAnexo(idPaciente, declarado))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> prontuario.anexos(idPaciente))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("executável não entra como anexo clínico")
    void tipoRecusado() {
        byte[] bytes = "MZ".getBytes(StandardCharsets.UTF_8);
        NovoAnexo exe = new NovoAnexo("documento", "a.exe", "application/x-msdownload",
                (long) bytes.length, sha256(bytes), null);

        assertThatThrownBy(() -> prontuario.iniciarAnexo(idPaciente, exe))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── apoio ───────────────────────────────────────────────────────────────

    private void como(Papel papel) {
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idAdmin, papel, UUID.randomUUID()));
    }

    private static NovoAnexo declarar(byte[] bytes) {
        return new NovoAnexo("radiografia", "rx.png", "image/png", (long) bytes.length,
                sha256(bytes), null);
    }

    private static int enviar(EnvioDeAnexo envio, byte[] bytes) throws Exception {
        HttpRequest.Builder put = HttpRequest.newBuilder(URI.create(envio.url()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes));
        envio.cabecalhos().forEach(put::header);
        return HTTP.send(put.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
