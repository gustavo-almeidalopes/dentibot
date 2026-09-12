package br.com.dentibot.agenda.infrastructure;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultaRepositorio {

    /** Linha crua da agenda: sem nome de paciente, que pertence a outro módulo. */
    public record LinhaConsulta(
            long idConsulta, long idPaciente, long idDentista,
            Instant inicioEm, Instant terminoEm, String status) {
    }

    private final JdbcClient jdbc;

    public ConsultaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Nenhuma verificação de conflito de horário aqui, de propósito.
     *
     * <p>A V1 fazia {@code SELECT COUNT(*) ... IF > 0 THEN RAISE}: duas
     * requisições simultâneas contam zero, as duas passam, e o dentista fica com
     * dois pacientes no mesmo horário. A garantia agora é a EXCLUDE constraint
     * {@code ex_dentista_sem_sobreposicao}, que o banco avalia sob qualquer
     * concorrência. A violação chega como DataIntegrityViolationException e vira
     * 409 no TratadorGlobalDeErros.
     */
    public long inserir(long idPaciente, long idDentista, Long idProcedimento,
                        Instant inicioEm, Instant terminoEm, String observacoes) {
        return jdbc.sql("""
                        INSERT INTO agenda.consultas
                            (id_clinica, id_paciente, id_dentista, id_procedimento,
                             inicio_em, termino_em, observacoes)
                        VALUES (:clinica, :paciente, :dentista, :procedimento,
                                :inicio, :termino, :obs)
                        RETURNING id_consulta
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("dentista", idDentista)
                .param("procedimento", idProcedimento)
                .param("inicio", Timestamp.from(inicioEm))
                .param("termino", Timestamp.from(terminoEm))
                .param("obs", observacoes)
                .query(Long.class)
                .single();
    }

    /**
     * @param idDentista quando não nulo, restringe à agenda daquele dentista —
     *                   é como o {@code Alcance.PROPRIOS} do avaliador de
     *                   permissão vira filtro de linha.
     */
    public List<LinhaConsulta> listar(Instant de, Instant ate, Long idDentista) {
        return jdbc.sql("""
                        SELECT id_consulta, id_paciente, id_dentista,
                               inicio_em, termino_em, status
                        FROM agenda.consultas
                        WHERE inicio_em >= :de AND inicio_em < :ate
                          AND (CAST(:dentista AS BIGINT) IS NULL OR id_dentista = :dentista)
                        ORDER BY inicio_em
                        """)
                .param("de", Timestamp.from(de))
                .param("ate", Timestamp.from(ate))
                .param("dentista", idDentista)
                .query((rs, n) -> new LinhaConsulta(
                        rs.getLong("id_consulta"),
                        rs.getLong("id_paciente"),
                        rs.getLong("id_dentista"),
                        rs.getTimestamp("inicio_em").toInstant(),
                        rs.getTimestamp("termino_em").toInstant(),
                        rs.getString("status")))
                .list();
    }

    public Optional<LinhaConsulta> buscar(long idConsulta) {
        return jdbc.sql("""
                        SELECT id_consulta, id_paciente, id_dentista,
                               inicio_em, termino_em, status
                        FROM agenda.consultas WHERE id_consulta = :id
                        """)
                .param("id", idConsulta)
                .query((rs, n) -> new LinhaConsulta(
                        rs.getLong("id_consulta"),
                        rs.getLong("id_paciente"),
                        rs.getLong("id_dentista"),
                        rs.getTimestamp("inicio_em").toInstant(),
                        rs.getTimestamp("termino_em").toInstant(),
                        rs.getString("status")))
                .optional();
    }

    /**
     * Transição de status com o estado ANTERIOR na cláusula WHERE.
     *
     * <p>Sem {@code AND status = :de}, dois cliques em "confirmar" e "cancelar"
     * chegando juntos deixariam o último a escrever vencer, e não há como saber
     * qual foi. Com ele, a segunda transição afeta zero linhas e o serviço
     * responde 409.
     *
     * @return quantas linhas mudaram: 1 se a transição valeu, 0 se o estado já
     *         era outro
     */
    public int transicionar(long idConsulta, String de, String para, String motivo) {
        return jdbc.sql("""
                        UPDATE agenda.consultas
                        SET status = :para,
                            motivo_cancelamento = COALESCE(:motivo, motivo_cancelamento),
                            termino_real_em = CASE WHEN :para = 'realizada' THEN NOW()
                                                   ELSE termino_real_em END
                        WHERE id_consulta = :id AND status = :de
                        """)
                .param("id", idConsulta)
                .param("de", de)
                .param("para", para)
                .param("motivo", motivo)
                .update();
    }

    public int transicionarDeQualquerUm(long idConsulta, List<String> deQualquerUm, String para,
                                        String motivo) {
        return jdbc.sql("""
                        UPDATE agenda.consultas
                        SET status = :para,
                            motivo_cancelamento = COALESCE(:motivo, motivo_cancelamento)
                        WHERE id_consulta = :id AND status IN (:de)
                        """)
                .param("id", idConsulta)
                .param("de", deQualquerUm)
                .param("para", para)
                .param("motivo", motivo)
                .update();
    }

    public boolean existeConsultaEntre(long idPaciente, long idDentista) {
        Integer achou = jdbc.sql("""
                        SELECT 1 FROM agenda.consultas
                        WHERE id_paciente = :paciente AND id_dentista = :dentista
                        LIMIT 1
                        """)
                .param("paciente", idPaciente)
                .param("dentista", idDentista)
                .query(Integer.class)
                .optional().orElse(null);
        return achou != null;
    }
}
