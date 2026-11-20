package br.com.dentibot.ia.infrastructure;

import br.com.dentibot.ia.ConsumoDeIa;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class IaRepositorio {

    public record Configuracao(List<String> recursosDesligados, BigDecimal cotaMensalUsd) {
        public static final Configuracao PADRAO = new Configuracao(List.of(), new BigDecimal("20.00"));
    }

    private final JdbcClient jdbc;

    public IaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long inserirChamada(Long idUsuario, String recurso, String provedor, String modelo,
                               String entradaRedigida, String entradaSha256, String saida,
                               int tokensEntrada, int tokensSaida, BigDecimal custoUsd,
                               String status, String erro, UUID correlacao) {
        return jdbc.sql("""
                        INSERT INTO ia.chamadas
                            (id_clinica, id_usuario, recurso, provedor, modelo, entrada_redigida,
                             entrada_sha256, saida, tokens_entrada, tokens_saida, custo_usd,
                             status, erro, correlacao_id)
                        VALUES (:clinica, :usuario, :recurso, :provedor, :modelo, :entrada,
                                :sha, :saida, :tin, :tout, :custo, :status, :erro, :correlacao)
                        RETURNING id_chamada
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("usuario", idUsuario)
                .param("recurso", recurso)
                .param("provedor", provedor)
                .param("modelo", modelo)
                .param("entrada", entradaRedigida)
                .param("sha", entradaSha256)
                .param("saida", saida)
                .param("tin", tokensEntrada)
                .param("tout", tokensSaida)
                .param("custo", custoUsd)
                .param("status", status)
                .param("erro", erro)
                .param("correlacao", correlacao)
                .query(Long.class)
                .single();
    }

    public void inserirConfirmacao(long idChamada, long idUsuario, boolean aceita, String registro) {
        jdbc.sql("""
                        INSERT INTO ia.confirmacoes (id_clinica, id_chamada, id_usuario, aceita, registro)
                        VALUES (:clinica, :chamada, :usuario, :aceita, :registro)
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("chamada", idChamada)
                .param("usuario", idUsuario)
                .param("aceita", aceita)
                .param("registro", registro)
                .update();
    }

    public BigDecimal gastoDesde(Instant inicio) {
        return jdbc.sql("SELECT COALESCE(sum(custo_usd), 0) FROM ia.chamadas WHERE created_at >= :inicio")
                .param("inicio", Timestamp.from(inicio))
                .query(BigDecimal.class)
                .single();
    }

    public Configuracao configuracao() {
        Optional<Configuracao> linha = jdbc.sql("""
                        SELECT recursos_desligados, cota_mensal_usd FROM ia.configuracao
                        """)
                .query((rs, n) -> {
                    Array desligados = rs.getArray("recursos_desligados");
                    return new Configuracao(
                            Arrays.asList((String[]) desligados.getArray()),
                            rs.getBigDecimal("cota_mensal_usd"));
                })
                .optional();
        return linha.orElse(Configuracao.PADRAO);
    }

    public void salvarConfiguracao(List<String> recursosDesligados, BigDecimal cotaMensalUsd) {
        jdbc.sql("""
                        INSERT INTO ia.configuracao (id_clinica, recursos_desligados, cota_mensal_usd)
                        VALUES (:clinica, :desligados, :cota)
                        ON CONFLICT (id_clinica) DO UPDATE
                           SET recursos_desligados = EXCLUDED.recursos_desligados,
                               cota_mensal_usd = EXCLUDED.cota_mensal_usd,
                               updated_at = now()
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("desligados", recursosDesligados.toArray(String[]::new))
                .param("cota", cotaMensalUsd)
                .update();
    }

    /** IA-59: por recurso, no intervalo — chamadas, erros, custo e o que virou registro. */
    public List<ConsumoDeIa> consumo(Instant de, Instant ate) {
        return jdbc.sql("""
                        SELECT c.recurso,
                               count(*)                                      AS chamadas,
                               count(*) FILTER (WHERE c.status = 'erro')     AS erros,
                               COALESCE(sum(c.custo_usd), 0)                 AS custo,
                               count(f.id_confirmacao) FILTER (WHERE f.aceita)     AS aceitas,
                               count(f.id_confirmacao) FILTER (WHERE NOT f.aceita) AS descartadas
                        FROM ia.chamadas c
                        LEFT JOIN ia.confirmacoes f ON f.id_chamada = c.id_chamada
                        WHERE c.created_at >= :de AND c.created_at < :ate
                        GROUP BY c.recurso
                        ORDER BY c.recurso
                        """)
                .param("de", Timestamp.from(de))
                .param("ate", Timestamp.from(ate))
                .query((rs, n) -> new ConsumoDeIa(rs.getString("recurso"), rs.getLong("chamadas"),
                        rs.getLong("erros"), rs.getBigDecimal("custo"), rs.getLong("aceitas"),
                        rs.getLong("descartadas")))
                .list();
    }
}
