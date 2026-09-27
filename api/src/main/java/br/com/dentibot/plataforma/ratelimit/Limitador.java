package br.com.dentibot.plataforma.ratelimit;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Contador de janela fixa no Redis.
 *
 * <p>Esta é a camada de DENTRO. Ela só atua depois que a requisição já consumiu
 * uma thread da aplicação — por isso ela não substitui o rate limit de borda do
 * Cloudflare, que barra volume antes de chegar aqui. As duas existem e fazem
 * coisas diferentes: a borda protege contra ataque e bot; esta protege o
 * contrato do plano e o vizinho de tenant.
 *
 * <p>Redis fora do ar libera a requisição, de propósito: Redis nunca é fonte de
 * verdade (camada 11), e derrubar a clínica inteira porque o cache caiu seria
 * trocar um problema de abuso por um de indisponibilidade. Quem segura o abuso
 * nesse intervalo é a borda.
 */
@Component
public class Limitador {

    private static final Logger log = LoggerFactory.getLogger(Limitador.class);

    private final StringRedisTemplate redis;

    public Limitador(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public record Resultado(boolean permitido, long restante, Duration esperar) {
        public static Resultado liberado(long restante) {
            return new Resultado(true, restante, Duration.ZERO);
        }
    }

    /**
     * @param chave  algo como {@code rate:clinica:42:relatorios}
     * @param limite quantas requisições cabem na janela
     * @param janela tamanho da janela
     */
    public Resultado consumir(String chave, int limite, Duration janela) {
        try {
            Long contagem = redis.opsForValue().increment(chave);
            if (contagem == null) {
                return Resultado.liberado(limite);
            }
            if (contagem == 1L) {
                // Primeira da janela: define quando ela expira. Se o EXPIRE
                // falhasse, a chave viveria para sempre e a clínica ficaria
                // bloqueada em definitivo — por isso o TTL vai junto do primeiro
                // incremento e não numa segunda ida.
                redis.expire(chave, janela);
            }
            if (contagem > limite) {
                Long ttl = redis.getExpire(chave);
                return new Resultado(false, 0,
                        Duration.ofSeconds(ttl == null || ttl < 0 ? janela.toSeconds() : ttl));
            }
            return Resultado.liberado(limite - contagem);
        } catch (RuntimeException e) {
            log.warn("Redis indisponível no rate limit — liberando. Chave={}", chave);
            return Resultado.liberado(limite);
        }
    }
}
