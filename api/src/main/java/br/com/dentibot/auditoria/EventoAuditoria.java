package br.com.dentibot.auditoria;

import java.time.Instant;

/**
 * Uma linha da trilha, como a tela de auditoria a mostra.
 *
 * <p>{@code dadosAnteriores} e {@code dadosPosteriores} ficam de fora de
 * propósito: eles contêm o registro clínico inteiro, e a tela de auditoria
 * responde "quem tocou no quê e quando" — não é uma segunda via do prontuário.
 * Quem precisa do conteúdo pede a exportação, que é outro fluxo, com outra
 * permissão e com a própria linha de auditoria.
 *
 * <p>Vem {@code idUsuario}, não o nome. Não é economia: {@code identidade} já
 * depende deste módulo para gravar a trilha, e buscar o nome aqui fecharia um
 * ciclo de construtor que o Spring recusa no boot. Quem monta a tela já tem a
 * lista da equipe e casa os dois lados lá.
 */
public record EventoAuditoria(
        long idEvento,
        Instant ocorridoEm,
        Long idUsuario,
        String staffPapel,
        String acao,
        String recurso,
        String idRecurso,
        String ipOrigem,
        String correlacaoId) {
}
