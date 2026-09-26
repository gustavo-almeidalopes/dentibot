package br.com.dentibot.plataforma.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A checagem do {@code azp} não pode ser desligada por uma variável vazia.
 *
 * <p>Com a lista de partes autorizadas vazia, o validador aceita token emitido
 * para qualquer origem — um token do Clerk obtido em outro site da mesma
 * instância seria replayado contra esta API. {@code DENTIBOT_CLERK_ORIGINS=""}
 * em produção fazia isso sem erro nenhum.
 */
@DisplayName("Configuração do Clerk")
class PropriedadesClerkTest {

    @Test
    @DisplayName("lista de origens vazia ou em branco derruba a subida")
    void semOrigemNaoSobe() {
        assertThatThrownBy(() -> new PropriedadesClerk("https://x.clerk.accounts.dev", List.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PropriedadesClerk("https://x.clerk.accounts.dev", List.of(" ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("com origem, sobe")
    void comOrigemSobe() {
        assertThat(new PropriedadesClerk("https://x.clerk.accounts.dev",
                List.of("https://app.dentibot.com.br")).partesAutorizadas())
                .containsExactly("https://app.dentibot.com.br");
    }
}
