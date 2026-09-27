package br.com.dentibot.plataforma.telemetria;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter;

/**
 * Conversores do Logback que passam mensagem e exceção pelo
 * {@link ScrubberDePii} antes de sair do processo. Ligados em
 * {@code logback-spring.xml}.
 *
 * <p>A exceção é o caso que importa: o Detail de uma violação de unicidade do
 * Postgres traz o valor duplicado — {@code Key (id_clinica, cpf)=(42,
 * 12345678901)} — e o {@code TratadorGlobalDeErros} loga a exceção inteira. Sem
 * isto, o CPF chega ao stdout e dali ao provedor de logs.
 */
public final class LogSemPii {

    private static final ScrubberDePii SCRUBBER = new ScrubberDePii();

    private LogSemPii() {
    }

    public static class Mensagem extends MessageConverter {
        @Override
        public String convert(ILoggingEvent evento) {
            return SCRUBBER.limparTexto(super.convert(evento));
        }
    }

    public static class Excecao extends ExtendedWhitespaceThrowableProxyConverter {
        @Override
        protected String throwableProxyToString(IThrowableProxy excecao) {
            return SCRUBBER.limparTexto(super.throwableProxyToString(excecao));
        }
    }
}
