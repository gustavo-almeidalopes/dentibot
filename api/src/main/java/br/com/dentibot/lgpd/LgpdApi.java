package br.com.dentibot.lgpd;

import java.util.List;

/**
 * Porta pública do módulo LGPD.
 *
 * <p>Três coisas moram aqui, e as três existem porque a lei pede prova, não
 * intenção:
 *
 * <ul>
 *   <li><b>Termos</b> — o texto que o paciente aceitou, versionado. Corrigir um
 *       termo publicado é impossível por desenho: o texto é imutável no banco e
 *       uma revisão nasce como versão nova. Um consentimento aponta para a
 *       versão exata que a pessoa leu, e sem isso "ela consentiu" não tem como
 *       ser demonstrado.</li>
 *   <li><b>Consentimentos</b> — quem aceitou o quê, quando, de qual IP. A
 *       revogação não apaga: marca. Apagar destruiria a prova de que houve
 *       consentimento no período em que o dado foi tratado.</li>
 *   <li><b>Solicitações do titular</b> — os direitos do art. 18, com prazo.</li>
 * </ul>
 */
public interface LgpdApi {

    List<Termo> listarTermos(boolean somenteAtivos);

    /** Publica uma versão. Nunca edita a anterior — ver {@link Termo}. */
    long publicarTermo(NovoTermo novo);

    /** Tira de circulação sem apagar: o texto continua provando o que provava. */
    void desativarTermo(long idTermo);

    List<Consentimento> consentimentosDoPaciente(long idPaciente);

    /**
     * @param ipOrigem  e {@code userAgent} vêm da requisição HTTP, nunca do
     *                  corpo: um IP enviado pelo cliente é um IP que o cliente
     *                  escolheu, e o que a V16 exige NOT NULL aqui é prova.
     */
    long registrarConsentimento(NovoConsentimento novo, String ipOrigem, String userAgent);

    void revogarConsentimento(long idConsentimento);

    List<SolicitacaoTitular> listarSolicitacoes(String status);

    long abrirSolicitacao(NovaSolicitacao nova);

    void responderSolicitacao(long idSolicitacao, RespostaSolicitacao resposta);
}
