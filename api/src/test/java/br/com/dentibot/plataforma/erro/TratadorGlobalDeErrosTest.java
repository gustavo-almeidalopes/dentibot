package br.com.dentibot.plataforma.erro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.dentibot.pacientes.interfaces.http.PacienteController;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Requisição malformada é erro do cliente: 4xx, não 500.
 *
 * <p>O handler de {@code Exception} pegava as exceções que o próprio Spring MVC
 * lança antes do controller — JSON quebrado, parâmetro com tipo errado, método
 * que a rota não aceita — e respondia 500 com {@code log.error}. O cliente não
 * sabia o que corrigir, e o alerta de erro disparava por bug de quem chamou.
 *
 * <p>Sem banco e sem contexto: nenhum destes casos chega ao serviço.
 */
@DisplayName("Tradução de erro para resposta")
class TratadorGlobalDeErrosTest {

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new PacienteController(null))
            .setControllerAdvice(new TratadorGlobalDeErros())
            .build();

    @Test
    @DisplayName("JSON malformado é 400")
    void jsonMalformado() throws Exception {
        mvc.perform(post("/api/v1/pacientes").contentType(MediaType.APPLICATION_JSON).content("{\"nome"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", endsWith("/requisicao-invalida")));
    }

    @Test
    @DisplayName("parâmetro com tipo errado é 400, e o valor recebido não volta na resposta")
    void parametroComTipoErrado() throws Exception {
        mvc.perform(get("/api/v1/pacientes").param("limite", "12345678901"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", not(containsString("12345678901"))));
    }

    @Test
    @DisplayName("método que a rota não aceita é 405")
    void metodoNaoSuportado() throws Exception {
        mvc.perform(delete("/api/v1/pacientes"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("escrita barrada pelo RLS é 404, não 500")
    void rlsViraNaoEncontrado() {
        ProblemDetail p = new TratadorGlobalDeErros().privilegioNoBanco(negadoPeloBanco(
                "new row violates row-level security policy for table \"pessoas\""));

        assertThat(p.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("alterar lançamento append-only é 403, não 500")
    void appendOnlyViraProibido() {
        ProblemDetail p = new TratadorGlobalDeErros().privilegioNoBanco(negadoPeloBanco(
                "financeiro.lancamentos é append-only. Para desfazer, insira um estorno"));

        assertThat(p.getStatus()).isEqualTo(403);
    }

    /** Como o Spring entrega o SQLState 42501: pela classe "42", como gramática. */
    private static BadSqlGrammarException negadoPeloBanco(String mensagem) {
        return new BadSqlGrammarException("INSERT", "INSERT ...", new SQLException(mensagem, "42501"));
    }

    @Test
    @DisplayName("campo inválido continua 400 com o nome do campo")
    void campoInvalido() throws Exception {
        mvc.perform(post("/api/v1/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nomeCompleto").exists());
    }
}
