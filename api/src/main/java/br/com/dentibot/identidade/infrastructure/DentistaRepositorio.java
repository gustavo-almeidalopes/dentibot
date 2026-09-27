package br.com.dentibot.identidade.infrastructure;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DentistaRepositorio {

    /** Linha crua, sem o nome: o nome é de {@code identidade.pessoas}. */
    public record LinhaDentista(long idDentista, long idPessoa, Long idUsuario,
                                String croNumero, String croUf, String especialidade,
                                String status) {
    }

    private final JdbcClient jdbc;

    public DentistaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> idPorUsuario(long idUsuario) {
        return jdbc.sql("""
                        SELECT id_dentista FROM identidade.dentistas
                        WHERE id_usuario = :usuario AND status = 'ativo'
                        """)
                .param("usuario", idUsuario)
                .query(Long.class)
                .optional();
    }

    public List<LinhaDentista> listarAtivos() {
        return jdbc.sql("""
                        SELECT id_dentista, id_pessoa, id_usuario,
                               cro_numero, cro_uf, especialidade, status
                        FROM identidade.dentistas
                        WHERE status = 'ativo'
                        ORDER BY id_dentista
                        """)
                .query(DentistaRepositorio::mapear)
                .list();
    }

    /** Todos, para casar com a listagem de equipe (que mostra inativos também). */
    public List<LinhaDentista> listar() {
        return jdbc.sql("""
                        SELECT id_dentista, id_pessoa, id_usuario,
                               cro_numero, cro_uf, especialidade, status
                        FROM identidade.dentistas
                        ORDER BY id_dentista
                        """)
                .query(DentistaRepositorio::mapear)
                .list();
    }

    public long inserir(long idPessoa, Long idUsuario, String croNumero, String croUf,
                        String especialidade) {
        return jdbc.sql("""
                        INSERT INTO identidade.dentistas
                            (id_clinica, id_pessoa, id_usuario, cro_numero, cro_uf, especialidade)
                        VALUES (:clinica, :pessoa, :usuario, :cro, :uf, :especialidade)
                        RETURNING id_dentista
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("pessoa", idPessoa)
                .param("usuario", idUsuario)
                .param("cro", croNumero)
                .param("uf", croUf)
                .param("especialidade", especialidade)
                .query(Long.class)
                .single();
    }

    /**
     * O dentista acompanha o status do usuário: desativar alguém na tela de
     * equipe e deixá-lo selecionável na agenda seria o tipo de meia-desativação
     * que não aparece em teste nenhum e reaparece como consulta agendada para
     * quem não trabalha mais ali.
     */
    public int atualizarStatusPorUsuario(long idUsuario, String status) {
        return jdbc.sql("""
                        UPDATE identidade.dentistas
                        SET status = :status
                        WHERE id_usuario = :usuario
                        """)
                .param("usuario", idUsuario)
                .param("status", status)
                .update();
    }

    private static LinhaDentista mapear(java.sql.ResultSet rs, int linha)
            throws java.sql.SQLException {
        // wasNull() fala sobre a ÚLTIMA coluna lida, então o teste precisa vir
        // colado ao getLong e antes de qualquer outra leitura. Dentro da lista
        // de argumentos do construtor ele se referiria a id_dentista, e todo
        // dentista sem login apareceria com o id_usuario errado em vez de nulo.
        long bruto = rs.getLong("id_usuario");
        Long idUsuario = rs.wasNull() ? null : bruto;
        return new LinhaDentista(
                rs.getLong("id_dentista"),
                rs.getLong("id_pessoa"),
                idUsuario,
                rs.getString("cro_numero"),
                rs.getString("cro_uf"),
                rs.getString("especialidade"),
                rs.getString("status"));
    }
}
