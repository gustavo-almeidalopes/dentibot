package br.com.dentibot.identidade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Celular como chave de conversa")
class CelularTest {

    @Test
    @DisplayName("cadastro e Meta concordam em DDD + oito finais, com ou sem o nono dígito")
    void chave() {
        assertThat(Celular.chave("(11) 99999-0004")).contains("1199990004");
        assertThat(Celular.chave("5511999990004")).contains("1199990004");
        assertThat(Celular.chave("551199990004")).contains("1199990004");
        assertThat(Celular.chave("12345")).isEmpty();
        assertThat(Celular.chave(null)).isEmpty();
    }

    @Test
    @DisplayName("destinatário da Meta: 55 + DDD + número")
    void paraWhatsApp() {
        assertThat(Celular.paraWhatsApp("(11) 99999-0004")).contains("5511999990004");
        assertThat(Celular.paraWhatsApp("+55 11 99999-0004")).contains("5511999990004");
        assertThat(Celular.paraWhatsApp("0800")).isEmpty();
    }
}
