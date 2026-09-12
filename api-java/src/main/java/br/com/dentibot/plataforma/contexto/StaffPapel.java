package br.com.dentibot.plataforma.contexto;

/**
 * Eixo B — staff da plataforma. Sem tenant, e por desenho sem alcance a dado
 * clínico: nenhuma tabela de {@code pacientes}, {@code prontuario}, {@code agenda}
 * ou {@code lgpd} tem política de RLS que consulte {@code plataforma.staff_atual()}.
 * Isso não é convenção de código — é verificado por teste sobre o catálogo do
 * Postgres.
 */
public enum StaffPapel {
    /** Suporte de primeiro nível: zero dado clínico, zero dado financeiro. */
    SUPORTE_N1("suporte_n1"),
    /** Operações e cobrança: assinatura, fatura, uso de plano. */
    OPS_BILLING("ops_billing"),
    /** Engenharia: acesso a tenant só via JIT aprovado (Fase 4). */
    ENGENHARIA("engenharia");

    private final String valorBanco;

    StaffPapel(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String valorBanco() {
        return valorBanco;
    }

    public static StaffPapel de(String valor) {
        for (StaffPapel p : values()) {
            if (p.valorBanco.equals(valor)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Papel de staff desconhecido: " + valor);
    }
}
