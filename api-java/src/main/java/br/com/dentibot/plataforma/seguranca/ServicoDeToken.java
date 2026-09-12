package br.com.dentibot.plataforma.seguranca;

import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.contexto.StaffPapel;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * Emissão e leitura dos tokens. O Java é o único emissor (invariante 2).
 *
 * <p>Os dois eixos de autorização viajam em claims mutuamente exclusivas:
 * {@code clinica_id + papel} OU {@code staff_role}. A leitura recusa um token
 * que traga as duas — se aparecesse, seria um token forjado por alguém que
 * entendeu que a separação dos eixos é o que impede staff de plataforma tocar em
 * prontuário.
 */
@Service
public class ServicoDeToken {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private static final String CLAIM_TIPO = "typ";
    private static final String CLAIM_CLINICA = "clinica_id";
    private static final String CLAIM_PAPEL = "papel";
    private static final String CLAIM_STAFF = "staff_role";
    private static final String CLAIM_FAMILIA = "fam";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final PropriedadesJwt props;
    private final ChavesJwt chaves;

    public ServicoDeToken(JwtEncoder encoder, JwtDecoder decoder,
                          PropriedadesJwt props, ChavesJwt chaves) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.props = props;
        this.chaves = chaves;
    }

    public record TokenEmitido(String valor, UUID jti, Instant expiraEm) {
    }

    public TokenEmitido emitirAccessDeClinica(long idUsuario, long idClinica, Papel papel) {
        return emitir(String.valueOf(idUsuario), TIPO_ACCESS, props.validadeAccess(),
                claims -> claims.claim(CLAIM_CLINICA, idClinica)
                        .claim(CLAIM_PAPEL, papel.valorBanco()));
    }

    public TokenEmitido emitirAccessDeStaff(long idStaff, StaffPapel papel) {
        return emitir(String.valueOf(idStaff), TIPO_ACCESS, props.validadeAccess(),
                claims -> claims.claim(CLAIM_STAFF, papel.valorBanco()));
    }

    /**
     * O refresh carrega a clínica porque a sessão correspondente mora numa
     * tabela protegida por RLS: sem saber o tenant não há como LER a sessão para
     * validar o token, e sem validar o token não há como saber o tenant. O claim
     * é assinado, então a clínica vinda dele é confiável — é o mesmo nível de
     * confiança do resto do token.
     *
     * <p>Isto não fere a camada 5: quem concede acesso a recurso é o access
     * token. O refresh só serve para trocar por um novo par.
     */
    public TokenEmitido emitirRefresh(String assunto, long idClinica, UUID familia) {
        return emitir(assunto, TIPO_REFRESH, props.validadeRefresh(),
                claims -> claims.claim(CLAIM_FAMILIA, familia.toString())
                        .claim(CLAIM_CLINICA, idClinica));
    }

    private TokenEmitido emitir(String assunto, String tipo, java.time.Duration validade,
                                java.util.function.Consumer<JwtClaimsSet.Builder> extras) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(validade);
        UUID jti = UUID.randomUUID();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(props.emissor())
                .subject(assunto)
                .issuedAt(agora)
                .expiresAt(expira)
                .id(jti.toString())
                .claim(CLAIM_TIPO, tipo);
        extras.accept(claims);

        JwsHeader cabecalho = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(chaves.idChave())
                .build();

        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims.build()))
                .getTokenValue();
        return new TokenEmitido(valor, jti, expira);
    }

    /** O que sai da leitura de um access token: contexto e o jti, para a blocklist. */
    public record AccessLido(ContextoRequisicao contexto, UUID jti, Instant expiraEm) {
    }

    /**
     * Valida assinatura e expiração e traduz para o contexto da aplicação.
     *
     * @throws JwtException token inválido, expirado ou com combinação de eixos
     *                      impossível
     */
    public AccessLido lerAccess(String token, UUID correlacao) {
        Jwt jwt = decoder.decode(token);

        if (!TIPO_ACCESS.equals(jwt.getClaimAsString(CLAIM_TIPO))) {
            throw new JwtException("Token não é de acesso — refresh não abre endpoint.");
        }

        Long clinicaId = jwt.getClaim(CLAIM_CLINICA) == null
                ? null : ((Number) jwt.getClaim(CLAIM_CLINICA)).longValue();
        String papel = jwt.getClaimAsString(CLAIM_PAPEL);
        String staff = jwt.getClaimAsString(CLAIM_STAFF);

        if (clinicaId != null && staff != null) {
            throw new JwtException(
                    "Token com clinica_id e staff_role ao mesmo tempo: eixos são exclusivos.");
        }

        UUID jti = UUID.fromString(jwt.getId());
        ContextoRequisicao contexto;
        if (staff != null) {
            contexto = ContextoRequisicao.deStaff(
                    Long.parseLong(jwt.getSubject()), StaffPapel.de(staff), correlacao);
        } else if (clinicaId == null || papel == null) {
            throw new JwtException("Token sem eixo de autorização definido.");
        } else {
            // Ordem dos argumentos: (clinica, usuario). O subject do token é o
            // USUÁRIO; trocar os dois daria a cada usuário o tenant de número
            // igual ao seu id — um vazamento silencioso e sistemático. Coberto
            // por ServicoDeTokenTest.
            contexto = ContextoRequisicao.deClinica(
                    clinicaId, Long.parseLong(jwt.getSubject()), Papel.de(papel), correlacao);
        }
        return new AccessLido(contexto, jti, jwt.getExpiresAt());
    }

    public Jwt lerRefresh(String token) {
        Jwt jwt = decoder.decode(token);
        if (!TIPO_REFRESH.equals(jwt.getClaimAsString(CLAIM_TIPO))) {
            throw new JwtException("Token não é de refresh.");
        }
        return jwt;
    }

    public UUID familiaDe(Jwt refresh) {
        return UUID.fromString(refresh.getClaimAsString(CLAIM_FAMILIA));
    }

    public long clinicaDe(Jwt refresh) {
        Object valor = refresh.getClaim(CLAIM_CLINICA);
        if (valor == null) {
            throw new JwtException("Refresh token sem clinica_id.");
        }
        return ((Number) valor).longValue();
    }
}
