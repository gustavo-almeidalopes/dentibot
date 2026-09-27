package br.com.dentibot.prontuario.application;

import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.identidade.IdentidadeApi;
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
import br.com.dentibot.prontuario.EvolucaoResumo;
import br.com.dentibot.prontuario.LancamentoOdontograma;
import br.com.dentibot.prontuario.NovaEvolucao;
import br.com.dentibot.prontuario.NovoLancamentoOdontograma;
import br.com.dentibot.prontuario.ProntuarioApi;
import br.com.dentibot.prontuario.infrastructure.ProntuarioRepositorio;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProntuarioServico implements ProntuarioApi {

    private final ProntuarioRepositorio prontuario;
    private final AuditoriaApi auditoria;
    private final IdentidadeApi identidade;
    private final AgendaApi agenda;
    private final PacientesApi pacientes;
    private final AvaliadorDePermissao permissao;
    private final Outbox outbox;

    public ProntuarioServico(ProntuarioRepositorio prontuario, AuditoriaApi auditoria,
                             IdentidadeApi identidade, AgendaApi agenda,
                             PacientesApi pacientes, AvaliadorDePermissao permissao,
                             Outbox outbox) {
        this.prontuario = prontuario;
        this.auditoria = auditoria;
        this.identidade = identidade;
        this.agenda = agenda;
        this.pacientes = pacientes;
        this.permissao = permissao;
        this.outbox = outbox;
    }

    /**
     * Leitura do histórico clínico — e o registro dessa leitura.
     *
     * <p>{@code @Transactional} sem {@code readOnly}, de propósito: a leitura
     * ESCREVE uma linha de auditoria. É o preço de atender à CFO-226 e à LGPD, e
     * é o que permite responder "quem abriu o prontuário deste paciente e
     * quando" com um SELECT em vez de uma suposição.
     *
     * <p>A linha de auditoria vai na MESMA transação da leitura (invariante 9).
     * Em transação separada ela sobreviveria a um rollback — registrando acesso
     * que não aconteceu — ou se perderia quando o commit dela falhasse.
     */
    @Override
    @Transactional
    public List<EvolucaoResumo> historico(long idPaciente) {
        exigirAcessoAoPaciente(idPaciente, Acao.LER);

        List<EvolucaoResumo> evolucoes = prontuario.historico(idPaciente);

        auditoria.registrarLeitura("prontuario.evolucao", String.valueOf(idPaciente));
        outbox.gravar(TiposDeEvento.PRONTUARIO_LIDO, Map.of(
                "paciente_id", idPaciente,
                "registros_lidos", evolucoes.size()));

        return evolucoes;
    }

    @Override
    @Transactional
    public long registrarEvolucao(NovaEvolucao nova) {
        exigirAcessoAoPaciente(nova.idPaciente(), Acao.CRIAR);
        long idDentista = dentistaAutenticado();

        long id = prontuario.inserirEvolucao(
                nova.idPaciente(), idDentista, nova.idConsulta(),
                nova.descricao(), null, null);

        auditoria.registrarCriacao("prontuario.evolucao", String.valueOf(id),
                Map.of("paciente_id", nova.idPaciente(), "consulta_id",
                        nova.idConsulta() == null ? "" : nova.idConsulta()));
        outbox.gravar(TiposDeEvento.PRONTUARIO_EVOLUCAO_REGISTRADA, Map.of(
                "evolucao_id", id, "paciente_id", nova.idPaciente()));
        return id;
    }

    /**
     * Correção de prontuário é ADENDO, nunca UPDATE.
     *
     * <p>O banco recusaria o UPDATE de qualquer forma (trigger
     * {@code trg_evolucao_imutavel} e REVOKE). O método existe para que a
     * correção tenha um caminho legítimo — sem ele, alguém acabaria pedindo para
     * afrouxar a imutabilidade "só nesse caso".
     */
    @Override
    @Transactional
    public long retificarEvolucao(long idEvolucaoOriginal, String descricao, String motivo) {
        Long dentistaOriginal = prontuario.dentistaDaEvolucao(idEvolucaoOriginal)
                .orElseThrow(() -> new AcessoNegadoException(Recurso.PRONTUARIO, Acao.ALTERAR));

        long idDentista = dentistaAutenticado();
        Alcance alcance = permissao.exigir(Recurso.PRONTUARIO, Acao.CRIAR);
        if (alcance == Alcance.PROPRIOS && dentistaOriginal != idDentista) {
            // Retificar registro assinado por outro profissional seria alterar
            // ato clínico alheio.
            throw new AcessoNegadoException(Recurso.PRONTUARIO, Acao.ALTERAR);
        }

        long id = prontuario.inserirEvolucao(
                pacienteDaEvolucao(idEvolucaoOriginal), idDentista, null,
                descricao, idEvolucaoOriginal, motivo);

        auditoria.registrarAlteracao("prontuario.evolucao",
                String.valueOf(idEvolucaoOriginal),
                Map.of("evolucao_original", idEvolucaoOriginal),
                Map.of("evolucao_adendo", id, "motivo", motivo));
        return id;
    }

    @Override
    @Transactional
    public List<LancamentoOdontograma> odontograma(long idPaciente) {
        exigirAcessoAoPaciente(idPaciente, Acao.LER);
        List<LancamentoOdontograma> estado = prontuario.estadoAtual(idPaciente);
        auditoria.registrarLeitura("prontuario.odontograma", String.valueOf(idPaciente));
        return estado;
    }

    @Override
    @Transactional
    public long lancarOdontograma(NovoLancamentoOdontograma lancamento) {
        exigirAcessoAoPaciente(lancamento.idPaciente(), Acao.CRIAR);
        long idDentista = dentistaAutenticado();

        long id = prontuario.inserirLancamento(
                lancamento.idPaciente(), idDentista, lancamento.dente(),
                lancamento.face(), lancamento.condicao(), lancamento.observacao());

        auditoria.registrarCriacao("prontuario.odontograma", String.valueOf(id),
                Map.of("paciente_id", lancamento.idPaciente(),
                        "dente", lancamento.dente(),
                        "condicao", lancamento.condicao()));
        outbox.gravar(TiposDeEvento.ODONTOGRAMA_ATUALIZADO, Map.of(
                "paciente_id", lancamento.idPaciente(), "dente", lancamento.dente()));
        return id;
    }

    // ─── autorização ─────────────────────────────────────────────────────────

    /**
     * Traduz {@code Alcance.PROPRIOS} para prontuário: "os SEUS pacientes" é
     * quem já teve consulta com este dentista. A pergunta vai para a porta da
     * agenda, que é a dona daquele dado.
     */
    private void exigirAcessoAoPaciente(long idPaciente, Acao acao) {
        Alcance alcance = permissao.exigir(Recurso.PRONTUARIO, acao);

        // Antes de ler qualquer coisa: o paciente existe NESTE tenant? Sem esta
        // checagem, pedir o prontuário de um paciente de outra clínica devolvia
        // 200 com lista vazia — o RLS não vazava nada, mas a API confirmava um
        // caminho que não é do chamador, e a auditoria registrava leitura de um
        // paciente que a clínica não tem.
        if (!pacientes.existe(idPaciente)) {
            throw new RecursoNaoEncontradoException("Paciente", idPaciente);
        }
        if (alcance != Alcance.PROPRIOS) {
            return;
        }
        long idDentista = dentistaAutenticado();
        if (!agenda.pacienteAtendidoPor(idPaciente, idDentista)) {
            throw new AcessoNegadoException(Recurso.PRONTUARIO, acao);
        }
    }

    private long dentistaAutenticado() {
        Long idUsuario = ContextoAtual.obter().usuarioId();
        if (idUsuario == null) {
            throw new AcessoNegadoException(Recurso.PRONTUARIO, Acao.CRIAR);
        }
        return identidade.dentistaDoUsuario(idUsuario)
                // Ato clínico exige profissional inscrito no CRO. Um admin que
                // não seja dentista administra a clínica, não assina evolução.
                .orElseThrow(() -> new AcessoNegadoException(Recurso.PRONTUARIO, Acao.CRIAR));
    }

    private long pacienteDaEvolucao(long idEvolucao) {
        return prontuario.pacienteDaEvolucao(idEvolucao)
                .orElseThrow(() -> new AcessoNegadoException(Recurso.PRONTUARIO, Acao.ALTERAR));
    }
}
