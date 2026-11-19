package br.com.dentibot.pacientes;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.identidade.DadosPessoais;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NovoPacienteTest {

    /**
     * Quinze campos, quase todos String, passados por posição: trocar bairro por
     * cidade compila. Cada campo recebe um valor diferente e precisa reaparecer
     * no campo de mesmo nome.
     */
    @Test
    void dadosPessoaisLevaCadaCampoParaOCampoDeMesmoNome() throws Exception {
        NovoPaciente novo = new NovoPaciente("nomeCompleto", "cpf", "rg", LocalDate.of(1990, 1, 2),
                "telefoneCelular", "email", "profissao", "responsavelLegal", "cep", "logradouro",
                "numero", "complemento", "bairro", "cidade", "uf", null, null, null);

        DadosPessoais dados = novo.dadosPessoais();

        for (RecordComponent c : DadosPessoais.class.getRecordComponents()) {
            Object esperado = NovoPaciente.class.getMethod(c.getName()).invoke(novo);
            assertThat(c.getAccessor().invoke(dados)).as(c.getName()).isEqualTo(esperado);
        }
    }
}
