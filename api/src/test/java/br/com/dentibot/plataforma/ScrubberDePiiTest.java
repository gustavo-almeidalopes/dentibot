package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.plataforma.telemetria.ScrubberDePii;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * "Escreva um teste que falha se um payload com CPF chegar no serializador de
 * telemetria" — camada 20, ao pé da letra.
 */
@DisplayName("Scrubbing de PII antes da telemetria")
class ScrubberDePiiTest {

    private final ScrubberDePii scrubber = new ScrubberDePii();

    @Test
    @DisplayName("CPF em campo nomeado não sai")
    void cpfEmCampoNomeado() {
        Map<String, Object> limpo = scrubber.limpar(Map.of(
                "cpf", "12345678901",
                "clinica_id", 42));

        assertThat(limpo.get("cpf")).isEqualTo("[REMOVIDO]");
        // Permissão ao lado da negação: o que PODE sair continua saindo,
        // senão o scrubber poderia estar apagando tudo e passaria igual.
        assertThat(limpo.get("clinica_id")).isEqualTo(42);
    }

    @Test
    @DisplayName("CPF solto no meio de texto livre também não sai")
    void cpfEmTextoLivre() {
        String mensagem = "duplicate key value violates unique constraint: (cpf)=(529.982.247-25)";
        assertThat(scrubber.limparTexto(mensagem))
                .doesNotContain("529.982.247-25")
                .contains("[REMOVIDO]");
    }

    @Test
    @DisplayName("e-mail e telefone somem de texto livre")
    void emailETelefone() {
        String texto = "paciente maria@exemplo.com.br, contato (11) 98765-4321";
        String limpo = scrubber.limparTexto(texto);
        assertThat(limpo).doesNotContain("maria@exemplo.com.br");
        assertThat(limpo).doesNotContain("98765-4321");
    }

    @Test
    @DisplayName("conteúdo clínico é removido por nome de campo")
    void conteudoClinico() {
        Map<String, Object> limpo = scrubber.limpar(Map.of(
                "descricao_sessao", "Restauração em resina no 36, paciente relatou dor",
                "alergias_relatadas", "penicilina",
                "evento", "prontuario.aberto"));

        assertThat(limpo.get("descricao_sessao")).isEqualTo("[REMOVIDO]");
        assertThat(limpo.get("alergias_relatadas")).isEqualTo("[REMOVIDO]");
        assertThat(limpo.get("evento")).isEqualTo("prontuario.aberto");
    }

    @Test
    @DisplayName("estrutura aninhada é limpa em profundidade")
    void aninhado() {
        Map<String, Object> limpo = scrubber.limpar(Map.of(
                "contexto", Map.of(
                        "paciente", Map.of("nomeCompleto", "Ana Ribeiro", "cpf", "11122233344"),
                        "clinica_id", 7)));

        @SuppressWarnings("unchecked")
        Map<String, Object> contexto = (Map<String, Object>) limpo.get("contexto");
        @SuppressWarnings("unchecked")
        Map<String, Object> paciente = (Map<String, Object>) contexto.get("paciente");

        assertThat(paciente.get("nomeCompleto")).isEqualTo("[REMOVIDO]");
        assertThat(paciente.get("cpf")).isEqualTo("[REMOVIDO]");
        assertThat(contexto.get("clinica_id")).isEqualTo(7);
    }

    @Test
    @DisplayName("lista de valores também é varrida")
    void listas() {
        Map<String, Object> limpo = scrubber.limpar(Map.of(
                "mensagens", List.of("ligar para (11) 3333-4444", "confirmar consulta")));

        @SuppressWarnings("unchecked")
        List<String> mensagens = (List<String>) limpo.get("mensagens");
        assertThat(mensagens.get(0)).doesNotContain("3333-4444");
        assertThat(mensagens.get(1)).isEqualTo("confirmar consulta");
    }

    @Test
    @DisplayName("token e senha nunca saem")
    void segredos() {
        Map<String, Object> limpo = scrubber.limpar(Map.of(
                "authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.abc.def",
                "senha", "minha-senha",
                "totp_secret", "JBSWY3DPEHPK3PXP"));

        assertThat(limpo.values()).containsOnly("[REMOVIDO]");
    }

    @Test
    @DisplayName("evento de produto carrega clinica_id, nunca paciente_id")
    void eventoDeProdutoNaoIdentificaPessoa() {
        Map<String, Object> evento = scrubber.eventoDeProduto("prontuario.aberto", 42L, "dentista");

        assertThat(evento).containsEntry("clinica_id", 42L);
        assertThat(evento).containsEntry("papel", "dentista");
        // A métrica responde "quantas vezes" e "por qual clínica" — nunca "de
        // quem". paciente_id é pseudônimo de pessoa, e o simples fato de existir
        // registro numa clínica odontológica já é informação sobre ela.
        assertThat(evento).doesNotContainKeys("paciente_id", "usuario_id", "nome");
    }
}
