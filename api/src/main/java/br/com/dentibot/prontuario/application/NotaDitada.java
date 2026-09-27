package br.com.dentibot.prontuario.application;

import br.com.dentibot.plataforma.erro.FalhaExternaException;
import br.com.dentibot.prontuario.LancamentoSugerido;
import br.com.dentibot.prontuario.NovoLancamentoOdontograma;
import br.com.dentibot.prontuario.RascunhoDeNota;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * IA-01: o que se pede ao modelo e como se lê o que ele devolve.
 *
 * <p>Duas barreiras, porque uma só falha: o schema da saída estruturada já
 * restringe a condição ao vocabulário do banco; a regra aqui confere de novo, e
 * confere o que o schema não expressa — dente na notação FDI. O que não passa
 * vai para "não confirmado", à vista do dentista; nunca some em silêncio.
 */
final class NotaDitada {

    static final String INSTRUCOES = """
            Você organiza o ditado de um cirurgião-dentista brasileiro em rascunho de \
            prontuário, para ele revisar antes de assinar.

            Regras:
            - Use somente o que está no ditado. Não invente procedimento, dente, face, \
            material nem achado.
            - Não faça diagnóstico nem sugira conduta: você organiza o que o profissional disse.
            - "evolucao": texto corrido, em português, na terceira pessoa, com o que foi \
            feito na sessão.
            - "lancamentos": um item por dente citado com condição clara. Dente em notação \
            FDI (11 a 48 permanentes, 51 a 85 decíduos). Face V, L, M, D, O, I ou P, ou null \
            se não foi dita.
            - "nao_ancorado": frases curtas com o que ficou ambíguo ou não coube nos campos.
            - [PACIENTE] é o nome do paciente, removido de propósito; mantenha assim.""";

    private static final Map<String, Object> TEXTO_OU_NULO =
            Map.of("anyOf", List.of(Map.of("type", "string"), Map.of("type", "null")));

    static final Map<String, Object> ESQUEMA = Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("evolucao", "lancamentos", "nao_ancorado"),
            "properties", Map.of(
                    "evolucao", Map.of("type", "string"),
                    "lancamentos", Map.of("type", "array", "items", Map.of(
                            "type", "object",
                            "additionalProperties", false,
                            "required", List.of("dente", "face", "condicao", "observacao"),
                            "properties", Map.of(
                                    "dente", Map.of("type", "integer"),
                                    "face", TEXTO_OU_NULO,
                                    "condicao", Map.of("type", "string",
                                            "enum", NovoLancamentoOdontograma.CONDICOES),
                                    "observacao", TEXTO_OU_NULO))),
                    "nao_ancorado", Map.of("type", "array", "items", Map.of("type", "string"))));

    private NotaDitada() {
    }

    static RascunhoDeNota interpretar(long idChamada, String texto, ObjectMapper json) {
        JsonNode raiz;
        try {
            raiz = json.readTree(texto);
        } catch (JacksonException e) {
            throw new FalhaExternaException(
                    "A IA não devolveu um rascunho utilizável. Escreva a evolução à mão.", e);
        }
        List<LancamentoSugerido> lancamentos = new ArrayList<>();
        List<String> naoConfirmado = new ArrayList<>();
        for (JsonNode l : raiz.path("lancamentos")) {
            int dente = l.path("dente").asInt(-1);
            String face = l.path("face").isString() ? l.path("face").asString() : null;
            String condicao = l.path("condicao").asString("");
            String observacao = l.path("observacao").isString() ? l.path("observacao").asString() : null;
            if (!denteFdi(dente) || (face != null && !face.matches("^[VLMDOIP]$"))
                    || !NovoLancamentoOdontograma.CONDICOES.contains(condicao)) {
                naoConfirmado.add("Sugestão descartada pela regra (dente " + dente + ", face "
                        + face + ", condição " + condicao + ").");
                continue;
            }
            lancamentos.add(new LancamentoSugerido(dente, face, condicao, observacao));
        }
        for (JsonNode n : raiz.path("nao_ancorado")) {
            naoConfirmado.add(n.asString());
        }
        String evolucao = raiz.path("evolucao").asString("").strip();
        if (evolucao.isEmpty() && lancamentos.isEmpty()) {
            throw new FalhaExternaException(
                    "A IA não achou nada para registrar neste ditado. Escreva a evolução à mão.", null);
        }
        return new RascunhoDeNota(idChamada, evolucao, List.copyOf(lancamentos), List.copyOf(naoConfirmado));
    }

    /** FDI: quadrantes 1–4 com dentes 1–8; decíduos 5–8 com dentes 1–5. */
    static boolean denteFdi(int dente) {
        int quadrante = dente / 10;
        int posicao = dente % 10;
        return (quadrante >= 1 && quadrante <= 4 && posicao >= 1 && posicao <= 8)
                || (quadrante >= 5 && quadrante <= 8 && posicao >= 1 && posicao <= 5);
    }
}
