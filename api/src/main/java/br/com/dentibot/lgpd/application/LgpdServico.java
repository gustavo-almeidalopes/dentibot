package br.com.dentibot.lgpd.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.lgpd.Consentimento;
import br.com.dentibot.lgpd.LgpdApi;
import br.com.dentibot.lgpd.NovaSolicitacao;
import br.com.dentibot.lgpd.NovoConsentimento;
import br.com.dentibot.lgpd.NovoTermo;
import br.com.dentibot.lgpd.RespostaSolicitacao;
import br.com.dentibot.lgpd.SolicitacaoTitular;
import br.com.dentibot.lgpd.Termo;
import br.com.dentibot.lgpd.infrastructure.LgpdRepositorio;
import br.com.dentibot.lgpd.infrastructure.LgpdRepositorio.LinhaConsentimento;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class LgpdServico implements LgpdApi {

    /**
     * Prazo do art. 19, §1º e §2º: quinze dias para a resposta completa.
     *
     * <p>Calculado na abertura e gravado na linha, em vez de derivado na
     * leitura. Se o prazo fosse calculado toda vez, mudar esta constante
     * reescreveria retroativamente o prazo de solicitações antigas — e o prazo
     * de uma solicitação é justamente o que se precisa provar depois.
     */
    private static final int DIAS_DE_PRAZO = 15;

    /** Espelha o CHECK de {@code lgpd.termos.tipo}. */
    private static final Set<String> TIPOS_DE_TERMO = Set.of(
            "politica_privacidade", "termo_consentimento", "termo_uso_imagem", "termo_tratamento");

    /** Espelha o CHECK de {@code lgpd.solicitacoes_titular.direito} (art. 18). */
    private static final Set<String> DIREITOS = Set.of(
            "confirmacao", "acesso", "correcao", "anonimizacao",
            "portabilidade", "eliminacao", "revogacao_consentimento");

    private static final Set<String> STATUS_DE_RESPOSTA = Set.of(
            "em_analise", "atendida", "recusada", "parcialmente_atendida");

    /** Os que a V16 exige justificar. */
    private static final Set<String> EXIGEM_JUSTIFICATIVA = Set.of("recusada", "parcialmente_atendida");

    private final LgpdRepositorio lgpd;
    private final AvaliadorDePermissao permissoes;
    private final AuditoriaApi auditoria;
    private final ObjectMapper json;

    public LgpdServico(LgpdRepositorio lgpd, AvaliadorDePermissao permissoes,
                       AuditoriaApi auditoria, ObjectMapper json) {
        this.lgpd = lgpd;
        this.permissoes = permissoes;
        this.auditoria = auditoria;
        this.json = json;
    }

    // ─── Termos ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Termo> listarTermos(boolean somenteAtivos) {
        permissoes.exigir(Recurso.LGPD, Acao.LER);
        return lgpd.listarTermos(somenteAtivos);
    }

    /**
     * Publicar uma versão desativa as anteriores do mesmo tipo, na mesma
     * transação. Duas versões ativas do mesmo termo é um estado sem resposta
     * para "qual delas o paciente devia assinar hoje?".
     */
    @Override
    @Transactional
    public long publicarTermo(NovoTermo novo) {
        permissoes.exigir(Recurso.LGPD, Acao.CRIAR);
        if (!TIPOS_DE_TERMO.contains(novo.tipo())) {
            throw new IllegalArgumentException("Tipo de termo inválido: " + novo.tipo());
        }

        long id = lgpd.inserirTermo(novo.tipo(), novo.versao(),
                novo.textoIntegral(), novo.ativoDesde());
        lgpd.desativarOutrasVersoes(novo.tipo(), id);

        // Sem o texto: a trilha registra QUE a versão foi publicada, e o texto
        // já está imutável na própria tabela. Duplicá-lo no JSONB da auditoria
        // só dobraria o volume da partição.
        auditoria.registrarCriacao("lgpd.termo", String.valueOf(id),
                Map.of("tipo", novo.tipo(), "versao", novo.versao()));
        return id;
    }

    @Override
    @Transactional
    public void desativarTermo(long idTermo) {
        permissoes.exigir(Recurso.LGPD, Acao.ALTERAR);
        if (lgpd.desativarTermo(idTermo) == 0) {
            throw new RecursoNaoEncontradoException("termo", idTermo);
        }
        auditoria.registrarAlteracao("lgpd.termo", String.valueOf(idTermo),
                Map.of("ativo", true), Map.of("ativo", false));
    }

    // ─── Consentimentos ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<Consentimento> consentimentosDoPaciente(long idPaciente) {
        permissoes.exigir(Recurso.LGPD, Acao.LER);

        // Consultar o consentimento de alguém é acesso a dado do titular, e a
        // trilha é do que a própria LGPD cobra. Por isso @Transactional sem
        // readOnly: esta leitura escreve.
        auditoria.registrarLeitura("lgpd.consentimento", String.valueOf(idPaciente));

        return lgpd.consentimentosDoPaciente(idPaciente).stream()
                .map(this::montar)
                .toList();
    }

    @Override
    @Transactional
    public long registrarConsentimento(NovoConsentimento novo, String ipOrigem,
                                       String userAgent) {
        permissoes.exigir(Recurso.LGPD, Acao.CRIAR);

        Termo termo = lgpd.buscarTermo(novo.idTermo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("termo", novo.idTermo()));
        if (!termo.ativo()) {
            // Colher aceite numa versão já retirada de circulação produz um
            // consentimento que não corresponde ao texto vigente — e o defeito
            // só aparece numa fiscalização, anos depois.
            throw new IllegalArgumentException(
                    "Este termo não está mais ativo. Colha o aceite na versão vigente.");
        }

        long id = lgpd.inserirConsentimento(
                novo.idPaciente(), novo.idTermo(),
                json.writeValueAsString(novo.finalidadesAceitas()),
                ipOrigem,
                // A coluna é VARCHAR(500) e navegador manda string longa.
                userAgent == null ? "" : userAgent.substring(0, Math.min(userAgent.length(), 500)));

        auditoria.registrarCriacao("lgpd.consentimento", String.valueOf(id),
                Map.of("idPaciente", novo.idPaciente(),
                        "idTermo", novo.idTermo(),
                        "finalidades", novo.finalidadesAceitas()));
        return id;
    }

    @Override
    @Transactional
    public void revogarConsentimento(long idConsentimento) {
        permissoes.exigir(Recurso.LGPD, Acao.ALTERAR);
        if (lgpd.revogarConsentimento(idConsentimento) == 0) {
            throw new RecursoNaoEncontradoException("consentimento", idConsentimento);
        }
        auditoria.registrarAlteracao("lgpd.consentimento", String.valueOf(idConsentimento),
                Map.of("vigente", true), Map.of("vigente", false));
    }

    // ─── Solicitações do titular ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SolicitacaoTitular> listarSolicitacoes(String status) {
        permissoes.exigir(Recurso.LGPD, Acao.LER);
        return lgpd.listarSolicitacoes(status);
    }

    @Override
    @Transactional
    public long abrirSolicitacao(NovaSolicitacao nova) {
        permissoes.exigir(Recurso.LGPD, Acao.CRIAR);
        if (!DIREITOS.contains(nova.direito())) {
            throw new IllegalArgumentException("Direito não previsto no art. 18: " + nova.direito());
        }

        long id = lgpd.inserirSolicitacao(nova.idPaciente(), nova.direito(),
                LocalDate.now().plusDays(DIAS_DE_PRAZO));

        auditoria.registrarCriacao("lgpd.solicitacao", String.valueOf(id),
                Map.of("idPaciente", nova.idPaciente(), "direito", nova.direito()));
        return id;
    }

    @Override
    @Transactional
    public void responderSolicitacao(long idSolicitacao, RespostaSolicitacao resposta) {
        permissoes.exigir(Recurso.LGPD, Acao.ALTERAR);

        if (!STATUS_DE_RESPOSTA.contains(resposta.status())) {
            throw new IllegalArgumentException("Status de resposta inválido: " + resposta.status());
        }
        boolean semJustificativa = resposta.justificativa() == null
                || resposta.justificativa().isBlank();
        if (EXIGEM_JUSTIFICATIVA.contains(resposta.status()) && semJustificativa) {
            // O CHECK da V16 recusaria a linha; a mensagem daqui explica a regra
            // em vez de devolver um erro de constraint.
            throw new IllegalArgumentException(
                    "Recusa e atendimento parcial exigem justificativa (art. 18, §4º).");
        }

        if (lgpd.responder(idSolicitacao, resposta.status(),
                resposta.justificativa(), resposta.chaveObjetoResposta()) == 0) {
            throw new IllegalArgumentException("Solicitação inexistente ou já respondida.");
        }
        auditoria.registrarAlteracao("lgpd.solicitacao", String.valueOf(idSolicitacao),
                Map.of("status", "aberta"), Map.of("status", resposta.status()));
    }

    private Consentimento montar(LinhaConsentimento l) {
        List<String> finalidades;
        try {
            finalidades = List.of(json.readValue(l.finalidadesJson(), String[].class));
        } catch (RuntimeException e) {
            // JSONB ilegível é dado corrompido, não motivo para derrubar a tela
            // que investiga justamente isso.
            finalidades = List.of();
        }
        return new Consentimento(
                l.idConsentimento(), l.idPaciente(), l.idTermo(),
                l.tipoDoTermo(), l.versaoDoTermo(), finalidades,
                l.aceitoEm(), l.ipOrigem(), l.revogadoEm());
    }
}
