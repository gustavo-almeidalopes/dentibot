package br.com.dentibot.comunicacao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.comunicacao.domain.Triagem.Intencao;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Triagem da conversa (IA-28, IA-31)")
class TriagemTest {

    @ParameterizedTest(name = "[{0}] \"{1}\" → {2}")
    @DisplayName("o que o paciente escreveu, dado o que a clínica tinha perguntado")
    @CsvSource(delimiter = '|', textBlock = """
            lembrete         | 1                                   | CONFIRMAR
            lembrete         | Sim, confirmo!                      | CONFIRMAR
            lembrete         | 2                                   | REMARCAR
            lembrete         | preciso remarcar                    | REMARCAR
            lembrete         | 3                                   | CANCELAR
            lembrete         | Não vou poder ir                    | CANCELAR
            lembrete         | quero falar com a recepção          | HUMANO
            lembrete         | PARAR                               | PARAR
            lembrete         | não quero mais receber mensagens    | PARAR
                             | voltar                              | VOLTAR
            pos_procedimento | 1                                   | POS_BEM
            pos_procedimento | 2                                   | POS_LEVE
            pos_procedimento | 3                                   | POS_PREOCUPANTE
            oferta_vaga      | SIM                                 | ACEITAR
            oferta_vaga      | não, obrigado                       | RECUSAR
            orientacao       | ok                                  | CIENTE
                             | o atendimento foi péssimo           | RECLAMACAO
                             | o dente está sensível               | CLINICO
                             | bom dia                             | MENU
            """)
    void interpretacao(String contexto, String texto, Intencao esperada) {
        assertThat(Triagem.interpretar(contexto, texto)).isEqualTo(esperada);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @DisplayName("urgência vence qualquer contexto — até o \"1\" com sangramento")
    @CsvSource(delimiter = '|', textBlock = """
            1, mas estou com sangramento
            meu rosto está muito inchado
            caí e quebrei o dente da frente
            estou com uma dor insuportável
            não consigo abrir a boca
            URGENTE
            """)
    void urgencia(String texto) {
        assertThat(Triagem.interpretar("lembrete", texto)).isEqualTo(Intencao.URGENCIA);
        assertThat(Triagem.interpretar("pos_procedimento", texto)).isEqualTo(Intencao.URGENCIA);
        assertThat(Intencao.URGENCIA.urgente()).isTrue();
    }

    @Test
    @DisplayName("o que é clínico sempre vai para gente; confirmar não")
    void escalonamento() {
        assertThat(Intencao.CLINICO.escala()).isTrue();
        assertThat(Intencao.POS_PREOCUPANTE.urgente()).isTrue();
        assertThat(Intencao.CONFIRMAR.escala()).isFalse();
    }
}
