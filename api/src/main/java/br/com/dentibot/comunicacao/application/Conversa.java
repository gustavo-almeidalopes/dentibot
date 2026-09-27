package br.com.dentibot.comunicacao.application;

import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.ConsultaResumo;
import br.com.dentibot.agenda.NovaConsulta;
import br.com.dentibot.clinicas.ClinicasApi;
import br.com.dentibot.comunicacao.Mensagem;
import br.com.dentibot.comunicacao.domain.Cadencia;
import br.com.dentibot.comunicacao.domain.Textos;
import br.com.dentibot.comunicacao.domain.Triagem;
import br.com.dentibot.comunicacao.domain.Triagem.Intencao;
import br.com.dentibot.comunicacao.infrastructure.ComunicacaoRepositorio;
import br.com.dentibot.comunicacao.infrastructure.ComunicacaoRepositorio.Oferta;
import br.com.dentibot.lgpd.LgpdApi;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O paciente escreveu (IA-28, IA-29, IA-31, IA-37, IA-38, IA-18).
 *
 * <p>A recepção virtual resolve o que é simples a qualquer hora — confirmar,
 * cancelar, aceitar vaga, parar mensagens — e passa para gente tudo o que não é:
 * urgência, sintoma, reclamação, remarcação. Ela nunca responde pergunta
 * clínica: responde que alguém vai responder, e no caso de urgência diz onde
 * procurar socorro agora.
 *
 * <p>Cada mensagem é uma transação, na clínica do número que recebeu. A
 * resposta vai para a fila (tipo {@code resposta}), e o webhook devolve 200 à
 * Meta sem esperar o envio.
 */
@Service
public class Conversa {

    private final ComunicacaoRepositorio repo;
    private final PacientesApi pacientes;
    private final AgendaApi agenda;
    private final LgpdApi lgpd;
    private final ClinicasApi clinicas;
    private final TransactionTemplate transacao;
    private final TransactionTemplate aninhada;

    public Conversa(ComunicacaoRepositorio repo, PacientesApi pacientes, AgendaApi agenda,
                    LgpdApi lgpd, ClinicasApi clinicas, TransactionTemplate transacao) {
        this.repo = repo;
        this.pacientes = pacientes;
        this.agenda = agenda;
        this.lgpd = lgpd;
        this.clinicas = clinicas;
        this.transacao = transacao;
        this.aninhada = new TransactionTemplate(transacao.getTransactionManager());
        this.aninhada.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    /** {@code midia}: áudio, foto, documento — não baixamos; vai para gente, que ouve no aparelho. */
    public record Recebida(String phoneNumberId, String idExterno, String telefone, String texto,
                           boolean midia) {
    }

    public record StatusRecebido(String phoneNumberId, String idExterno, String status, Instant em) {
    }

    /** Mensagem de número sem canal ativo é descartada: não há clínica a quem entregar. */
    public void receber(Recebida r) {
        emClinica(r.phoneNumberId(), () -> atender(r));
    }

    public void status(StatusRecebido s) {
        String nosso = switch (s.status()) {
            case "sent" -> "enviada";
            case "delivered" -> "entregue";
            case "read" -> "lida";
            case "failed" -> "falhou";
            default -> null;
        };
        if (nosso != null) {
            emClinica(s.phoneNumberId(), () -> repo.atualizarStatus(s.idExterno(), nosso, s.em()));
        }
    }

    private void emClinica(String phoneNumberId, Runnable acao) {
        ContextoRequisicao anterior = ContextoAtual.obter();
        try {
            Optional<Long> clinica = transacao.execute(s -> repo.clinicaDoCanal(phoneNumberId));
            if (clinica == null || clinica.isEmpty()) {
                return;
            }
            // Usuário 0 com papel de admin: é o sistema agindo a pedido do
            // paciente — confirmar, cancelar —, e a trilha registra assim.
            ContextoAtual.definir(ContextoRequisicao.deClinica(
                    clinica.get(), 0L, Papel.ADMIN,
                    anterior.correlacaoId() == null ? UUID.randomUUID() : anterior.correlacaoId()));
            transacao.executeWithoutResult(s -> acao.run());
        } finally {
            ContextoAtual.definir(anterior);
        }
    }

    private void atender(Recebida r) {
        List<Long> doNumero = pacientes.porCelular(r.telefone());
        Long idPaciente = doNumero.size() == 1 ? doNumero.getFirst() : null;
        Optional<Long> idEntrada = repo.registrarEntrada(idPaciente, r.telefone(), r.texto(),
                r.idExterno());
        if (idEntrada.isEmpty()) {
            return;
        }
        Optional<Mensagem> pergunta = repo.ultimaPergunta(r.telefone());
        // Áudio pode ser alguém descrevendo uma urgência: ninguém "entende" por
        // regra o que não leu. Vai para gente.
        Intencao intencao = r.midia() ? Intencao.HUMANO
                : Triagem.interpretar(pergunta.map(Mensagem::tipo).orElse(null), r.texto());
        String resposta = agir(intencao, doNumero, pergunta);

        // SEM_CONSULTA promete que alguém responde — então escala também.
        String escalada = intencao.urgente() ? "urgente"
                : intencao.escala() || resposta.equals(Textos.SEM_CONSULTA) ? "normal" : null;
        repo.classificar(idEntrada.get(), intencao.name().toLowerCase(), escalada);
        repo.agendar(idPaciente, r.telefone(), "resposta", null, resposta, Instant.now());
    }

    private String agir(Intencao intencao, List<Long> doNumero, Optional<Mensagem> pergunta) {
        return switch (intencao) {
            case URGENCIA, POS_PREOCUPANTE -> Textos.URGENCIA;
            case RECLAMACAO, CLINICO, HUMANO -> Textos.ESCALADA;
            case REMARCAR -> Textos.REMARCAR;
            case PARAR -> {
                doNumero.forEach(p -> lgpd.preferenciaPeloWhatsApp(p, false));
                yield Textos.PARADO;
            }
            case VOLTAR -> {
                doNumero.forEach(p -> lgpd.preferenciaPeloWhatsApp(p, true));
                yield Textos.VOLTOU;
            }
            case CONFIRMAR -> proximaConsulta(doNumero)
                    .map(c -> {
                        if (c.status().equals("agendada")) {
                            agenda.confirmar(c.idConsulta());
                        }
                        return Textos.confirmado(quando(c.inicioEm()));
                    })
                    .orElse(Textos.SEM_CONSULTA);
            case CANCELAR -> proximaConsulta(doNumero)
                    .map(c -> {
                        agenda.cancelar(c.idConsulta(), "Cancelada pelo paciente pelo WhatsApp");
                        return Textos.cancelado(quando(c.inicioEm()));
                    })
                    .orElse(Textos.SEM_CONSULTA);
            case ACEITAR -> aceitarOferta(doNumero);
            case RECUSAR -> {
                repo.ofertaAberta(doNumero).ifPresent(o -> repo.fecharOferta(o.idOferta(), "recusada"));
                yield Textos.OFERTA_RECUSADA;
            }
            case POS_BEM -> Textos.POS_BEM;
            case POS_LEVE -> {
                // Pergunta de novo em dois dias — uma vez só por consulta.
                pergunta.filter(m -> m.idConsulta() != null
                                && repo.contarDaConsulta(m.idConsulta(), "pos_procedimento") < 2)
                        .ifPresent(m -> repo.agendar(m.idPaciente(), m.telefone(),
                                "pos_procedimento", m.idConsulta(),
                                Textos.posProcedimento(clinicas.identificacao().nome()),
                                Cadencia.diaSeguinte(Instant.now(), fuso(), 2)));
                yield Textos.POS_LEVE;
            }
            case CIENTE -> {
                pergunta.ifPresent(m -> repo.registrarCiencia(m.idMensagem()));
                yield Textos.CIENTE;
            }
            case MENU -> Textos.MENU;
        };
    }

    /**
     * A vaga é de quem responder primeiro: fechar a oferta é um UPDATE com
     * {@code status = 'aberta'} na condição, e o agendamento ainda passa pela
     * trava de conflito da agenda. Dois SIM ao mesmo tempo: um marca, o outro
     * ouve que a vaga acabou de ser preenchida.
     */
    private String aceitarOferta(List<Long> doNumero) {
        Optional<Oferta> aberta = repo.ofertaAberta(doNumero);
        if (aberta.isEmpty() || repo.fecharOferta(aberta.get().idOferta(), "aceita") == 0) {
            return Textos.OFERTA_PERDIDA;
        }
        Oferta o = aberta.get();
        try {
            // Savepoint: o conflito de horário estoura dentro do Postgres, e sem
            // ele a transação inteira — com a mensagem recebida — iria junto.
            aninhada.executeWithoutResult(s -> agenda.agendar(new NovaConsulta(o.idPaciente(), o.idDentista(), null, o.inicioEm(),
                    o.terminoEm(), "Vaga ofertada pelo WhatsApp (lista de espera)")));
        } catch (RuntimeException e) {
            // Conflito: a recepção encaixou alguém por fora. A oferta volta a
            // não valer, e o paciente continua na lista.
            repo.fecharOferta(o.idOferta(), "expirada");
            return Textos.OFERTA_PERDIDA;
        }
        repo.expirarConcorrentes(o);
        repo.encerrarEspera(o.idEspera());
        return Textos.ofertaAceita(quando(o.inicioEm()));
    }

    /** A próxima consulta marcada de quem usa este número — família dividindo celular inclusa. */
    private Optional<ConsultaResumo> proximaConsulta(List<Long> doNumero) {
        Instant agora = Instant.now();
        return doNumero.stream()
                .flatMap(p -> agenda.historicoDoPaciente(p).stream())
                .filter(c -> c.inicioEm().isAfter(agora)
                        && (c.status().equals("agendada") || c.status().equals("confirmada")))
                .min(Comparator.comparing(ConsultaResumo::inicioEm));
    }

    private String quando(Instant instante) {
        return Textos.quando(instante, fuso());
    }

    private ZoneId fuso() {
        return clinicas.identificacao().fuso();
    }
}
