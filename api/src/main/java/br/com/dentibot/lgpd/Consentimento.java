package br.com.dentibot.lgpd;

import java.time.Instant;
import java.util.List;

/**
 * O aceite de um paciente a uma versão específica de um termo.
 *
 * <p>{@code ipOrigem} e o user-agent são NOT NULL na V16 de propósito: sem eles
 * o registro não é prova de nada, é uma anotação. {@code revogadoEm} preenchido
 * significa que o consentimento acabou — a linha continua ali provando que ele
 * existiu enquanto o dado foi tratado.
 */
public record Consentimento(
        long idConsentimento,
        long idPaciente,
        long idTermo,
        String tipoDoTermo,
        String versaoDoTermo,
        List<String> finalidadesAceitas,
        Instant aceitoEm,
        String ipOrigem,
        Instant revogadoEm) {

    public boolean vigente() {
        return revogadoEm == null;
    }
}
