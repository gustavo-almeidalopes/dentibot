package br.com.dentibot.identidade.infrastructure;

import br.com.dentibot.identidade.PessoaResumo;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PessoaRepositorio {

    private final JdbcClient jdbc;

    public PessoaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * {@code id_clinica} vem do contexto, nunca do corpo da requisição
     * (invariante 5). O RLS ainda confere no WITH CHECK: se o contexto e o valor
     * divergirem, o INSERT é recusado pelo banco.
     */
    public long inserir(String nomeCompleto, String cpf, String telefoneCelular, String email) {
        return jdbc.sql("""
                        INSERT INTO identidade.pessoas
                            (id_clinica, nome_completo, cpf, telefone_celular, email)
                        VALUES (:clinica, :nome, :cpf, :telefone, :email)
                        RETURNING id_pessoa
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("nome", nomeCompleto)
                .param("cpf", cpf)
                .param("telefone", telefoneCelular)
                .param("email", email)
                .query(Long.class)
                .single();
    }

    public List<PessoaResumo> buscarResumos(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        // IN (:ids) e não = ANY(:ids): o NamedParameterJdbcTemplate expande a
        // coleção em placeholders sozinho, sem precisar de createArrayOf.
        return jdbc.sql("""
                        SELECT id_pessoa, nome_completo, telefone_celular
                        FROM identidade.pessoas
                        WHERE id_pessoa IN (:ids) AND deleted_at IS NULL
                        """)
                .param("ids", List.copyOf(ids))
                .query((rs, n) -> new PessoaResumo(
                        rs.getLong("id_pessoa"),
                        rs.getString("nome_completo"),
                        rs.getString("telefone_celular")))
                .list();
    }
}
