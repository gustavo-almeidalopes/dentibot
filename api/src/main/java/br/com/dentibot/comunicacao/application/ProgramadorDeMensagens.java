package br.com.dentibot.comunicacao.application;

import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.ConsultaResumo;
import br.com.dentibot.clinicas.ClinicasApi;
import br.com.dentibot.clinicas.ClinicasApi.Identificacao;
import br.com.dentibot.comunicacao.Espera;
import br.com.dentibot.comunicacao.domain.Cadencia;
import br.com.dentibot.comunicacao.domain.Textos;
import br.com.dentibot.comunicacao.infrastructure.ComunicacaoRepositorio;
import br.com.dentibot.comunicacao.infrastructure.ComunicacaoRepositorio.Canal;
import br.com.dentibot.identidade.Celular;
import br.com.dentibot.pacientes.PacienteResumo;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.outbox.ConsumidorDeEvento;
import br.com.dentibot.plataforma.outbox.EventoDominio;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import br.com.dentibot.plataforma.tenant.ContextoBanco;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A agenda mexeu; o WhatsApp reage (IA-16, IA-18, IA-37).
 *
 * <p>Por evento, como o financeiro: a agenda não sabe que existe WhatsApp, e
 * uma falha aqui não impede ninguém de marcar consulta. Aqui só se PROGRAMA —
 * quem manda é o {@link Despachante}, que confere de novo, na hora, se a
 * mensagem ainda faz sentido.
 *
 * <p>Idempotente pela contagem do que já existe para a consulta: a reentrega do
 * outbox não duplica lembrete.
 */
@Component
public class ProgramadorDeMensagens implements ConsumidorDeEvento {

    private static final Set<String> TIPOS = Set.of(TiposDeEvento.CONSULTA_AGENDADA,
            TiposDeEvento.CONSULTA_CANCELADA, TiposDeEvento.CONSULTA_REALIZADA);

    /** Três avisados por vaga: suficiente para preencher, pouco para frustrar. */
    private static final int OFERTAS_POR_VAGA = 3;

    private final ComunicacaoRepositorio repo;
    private final AgendaApi agenda;
    private final PacientesApi pacientes;
    private final ClinicasApi clinicas;
    private final ContextoBanco contextoBanco;

    public ProgramadorDeMensagens(ComunicacaoRepositorio repo, AgendaApi agenda,
                                  PacientesApi pacientes, ClinicasApi clinicas,
                                  ContextoBanco contextoBanco) {
        this.repo = repo;
        this.agenda = agenda;
        this.pacientes = pacientes;
        this.clinicas = clinicas;
        this.contextoBanco = contextoBanco;
    }

    @Override
    public String nome() {
        return "comunicacao.programador";
    }

    @Override
    public boolean interessadoEm(String tipoDeEvento) {
        return TIPOS.contains(tipoDeEvento);
    }

    /** Mesmo arranjo de tenant do {@code ConsumidorDeOrcamentoAprovado}. */
    @Override
    @Transactional
    public void consumir(EventoDominio evento) {
        ContextoRequisicao anterior = ContextoAtual.obter();
        contextoBanco.promoverClinica(evento.clinicaId());
        ContextoAtual.definir(ContextoRequisicao.deClinica(
                evento.clinicaId(), 0L, Papel.ADMIN, evento.correlationId()));
        try {
            Optional<Canal> canal = repo.canal().filter(Canal::ativo);
            if (canal.isEmpty()) {
                return;
            }
            long idConsulta = ((Number) evento.data().get("consulta_id")).longValue();
            ConsultaResumo consulta = agenda.buscar(idConsulta).orElse(null);
            if (consulta == null) {
                return;
            }
            switch (evento.eventType()) {
                case TiposDeEvento.CONSULTA_AGENDADA -> lembrar(canal.get(), consulta);
                case TiposDeEvento.CONSULTA_CANCELADA -> liberar(canal.get(), consulta);
                case TiposDeEvento.CONSULTA_REALIZADA -> acompanhar(canal.get(), consulta);
                default -> { }
            }
        } finally {
            ContextoAtual.definir(anterior);
        }
    }

    private void lembrar(Canal canal, ConsultaResumo c) {
        Optional<String> telefone = Celular.paraWhatsApp(c.telefonePaciente());
        if (!canal.liga("confirmacao") || telefone.isEmpty()
                || repo.contarDaConsulta(c.idConsulta(), "lembrete")
                        + repo.contarDaConsulta(c.idConsulta(), "confirmacao") > 0) {
            return;
        }
        Identificacao clinica = clinicas.identificacao();
        String quando = Textos.quando(c.inicioEm(), clinica.fuso());
        Cadencia.Risco risco = Cadencia.risco(c.faltasRecentes(), c.consultasRecentes());
        for (Cadencia.Envio e : Cadencia.plano(risco, c.inicioEm(), Instant.now(), clinica.fuso())) {
            String texto = switch (e.tipo()) {
                case "lembrete" -> Textos.lembrete(clinica.nome(), quando);
                case "confirmacao" -> Textos.confirmacao(clinica.nome(), quando);
                default -> Textos.reforco(clinica.nome(), quando);
            };
            repo.agendar(c.idPaciente(), telefone.get(), e.tipo(), c.idConsulta(), texto, e.quando());
        }
    }

    /**
     * Consulta cancelada: o que estava programado para ela morre, e a vaga vai
     * para a lista de espera (IA-18) — se ainda houver tempo de alguém chegar.
     */
    private void liberar(Canal canal, ConsultaResumo c) {
        repo.cancelarPendentesDaConsulta(c.idConsulta(), "consulta cancelada");
        Instant agora = Instant.now();
        if (!canal.liga("oferta_vaga") || c.inicioEm().isBefore(agora.plus(2, ChronoUnit.HOURS))
                || repo.contarDaConsulta(c.idConsulta(), "oferta_vaga") > 0) {
            return;
        }
        Identificacao clinica = clinicas.identificacao();
        String periodo = c.inicioEm().atZone(clinica.fuso()).getHour() < 12 ? "manha" : "tarde";
        List<Espera> candidatos = repo.candidatos(c.idDentista(), periodo, c.idPaciente(),
                OFERTAS_POR_VAGA);
        if (candidatos.isEmpty()) {
            return;
        }
        var resumos = pacientes.mapaDeResumos(candidatos.stream().map(Espera::idPaciente).toList());
        // A oferta vale até uma hora antes, ou quatro horas — o que vier primeiro.
        Instant expira = c.inicioEm().minus(1, ChronoUnit.HOURS);
        if (expira.isAfter(agora.plus(4, ChronoUnit.HOURS))) {
            expira = agora.plus(4, ChronoUnit.HOURS);
        }
        String texto = Textos.oferta(clinica.nome(), Textos.quando(c.inicioEm(), clinica.fuso()));
        Instant quando = Cadencia.avancarParaHorarioUtil(agora, clinica.fuso());
        for (Espera e : candidatos) {
            PacienteResumo p = resumos.get(e.idPaciente());
            Optional<String> telefone = Celular.paraWhatsApp(p == null ? null : p.telefoneCelular());
            if (telefone.isEmpty()) {
                continue;
            }
            repo.criarOferta(e.idEspera(), e.idPaciente(), c.idDentista(), c.inicioEm(),
                    c.terminoEm(), expira);
            repo.agendar(e.idPaciente(), telefone.get(), "oferta_vaga", c.idConsulta(), texto, quando);
        }
    }

    /** IA-37: no dia seguinte, às 10h, "como você está?". */
    private void acompanhar(Canal canal, ConsultaResumo c) {
        Optional<String> telefone = Celular.paraWhatsApp(c.telefonePaciente());
        if (!canal.liga("pos_procedimento") || telefone.isEmpty()
                || repo.contarDaConsulta(c.idConsulta(), "pos_procedimento") > 0) {
            return;
        }
        Identificacao clinica = clinicas.identificacao();
        repo.agendar(c.idPaciente(), telefone.get(), "pos_procedimento", c.idConsulta(),
                Textos.posProcedimento(clinica.nome()),
                Cadencia.diaSeguinte(Instant.now(), clinica.fuso(), 1));
    }
}
