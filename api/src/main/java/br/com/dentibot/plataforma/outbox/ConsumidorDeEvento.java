package br.com.dentibot.plataforma.outbox;

/**
 * Reage a um evento publicado pelo outbox.
 *
 * <p>Contrato obrigatório: <b>seja idempotente</b>. O outbox entrega
 * at-least-once, e o worker pode entregar o mesmo {@code event_id} duas vezes
 * (crash entre publicar e marcar como enviado, por exemplo). O
 * {@link OutboxWorker} já deduplica por {@code (event_id, consumidor)} em
 * {@code plataforma.eventos_processados} antes de chamar este método — mas um
 * consumidor que faz efeito externo (e-mail, cobrança) deve tratar repetição
 * como possível de qualquer forma.
 */
public interface ConsumidorDeEvento {

    /** Nome estável; é a chave de deduplicação junto com o event_id. */
    String nome();

    /** {@code true} se este consumidor se interessa por este tipo de evento. */
    boolean interessadoEm(String tipoDeEvento);

    void consumir(EventoDominio evento);
}
