package br.com.dentibot.plataforma.contexto;

/**
 * Eixo A — papéis dentro da clínica. A matriz de permissão correspondente vive
 * em {@code plataforma.seguranca.AvaliadorDePermissao}, e em nenhum outro lugar
 * (invariante 3).
 *
 * <p>Os nomes batem exatamente com o CHECK de {@code identidade.usuarios.papel}.
 */
public enum Papel {
    ADMIN("admin"),
    DENTISTA("dentista"),
    RECEPCIONISTA("recepcionista"),
    FINANCEIRO("financeiro"),
    AUXILIAR("auxiliar");

    private final String valorBanco;

    Papel(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String valorBanco() {
        return valorBanco;
    }

    /**
     * Papel desconhecido é 403, não "assume o menor privilégio". Fail closed:
     * um papel que o banco tem e o código não conhece significa que as duas
     * pontas divergiram, e adivinhar qual o certo é como se vazam permissões.
     */
    public static Papel de(String valor) {
        for (Papel p : values()) {
            if (p.valorBanco.equals(valor)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Papel desconhecido: " + valor);
    }
}
