package br.com.dentibot.copiloto.application;

import br.com.dentibot.copiloto.PlanoExplicado;
import br.com.dentibot.orcamento.ItemResumo;
import br.com.dentibot.orcamento.OrcamentoDetalhado;
import br.com.dentibot.plataforma.erro.FalhaExternaException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** IA-04: o que se pede ao modelo e como se lê a resposta. */
final class PlanoEmDuasLinguagens {

    static final String INSTRUCOES = """
            Você ajuda um cirurgião-dentista brasileiro a apresentar um plano de tratamento \
            que ele já montou. Escreva duas versões do mesmo plano.

            "tecnica": para o prontuário e o convênio — terminologia odontológica, dente em \
            notação FDI, etapa por etapa.
            "paciente": para o paciente ler em casa — português simples, sem jargão; para cada \
            etapa, o que é e por que é feita; alternativas quando existirem; o que tende a \
            acontecer se nada for feito.

            Regras:
            - Use somente os procedimentos da lista. Não acrescente nem tire etapa.
            - Não prometa resultado nem prazo de cura. Não cite valor diferente do informado.
            - Não faça diagnóstico novo: o plano é do dentista.""";

    static final Map<String, Object> ESQUEMA = Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("tecnica", "paciente"),
            "properties", Map.of(
                    "tecnica", Map.of("type", "string"),
                    "paciente", Map.of("type", "string")));

    private PlanoEmDuasLinguagens() {
    }

    static List<String> procedimentos(OrcamentoDetalhado orcamento) {
        return orcamento.itens().stream()
                .filter(i -> !"cancelado".equals(i.statusExecucao()))
                .map(PlanoEmDuasLinguagens::rotulo)
                .toList();
    }

    static String entrada(OrcamentoDetalhado orcamento) {
        NumberFormat reais = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        StringBuilder texto = new StringBuilder("Plano de tratamento proposto:\n");
        for (ItemResumo i : orcamento.itens()) {
            if (!"cancelado".equals(i.statusExecucao())) {
                texto.append("- ").append(rotulo(i)).append(": ")
                        .append(reais.format(i.valorCobrado())).append('\n');
            }
        }
        return texto.append("Total: ").append(reais.format(orcamento.cabecalho().valorFinal())).toString();
    }

    static PlanoExplicado interpretar(long idChamada, String texto, List<String> procedimentos,
                                      ObjectMapper json) {
        JsonNode raiz;
        try {
            raiz = json.readTree(texto);
        } catch (JacksonException e) {
            throw new FalhaExternaException("A IA não devolveu um plano utilizável.", e);
        }
        String tecnica = raiz.path("tecnica").asString("").strip();
        String paciente = raiz.path("paciente").asString("").strip();
        if (tecnica.isEmpty() || paciente.isEmpty()) {
            throw new FalhaExternaException("A IA devolveu o plano incompleto.", null);
        }
        return new PlanoExplicado(idChamada, tecnica, paciente, procedimentos);
    }

    private static String rotulo(ItemResumo i) {
        String nome = i.nomeProcedimento() == null ? "Procedimento" : i.nomeProcedimento();
        String dente = i.dente() == null ? "" : " — dente " + i.dente() + (i.face() == null ? "" : ", face " + i.face());
        return nome + dente;
    }
}
