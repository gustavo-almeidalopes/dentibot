package br.com.dentibot.lgpd.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.auditoria.EventoAuditoria;
import br.com.dentibot.clinicas.ClinicasApi;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.MembroEquipe;
import br.com.dentibot.lgpd.Finalidade;
import br.com.dentibot.lgpd.Preferencia;
import br.com.dentibot.lgpd.infrastructure.LgpdRepositorio;
import br.com.dentibot.lgpd.infrastructure.LgpdRepositorio.Titular;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.tenant.ContextoBanco;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O paciente, pelo link, sem conta (IA-52, IA-53).
 *
 * <p>O link é a credencial: resolvido pela função SECURITY DEFINER da V25, ele
 * dá o tenant e o paciente, e mais nada — tudo aqui é sobre ESTE paciente, e
 * nenhuma rota aceita outro id. Link inválido, expirado ou revogado responde o
 * mesmo 404, para não ensinar a quem tenta adivinhar qual dos três foi.
 */
@Service
public class TitularServico {

    /** O extrato cobre um ano: o bastante para "quem abriu meu prontuário". */
    private static final int DIAS_DE_EXTRATO = 365;
    private static final int DIAS_DE_PRAZO = 15;

    public record Acesso(long idEvento, Instant em, String quem, String acao, String recurso) {
    }

    public record Painel(String clinica, List<Preferencia> preferencias, List<Acesso> acessos) {
    }

    private final LgpdRepositorio lgpd;
    private final LgpdServico servico;
    private final AuditoriaApi auditoria;
    private final IdentidadeApi identidade;
    private final ClinicasApi clinicas;
    private final ContextoBanco contextoBanco;
    private final TransactionTemplate transacao;

    public TitularServico(LgpdRepositorio lgpd, LgpdServico servico, AuditoriaApi auditoria,
                          IdentidadeApi identidade, ClinicasApi clinicas,
                          ContextoBanco contextoBanco, TransactionTemplate transacao) {
        this.lgpd = lgpd;
        this.servico = servico;
        this.auditoria = auditoria;
        this.identidade = identidade;
        this.clinicas = clinicas;
        this.contextoBanco = contextoBanco;
        this.transacao = transacao;
    }

    public Painel painel(String token) {
        return comoTitular(token, t -> {
            // Olhar o próprio extrato também fica na trilha — fora do extrato,
            // que é sobre o que OUTROS fizeram com o dado.
            auditoria.registrarLeitura("lgpd.extrato_titular", String.valueOf(t.idPaciente()));
            return new Painel(clinicas.identificacao().nome(),
                    servico.montarPreferencias(t.idPaciente()), acessos(t.idPaciente()));
        });
    }

    public List<Preferencia> alterar(String token, Finalidade finalidade, boolean permitido) {
        return comoTitular(token, t -> {
            servico.gravarPreferencia(t.idPaciente(), finalidade, permitido, "titular", null);
            return servico.montarPreferencias(t.idPaciente());
        });
    }

    /**
     * Contesta um acesso do extrato. Vira solicitação de oposição (art. 18
     * §2º), com o prazo de sempre e na fila que a clínica já responde — em vez
     * de um canal paralelo que ninguém olha.
     */
    public long opor(String token, long idEvento, String texto) {
        return comoTitular(token, t -> {
            Acesso acesso = acessos(t.idPaciente()).stream()
                    .filter(a -> a.idEvento() == idEvento)
                    .findFirst()
                    .orElseThrow(() -> new RecursoNaoEncontradoException("acesso", idEvento));
            String detalhe = "Acesso #%d em %s por %s (%s). %s".formatted(
                    acesso.idEvento(), acesso.em(), acesso.quem(), acesso.recurso(), texto.strip());
            long id = lgpd.inserirOposicao(t.idPaciente(),
                    detalhe.substring(0, Math.min(detalhe.length(), 500)),
                    LocalDate.now().plusDays(DIAS_DE_PRAZO));
            auditoria.registrarCriacao("lgpd.solicitacao", String.valueOf(id),
                    Map.of("idPaciente", t.idPaciente(), "direito", "oposicao",
                            "idEvento", idEvento));
            return id;
        });
    }

    private List<Acesso> acessos(long idPaciente) {
        Map<Long, MembroEquipe> equipe = identidade.listarEquipe().stream()
                .collect(Collectors.toMap(MembroEquipe::idUsuario, Function.identity()));
        return auditoria.acessosAoPaciente(idPaciente,
                        Instant.now().minus(DIAS_DE_EXTRATO, ChronoUnit.DAYS)).stream()
                .map(e -> new Acesso(e.idEvento(), e.ocorridoEm(), quem(e, equipe),
                        e.acao(), e.recurso()))
                .toList();
    }

    /** Nome e papel — o paciente tem direito de saber QUEM, não um id. */
    private static String quem(EventoAuditoria e, Map<Long, MembroEquipe> equipe) {
        if (e.staffPapel() != null) {
            return "Suporte DentiBot (" + e.staffPapel() + ")";
        }
        MembroEquipe m = e.idUsuario() == null ? null : equipe.get(e.idUsuario());
        if (m == null) {
            return e.idUsuario() == null || e.idUsuario() == 0L
                    ? "Sistema (automático)" : "Ex-integrante da equipe";
        }
        return m.nomeCompleto() + " (" + m.papel().name().toLowerCase() + ")";
    }

    private <T> T comoTitular(String token, Function<Titular, T> acao) {
        ContextoRequisicao anterior = ContextoAtual.obter();
        try {
            return transacao.execute(s -> {
                Titular t = lgpd.titularDoToken(sha256(token))
                        .orElseThrow(() -> new RecursoNaoEncontradoException("link", "titular"));
                contextoBanco.promoverClinica(t.idClinica());
                // Usuário 0: é o sistema agindo a pedido do titular, e a trilha
                // registra assim — não um membro da equipe que não esteve lá.
                ContextoAtual.definir(ContextoRequisicao.deClinica(
                        t.idClinica(), 0L, Papel.ADMIN, anterior.correlacaoId()));
                return acao.apply(t);
            });
        } finally {
            ContextoAtual.definir(anterior);
        }
    }

    static String sha256(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
