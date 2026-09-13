package br.com.dentibot.arquitetura;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O {@code Alcance} devolvido por {@code exigir()} não pode ser descartado numa
 * escrita.
 *
 * <p>Existe por causa de um bug real, encontrado em revisão de segurança. Os
 * seis mutadores de orçamento chamavam
 * {@code permissoes.exigir(ORCAMENTO, ALTERAR)} e jogavam o retorno fora.
 * {@code PROPRIOS} satisfaz {@code permite()}, então a chamada passava — e nada
 * mais no caminho olhava para o {@code id_dentista}. Na prática um dentista
 * aprovava o orçamento de um colega da mesma clínica só pelo id, e o evento
 * {@code orcamento.aprovado} saía com o {@code idDentista} dele, abrindo
 * recebível e comissão para um plano que ele nunca aprovou.
 *
 * <p>O RLS não pega isto: ele fecha o eixo do tenant, e os dois dentistas estão
 * no mesmo. Nenhum teste de isolamento ficaria vermelho.
 *
 * <p>É exatamente o tipo de defeito que some numa revisão — a linha
 * {@code permissoes.exigir(...)} está lá, parece uma checagem completa, e a
 * diferença entre checar o verbo e checar o objeto não aparece na leitura. Por
 * isso a garantia é de máquina, no mesmo espírito do {@link FronteiraDeSchemaTest}.
 */
@DisplayName("Escopo PROPRIOS nas escritas de orçamento")
class EscopoDeOrcamentoTest {

    private static final Path SERVICO = Path.of(
            "src/main/java/br/com/dentibot/orcamento/application/OrcamentoServico.java");

    /** Quem confere a linha contra o alcance antes de deixar escrever. */
    private static final List<String> PORTAS = List.of("carregar(", "autorPermitido(");

    @Test
    @DisplayName("todo método que exige ALTERAR ou CRIAR passa por carregar() ou autorPermitido()")
    void escritaNaoDescartaOAlcance() throws IOException {
        Map<String, String> metodos = metodosDe(Files.readString(SERVICO, StandardCharsets.UTF_8));

        List<String> descuidados = new ArrayList<>();
        int escritas = 0;

        for (Map.Entry<String, String> metodo : metodos.entrySet()) {
            String corpo = metodo.getValue();
            // Só quem CHAMA exigir() é ponto de entrada autorizado. Bastar citar
            // Acao.CRIAR pegaria o próprio autorPermitido(), que menciona a ação
            // apenas para montar a exceção de negação — e um teste que acusa o
            // guarda de não ter guarda é um teste que alguém desliga.
            boolean escreve = corpo.contains("permissoes.exigir(")
                    && (corpo.contains("Acao.ALTERAR") || corpo.contains("Acao.CRIAR"));
            if (!escreve) {
                continue;
            }
            escritas++;
            if (PORTAS.stream().noneMatch(corpo::contains)) {
                descuidados.add(metodo.getKey());
            }
        }

        assertThat(descuidados)
                .as("""
                    Escrita de orçamento que exige permissão mas não confere o ALCANCE. \
                    exigir() devolve TODOS ou PROPRIOS; descartar esse retorno deixa um \
                    dentista mutar o orçamento de um colega da mesma clínica — o RLS não \
                    fecha isso, porque os dois estão no mesmo tenant. Passe a linha por \
                    carregar(id, alcance) antes de mutar.""")
                .isEmpty();

        // Sem esta asserção, um erro no parser faria o teste acima passar para
        // sempre olhando o conjunto vazio — a armadilha que o
        // FronteiraDeSchemaTest também precisou fechar.
        assertThat(escritas)
                .as("o parser precisa estar encontrando os métodos de escrita de verdade")
                .isGreaterThanOrEqualTo(6);
    }

    @Test
    @DisplayName("carregar() confere o alcance, e não só carrega a linha")
    void aPortaRealmenteConfere() throws IOException {
        String corpo = metodosDe(Files.readString(SERVICO, StandardCharsets.UTF_8))
                .get("carregar");

        assertThat(corpo)
                .as("carregar() é a porta única das escritas; sem exigirAlcance ela "
                        + "vira só um buscar() com nome bonito e o teste acima fica vácuo")
                .isNotNull()
                .contains("exigirAlcance(");
    }

    /**
     * Fatia o arquivo em métodos por contagem de chaves.
     *
     * <p>Regex sobre corpo de método não serve aqui: o que se quer saber é o que
     * está DENTRO de cada método, e uma expressão que atravesse a fronteira
     * entre dois deles daria o teste por satisfeito com a checagem do vizinho.
     */
    private static Map<String, String> metodosDe(String fonte) {
        Map<String, String> metodos = new LinkedHashMap<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?m)^    (?:public|private)[\\w\\s<>,\\[\\].]*?\\s(\\w+)\\([^)]*\\)\\s*\\{")
                .matcher(fonte);

        while (m.find()) {
            int profundidade = 1;
            int i = m.end();
            while (i < fonte.length() && profundidade > 0) {
                char c = fonte.charAt(i);
                if (c == '{') {
                    profundidade++;
                } else if (c == '}') {
                    profundidade--;
                }
                i++;
            }
            // Sobrecarga: o nome se repete. Concatenar em vez de sobrescrever
            // mantém as duas no teste — descartar uma esconderia justamente a
            // que não confere.
            metodos.merge(m.group(1), fonte.substring(m.end(), i), (a, b) -> a + "\n" + b);
        }
        return metodos;
    }
}
