package br.com.dentibot.plataforma.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato de evento da camada 16.
 *
 * <p>Regra de evolução: campo novo é sempre OPCIONAL. Mudança incompatível cria
 * {@code version = 2}, e as duas versões convivem até o último consumidor
 * migrar. Um consumidor que quebra porque o produtor mudou o formato é um
 * acoplamento pior que o JOIN que o evento veio evitar.
 *
 * @param actor quem causou. {@code actingAs} existe desde já — vazio hoje — para
 *              a impersonation da Fase 4 não exigir migração de contrato depois.
 */
public record EventoDominio(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        long clinicaId,
        UUID correlationId,
        Ator actor,
        Map<String, Object> data) {

    public record Ator(Long userId, String actingAs) {
        public static Ator sistema() {
            return new Ator(null, null);
        }

        public static Ator usuario(Long id) {
            return new Ator(id, null);
        }
    }
}
