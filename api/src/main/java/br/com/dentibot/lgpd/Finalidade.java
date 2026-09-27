package br.com.dentibot.lgpd;

/**
 * O que o paciente liga e desliga, uma finalidade por vez (IA-53).
 *
 * <p>{@code padrao} é o que vale antes de o paciente dizer qualquer coisa, e
 * cada um tem sua base legal: lembrete de consulta é execução do contrato de
 * atendimento (art. 7º, V) — vale até o paciente desligar; imagem em ensino e
 * contribuição para modelo só com consentimento (art. 7º, I; art. 11, I) —
 * desligados até ele ligar.
 */
public enum Finalidade {
    WHATSAPP("whatsapp", true,
            "Lembretes, confirmações e avisos da clínica pelo WhatsApp"),
    IMAGEM_ENSINO("imagem_ensino", false,
            "Uso de fotos e radiografias do seu tratamento em aulas, sem identificar você"),
    DADOS_ANONIMIZADOS("dados_anonimizados", false,
            "Contribuir, sem identificação, para melhorar as ferramentas de IA da clínica");

    private final String valor;
    private final boolean padrao;
    private final String descricao;

    Finalidade(String valor, boolean padrao, String descricao) {
        this.valor = valor;
        this.padrao = padrao;
        this.descricao = descricao;
    }

    public String valor() {
        return valor;
    }

    public boolean padrao() {
        return padrao;
    }

    public String descricao() {
        return descricao;
    }

    public static Finalidade de(String valor) {
        for (Finalidade f : values()) {
            if (f.valor.equals(valor)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Finalidade desconhecida: " + valor);
    }
}
