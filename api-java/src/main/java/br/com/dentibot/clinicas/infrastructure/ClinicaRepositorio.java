package br.com.dentibot.clinicas.infrastructure;

import br.com.dentibot.clinicas.domain.Clinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.clinicas.domain.StatusClinica;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ClinicaRepositorio {

    private final JdbcClient jdbc;

    public ClinicaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Cria o tenant. Note que não há {@code INSERT INTO clinicas.clinicas} aqui:
     * a app não tem esse privilégio. A única porta é a função
     * {@code clinicas.provisionar()}, que roda como um role próprio — ver
     * V11__onboarding.sql.
     */
    public long provisionar(String cnpj, String razaoSocial, String nomeFantasia,
                            ZoneId timezone, Plano plano) {
        return jdbc.sql("""
                        SELECT clinicas.provisionar(
                            CAST(:cnpj AS VARCHAR(14)),
                            CAST(:razao AS VARCHAR(144)),
                            CAST(:fantasia AS VARCHAR(60)),
                            :timezone,
                            :plano)
                        """)
                .param("cnpj", cnpj)
                .param("razao", razaoSocial)
                .param("fantasia", nomeFantasia)
                .param("timezone", timezone.getId())
                .param("plano", plano.valorBanco())
                .query(Long.class)
                .single();
    }

    public void criarConfiguracoesPadrao(long idClinica) {
        jdbc.sql("INSERT INTO clinicas.configuracoes (id_clinica) VALUES (:id)")
                .param("id", idClinica)
                .update();
    }

    /**
     * Sem {@code WHERE id_clinica = ?}. Não é esquecimento: o RLS já restringe a
     * linha visível ao tenant da transação, e repetir o filtro na aplicação
     * criaria um segundo lugar que pode divergir. O {@code WHERE} está no banco
     * (invariante 5).
     */
    public Optional<Clinica> buscarAtual() {
        return jdbc.sql("""
                        SELECT id_clinica, cnpj, razao_social, nome_fantasia,
                               timezone, plano, status, created_at
                        FROM clinicas.clinicas
                        WHERE deleted_at IS NULL
                        """)
                .query((rs, n) -> new Clinica(
                        rs.getLong("id_clinica"),
                        rs.getString("cnpj"),
                        rs.getString("razao_social"),
                        rs.getString("nome_fantasia"),
                        ZoneId.of(rs.getString("timezone")),
                        Plano.de(rs.getString("plano")),
                        StatusClinica.de(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant()))
                .optional();
    }
}
