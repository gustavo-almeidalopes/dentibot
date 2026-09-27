package br.com.dentibot.clinicas.domain;

/**
 * Planos vendidos na landing. Os limites vivem em {@code billing.planos}, no
 * banco — aqui fica só a identidade do plano, para não haver dois lugares
 * dizendo quantos profissionais o Solo permite.
 */
public enum Plano {
    SOLO("solo"),
    CLINICA("clinica");

    private final String valorBanco;

    Plano(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String valorBanco() {
        return valorBanco;
    }

    public static Plano de(String valor) {
        for (Plano p : values()) {
            if (p.valorBanco.equals(valor)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Plano desconhecido: " + valor);
    }
}
