package br.com.dentibot.prontuario.infrastructure;

import br.com.dentibot.plataforma.erro.ServicoIndisponivelException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * O bucket dos anexos clínicos (ST-41): R2 em produção, MinIO em dev — os dois
 * falam S3.
 *
 * <p>A API nunca toca os bytes. Ela assina um PUT que amarra tamanho, tipo e
 * SHA-256; o navegador envia direto; e na confirmação a API pergunta ao bucket
 * (HEAD) se o que chegou é o que foi declarado. Radiografia de 50 MB não passa
 * pela memória de nenhuma instância.
 *
 * <p>Sem endpoint ou credencial, a instância sobe mesmo assim e os anexos
 * respondem 503 dizendo o que falta — o resto do prontuário não depende disto.
 */
@Component
public class ArmazemDeAnexos implements DisposableBean {

    public record Objeto(long tamanho, String sha256Base64) {
    }

    public record EnvioPreAssinado(String url, Map<String, String> cabecalhos, Instant expiraEm) {
    }

    private final String bucket;
    private final S3Client s3;
    private final S3Presigner presigner;

    public ArmazemDeAnexos(@Value("${dentibot.anexos.endpoint:}") String endpoint,
                           @Value("${dentibot.anexos.regiao:auto}") String regiao,
                           @Value("${dentibot.anexos.bucket:dentibot-anexos}") String bucket,
                           @Value("${dentibot.anexos.chave-acesso:}") String chaveAcesso,
                           @Value("${dentibot.anexos.segredo:}") String segredo) {
        this.bucket = bucket;
        if (endpoint.isBlank() || chaveAcesso.isBlank() || segredo.isBlank()) {
            this.s3 = null;
            this.presigner = null;
            return;
        }
        var credenciais = StaticCredentialsProvider.create(AwsBasicCredentials.create(chaveAcesso, segredo));
        // Path-style: MinIO exige, e o R2 aceita. Com virtual-host o nome do
        // bucket vira subdomínio, que o MinIO local não resolve.
        var configuracao = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(regiao))
                .credentialsProvider(credenciais)
                .serviceConfiguration(configuracao)
                .httpClient(UrlConnectionHttpClient.create())
                // O único checksum que importa aqui é o SHA-256 declarado pelo
                // cliente; o CRC automático do SDK só cria cabeçalho que o R2 não
                // reconhece.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(regiao))
                .credentialsProvider(credenciais)
                .serviceConfiguration(configuracao)
                .build();
    }

    public void exigirConfigurado() {
        if (s3 == null) {
            throw new ServicoIndisponivelException(
                    "Armazenamento de anexos não configurado nesta instância "
                            + "(DENTIBOT_S3_ENDPOINT, DENTIBOT_S3_CHAVE, DENTIBOT_S3_SEGREDO).");
        }
    }

    public EnvioPreAssinado urlDeEnvio(String chave, String contentType, long tamanho,
                                       String sha256Base64, Duration validade) {
        exigirConfigurado();
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(chave)
                .contentType(contentType)
                .contentLength(tamanho)
                .checksumSHA256(sha256Base64)
                .build();
        PresignedPutObjectRequest assinado = presigner.presignPutObject(
                p -> p.signatureDuration(validade).putObjectRequest(put));

        // Host e Content-Length o próprio cliente HTTP escreve — o navegador nem
        // deixa defini-los. O resto é o que a assinatura cobre.
        Map<String, String> cabecalhos = new LinkedHashMap<>();
        assinado.signedHeaders().forEach((nome, valores) -> {
            if (!nome.equalsIgnoreCase("host") && !nome.equalsIgnoreCase("content-length")) {
                cabecalhos.put(nome, String.join(",", valores));
            }
        });
        return new EnvioPreAssinado(assinado.url().toString(), cabecalhos, assinado.expiration());
    }

    public Optional<Objeto> cabeca(String chave) {
        exigirConfigurado();
        try {
            HeadObjectResponse r = s3.headObject(h -> h.bucket(bucket).key(chave)
                    .checksumMode(ChecksumMode.ENABLED));
            return Optional.of(new Objeto(r.contentLength(), r.checksumSHA256()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public String urlDeLeitura(String chave, String nomeArquivo, Duration validade) {
        exigirConfigurado();
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(chave)
                // inline: radiografia abre no navegador em vez de virar download.
                .responseContentDisposition("inline; filename=\"" + nomeSeguro(nomeArquivo) + "\"")
                .build();
        return presigner.presignGetObject(p -> p.signatureDuration(validade).getObjectRequest(get))
                .url().toString();
    }

    /** Aspas e quebra de linha no nome viram injeção de cabeçalho na resposta do bucket. */
    static String nomeSeguro(String nome) {
        String limpo = nome.replaceAll("[\"\\\\\\r\\n]", "_");
        return limpo.isBlank() ? "anexo" : limpo;
    }

    /** Para os testes criarem o bucket; em produção ele já existe. */
    public void criarBucketSeFaltar() {
        exigirConfigurado();
        if (s3.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(bucket))) {
            s3.createBucket(c -> c.bucket(bucket));
        }
    }

    @Override
    public void destroy() {
        if (s3 != null) {
            s3.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }
}
