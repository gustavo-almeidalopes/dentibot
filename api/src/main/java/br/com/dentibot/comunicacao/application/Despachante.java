package br.com.dentibot.comunicacao.application;

import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.comunicacao.Mensagem;
import br.com.dentibot.comunicacao.infrastructure.ClienteWhatsApp;
import br.com.dentibot.comunicacao.infrastructure.ComunicacaoRepositorio;
import br.com.dentibot.comunicacao.infrastructure.ComunicacaoRepositorio.Canal;
import br.com.dentibot.comunicacao.infrastructure.ComunicacaoRepositorio.Pendente;
import br.com.dentibot.lgpd.Finalidade;
import br.com.dentibot.lgpd.LgpdApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tira da fila o que venceu e manda (IA-16, IA-32, IA-37, IA-38).
 *
 * <p>Três transações por mensagem, e a chamada à Meta fora de todas: preparar
 * (com a clínica promovida) → enviar → marcar. Conexão de banco presa esperando
 * a Meta responder é o jeito de um WhatsApp lento derrubar a API inteira.
 *
 * <p>Na preparação, a mensagem é conferida DE NOVO contra o mundo de agora:
 * consulta cancelada, confirmação que já chegou, paciente que respondeu PARAR,
 * limite de insistência. Programar e mandar são momentos diferentes, e só o
 * segundo sabe o que é verdade.
 */
@Component
public class Despachante {

    private static final Logger log = LoggerFactory.getLogger(Despachante.class);
    private static final int LOTE = 20;

    /** Mais que isso em 24 horas vira cobrança, não lembrete (IA-32). */
    static final int LIMITE_AUTOMATICAS_24H = 3;

    /** Iniciativa da clínica: dependem da preferência do paciente. Resposta não. */
    private static final Set<String> DE_INICIATIVA_DA_CLINICA = Set.of(
            "lembrete", "confirmacao", "reforco", "oferta_vaga", "pos_procedimento", "orientacao");

    /** Em que estado a consulta precisa estar para a mensagem ainda fazer sentido. */
    private static final Map<String, Set<String>> STATUS_QUE_VALEM = Map.of(
            "lembrete", Set.of("agendada", "confirmada"),
            "confirmacao", Set.of("agendada"),
            "reforco", Set.of("agendada"),
            "pos_procedimento", Set.of("realizada"));

    private final ComunicacaoRepositorio repo;
    private final ClienteWhatsApp whatsapp;
    private final AgendaApi agenda;
    private final LgpdApi lgpd;
    private final TransactionTemplate transacao;

    public Despachante(ComunicacaoRepositorio repo, ClienteWhatsApp whatsapp, AgendaApi agenda,
                       LgpdApi lgpd, TransactionTemplate transacao) {
        this.repo = repo;
        this.whatsapp = whatsapp;
        this.agenda = agenda;
        this.lgpd = lgpd;
        this.transacao = transacao;
    }

    record Preparada(long idMensagem, String phoneNumberId, String telefone, String texto,
                     boolean dentroDaJanela) {
    }

    @Scheduled(fixedDelayString = "${dentibot.comunicacao.intervalo:30000}")
    public void despachar() {
        if (!whatsapp.configurado()) {
            return;
        }
        List<Pendente> lote;
        ContextoAtual.definir(ContextoRequisicao.deWorker(UUID.randomUUID()));
        try {
            lote = transacao.execute(s -> {
                repo.falharTravadas();
                return repo.travarVencidas(LOTE);
            });
        } finally {
            ContextoAtual.limpar();
        }
        for (Pendente p : lote) {
            ContextoAtual.definir(ContextoRequisicao.deClinica(
                    p.idClinica(), 0L, Papel.ADMIN, UUID.randomUUID()));
            try {
                enviar(p.idMensagem());
            } catch (RuntimeException e) {
                // Uma mensagem ruim não para o lote.
                log.error("Falha ao despachar a mensagem {}", p.idMensagem(), e);
            } finally {
                ContextoAtual.limpar();
            }
        }
    }

    private void enviar(long idMensagem) {
        Preparada prep = transacao.execute(s -> preparar(idMensagem));
        if (prep == null) {
            return;
        }
        try {
            String idExterno = whatsapp.enviar(prep.phoneNumberId(), prep.telefone(), prep.texto(),
                    prep.dentroDaJanela());
            transacao.executeWithoutResult(s -> repo.marcarEnviada(idMensagem, idExterno));
        } catch (RuntimeException e) {
            transacao.executeWithoutResult(s -> repo.encerrar(idMensagem, "falhou", e.getMessage()));
        }
    }

    /** Null = não manda; o motivo fica na linha. */
    private Preparada preparar(long idMensagem) {
        Mensagem m = repo.buscar(idMensagem).orElse(null);
        if (m == null) {
            return null;
        }
        String motivo = motivoParaNaoEnviar(m);
        if (motivo != null) {
            repo.encerrar(idMensagem, "cancelada", motivo);
            return null;
        }
        Canal canal = repo.canal().orElseThrow();
        return new Preparada(idMensagem, canal.phoneNumberId(), m.telefone(), m.texto(),
                repo.janelaAberta(m.telefone()));
    }

    private String motivoParaNaoEnviar(Mensagem m) {
        Canal canal = repo.canal().orElse(null);
        if (canal == null || !canal.ativo()) {
            return "canal desligado";
        }
        if (DE_INICIATIVA_DA_CLINICA.contains(m.tipo())) {
            if (!m.tipo().equals("orientacao") && !canal.liga(tipoDaConfiguracao(m.tipo()))) {
                return "tipo desligado pela clínica";
            }
            if (m.idPaciente() != null && !lgpd.permite(m.idPaciente(), Finalidade.WHATSAPP)) {
                return "paciente desligou o WhatsApp";
            }
            if (!m.tipo().equals("orientacao")
                    && repo.automaticasNasUltimas24h(m.telefone()) >= LIMITE_AUTOMATICAS_24H) {
                return "limite de mensagens em 24 horas";
            }
        }
        Set<String> valem = STATUS_QUE_VALEM.get(m.tipo());
        if (valem != null && m.idConsulta() != null) {
            String status = agenda.buscar(m.idConsulta()).map(c -> c.status()).orElse(null);
            if (status == null || !valem.contains(status)) {
                return "consulta " + (status == null ? "inexistente" : status);
            }
        }
        return null;
    }

    /** Lembrete e reforço ligam e desligam junto com a confirmação. */
    static String tipoDaConfiguracao(String tipo) {
        return switch (tipo) {
            case "lembrete", "reforco" -> "confirmacao";
            default -> tipo;
        };
    }
}
