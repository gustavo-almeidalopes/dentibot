package br.com.dentibot.plataforma.contexto;

import java.util.UUID;

/**
 * O contexto da requisição em curso, por thread.
 *
 * <p>Preenchido pelo filtro de autenticação e lido pelo gerenciador de transação,
 * que o traduz em {@code set_config('app.clinica', ...)} dentro da transação.
 * Nenhum código de negócio lê o tenant daqui para montar {@code WHERE} — o
 * {@code WHERE} é do banco (invariante 5). Isto existe para ALIMENTAR o RLS,
 * não para substituí-lo.
 */
public final class ContextoAtual {

    private static final ThreadLocal<ContextoRequisicao> ATUAL = new ThreadLocal<>();

    private ContextoAtual() {
    }

    public static void definir(ContextoRequisicao contexto) {
        ATUAL.set(contexto);
    }

    public static void limpar() {
        ATUAL.remove();
    }

    /**
     * Nunca devolve {@code null}: sem contexto definido, devolve anônimo — que
     * não tem tenant e portanto enxerga zero linhas. Devolver null aqui só
     * trocaria um resultado vazio por um NullPointerException três camadas
     * acima, longe da causa.
     */
    public static ContextoRequisicao obter() {
        ContextoRequisicao c = ATUAL.get();
        return c != null ? c : ContextoRequisicao.anonimo(null);
    }

    /**
     * O tenant, exigindo que exista. Use quando o código precisa nomear a
     * clínica (por exemplo para gravar {@code id_clinica} numa inserção), e não
     * quando basta filtrar — filtrar é trabalho do RLS.
     */
    public static long clinicaObrigatoria() {
        Long id = obter().clinicaId();
        if (id == null) {
            throw new IllegalStateException(
                    "Operação exige contexto de clínica e o contexto atual não tem tenant.");
        }
        return id;
    }

    public static UUID correlacao() {
        return obter().correlacaoId();
    }
}
