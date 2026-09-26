package br.com.dentibot.auditoria;

import java.time.Instant;

/**
 * Recorte da consulta à trilha.
 *
 * <p>{@code de} e {@code ate} não são opcionais por capricho: a tabela é
 * particionada por {@code ocorrido_em}, e uma consulta sem faixa de data lê
 * todas as partições. Exigir a janela é o que mantém o partition pruning
 * funcionando em vez de decorativo.
 *
 * @param apos paginação por keyset — o {@code idEvento} da última linha da
 *             página anterior, ou zero para a primeira.
 */
public record FiltroDeTrilha(
        Instant de,
        Instant ate,
        String acao,
        String recurso,
        Long idUsuario,
        long apos,
        int limite) {
}
