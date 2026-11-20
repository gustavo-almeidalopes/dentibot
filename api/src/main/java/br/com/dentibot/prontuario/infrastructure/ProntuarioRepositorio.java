package br.com.dentibot.prontuario.infrastructure;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.prontuario.AnexoResumo;
import br.com.dentibot.prontuario.EvolucaoResumo;
import br.com.dentibot.prontuario.LancamentoOdontograma;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // ─── Anexos (ST-41) ──────────────────────────────────────────────────────

    public long inserirAnexo(long idPaciente, Long idConsulta, String tipo, String chave,
                             String nomeArquivo, String contentType, long tamanho,
                             String sha256, Long enviadoPor) {
        return jdbc.sql("""
                        INSERT INTO prontuario.anexos
                            (id_clinica, id_paciente, id_consulta, tipo, chave_objeto,
                             nome_arquivo, content_type, tamanho_bytes, hash_sha256, enviado_por)
                        VALUES (:clinica, :paciente, :consulta, :tipo, :chave,
                                :nome, :contentType, :tamanho, :hash, :enviadoPor)
                        RETURNING id_anexo
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("consulta", idConsulta)
                .param("tipo", tipo)
                .param("chave", chave)
                .param("nome", nomeArquivo)
                .param("contentType", contentType)
                .param("tamanho", tamanho)
                .param("hash", sha256)
                .param("enviadoPor", enviadoPor)
                .query(Long.class)
                .single();
    }

    public List<AnexoResumo> anexos(long idPaciente) {
        return jdbc.sql("""
                        SELECT id_anexo, id_consulta, tipo, nome_arquivo, content_type,
                               tamanho_bytes, hash_sha256, created_at
                        FROM prontuario.anexos
                        WHERE id_paciente = :paciente AND deleted_at IS NULL
                        ORDER BY created_at DESC
                        """)
                .param("paciente", idPaciente)
                .query((rs, n) -> {
                    long consulta = rs.getLong("id_consulta");
                    return new AnexoResumo(
                            rs.getLong("id_anexo"),
                            rs.wasNull() ? null : consulta,
                            rs.getString("tipo"),
                            rs.getString("nome_arquivo"),
                            rs.getString("content_type"),
                            rs.getLong("tamanho_bytes"),
                            rs.getString("hash_sha256"),
                            rs.getTimestamp("created_at").toInstant());
                })
                .list();
    }

    /** Chave e nome do anexo — só se ele for deste paciente. */
    public java.util.Optional<String[]> chaveDoAnexo(long idPaciente, long idAnexo) {
        return jdbc.sql("""
                        SELECT chave_objeto, nome_arquivo FROM prontuario.anexos
                        WHERE id_anexo = :anexo AND id_paciente = :paciente AND deleted_at IS NULL
                        """)
                .param("anexo", idAnexo)
                .param("paciente", idPaciente)
                .query((rs, n) -> new String[] {rs.getString("chave_objeto"), rs.getString("nome_arquivo")})
                .optional();
    }

    public Set<Long> consultasComEvolucao(Collection<Long> idsConsulta) {
        return new HashSet<>(jdbc.sql("""
                        SELECT DISTINCT id_consulta FROM prontuario.evolucoes
                        WHERE id_consulta IN (:ids)
                        """)
                .param("ids", List.copyOf(idsConsulta))
                .query(Long.class)
                .list());
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
