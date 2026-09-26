package br.com.dentibot.plataforma.outbox;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lê o outbox e entrega aos consumidores.
 *
 * <p>{@code FOR UPDATE SKIP LOCKED} é o que permite rodar várias instâncias da
 * API sem coordenação: cada uma trava as linhas que pegou e as outras
 * simplesmente pulam, em vez de esperar. Sem {@code SKIP LOCKED}, duas
 * instâncias serializam no mesmo lote e a fila anda na velocidade de uma só.
 *
 * <p>Backoff exponencial no erro. Sem ele, um consumidor quebrado vira um laço
 * quente que consome a conexão de banco da aplicação inteira tentando o mesmo
 * evento milhares de vezes por minuto.
 */
@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final int LOTE = 50;
    private static final int MAX_TENTATIVAS = 10;

    private final JdbcClient jdbc;
    private final TransactionTemplate transacao;
    private final ObjectMapper json;
    private final List<ConsumidorDeEvento> consumidores;

    public OutboxWorker(JdbcClient jdbc, TransactionTemplate transacao, ObjectMapper json,
                        List<ConsumidorDeEvento> consumidores) {
        this.jdbc = jdbc;
        this.transacao = transacao;
        this.json = json;
        this.consumidores = consumidores;
    }

    @Scheduled(fixedDelayString = "${dentibot.outbox.intervalo:2000}")
    public void processar() {
        // Contexto de worker: sem tenant, com app.worker = true. É o que a
        // política worker_publica do RLS exige para enxergar a fila inteira.
        ContextoAtual.definir(ContextoRequisicao.deWorker(UUID.randomUUID()));
        try {
            transacao.executeWithoutResult(s -> processarLote());
        } catch (RuntimeException e) {
            log.error("Falha no ciclo do outbox", e);
        } finally {
            ContextoAtual.limpar();
        }
    }

    private void processarLote() {
        List<EventoDominio> pendentes = jdbc.sql("""
                        SELECT event_id, event_type, version, occurred_at, clinica_id,
                               correlation_id, actor::text AS actor, data::text AS data
                        FROM plataforma.outbox
                        WHERE publicado_em IS NULL
                          AND proxima_tentativa_em <= NOW()
                          AND tentativas < :maxTentativas
                        ORDER BY occurred_at
                        LIMIT :lote
                        FOR UPDATE SKIP LOCKED
                        """)
                .param("lote", LOTE)
                .param("maxTentativas", MAX_TENTATIVAS)
                .query((rs, n) -> new EventoDominio(
                        UUID.fromString(rs.getString("event_id")),
                        rs.getString("event_type"),
                        rs.getInt("version"),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getLong("clinica_id"),
                        rs.getString("correlation_id") == null
                                ? null : UUID.fromString(rs.getString("correlation_id")),
                        lerAtor(rs.getString("actor")),
                        lerMapa(rs.getString("data"))))
                .list();

        for (EventoDominio evento : pendentes) {
            entregar(evento);
        }
    }

    private void entregar(EventoDominio evento) {
        try {
            for (ConsumidorDeEvento consumidor : consumidores) {
                if (!consumidor.interessadoEm(evento.eventType())) {
                    continue;
                }
                // Deduplicação: at-least-once do outbox vira exactly-once-por-
                // consumidor aqui. Sem esta linha, um crash entre entregar e
                // marcar como publicado manda o mesmo e-mail de novo.
                int novo = jdbc.sql("""
                                INSERT INTO plataforma.eventos_processados (event_id, consumidor)
                                VALUES (CAST(:id AS UUID), :consumidor)
                                ON CONFLICT DO NOTHING
                                """)
                        .param("id", evento.eventId().toString())
                        .param("consumidor", consumidor.nome())
                        .update();
                if (novo == 1) {
                    consumidor.consumir(evento);
                }
            }
            marcarPublicado(evento.eventId());
        } catch (RuntimeException e) {
            log.warn("Evento {} ({}) falhou; reagendando", evento.eventId(), evento.eventType(), e);
            reagendar(evento.eventId(), e.getMessage());
        }
    }

    private void marcarPublicado(UUID eventId) {
        jdbc.sql("""
                        UPDATE plataforma.outbox SET publicado_em = NOW()
                        WHERE event_id = CAST(:id AS UUID)
                        """)
                .param("id", eventId.toString())
                .update();
    }

    private void reagendar(UUID eventId, String erro) {
        jdbc.sql("""
                        UPDATE plataforma.outbox
                        SET tentativas = tentativas + 1,
                            ultimo_erro = :erro,
                            proxima_tentativa_em = NOW()
                                + (LEAST(POWER(2, tentativas + 1), 3600) || ' seconds')::INTERVAL
                        WHERE event_id = CAST(:id AS UUID)
                        """)
                .param("id", eventId.toString())
                .param("erro", erro == null ? "sem mensagem" : erro.substring(0,
                        Math.min(erro.length(), 500)))
                .update();
    }

    /**
     * Eventos que estouraram as tentativas ficam parados e visíveis. Não se
     * apaga: um evento de cobrança que nunca saiu é informação de negócio, não
     * lixo de fila.
     */
    @Scheduled(cron = "${dentibot.outbox.alerta-cron:0 */15 * * * *}")
    public void alertarPresos() {
        ContextoAtual.definir(ContextoRequisicao.deWorker(UUID.randomUUID()));
        try {
            Integer presos = transacao.execute(s -> jdbc.sql("""
                            SELECT count(*) FROM plataforma.outbox
                            WHERE publicado_em IS NULL AND tentativas >= :max
                            """)
                    .param("max", MAX_TENTATIVAS)
                    .query(Integer.class).single());
            if (presos != null && presos > 0) {
                log.error("{} evento(s) no outbox esgotaram as tentativas e não foram entregues.",
                        presos);
            }
        } finally {
            ContextoAtual.limpar();
        }
    }

    private EventoDominio.Ator lerAtor(String texto) {
        try {
            return json.readValue(texto, EventoDominio.Ator.class);
        } catch (Exception e) {
            return EventoDominio.Ator.sistema();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lerMapa(String texto) {
        try {
            return json.readValue(texto, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Usado apenas em teste: processa um lote na hora, sem esperar o agendador. */
    public void processarAgora() {
        processar();
    }
}
