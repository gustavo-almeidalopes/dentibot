package br.com.dentibot.clinicas.domain;

public enum StatusClinica {
    TRIAL("trial"),
    ATIVA("ativa"),
    INADIMPLENTE("inadimplente"),
    SUSPENSA("suspensa"),
    ENCERRADA("encerrada");

    private final String valorBanco;

    StatusClinica(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String valorBanco() {
        return valorBanco;
    }

    /** Clínica suspensa ou encerrada não opera — só lê a própria fatura. */
    public boolean permiteOperar() {
        return this == TRIAL || this == ATIVA || this == INADIMPLENTE;
    }

    public static StatusClinica de(String valor) {
        for (StatusClinica s : values()) {
            if (s.valorBanco.equals(valor)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Status de clínica desconhecido: " + valor);
    }
}
