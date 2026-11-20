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
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.erro.TransicaoInvalidaException;
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
     * Fora dela, um rollback após o publish confirma ao paciente um horário que
     * não existe, e um publish que falha após o commit deixa a consulta sem aviso.
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
        return identidade.dentistaCorrente().orElse(-1L);
    }

    /**
     * {@code exigir()} devolve PROPRIOS para o dentista, e PROPRIOS satisfaz
     * {@code permite()}: sem conferir o dono, um dentista cancelaria a consulta
     * de um colega da mesma clínica — e o RLS não pega, é o mesmo tenant.
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
     * Duas consultas, não um JOIN: {@code agenda.consultas} e
     * {@code identidade.pessoas} são de módulos diferentes, e o JOIN é o que
     * impediria extrair a agenda depois.
     */
    private List<ConsultaResumo> montar(List<LinhaConsulta> linhas) {
        if (linhas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = linhas.stream().map(LinhaConsulta::idPaciente).distinct().toList();
        Map<Long, PacienteResumo> resumos = pacientes.mapaDeResumos(ids);
        Map<Long, int[]> faltas = consultas.historicoDeFaltas(ids);

        // `acoes` diz o que ESTE usuário pode fazer com cada consulta, pelo mesmo
        // critério que transicionar() aplica: status e alcance de AGENDA/ALTERAR.
        Alcance alterar = permissao.alcance(Recurso.AGENDA, Acao.ALTERAR);
        long dentista = alterar == Alcance.PROPRIOS ? dentistaCorrente() : -1L;

        return linhas.stream()
                .map(l -> {
                    PacienteResumo p = resumos.get(l.idPaciente());
                    int[] f = faltas.getOrDefault(l.idPaciente(), new int[] {0, 0});
                    boolean podeAlterar = alterar == Alcance.TODOS
                            || (alterar == Alcance.PROPRIOS && l.idDentista() == dentista);
                    return new ConsultaResumo(
                            l.idConsulta(), l.idPaciente(),
                            p == null ? null : p.nomeCompleto(),
                            p == null ? null : p.telefoneCelular(),
                            l.idDentista(), l.inicioEm(), l.terminoEm(), l.status(),
                            podeAlterar ? TransicaoConsulta.disponiveis(l.status()) : List.of(),
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
    public void transicionar(long idConsulta, String acao, String motivo) {
        TransicaoConsulta t = TransicaoConsulta.doCaminho(acao);
        if (t.exigeMotivo() && (motivo == null || motivo.isBlank())) {
            throw new IllegalArgumentException("Informe o motivo.");
        }
        exigirAlcance(idConsulta, permissao.exigir(Recurso.AGENDA, Acao.ALTERAR));

        String guardado = t.exigeMotivo() ? motivo.trim() : null;
        // Zero linhas é ambíguo entre "não existe" e "está noutro estado". O RLS
        // já garante que não é de outro tenant; responder 409 evita confirmar a
        // existência de um id que o usuário não deveria conhecer.
        if (consultas.transicionar(idConsulta, t.origens, t.destino, guardado) == 0) {
            throw new TransicaoInvalidaException(t.recusa);
        }
        outbox.gravar(t.evento, guardado == null
                ? Map.of("consulta_id", idConsulta)
                : Map.of("consulta_id", idConsulta, "motivo", guardado));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean pacienteAtendidoPor(long idPaciente, long idDentista) {
        return consultas.existeConsultaEntre(idPaciente, idDentista);
    }
}
