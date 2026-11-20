package br.com.dentibot.prontuario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.plataforma.erro.FalhaExternaException;
import br.com.dentibot.prontuario.LancamentoSugerido;
import br.com.dentibot.prontuario.RascunhoDeNota;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Nota ditada (IA-01): leitura da resposta")
class NotaDitadaTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("o que passa na regra vira sugestão; o resto vai para 'não confirmado'")
    void separaPelaRegra() {
        RascunhoDeNota r = NotaDitada.interpretar(7, """
                {"evolucao":"Restauração em resina no 36.",
                 "lancamentos":[
                   {"dente":36,"face":"O","condicao":"restauracao","observacao":"resina A2"},
                   {"dente":99,"face":null,"condicao":"carie","observacao":null},
                   {"dente":11,"face":"X","condicao":"carie","observacao":null}],
                 "nao_ancorado":["cor da resina não confirmada"]}""", json);

        assertThat(r.idChamada()).isEqualTo(7);
        assertThat(r.lancamentos()).containsExactly(
                new LancamentoSugerido(36, "O", "restauracao", "resina A2"));
        assertThat(r.naoConfirmado()).hasSize(3).contains("cor da resina não confirmada");
    }

    @Test
    @DisplayName("resposta que não é JSON vira 502, e não um rascunho vazio")
    void naoJson() {
        assertThatThrownBy(() -> NotaDitada.interpretar(1, "Claro! Aqui está:", json))
                .isInstanceOf(FalhaExternaException.class);
    }

    @Test
    @DisplayName("FDI: permanentes 11–48, decíduos 51–85, nada fora disso")
    void fdi() {
        assertThat(NotaDitada.denteFdi(11)).isTrue();
        assertThat(NotaDitada.denteFdi(48)).isTrue();
        assertThat(NotaDitada.denteFdi(85)).isTrue();
        assertThat(NotaDitada.denteFdi(19)).isFalse();
        assertThat(NotaDitada.denteFdi(56)).isFalse();
        assertThat(NotaDitada.denteFdi(91)).isFalse();
    }
}
