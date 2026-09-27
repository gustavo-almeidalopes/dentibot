package br.com.dentibot.agenda.application;

import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.ConsultaResumo;
import br.com.dentibot.agenda.NovaConsulta;
import br.com.dentibot.agenda.SituacaoNaAgenda;
import br.com.dentibot.agenda.infrastructure.ConsultaRepositorio;
import br.com.dentibot.agenda.infrastructure.ConsultaRepositorio.LinhaConsulta;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.pacientes.PacienteResumo;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.outbox.Outbox;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.Alcance;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaServico implements AgendaApi {

    private final ConsultaRepositorio consultas;
    private final PacientesApi pacientes;
    private final IdentidadeApi identidade;
    private final AvaliadorDePermissao permissao;
    private final Outbox outbox;

    public AgendaServico(ConsultaRepositorio consultas, PacientesApi pacientes,
                         IdentidadeApi identidade, AvaliadorDePermissao permissao,
                         Outbox outbox) {
        this.consultas = consultas;
        this.pacientes = pacientes;
        this.identidade = identidade;
        this.permissao = permissao;
        this.outbox = outbox;
    }

    public static class TransicaoInvalidaException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TransicaoInvalidaException(String mensagem) {
            super(mensagem);
        }
    }

    /**
     * Agenda e emite {@code consulta.agendada} na MESMA transação (invariante 8).
     *
     * <p>A ordem errada — publicar o evento fora da transação — falha de duas
     * formas, ambas invisíveis em desenvolvimento e certas em produção: se a
     * transação der rollback depois do publish, o paciente recebe confirmação de
     * um horário que ninguém marcou; se o publish falhar depois do commit, a
     * consulta existe e ninguém é avisado.
     */
    @Override
    @Transactional
    public long agendar(NovaConsulta nova) {
        Alcance alcance = permissao.exigir(Recurso.AGENDA, Acao.CRIAR);
        // idDentista vem do corpo. Com PROPRIOS, aceitá-lo sem conferir deixaria
        // um dentista encher a agenda de um colega.
        if (alcance == Alcance.PROPRIOS && dentistaCorrente() != nova.idDentista()) {
            throw new AcessoNegadoException(Recurso.AGENDA, Acao.CRIAR);
        }

        long id = consultas.inserir(
                nova.idPaciente(), nova.idDentista(), nova.idProcedimento(),
                nova.inicioEm(), nova.terminoEm(), nova.observacoes());

        outbox.gravar(TiposDeEvento.CONSULTA_AGENDADA, Map.of(
                "consulta_id", id,
                "paciente_id", nova.idPaciente(),
                "dentista_id", nova.idDentista(),
                "inicio_em", nova.inicioEm().toString()));

        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaResumo> listar(Instant de, Instant ate, Long idDentista) {
        Alcance alcance = permissao.exigir(Recurso.AGENDA, Acao.LER);

        // É aqui que Alcance.PROPRIOS vira filtro de linha. O dentista não
        // escolhe de quem é a agenda que vê: o filtro é imposto e o parâmetro
        // que ele mandou é descartado.
        //
        // id_usuario != id_dentista. Traduzir um pelo outro é obrigatório: sem
        // a tradução, o filtro casaria por coincidência numérica e mostraria a
        // agenda de outro profissional.
        //
        // Long.valueOf explícito: com `long` num lado e `Long` no outro, o
        // ternário desembrulhava idDentista — e a agenda da clínica inteira
        // (recepção sem filtro de dentista, que é o padrão da tela) dava NPE.
        Long filtro = alcance == Alcance.PROPRIOS ? Long.valueOf(dentistaCorrente()) : idDentista;

        return montar(consultas.listar(de, ate, filtro));
    }

    /**
     * O id_dentista de quem está falando, vindo do contexto e nunca da
     * requisição. Usuário com alcance "próprios" que não é dentista não tem
     * agenda própria: -1 não casa com linha nenhuma — zero linhas, não a agenda
     * inteira.
     */
    private long dentistaCorrente() {
        Long idUsuario = ContextoAtual.obter().usuarioId();
        return idUsuario == null ? -1L : identidade.dentistaDoUsuario(idUsuario).orElse(-1L);
    }

    /**
     * Toda transição passa por aqui antes de mudar status. {@code exigir()}
     * devolve PROPRIOS para o dentista e PROPRIOS satisfaz {@code permite()} —
     * descartar o retorno deixava um dentista cancelar ou marcar falta na
     * consulta de um colega da mesma clínica, e o RLS não pega: os dois estão
     * no mesmo tenant. Mesmo defeito que o orçamento teve; ver
     * {@code OrcamentoServico.carregar}.
     */
    private void exigirAlcance(long idConsulta, Alcance alcance) {
        if (alcance == Alcance.PROPRIOS
                && consultas.buscar(idConsulta)
                        .filter(c -> c.idDentista() == dentistaCorrente()).isEmpty()) {
            // 404 e não 403: confirmar que a consulta existe já diz algo sobre o
            // paciente de outra pessoa.
            throw new RecursoNaoEncontradoException("consulta", idConsulta);
        }
    }

    /**
     * Duas consultas, não um JOIN.
     *
     * <p>{@code agenda.consultas} e {@code identidade.pessoas} são de módulos
     * diferentes. O JOIN funcionaria hoje, seria mais rápido, e é exatamente o
     * que impediria extrair a agenda depois — a fronteira some em silêncio,
     * porque na revisão parece só uma consulta eficiente.
     */
    private List<ConsultaResumo> montar(List<LinhaConsulta> linhas) {
        if (linhas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = linhas.stream().map(LinhaConsulta::idPaciente).distinct().toList();
        Map<Long, PacienteResumo> resumos = pacientes.mapaDeResumos(ids);
        Map<Long, int[]> faltas = consultas.historicoDeFaltas(ids);

        return linhas.stream()
                .map(l -> {
                    PacienteResumo p = resumos.get(l.idPaciente());
                    int[] f = faltas.getOrDefault(l.idPaciente(), new int[] {0, 0});
                    return new ConsultaResumo(
                            l.idConsulta(), l.idPaciente(),
                            p == null ? null : p.nomeCompleto(),
                            p == null ? null : p.telefoneCelular(),
                            l.idDentista(), l.inicioEm(), l.terminoEm(), l.status(),
                            f[0], f[1]);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, SituacaoNaAgenda> situacaoDosPacientes(Collection<Long> idsPaciente) {
        Alcance alcance = permissao.exigir(Recurso.AGENDA, Acao.LER);
        if (idsPaciente.isEmpty()) {
            return Map.of();
        }
        return consultas.situacao(idsPaciente,
                alcance == Alcance.PROPRIOS ? dentistaCorrente() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaResumo> historicoDoPaciente(long idPaciente) {
        Alcance alcance = permissao.exigir(Recurso.AGENDA, Acao.LER);
        return montar(consultas.doPaciente(idPaciente,
                alcance == Alcance.PROPRIOS ? dentistaCorrente() : null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsultaResumo> buscar(long idConsulta) {
        exigirAlcance(idConsulta, permissao.exigir(Recurso.AGENDA, Acao.LER));
        return consultas.buscar(idConsulta).map(l -> montar(List.of(l)).getFirst());
    }

    @Override
    @Transactional
    public void confirmar(long idConsulta) {
        transicionar(idConsulta, "agendada", "confirmada", null,
                TiposDeEvento.CONSULTA_CONFIRMADA);
    }

    @Override
    @Transactional
    public void cancelar(long idConsulta, String motivo) {
        exigirAlcance(idConsulta, permissao.exigir(Recurso.AGENDA, Acao.ALTERAR));
        int linhas = consultas.transicionarDeQualquerUm(
                idConsulta, List.of("agendada", "confirmada"), "cancelada", motivo);
        if (linhas == 0) {
            throw new TransicaoInvalidaException(
                    "Só é possível cancelar consulta agendada ou confirmada.");
        }
        outbox.gravar(TiposDeEvento.CONSULTA_CANCELADA,
                Map.of("consulta_id", idConsulta, "motivo", motivo == null ? "" : motivo));
    }

    @Override
    @Transactional
    public void registrarFalta(long idConsulta) {
        exigirAlcance(idConsulta, permissao.exigir(Recurso.AGENDA, Acao.ALTERAR));
        int linhas = consultas.transicionarDeQualquerUm(
                idConsulta, List.of("agendada", "confirmada"), "faltou", null);
        if (linhas == 0) {
            throw new TransicaoInvalidaException(
                    "Só é possível registrar falta em consulta agendada ou confirmada.");
        }
        // Alimenta o score de falta do svc-python (Fase 5).
        outbox.gravar(TiposDeEvento.CONSULTA_FALTOU, Map.of("consulta_id", idConsulta));
    }

    @Override
    @Transactional
    public void concluir(long idConsulta) {
        exigirAlcance(idConsulta, permissao.exigir(Recurso.AGENDA, Acao.ALTERAR));
        int linhas = consultas.transicionarDeQualquerUm(
                idConsulta, List.of("confirmada", "em_atendimento", "agendada"),
                "realizada", null);
        if (linhas == 0) {
            throw new TransicaoInvalidaException("Consulta não está em estado que permita concluir.");
        }
        outbox.gravar(TiposDeEvento.CONSULTA_REALIZADA, Map.of("consulta_id", idConsulta));
    }

    private void transicionar(long idConsulta, String de, String para, String motivo,
                              String evento) {
        exigirAlcance(idConsulta, permissao.exigir(Recurso.AGENDA, Acao.ALTERAR));
        int linhas = consultas.transicionar(idConsulta, de, para, motivo);
        if (linhas == 0) {
            // Zero linhas é ambíguo entre "não existe" e "está noutro estado".
            // O RLS já garante que não é de outro tenant; responder 409 evita
            // confirmar existência de id que o usuário não deveria conhecer.
            throw new TransicaoInvalidaException(
                    "Transição de '%s' para '%s' não é válida no estado atual.".formatted(de, para));
        }
        outbox.gravar(evento, Map.of("consulta_id", idConsulta));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean pacienteAtendidoPor(long idPaciente, long idDentista) {
        return consultas.existeConsultaEntre(idPaciente, idDentista);
    }
}
