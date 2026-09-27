package br.com.dentibot.clinicas.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.clinicas.ClinicasApi;
import br.com.dentibot.clinicas.NovoProcedimento;
import br.com.dentibot.clinicas.ProcedimentoResumo;
import br.com.dentibot.clinicas.domain.Clinica;
import br.com.dentibot.clinicas.infrastructure.ClinicaRepositorio;
import br.com.dentibot.clinicas.infrastructure.ProcedimentoRepositorio;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuração da clínica: seus dados e sua tabela de procedimentos.
 *
 * <p>Separado do {@link OnboardingServico} de propósito: aquele é o nascimento
 * do tenant, um fluxo que roda sem tenant e acontece uma vez; este é a operação
 * do dia a dia, que sempre roda dentro de um.
 */
@Service
public class ClinicaServico implements ClinicasApi {

    private final ProcedimentoRepositorio procedimentos;
    private final ClinicaRepositorio clinicas;
    private final AvaliadorDePermissao permissoes;
    private final AuditoriaApi auditoria;

    public ClinicaServico(ProcedimentoRepositorio procedimentos, ClinicaRepositorio clinicas,
                          AvaliadorDePermissao permissoes, AuditoriaApi auditoria) {
        this.procedimentos = procedimentos;
        this.clinicas = clinicas;
        this.permissoes = permissoes;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedimentoResumo> listarProcedimentos(boolean somenteAtivos) {
        permissoes.exigir(Recurso.CONFIGURACAO, Acao.LER);
        return procedimentos.listar(somenteAtivos);
    }

    /**
     * Sem checagem de permissão: é chamada por outro módulo (orçamento) que já
     * checou ORCAMENTO na própria entrada. Exigir CONFIGURACAO-LER aqui
     * impediria o dentista de montar um orçamento — ele tem a leitura de
     * configuração, mas a regra é da porta de entrada, não desta consulta.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ProcedimentoResumo> buscarProcedimento(long idProcedimento) {
        return procedimentos.buscar(idProcedimento);
    }

    @Override
    @Transactional
    public long criarProcedimento(NovoProcedimento novo) {
        permissoes.exigir(Recurso.CONFIGURACAO, Acao.CRIAR);
        long id = procedimentos.inserir(novo);
        auditoria.registrarCriacao("clinica.procedimento", String.valueOf(id), novo);
        return id;
    }

    @Override
    @Transactional
    public void atualizarProcedimento(long idProcedimento, NovoProcedimento dados, boolean ativo) {
        permissoes.exigir(Recurso.CONFIGURACAO, Acao.ALTERAR);
        ProcedimentoResumo antes = procedimentos.buscar(idProcedimento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("procedimento", idProcedimento));
        procedimentos.atualizar(idProcedimento, dados, ativo);
        auditoria.registrarAlteracao("clinica.procedimento", String.valueOf(idProcedimento),
                antes, dados);
    }

    @Transactional(readOnly = true)
    public Clinica clinicaAtual() {
        permissoes.exigir(Recurso.CONFIGURACAO, Acao.LER);
        return clinicas.buscarAtual()
                .orElseThrow(() -> new RecursoNaoEncontradoException("clinica", "atual"));
    }
}
