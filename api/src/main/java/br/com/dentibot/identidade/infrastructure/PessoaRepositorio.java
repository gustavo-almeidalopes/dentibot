package br.com.dentibot.identidade.infrastructure;

import br.com.dentibot.identidade.DadosPessoais;
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
    public long inserir(DadosPessoais d) {
        return jdbc.sql("""
                        INSERT INTO identidade.pessoas
                            (id_clinica, nome_completo, cpf, rg, data_nascimento,
                             telefone_celular, email, profissao, responsavel_legal,
                             cep, logradouro, numero, complemento, bairro, cidade, uf)
                        VALUES (:clinica, :nome, :cpf, :rg, :nascimento,
                                :telefone, :email, :profissao, :responsavel,
                                :cep, :logradouro, :numero, :complemento, :bairro, :cidade, :uf)
                        RETURNING id_pessoa
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("nome", d.nomeCompleto())
                .param("cpf", d.cpf())
                .param("rg", d.rg())
                .param("nascimento", d.dataNascimento())
                .param("telefone", d.telefoneCelular())
                .param("email", d.email())
                .param("profissao", d.profissao())
                .param("responsavel", d.responsavelLegal())
                .param("cep", d.cep())
                .param("logradouro", d.logradouro())
                .param("numero", d.numero())
                .param("complemento", d.complemento())
                .param("bairro", d.bairro())
                .param("cidade", d.cidade())
                .param("uf", d.uf())
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
