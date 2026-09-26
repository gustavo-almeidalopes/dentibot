package br.com.dentibot.plataforma.telemetria;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Erros no Sentry (ST-28)")
class ErrosNoSentryTest {

    private final ScrubberDePii scrubber = new ScrubberDePii();

    @Test
    @DisplayName("CPF e e-mail saem da mensagem e da exceção antes do envio")
    void limpaAntesDeEnviar() {
        SentryEvent evento = new SentryEvent();
        Message mensagem = new Message();
        mensagem.setMessage("falha para 123.456.789-09");
        mensagem.setFormatted("falha para 123.456.789-09");
        evento.setMessage(mensagem);
        SentryException excecao = new SentryException();
        excecao.setValue("duplicate key: ana@clinica.com.br");
        evento.setExceptions(List.of(excecao));

        SentryEvent limpo = ErrosNoSentry.limpar(evento, scrubber);

        assertThat(limpo.getMessage().getFormatted()).doesNotContain("123.456.789-09");
        assertThat(limpo.getMessage().getMessage()).doesNotContain("123.456.789-09");
        assertThat(limpo.getExceptions().get(0).getValue()).doesNotContain("ana@clinica.com.br");
    }

    @Test
    @DisplayName("evento sem mensagem nem exceção passa sem quebrar")
    void eventoVazio() {
        assertThat(ErrosNoSentry.limpar(new SentryEvent(), scrubber)).isNotNull();
    }
}
