package br.com.dentibot.plataforma.telemetria;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Última barreira antes de qualquer payload sair para Sentry, PostHog ou Better
 * Stack (invariante 11).
 *
 * <p>O raciocínio de produto que justifica isto: a pergunta que telemetria
 * responde é "a funcionalidade é usada, e por qual clínica" — nunca "por quem".
 * A segunda não precisa de nome nem de CPF para ser respondida, e mandar PII
 * para telemetria é o vazamento mais fácil de cometer e o mais difícil de
 * desfazer: uma vez enviado, está no provedor.
 *
 * <p>Duas camadas, porque uma só falha:
 *
 * <ul>
 *   <li><b>por nome de campo</b> — pega {@code cpf}, {@code nomeCompleto},
 *       {@code telefone} mesmo quando o valor não tem forma reconhecível;</li>
 *   <li><b>por forma do valor</b> — pega o CPF que alguém colocou dentro de uma
 *       mensagem de erro livre, onde nenhum nome de campo ajudaria.</li>
 * </ul>
 */
@Component
public class ScrubberDePii {

    private static final String MASCARA = "[REMOVIDO]";

    /** Campos cujo VALOR nunca sai, qualquer que seja o conteúdo. */
    private static final List<String> CAMPOS_PROIBIDOS = List.of(
            "cpf", "cnpj", "nome", "nomecompleto", "nome_completo", "razaosocial", "razao_social",
            "email", "telefone", "telefonecelular", "telefone_celular", "celular",
            "cep", "logradouro", "endereco", "complemento", "bairro",
            "senha", "password", "senhahash", "senha_hash", "token", "authorization",
            "refresh", "access_token", "totp", "totpsecret", "totp_secret",
            "descricao_sessao", "descricaosessao", "anamnese", "alergias", "alergias_relatadas",
            "questionario", "observacao", "observacoes", "diagnostico", "prontuario",
            "cartao", "numerocartao", "numero_cartao", "carteirinha", "num_carteirinha");

    /** CPF com ou sem pontuação. */
    private static final Pattern CPF = Pattern.compile(
            "\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    /** CNPJ com ou sem pontuação. */
    private static final Pattern CNPJ = Pattern.compile(
            "\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b");
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    /** Celular brasileiro, com ou sem DDI/DDD e separadores. */
    private static final Pattern TELEFONE = Pattern.compile(
            "\\b(?:\\+?55\\s?)?\\(?\\d{2}\\)?\\s?9?\\d{4}[-\\s]?\\d{4}\\b");

    public Map<String, Object> limpar(Map<String, Object> original) {
        Map<String, Object> limpo = new LinkedHashMap<>();
        original.forEach((chave, valor) -> limpo.put(chave, limparEntrada(chave, valor)));
        return limpo;
    }

    @SuppressWarnings("unchecked")
    private Object limparEntrada(String chave, Object valor) {
        if (campoProibido(chave)) {
            return MASCARA;
        }
        if (valor instanceof Map<?, ?> mapa) {
            return limpar((Map<String, Object>) mapa);
        }
        if (valor instanceof List<?> lista) {
            return lista.stream().map(item -> limparEntrada(chave, item)).toList();
        }
        if (valor instanceof String texto) {
            return limparTexto(texto);
        }
        return valor;
    }

    /**
     * Remove PII de texto livre. É o caminho que pega a mensagem de exceção do
     * banco — "duplicate key value violates unique constraint ... (cpf)=(12345678901)"
     * — que sairia inteira no breadcrumb do Sentry.
     */
    public String limparTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String r = CPF.matcher(texto).replaceAll(MASCARA);
        r = CNPJ.matcher(r).replaceAll(MASCARA);
        r = EMAIL.matcher(r).replaceAll(MASCARA);
        r = TELEFONE.matcher(r).replaceAll(MASCARA);
        return r;
    }

    private boolean campoProibido(String chave) {
        if (chave == null) {
            return false;
        }
        String normalizada = chave.toLowerCase(Locale.ROOT);
        return CAMPOS_PROIBIDOS.stream().anyMatch(normalizada::contains);
    }

    /**
     * O que PODE sair: identificadores que não descrevem pessoa.
     *
     * <p>{@code clinica_id} é aceitável — a clínica é o cliente, e a métrica de
     * produto é sobre ela. {@code paciente_id} NÃO entra: é pseudônimo de uma
     * pessoa, e dado de saúde é sensível (LGPD art. 11); o simples fato de
     * existir um registro numa clínica odontológica já é informação sobre a
     * pessoa.
     */
    public Map<String, Object> eventoDeProduto(String nomeDoEvento, long clinicaId, String papel) {
        return Map.of(
                "evento", nomeDoEvento,
                "clinica_id", clinicaId,
                "papel", papel == null ? "desconhecido" : papel);
    }
}
