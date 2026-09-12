package br.com.dentibot.prontuario.infrastructure;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.prontuario.EvolucaoResumo;
import br.com.dentibot.prontuario.LancamentoOdontograma;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProntuarioRepositorio {

    private final JdbcClient jdbc;

    public ProntuarioRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<EvolucaoResumo> historico(long idPaciente) {
        return jdbc.sql("""
                        SELECT id_evolucao, id_paciente, id_dentista, id_consulta,
                               descricao_sessao, retifica_evolucao, motivo_retificacao,
                               registrado_em
                        FROM prontuario.evolucoes
                        WHERE id_paciente = :paciente
                        ORDER BY registrado_em DESC
                        """)
                .param("paciente", idPaciente)
                .query((rs, n) -> new EvolucaoResumo(
                        rs.getLong("id_evolucao"),
                        rs.getLong("id_paciente"),
                        rs.getLong("id_dentista"),
                        (Long) rs.getObject("id_consulta"),
                        rs.getString("descricao_sessao"),
                        (Long) rs.getObject("retifica_evolucao"),
                        rs.getString("motivo_retificacao"),
                        rs.getTimestamp("registrado_em").toInstant()))
                .list();
    }

    public long inserirEvolucao(long idPaciente, long idDentista, Long idConsulta,
                                String descricao, Long retifica, String motivo) {
        return jdbc.sql("""
                        INSERT INTO prontuario.evolucoes
                            (id_clinica, id_paciente, id_dentista, id_consulta,
                             descricao_sessao, retifica_evolucao, motivo_retificacao)
                        VALUES (:clinica, :paciente, :dentista, :consulta,
                                :descricao, :retifica, :motivo)
                        RETURNING id_evolucao
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("dentista", idDentista)
                .param("consulta", idConsulta)
                .param("descricao", descricao)
                .param("retifica", retifica)
                .param("motivo", motivo)
                .query(Long.class)
                .single();
    }

    /**
     * Estado ATUAL do odontograma: o último lançamento por (dente, face).
     *
     * <p>{@code DISTINCT ON} é específico do Postgres e resolve isso numa
     * varredura só, apoiado no índice
     * {@code (id_clinica, id_paciente, dente, face, registrado_em DESC)}.
     * A alternativa portável — subconsulta com MAX e auto-join — faria duas
     * passadas para a mesma resposta.
     */
    public List<LancamentoOdontograma> estadoAtual(long idPaciente) {
        return jdbc.sql("""
                        SELECT DISTINCT ON (dente, face)
                               id_lancamento, dente, face, condicao, observacao,
                               id_dentista, registrado_em
                        FROM prontuario.odontograma_lancamentos
                        WHERE id_paciente = :paciente
                        ORDER BY dente, face, registrado_em DESC
                        """)
                .param("paciente", idPaciente)
                .query((rs, n) -> new LancamentoOdontograma(
                        rs.getLong("id_lancamento"),
                        rs.getInt("dente"),
                        rs.getString("face"),
                        rs.getString("condicao"),
                        rs.getString("observacao"),
                        rs.getLong("id_dentista"),
                        rs.getTimestamp("registrado_em").toInstant()))
                .list();
    }

    public long inserirLancamento(long idPaciente, long idDentista, int dente, String face,
                                  String condicao, String observacao) {
        return jdbc.sql("""
                        INSERT INTO prontuario.odontograma_lancamentos
                            (id_clinica, id_paciente, id_dentista, dente, face,
                             condicao, observacao)
                        VALUES (:clinica, :paciente, :dentista, :dente, :face,
                                :condicao, :obs)
                        RETURNING id_lancamento
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("dentista", idDentista)
                .param("dente", dente)
                .param("face", face)
                .param("condicao", condicao)
                .param("obs", observacao)
                .query(Long.class)
                .single();
    }

    /** O dentista responsável por uma evolução — usado para checar o alcance PROPRIOS. */
    public java.util.Optional<Long> dentistaDaEvolucao(long idEvolucao) {
        return jdbc.sql("SELECT id_dentista FROM prontuario.evolucoes WHERE id_evolucao = :id")
                .param("id", idEvolucao)
                .query(Long.class)
                .optional();
    }

    public java.util.Optional<Long> pacienteDaEvolucao(long idEvolucao) {
        return jdbc.sql("SELECT id_paciente FROM prontuario.evolucoes WHERE id_evolucao = :id")
                .param("id", idEvolucao)
                .query(Long.class)
                .optional();
    }
}
