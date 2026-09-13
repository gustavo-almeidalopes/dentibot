package br.com.dentibot.pacientes.infrastructure;

import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PacienteRepositorio {

    /**
     * Linha de paciente SEM o nome: o nome mora em {@code identidade.pessoas},
     * que é outro módulo. Quem monta o resumo é o serviço, chamando a porta de
     * identidade — nunca um JOIN entre schemas de módulos diferentes (camada 7).
     */
    public record LinhaPaciente(long idPaciente, long idPessoa, String status) {
    }

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public PacienteRepositorio(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long inserir(long idPessoa, Long idPlano, String numeroCarteirinha,
                        NovoPaciente.Anamnese anamnese) {
        return jdbc.sql("""
                        INSERT INTO pacientes.pacientes
                            (id_clinica, id_pessoa, id_plano, num_carteirinha, anamnese)
                        VALUES (:clinica, :pessoa, :plano, :carteirinha,
                                CAST(:anamnese AS JSONB))
                        RETURNING id_paciente
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("pessoa", idPessoa)
                .param("plano", idPlano)
                .param("carteirinha", numeroCarteirinha)
                .param("anamnese", comoJson(anamnese))
                .query(Long.class)
                .single();
    }

    /**
     * Paginação por keyset, não OFFSET: com OFFSET o banco lê e descarta as N
     * primeiras linhas a cada página, e a página 50 custa 50 vezes a primeira.
     */
    public List<LinhaPaciente> listar(int limite, long apos) {
        return jdbc.sql("""
                        SELECT id_paciente, id_pessoa, status
                        FROM pacientes.pacientes
                        WHERE deleted_at IS NULL AND id_paciente > :apos
                        ORDER BY id_paciente
                        LIMIT :limite
                        """)
                .param("apos", apos)
                .param("limite", limite)
                .query((rs, n) -> new LinhaPaciente(
                        rs.getLong("id_paciente"),
                        rs.getLong("id_pessoa"),
                        rs.getString("status")))
                .list();
    }

    public List<LinhaPaciente> buscarPorIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                        SELECT id_paciente, id_pessoa, status
                        FROM pacientes.pacientes
                        WHERE id_paciente IN (:ids) AND deleted_at IS NULL
                        """)
                .param("ids", ids)
                .query((rs, n) -> new LinhaPaciente(
                        rs.getLong("id_paciente"),
                        rs.getLong("id_pessoa"),
                        rs.getString("status")))
                .list();
    }

    public int contar() {
        return jdbc.sql("SELECT count(*) FROM pacientes.pacientes WHERE deleted_at IS NULL")
                .query(Integer.class)
                .single();
    }

    public boolean existe(long idPaciente) {
        Integer achou = jdbc.sql("""
                        SELECT 1 FROM pacientes.pacientes
                        WHERE id_paciente = :id AND deleted_at IS NULL
                        """)
                .param("id", idPaciente)
                .query(Integer.class)
                .optional().orElse(null);
        return achou != null;
    }

    /* Jackson 3 não declara exceção checada aqui, então não há try/catch a
       escrever. O null explícito é que importa: sem ele o mapper devolveria a
       string "null", que o JSONB aceita como valor JSON válido — uma anamnese
       que existe e diz nada, em vez de coluna vazia. */
    private String comoJson(Object valor) {
        return valor == null ? null : json.writeValueAsString(valor);
    }
}
