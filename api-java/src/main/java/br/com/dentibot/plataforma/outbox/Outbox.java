package br.com.dentibot.plataforma.outbox;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Grava o evento na MESMA transação da escrita de negócio (invariante 8).
 *
 * <p>Por que não publicar direto num broker: publicar e commitar não são
 * atômicos. Se o publish acontece e a transação faz rollback, saiu evento de
 * consulta que não existe — e o paciente recebe confirmação de um horário que
 * ninguém marcou. Se o publish falha depois do commit, a consulta existe e
 * ninguém é avisado. Broker nenhum resolve isso; ele só muda o lugar do
 * problema. O outbox resolve porque o evento vira uma linha da mesma transação.
 *
 * <p>A entrega é <b>at-least-once</b>. O consumidor PRECISA deduplicar por
 * {@code event_id} — sem isso o e-mail sai duas vezes, e a cobrança também.
 */
@Component
public class Outbox {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public Outbox(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public UUID gravar(String tipo, Map<String, Object> dados) {
        return gravar(tipo, 1, dados);
    }

    public UUID gravar(String tipo, int versao, Map<String, Object> dados) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // Fora de transação o evento não tem a quem se atar, e a garantia
            // inteira do outbox some. Melhor explodir aqui.
            throw new IllegalStateException(
                    "Evento '%s' gravado fora de transação: o outbox só vale se a linha do "
                            .formatted(tipo)
                            + "evento e a escrita de negócio commitarem juntas (invariante 8).");
        }

        ContextoRequisicao ctx = ContextoAtual.obter();
        EventoDominio evento = new EventoDominio(
                UUID.randomUUID(),
                tipo,
                versao,
                Instant.now(),
                ContextoAtual.clinicaObrigatoria(),
                ctx.correlacaoId(),
                ctx.usuarioId() == null
                        ? EventoDominio.Ator.sistema()
                        : EventoDominio.Ator.usuario(ctx.usuarioId()),
                dados);

        jdbc.sql("""
                        INSERT INTO plataforma.outbox
                            (event_id, event_type, version, occurred_at, clinica_id,
                             correlation_id, actor, data)
                        VALUES (CAST(:id AS UUID), :tipo, :versao, :ocorrido, :clinica,
                                CAST(:correlacao AS UUID), CAST(:ator AS JSONB), CAST(:dados AS JSONB))
                        """)
                .param("id", evento.eventId().toString())
                .param("tipo", evento.eventType())
                .param("versao", evento.version())
                .param("ocorrido", java.sql.Timestamp.from(evento.occurredAt()))
                .param("clinica", evento.clinicaId())
                .param("correlacao", evento.correlationId() == null
                        ? null : evento.correlationId().toString())
                .param("ator", serializar(evento.actor()))
                .param("dados", serializar(evento.data()))
                .update();

        return evento.eventId();
    }

    private String serializar(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (RuntimeException e) {
            // Jackson 3 lança JacksonException, que é unchecked.
            throw new IllegalArgumentException("Payload de evento não serializável", e);
        }
    }
}
