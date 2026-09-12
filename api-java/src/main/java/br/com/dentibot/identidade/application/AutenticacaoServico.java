package br.com.dentibot.identidade.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.identidade.domain.CredenciaisUsuario;
import br.com.dentibot.identidade.domain.Sessao;
import br.com.dentibot.identidade.infrastructure.SessaoRepositorio;
import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.seguranca.BlocklistDeToken;
import br.com.dentibot.plataforma.seguranca.ServicoDeToken;
import br.com.dentibot.plataforma.tenant.ContextoBanco;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Login, refresh rotativo e logout.
 *
 * <p>Três decisões que valem a leitura:
 *
 * <ol>
 *   <li><b>Resposta única para qualquer falha.</b> E-mail inexistente, senha
 *       errada e conta bloqueada devolvem a mesma coisa. Distinguir permitiria
 *       enumerar usuários — e "esta conta está bloqueada" já confirma que ela
 *       existe.</li>
 *   <li><b>Hash sempre calculado</b>, mesmo sem usuário. Sem isso, o tempo de
 *       resposta denuncia quais e-mails existem: Argon2 leva ~50 ms e um
 *       "não achei" leva 1 ms.</li>
 *   <li><b>Refresh rotativo com detecção de reuso.</b> Cada uso queima o token e
 *       emite outro. Se um token já queimado reaparece, a família inteira cai.</li>
 * </ol>
 */
@Service
public class AutenticacaoServico {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoServico.class);

    /** Hash de descarte, para gastar o mesmo tempo quando o e-mail não existe. */
    private static final String HASH_FALSO =
            "$argon2id$v=19$m=19456,t=2,p=1$c29tZXNhbHRzb21lc2FsdA$"
                    + "J0kZ2Xm8sWvVQnV0zJ4kMhqEt0mJQ7bYQm0bDkVv0hI";

    private final UsuarioRepositorio usuarios;
    private final SessaoRepositorio sessoes;
    private final PasswordEncoder encoder;
    private final ServicoDeToken tokens;
    private final BlocklistDeToken blocklist;
    private final ContextoBanco contextoBanco;
    private final AuditoriaApi auditoria;
    private final int maxFalhas;
    private final Duration duracaoBloqueio;
    /** Transação independente, para escritas que precisam sobreviver a rollback. */
    private final TransactionTemplate transacaoPropria;

    public AutenticacaoServico(UsuarioRepositorio usuarios, SessaoRepositorio sessoes,
                               PasswordEncoder encoder, ServicoDeToken tokens,
                               BlocklistDeToken blocklist, ContextoBanco contextoBanco,
                               AuditoriaApi auditoria,
                               PlatformTransactionManager gerenciadorDeTransacao,
                               @Value("${dentibot.seguranca.max-falhas-login:5}") int maxFalhas,
                               @Value("${dentibot.seguranca.duracao-bloqueio:PT30M}") Duration duracaoBloqueio) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.encoder = encoder;
        this.tokens = tokens;
        this.blocklist = blocklist;
        this.contextoBanco = contextoBanco;
        this.auditoria = auditoria;
        this.maxFalhas = maxFalhas;
        this.duracaoBloqueio = duracaoBloqueio;
        this.transacaoPropria = new TransactionTemplate(gerenciadorDeTransacao);
        this.transacaoPropria.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public record Credenciais(String email, String senha, String ip, String userAgent) {
    }

    public record ParDeTokens(String access, String refresh, java.time.Instant accessExpiraEm) {
    }

    public static class CredenciaisInvalidasException extends RuntimeException {
        public CredenciaisInvalidasException() {
            super("Credenciais inválidas.");
        }
    }

    @Transactional
    public ParDeTokens autenticar(Credenciais cred) {
        // Antes disto não existe tenant. A função SECURITY DEFINER da V3 é a
        // única travessia legítima do RLS no sistema.
        Optional<Long> clinica = usuarios.resolverClinicaPorEmail(cred.email());

        if (clinica.isEmpty()) {
            // Gasta o mesmo tempo de um Argon2 real. Sem isto, medir a latência
            // do endpoint entrega a lista de e-mails cadastrados.
            encoder.matches(cred.senha(), HASH_FALSO);
            throw new CredenciaisInvalidasException();
        }

        long idClinica = clinica.get();
        contextoBanco.promoverClinica(idClinica);
        ContextoAtual.definir(ContextoRequisicao.deClinica(
                idClinica, 0L, br.com.dentibot.plataforma.contexto.Papel.ADMIN,
                ContextoAtual.correlacao()));

        CredenciaisUsuario u = usuarios.buscarCredenciaisPorEmail(cred.email())
                .orElseThrow(CredenciaisInvalidasException::new);

        usuarios.desbloquearSeExpirado(u.idUsuario());

        if (u.bloqueado() || !u.ativo()) {
            auditoria.registrarAutenticacao("falha_login", idClinica, u.idUsuario(),
                    cred.ip(), cred.userAgent());
            throw new CredenciaisInvalidasException();
        }

        if (!encoder.matches(cred.senha(), u.senhaHash())) {
            // Mesmo motivo da revogação de família: em transação própria, senão
            // o throw abaixo desfaz a contagem e o limite de tentativas nunca é
            // atingido — o lockout viraria enfeite contra força bruta.
            Integer falhas = transacaoPropria.execute(s ->
                    usuarios.registrarFalhaLogin(u.idUsuario(), maxFalhas, duracaoBloqueio));
            auditoria.registrarAutenticacao("falha_login", idClinica, u.idUsuario(),
                    cred.ip(), cred.userAgent());
            log.info("Falha de login: usuario={} falhas={}", u.idUsuario(), falhas);
            throw new CredenciaisInvalidasException();
        }

        usuarios.registrarLoginBemSucedido(u.idUsuario());
        auditoria.registrarAutenticacao("login", idClinica, u.idUsuario(),
                cred.ip(), cred.userAgent());

        return emitirPar(u.idClinica(), u.idUsuario(), u.papel(), UUID.randomUUID(),
                cred.ip(), cred.userAgent());
    }

    /**
     * Rotaciona o refresh. O token apresentado é queimado e outro nasce na mesma
     * família.
     */
    @Transactional
    public ParDeTokens renovar(String refreshToken, String ip, String userAgent) {
        Jwt jwt = tokens.lerRefresh(refreshToken);
        UUID jti = UUID.fromString(jwt.getId());
        UUID familia = tokens.familiaDe(jwt);

        // O tenant vem do claim assinado. A tabela de sessões tem RLS: sem
        // promover o contexto antes, a busca por jti devolveria vazio e um
        // refresh legítimo pareceria inválido.
        long idClinica = tokens.clinicaDe(jwt);
        contextoBanco.promoverClinica(idClinica);
        ContextoAtual.definir(ContextoRequisicao.deClinica(
                idClinica, Long.parseLong(jwt.getSubject()),
                br.com.dentibot.plataforma.contexto.Papel.ADMIN, ContextoAtual.correlacao()));

        Sessao sessao = sessoes.buscarPorJti(jti)
                .orElseThrow(CredenciaisInvalidasException::new);

        if (sessao.indicaReuso()) {
            // REQUIRES_NEW, e é o ponto inteiro deste bloco: a resposta de
            // segurança precisa SOBREVIVER à exceção que a reporta. Na
            // transação corrente, o throw logo abaixo daria rollback e
            // desfaria a revogação — o atacante seria "detectado" e continuaria
            // com a sessão viva. Só apareceu porque havia teste cobrando que o
            // token bom também morresse.
            int derrubadas = transacaoPropria.execute(s ->
                    sessoes.revogarFamilia(familia, "reuso de refresh token"));
            log.warn("Reuso de refresh detectado: familia={} sessoes_revogadas={}",
                    familia, derrubadas);
            throw new CredenciaisInvalidasException();
        }
        if (!sessao.valida() || !hash(refreshToken).equals(sessao.refreshHash())) {
            throw new CredenciaisInvalidasException();
        }

        sessoes.revogar(jti, "rotacionado");

        CredenciaisUsuario u = usuarios.buscarCredenciaisPorId(sessao.idUsuario())
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!u.ativo()) {
            throw new CredenciaisInvalidasException();
        }

        return emitirPar(sessao.idClinica(), sessao.idUsuario(), u.papel(), familia, ip, userAgent);
    }

    /**
     * Logout. Derruba as sessões (refresh) do usuário; o access token corrente
     * morre sozinho em minutos.
     */
    @Transactional
    public void sairDeTodasAsSessoes(long idUsuario) {
        sessoes.revogarTodasDoUsuario(idUsuario, "logout");
        auditoria.registrarAutenticacao("logout", ContextoAtual.clinicaObrigatoria(),
                idUsuario, null, null);
    }

    /**
     * Revogação imediata de um access token específico — usada quando não dá
     * para esperar a expiração natural: troca de senha, desativação de usuário,
     * suspeita de comprometimento.
     */
    @Transactional
    public void revogarAccess(UUID jti, java.time.Instant expiraEm) {
        blocklist.revogar(jti, expiraEm);
    }

    private ParDeTokens emitirPar(long idClinica, long idUsuario,
                                  br.com.dentibot.plataforma.contexto.Papel papel,
                                  UUID familia, String ip, String userAgent) {
        ServicoDeToken.TokenEmitido access =
                tokens.emitirAccessDeClinica(idUsuario, idClinica, papel);
        ServicoDeToken.TokenEmitido refresh =
                tokens.emitirRefresh(String.valueOf(idUsuario), idClinica, familia);

        // Guarda o HASH do refresh, nunca o token. Um dump do banco não pode
        // virar sessão ativa de ninguém.
        sessoes.criar(idClinica, idUsuario, refresh.jti(), familia,
                hash(refresh.valor()), userAgent, ip, refresh.expiraEm());

        return new ParDeTokens(access.valor(), refresh.valor(), access.expiraEm());
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
