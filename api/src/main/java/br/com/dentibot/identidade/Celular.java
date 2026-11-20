package br.com.dentibot.identidade;

import java.util.Optional;

/**
 * Celular brasileiro como chave de conversa.
 *
 * <p>O cadastro guarda "(11) 99999-0004"; a Meta manda "5511999990004" — ou
 * "551199990004", sem o nono dígito, para contas antigas. Os dois lados só
 * concordam em DDD + oito dígitos finais, e é isso que a chave é.
 */
public final class Celular {

    private Celular() {
    }

    /** DDD + oito dígitos finais, ou vazio se não parece celular brasileiro. */
    public static Optional<String> chave(String qualquer) {
        String d = nacional(qualquer);
        return d.length() < 10 || d.length() > 11
                ? Optional.empty()
                : Optional.of(d.substring(0, 2) + d.substring(d.length() - 8));
    }

    /** Como a Meta quer o destinatário: 55 + DDD + número, só dígitos. */
    public static Optional<String> paraWhatsApp(String qualquer) {
        String d = nacional(qualquer);
        return d.length() < 10 || d.length() > 11 ? Optional.empty() : Optional.of("55" + d);
    }

    private static String nacional(String qualquer) {
        String d = qualquer == null ? "" : qualquer.replaceAll("\\D", "");
        return d.startsWith("55") && d.length() >= 12 ? d.substring(2) : d;
    }
}
