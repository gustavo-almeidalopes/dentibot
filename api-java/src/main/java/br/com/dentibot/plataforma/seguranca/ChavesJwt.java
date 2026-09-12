package br.com.dentibot.plataforma.seguranca;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Par RSA de assinatura dos tokens. O Java é o único emissor (invariante 2);
 * {@code svc-python} e o agente .NET validam pela chave pública publicada no
 * JWKS.
 *
 * <p>Sem chave configurada, gera um par efêmero — e isso é aceitável APENAS em
 * desenvolvimento e teste. Em produção a ausência precisa derrubar o boot: uma
 * chave que muda a cada restart invalida toda sessão viva, e — pior — uma chave
 * gerada em memória não pode ser rotacionada nem revogada de forma coordenada
 * entre instâncias.
 */
public final class ChavesJwt {

    private static final Logger log = LoggerFactory.getLogger(ChavesJwt.class);

    private final RSAPublicKey publica;
    private final RSAPrivateKey privada;
    private final String idChave;

    private ChavesJwt(RSAPublicKey publica, RSAPrivateKey privada, String idChave) {
        this.publica = publica;
        this.privada = privada;
        this.idChave = idChave;
    }

    public RSAPublicKey publica() {
        return publica;
    }

    public RSAPrivateKey privada() {
        return privada;
    }

    /** {@code kid} do JWKS: permite rotacionar sem invalidar token ainda válido. */
    public String idChave() {
        return idChave;
    }

    public static ChavesJwt de(PropriedadesJwt props, boolean ambienteDeDesenvolvimento) {
        if (props.temChaveConfigurada()) {
            return carregarDoPem(props);
        }
        if (!ambienteDeDesenvolvimento) {
            throw new IllegalStateException("""
                    DENTIBOT_JWT_PRIVATE_KEY não configurada. Fora de dev/test isto é fatal \
                    de propósito: par gerado em memória muda a cada restart, derruba toda \
                    sessão viva e não pode ser rotacionado de forma coordenada entre \
                    instâncias.""");
        }
        log.warn("Nenhuma chave JWT configurada — gerando par efêmero. "
                + "Tokens não sobrevivem a um restart. Aceitável só em dev/test.");
        return gerarEfemera();
    }

    private static ChavesJwt carregarDoPem(PropriedadesJwt props) {
        try {
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            RSAPrivateKey privada = (RSAPrivateKey) fabrica.generatePrivate(
                    new PKCS8EncodedKeySpec(decodificarPem(props.chavePrivadaPem())));
            RSAPublicKey publica = (RSAPublicKey) fabrica.generatePublic(
                    new X509EncodedKeySpec(decodificarPem(props.chavePublicaPem())));
            return new ChavesJwt(publica, privada, "dentibot-1");
        } catch (Exception e) {
            throw new IllegalStateException("Chave JWT inválida: verifique o PEM PKCS#8.", e);
        }
    }

    private static ChavesJwt gerarEfemera() {
        try {
            KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
            gerador.initialize(2048);
            KeyPair par = gerador.generateKeyPair();
            return new ChavesJwt((RSAPublicKey) par.getPublic(),
                    (RSAPrivateKey) par.getPrivate(), "dentibot-efemera");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar par RSA efêmero.", e);
        }
    }

    private static byte[] decodificarPem(String pem) {
        String limpo = pem
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(limpo);
    }
}
