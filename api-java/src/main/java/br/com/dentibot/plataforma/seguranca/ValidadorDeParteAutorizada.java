package br.com.dentibot.plataforma.seguranca;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Confere o claim {@code azp} (authorized party) do token do Clerk.
 *
 * <p>O Clerk grava em {@code azp} a origem que pediu o token. Sem conferir, um
 * token legítimo emitido para outro site da mesma instância do Clerk vale nesta
 * API — é o mesmo formato de ataque que o {@code aud} previne em OAuth clássico,
 * e é por isso que a própria documentação do Clerk trata esta checagem como
 * parte da verificação, não como extra.
 *
 * <p>Lista vazia desliga a checagem. Só o perfil de teste faz isso, onde não
 * existe browser nem origem do outro lado — em produção, uma lista vazia seria
 * a checagem inteira virando enfeite em silêncio, então o log avisa.
 */
public class ValidadorDeParteAutorizada implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERRO = new OAuth2Error(
            "invalid_token",
            "O claim azp não está entre as origens autorizadas desta API.",
            null);

    private final List<String> autorizadas;

    public ValidadorDeParteAutorizada(List<String> autorizadas) {
        this.autorizadas = List.copyOf(autorizadas);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (autorizadas.isEmpty()) {
            return OAuth2TokenValidatorResult.success();
        }
        String azp = token.getClaimAsString("azp");
        // Token sem azp é recusado quando há lista: o claim ausente não pode
        // valer como "qualquer origem serve", senão bastaria emitir um token
        // sem ele para pular a checagem inteira.
        return azp != null && autorizadas.contains(azp)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(ERRO);
    }
}
