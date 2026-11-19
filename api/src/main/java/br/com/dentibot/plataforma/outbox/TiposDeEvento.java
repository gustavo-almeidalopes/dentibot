package br.com.dentibot.plataforma.outbox;

/**
 * Catálogo de eventos do DentiBot (camada 15). Constante em vez de string solta
 * para que renomear um tipo seja um erro de compilação e não um consumidor que
 * simplesmente para de receber, em silêncio.
 */
public final class TiposDeEvento {

    private TiposDeEvento() {
    }

    public static final String CONSULTA_AGENDADA = "consulta.agendada";
    public static final String CONSULTA_CONFIRMADA = "consulta.confirmada";
    public static final String CONSULTA_CANCELADA = "consulta.cancelada";
    public static final String CONSULTA_FALTOU = "consulta.faltou";
    public static final String CONSULTA_REALIZADA = "consulta.realizada";

    public static final String PRONTUARIO_EVOLUCAO_REGISTRADA = "prontuario.evolucao_registrada";
    /**
     * Não é opcional: alimenta a auditoria de LEITURA exigida pela LGPD e pela
     * CFO-226. A linha durável vai para auditoria.eventos na mesma transação da
     * leitura; este evento é o aviso assíncrono para quem quiser reagir.
     */
    public static final String PRONTUARIO_LIDO = "prontuario.lido";
    public static final String ODONTOGRAMA_ATUALIZADO = "odontograma.atualizado";

    public static final String ORCAMENTO_CRIADO = "orcamento.criado";
    public static final String ORCAMENTO_APROVADO = "orcamento.aprovado";

    public static final String COBRANCA_PAGA = "cobranca.paga";
    public static final String COBRANCA_ESTORNADA = "cobranca.estornada";

    public static final String ASSINATURA_CRIADA = "assinatura.criada";
    public static final String ASSINATURA_ATUALIZADA = "assinatura.atualizada";
    public static final String ASSINATURA_CANCELADA = "assinatura.cancelada";
    public static final String ASSINATURA_INADIMPLENTE = "assinatura.inadimplente";
    public static final String LIMITE_PLANO_ATINGIDO = "limite_plano.atingido";
}
