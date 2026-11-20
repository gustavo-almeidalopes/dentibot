package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Métricas sem senha configurada")
class MetricasFechadasTest extends TesteIntegracao {

    @LocalServerPort
    int porta;

    @Test
    @DisplayName("o endpoint fica fechado para todo mundo — não há credencial padrão")
    void fechado() throws Exception {
        assertThat(OperacaoTest.get(porta, "/actuator/prometheus", "prometheus:").statusCode())
                .isIn(401, 403);
    }
}
