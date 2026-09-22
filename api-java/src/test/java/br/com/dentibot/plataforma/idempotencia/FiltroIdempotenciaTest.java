package br.com.dentibot.plataforma.idempotencia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Quais POSTs exigem {@code Idempotency-Key}.
 *
 * <p>O duplo toque que mais custa é o de dinheiro: "registrar recebimento" duas
 * vezes vira dois lançamentos no ledger, que é append-only e só se desfaz por
 * estorno. A lista chegou a exigir a chave em rotas que não existem
 * ({@code /cobrancas}, {@code /mensagens}) e a dispensar em
 * {@code /financeiro}.
 */
@DisplayName("Rotas que exigem Idempotency-Key")
class FiltroIdempotenciaTest {

    private final FiltroIdempotencia filtro = new FiltroIdempotencia(null);

    @Test
    @DisplayName("POST de dinheiro, paciente e estoque exige a chave")
    void escritasQueDuplicamExigemChave() {
        for (String rota : new String[] {
                "/api/v1/financeiro/lancamentos",
                "/api/v1/financeiro/lancamentos/7/estornar",
                "/api/v1/financeiro/despesas/3/pagar",
                "/api/v1/pacientes",
                "/api/v1/estoque/movimentacoes",
                "/api/v1/consultas",
                "/api/v1/orcamentos/5/aprovar"}) {
            assertThat(filtro.shouldNotFilter(new MockHttpServletRequest("POST", rota)))
                    .as("POST %s sem Idempotency-Key", rota)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("leitura nunca exige")
    void leituraNaoExige() {
        assertThat(filtro.shouldNotFilter(new MockHttpServletRequest("GET", "/api/v1/financeiro/recebiveis")))
                .isTrue();
    }
}
