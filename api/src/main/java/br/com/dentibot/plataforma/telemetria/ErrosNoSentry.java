package br.com.dentibot.plataforma.telemetria;

import io.sentry.DataCollection;
import io.sentry.KeyValueCollectionBehavior;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Erro 500 vai para o Sentry (ST-28), e só ele, e só depois do
 * {@link ScrubberDePii}.
 *
 * <p>SDK puro, sem a integração Spring: ela anexaria corpo de requisição,
 * cabeçalho e breadcrumb de log por padrão — num sistema de prontuário, cada um
 * desses é um vazamento em potencial. Aqui entra a exceção, com a mensagem
 * limpa, e as tags que o {@code TratadorGlobalDeErros} põe.
 *
 * <p>Sem {@code SENTRY_DSN} o SDK não é iniciado, e {@code Sentry.captureException}
 * vira no-op: dev e teste não precisam de nada.
 */
@Configuration(proxyBeanMethods = false)
public class ErrosNoSentry {

    private static final Logger log = LoggerFactory.getLogger(ErrosNoSentry.class);

    public ErrosNoSentry(@Value("${dentibot.sentry.dsn:}") String dsn,
                         @Value("${dentibot.sentry.ambiente:local}") String ambiente,
                         ScrubberDePii scrubber) {
        if (dsn.isBlank()) {
            return;
        }
        Sentry.init(o -> {
            o.setDsn(dsn);
            o.setEnvironment(ambiente);
            semColetaAutomatica(o.getDataCollection());
            o.setBeforeSend((evento, dica) -> limpar(evento, scrubber));
        });
        log.info("Sentry ligado no ambiente {}", ambiente);
    }

    /**
     * Desliga, um a um, os dados que o SDK coleta sozinho. Substitui o
     * {@code setSendDefaultPii(false)}, depreciado no 8.59 e removido no 9.0.
     *
     * <p>Um a um porque o {@code DataCollection} tem uma armadilha: basta
     * configurar um campo para todos os outros assumirem o padrão documentado —
     * e o padrão é coletar usuário, SQL, caminho de arquivo e todos os corpos
     * HTTP. Desligar só o usuário ligaria o resto.
     */
    static void semColetaAutomatica(DataCollection coleta) {
        coleta.setUserInfo(false);
        coleta.setCookies(KeyValueCollectionBehavior.off());
        coleta.setUrlQueryParams(KeyValueCollectionBehavior.off());
        coleta.getHttpHeaders().setRequest(KeyValueCollectionBehavior.off());
        coleta.getHttpHeaders().setResponse(KeyValueCollectionBehavior.off());
        coleta.setHttpBodies(Set.of());
        coleta.setDatabaseQueryData(false);
        coleta.setFilePaths(false);
        coleta.getGraphql().setDocument(false);
        coleta.getGraphql().setVariables(false);
    }

    static SentryEvent limpar(SentryEvent evento, ScrubberDePii scrubber) {
        Message mensagem = evento.getMessage();
        if (mensagem != null) {
            mensagem.setMessage(scrubber.limparTexto(mensagem.getMessage()));
            mensagem.setFormatted(scrubber.limparTexto(mensagem.getFormatted()));
        }
        if (evento.getExceptions() != null) {
            for (SentryException e : evento.getExceptions()) {
                e.setValue(scrubber.limparTexto(e.getValue()));
            }
        }
        return evento;
    }
}
