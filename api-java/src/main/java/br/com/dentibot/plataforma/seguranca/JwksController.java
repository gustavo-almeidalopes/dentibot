package br.com.dentibot.plataforma.seguranca;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWKS público.
 *
 * <p>É como {@code svc-python} e o agente {@code .NET} validam token sem nunca
 * receber a chave privada — o Java continua sendo o único emissor
 * (invariante 2), e os outros só conferem assinatura.
 *
 * <p>Publicar a chave PÚBLICA é o desenho correto, não um vazamento: ela serve
 * para verificar, nunca para assinar.
 */
@RestController
public class JwksController {

    private final JWKSet conjunto;

    public JwksController(ChavesJwt chaves) {
        this.conjunto = new JWKSet(
                new RSAKey.Builder(chaves.publica()).keyID(chaves.idChave()).build());
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        // toJSONObject(true) = somente as partes públicas. O `true` é
        // literalmente a diferença entre publicar a chave de verificação e
        // publicar a chave de assinatura.
        return conjunto.toJSONObject(true);
    }
}
