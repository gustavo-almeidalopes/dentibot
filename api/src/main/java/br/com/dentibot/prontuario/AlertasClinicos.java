package br.com.dentibot.prontuario;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * O que o dentista precisa saber antes de tocar no paciente (IA-06, IA-27).
 *
 * <p>{@code avisos} são frases prontas, montadas aqui e não nos clientes: a
 * tela e a voz do web e do app dizem exatamente a mesma coisa, e a regra de
 * "o que merece aviso" tem um lugar só.
 *
 * <p>Anticoagulante é reconhecido por lista explícita de princípios ativos, não
 * por modelo: um falso negativo aqui é sangramento na cadeira, e a lista é
 * auditável por qualquer dentista. Ela avisa; quem decide é o profissional.
 */
public record AlertasClinicos(
        String alergia,
        String medicamentoContinuo,
        Boolean condicaoSistemica,
        Boolean gravidez,
        Boolean emTratamentoMedico,
        List<String> avisos) {

    private static final List<String> ANTICOAGULANTES = List.of(
            "varfarina", "marevan", "coumadin", "rivaroxabana", "xarelto", "apixabana",
            "eliquis", "dabigatrana", "pradaxa", "edoxabana", "heparina", "enoxaparina",
            "clexane", "clopidogrel", "plavix", "ticagrelor", "prasugrel", "aas",
            "acido acetilsalicilico", "aspirina");

    public static AlertasClinicos de(String alergia, String medicamentoContinuo,
                                     Boolean condicaoSistemica, Boolean gravidez,
                                     Boolean emTratamentoMedico) {
        List<String> avisos = new ArrayList<>();
        if (temTexto(alergia)) {
            avisos.add("Alergia: " + alergia.strip() + ".");
        }
        String anticoagulante = anticoagulanteEm(medicamentoContinuo);
        if (anticoagulante != null) {
            avisos.add("Possível anticoagulante em uso: " + anticoagulante + ".");
        } else if (temTexto(medicamentoContinuo)) {
            avisos.add("Medicamento contínuo: " + medicamentoContinuo.strip() + ".");
        }
        if (Boolean.TRUE.equals(condicaoSistemica)) {
            avisos.add("Condição sistêmica declarada.");
        }
        if (Boolean.TRUE.equals(gravidez)) {
            avisos.add("Gestante.");
        }
        if (Boolean.TRUE.equals(emTratamentoMedico)) {
            avisos.add("Em tratamento médico.");
        }
        return new AlertasClinicos(alergia, medicamentoContinuo, condicaoSistemica, gravidez,
                emTratamentoMedico, List.copyOf(avisos));
    }

    public static AlertasClinicos nenhum() {
        return de(null, null, null, null, null);
    }

    static String anticoagulanteEm(String medicamentos) {
        if (!temTexto(medicamentos)) {
            return null;
        }
        String normal = Normalizer.normalize(medicamentos, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        for (String nome : ANTICOAGULANTES) {
            if (normal.matches("(?s).*\\b" + nome + "\\b.*")) {
                return medicamentos.strip();
            }
        }
        return null;
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }
}
