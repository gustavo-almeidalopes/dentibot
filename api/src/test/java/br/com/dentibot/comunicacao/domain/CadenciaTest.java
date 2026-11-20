package br.com.dentibot.comunicacao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.comunicacao.domain.Cadencia.Envio;
import br.com.dentibot.comunicacao.domain.Cadencia.Risco;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cadência de confirmação (IA-16, IA-32)")
class CadenciaTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    private static Instant sp(int dia, int hora) {
        return ZonedDateTime.of(2026, 10, dia, hora, 0, 0, 0, SP).toInstant();
    }

    @Test
    @DisplayName("risco é contagem do histórico: nunca faltou, faltou uma, falta sempre")
    void risco() {
        assertThat(Cadencia.risco(0, 0)).isEqualTo(Risco.BAIXO);
        assertThat(Cadencia.risco(0, 8)).isEqualTo(Risco.BAIXO);
        assertThat(Cadencia.risco(1, 10)).isEqualTo(Risco.MEDIO);
        assertThat(Cadencia.risco(1, 3)).isEqualTo(Risco.ALTO);
        assertThat(Cadencia.risco(2, 10)).isEqualTo(Risco.ALTO);
    }

    @Test
    @DisplayName("baixo risco: um lembrete na véspera; alto: confirmação cedo e reforços")
    void planoPorRisco() {
        Instant consulta = sp(20, 14);
        Instant agora = sp(10, 9);
        assertThat(Cadencia.plano(Risco.BAIXO, consulta, agora, SP))
                .containsExactly(new Envio("lembrete", sp(19, 14)));
        assertThat(Cadencia.plano(Risco.ALTO, consulta, agora, SP)).containsExactly(
                new Envio("confirmacao", sp(17, 14)),
                new Envio("reforco", sp(19, 14)),
                new Envio("reforco", sp(20, 11)));
    }

    @Test
    @DisplayName("nada de madrugada: o reforço das 5h vai para as 19h da véspera, e não colide")
    void horarioUtil() {
        Instant consulta = sp(20, 8);
        List<Envio> plano = Cadencia.plano(Risco.ALTO, consulta, sp(10, 9), SP);
        // -24h = 19/10 8h; -3h = 20/10 5h → 19/10 19h, onze horas depois do outro.
        assertThat(plano).extracting(Envio::quando)
                .containsExactly(sp(17, 8), sp(19, 8), sp(19, 19));
        assertThat(plano).allSatisfy(e -> assertThat(e.quando().atZone(SP).getHour())
                .isBetween(Cadencia.ABRE, Cadencia.FECHA - 1));
    }

    @Test
    @DisplayName("marcou em cima da hora: confirma agora; se for de noite, às 8h")
    void emCimaDaHora() {
        assertThat(Cadencia.plano(Risco.BAIXO, sp(20, 14), sp(20, 9), SP))
                .containsExactly(new Envio("confirmacao", sp(20, 9)));
        assertThat(Cadencia.plano(Risco.BAIXO, sp(21, 11), sp(20, 22), SP))
                .containsExactly(new Envio("confirmacao", sp(21, 8)));
        // Menos de duas horas: não adianta mais perguntar.
        assertThat(Cadencia.plano(Risco.BAIXO, sp(20, 10), sp(20, 9), SP)).isEmpty();
    }
}
