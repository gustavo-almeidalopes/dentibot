package br.com.dentibot.plataforma.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("Rate limit")
class FiltroRateLimitTest {

    private final Limitador limitador = mock(Limitador.class);
    private final FiltroRateLimit filtro = new FiltroRateLimit(limitador, 10, 300, Duration.ofMinutes(1));

    @Test
    @DisplayName("probe e métrica não passam pelo Redis — Redis fora não pode matar instância sadia")
    void actuatorForaDoLimite() throws Exception {
        for (String caminho : new String[] {"/actuator/health/liveness", "/actuator/prometheus"}) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filtro.doFilter(new MockHttpServletRequest("GET", caminho), res, new MockFilterChain());
            assertThat(res.getStatus()).as(caminho).isEqualTo(200);
        }
        verify(limitador, never()).consumir(anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("rota da API continua limitada")
    void apiLimitada() throws Exception {
        given(limitador.consumir(anyString(), anyInt(), any()))
                .willReturn(new Limitador.Resultado(false, 0, Duration.ofSeconds(30)));
        MockHttpServletResponse res = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/api/v1/pacientes"), res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);
    }
}
