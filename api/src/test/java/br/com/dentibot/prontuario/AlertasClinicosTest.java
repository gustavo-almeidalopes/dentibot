package br.com.dentibot.prontuario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Alertas clínicos")
class AlertasClinicosTest {

    @Test
    @DisplayName("anticoagulante é reconhecido com e sem acento, por princípio ativo ou marca")
    void anticoagulante() {
        assertThat(AlertasClinicos.anticoagulanteEm("Marevan 5mg")).isNotNull();
        assertThat(AlertasClinicos.anticoagulanteEm("ácido acetilsalicílico 100")).isNotNull();
        assertThat(AlertasClinicos.anticoagulanteEm("AAS infantil")).isNotNull();
        assertThat(AlertasClinicos.anticoagulanteEm("losartana")).isNull();
        // "aas" é palavra, não pedaço: "glaaser" não é aspirina.
        assertThat(AlertasClinicos.anticoagulanteEm("creme Glaaser")).isNull();
    }

    @Test
    @DisplayName("os avisos saem na ordem de risco, e só o que foi respondido sim")
    void avisos() {
        AlertasClinicos a = AlertasClinicos.de("dipirona", "Xarelto 20mg", true, false, null);

        assertThat(a.avisos()).containsExactly(
                "Alergia: dipirona.",
                "Possível anticoagulante em uso: Xarelto 20mg.",
                "Condição sistêmica declarada.");
    }

    @Test
    @DisplayName("sem triagem, nenhum aviso — e não um 'nega alergia' que ninguém disse")
    void semTriagem() {
        assertThat(AlertasClinicos.nenhum().avisos()).isEmpty();
    }
}
