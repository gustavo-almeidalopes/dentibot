package br.com.dentibot.comunicacao.infrastructure;

import br.com.dentibot.comunicacao.Espera;
import br.com.dentibot.comunicacao.Mensagem;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ComunicacaoRepositorio {

    /** Os tipos que a clínica manda por iniciativa própria — os que contam no limite. */
    private static final String AUTOMATICOS =
            "('lembrete','confirmacao','reforco','oferta_vaga','pos_procedimento')";

    private final JdbcClient jdbc;

    public ComunicacaoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ─── Canal ───────────────────────────────────────────────────────────────

    public record Canal(String phoneNumberId, boolean ativo, List<String> tiposDesligados) {

        public boolean liga(String tipo) {
            return ativo && !tiposDesligados.contains(tipo);
        }
    }

    public Optional<Canal> canal() {
        return jdbc.sql("""
                        SELECT phone_number_id, ativo, tipos_desligados
                        FROM comunicacao.canais
                        """)
                .query((rs, n) -> new Canal(rs.getString("phone_number_id"),
                        rs.getBoolean("ativo"), texto(rs.getArray("tipos_desligados"))))
                .optional();
    }

    public void salvarCanal(String phoneNumberId, boolean ativo, List<String> desligados) {
        jdbc.sql("""
                        INSERT INTO comunicacao.canais
                            (id_clinica, phone_number_id, ativo, tipos_desligados)
                        VALUES (:clinica, :numero, :ativo, CAST(:desligados AS TEXT[]))
                        ON CONFLICT (id_clinica) DO UPDATE
                        SET phone_number_id = EXCLUDED.phone_number_id,
                            ativo = EXCLUDED.ativo,
                            tipos_desligados = EXCLUDED.tipos_desligados,
                            updated_at = now()
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("numero", phoneNumberId)
                .param("ativo", ativo)
                .param("desligados", "{" + String.join(",", desligados) + "}")
                .update();
    }

    /** Pela função SECURITY DEFINER da V24: o webhook ainda não tem tenant. */
    public Optional<Long> clinicaDoCanal(String phoneNumberId) {
        return jdbc.sql("""
                        SELECT id FROM (SELECT comunicacao.clinica_do_canal(:numero) AS id) c
                        WHERE id IS NOT NULL
                        """)
                .param("numero", phoneNumberId)
                .query(Long.class)
                .optional();
    }

    // ─── Programação ─────────────────────────────────────────────────────────

    public long agendar(Long idPaciente, String telefone, String tipo, Long idConsulta,
                        String texto, Instant enviarEm) {
        return jdbc.sql("""
                        INSERT INTO comunicacao.mensagens
                            (id_clinica, id_paciente, telefone, sentido, tipo, id_consulta,
                             texto, status, enviar_em)
                        VALUES (:clinica, :paciente, :telefone, 'saida', :tipo, :consulta,
                                :texto, 'agendada', :quando)
                        RETURNING id_mensagem
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("telefone", telefone)
                .param("tipo", tipo)
                .param("consulta", idConsulta)
                .param("texto", texto)
                .param("quando", Timestamp.from(enviarEm))
                .query(Long.class)
                .single();
    }

    public int contarDaConsulta(long idConsulta, String tipo) {
        return jdbc.sql("""
                        SELECT count(*) FROM comunicacao.mensagens
                        WHERE id_consulta = :consulta AND tipo = :tipo
                        """)
                .param("consulta", idConsulta)
                .param("tipo", tipo)
                .query(Integer.class)
                .single();
    }

    public int cancelarPendentesDaConsulta(long idConsulta, String motivo) {
        return jdbc.sql("""
                        UPDATE comunicacao.mensagens SET status = 'cancelada', motivo = :motivo
                        WHERE id_consulta = :consulta AND status = 'agendada'
                        """)
                .param("consulta", idConsulta)
                .param("motivo", motivo)
                .update();
    }

    // ─── Despacho ────────────────────────────────────────────────────────────

    public record Pendente(long idMensagem, long idClinica) {
    }

    /**
     * Em modo worker: trava o que venceu, de todas as clínicas. SKIP LOCKED
     * deixa duas instâncias da API dividirem a fila sem mandar a mesma
     * mensagem duas vezes.
     */
    public List<Pendente> travarVencidas(int limite) {
        return jdbc.sql("""
                        UPDATE comunicacao.mensagens SET status = 'enviando'
                        WHERE id_mensagem IN (
                            SELECT id_mensagem FROM comunicacao.mensagens
                            WHERE status = 'agendada' AND enviar_em <= now()
                            ORDER BY enviar_em
                            LIMIT :limite
                            FOR UPDATE SKIP LOCKED)
                        RETURNING id_mensagem, id_clinica
                        """)
                .param("limite", limite)
                .query((rs, n) -> new Pendente(rs.getLong("id_mensagem"), rs.getLong("id_clinica")))
                .list();
    }

    /**
     * Travada há mais de 15 minutos: a instância caiu no meio do envio. Vira
     * falha e não reenvio — não dá para saber se a Meta recebeu, e mensagem
     * repetida ao paciente é pior que uma a menos.
     */
    public int falharTravadas() {
        return jdbc.sql("""
                        UPDATE comunicacao.mensagens
                        SET status = 'falhou', motivo = 'envio interrompido'
                        WHERE status = 'enviando' AND enviar_em < now() - interval '15 minutes'
                        """)
                .update();
    }

    public Optional<Mensagem> buscar(long idMensagem) {
        return jdbc.sql("SELECT * FROM comunicacao.mensagens WHERE id_mensagem = :id")
                .param("id", idMensagem)
                .query(ComunicacaoRepositorio::mapear)
                .optional();
    }

    /** O paciente escreveu nas últimas 24 horas? Então texto livre passa. */
    public boolean janelaAberta(String telefone) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM comunicacao.mensagens
                                       WHERE telefone = :telefone AND sentido = 'entrada'
                                         AND created_at > now() - interval '24 hours')
                        """)
                .param("telefone", telefone)
                .query(Boolean.class)
                .single();
    }

    public int automaticasNasUltimas24h(String telefone) {
        return jdbc.sql("""
                        SELECT count(*) FROM comunicacao.mensagens
                        WHERE telefone = :telefone AND tipo IN %s
                          AND status IN ('enviada','entregue','lida')
                          AND enviada_em > now() - interval '24 hours'
                        """.formatted(AUTOMATICOS))
                .param("telefone", telefone)
                .query(Integer.class)
                .single();
    }

    public void marcarEnviada(long idMensagem, String idExterno) {
        jdbc.sql("""
                        UPDATE comunicacao.mensagens
                        SET status = 'enviada', enviada_em = now(), id_externo = NULLIF(:externo, '')
                        WHERE id_mensagem = :id
                        """)
                .param("id", idMensagem)
                .param("externo", idExterno)
                .update();
    }

    public void encerrar(long idMensagem, String status, String motivo) {
        jdbc.sql("""
                        UPDATE comunicacao.mensagens SET status = :status, motivo = :motivo
                        WHERE id_mensagem = :id
                        """)
                .param("id", idMensagem)
                .param("status", status)
                .param("motivo", motivo == null ? null : motivo.substring(0, Math.min(motivo.length(), 300)))
                .update();
    }

    // ─── Webhook ─────────────────────────────────────────────────────────────

    /**
     * Status só anda para frente: a Meta não garante ordem, e um "entregue"
     * atrasado não pode apagar o "lida" — que é a prova da orientação (IA-38).
     */
    public void atualizarStatus(String idExterno, String status, Instant em) {
        jdbc.sql("""
                        UPDATE comunicacao.mensagens
                        SET status = :status,
                            lida_em = CASE WHEN :status = 'lida' THEN :em ELSE lida_em END
                        WHERE id_externo = :externo
                          AND ((:status = 'falhou' AND status IN ('enviando','enviada'))
                               OR array_position(ARRAY['enviando','enviada','entregue','lida'], status)
                                  < array_position(ARRAY['enviando','enviada','entregue','lida'],
                                                   CAST(:status AS TEXT)))
                        """)
                .param("externo", idExterno)
                .param("status", status)
                .param("em", Timestamp.from(em))
                .update();
    }

    /** Vazio = a Meta reenviou; esta mensagem já foi tratada. */
    public Optional<Long> registrarEntrada(Long idPaciente, String telefone, String texto,
                                           String idExterno) {
        return jdbc.sql("""
                        INSERT INTO comunicacao.mensagens
                            (id_clinica, id_paciente, telefone, sentido, tipo, texto, status,
                             id_externo)
                        VALUES (:clinica, :paciente, :telefone, 'entrada', 'recebida', :texto,
                                'recebida', :externo)
                        ON CONFLICT (id_externo) DO NOTHING
                        RETURNING id_mensagem
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("telefone", telefone)
                .param("texto", texto)
                .param("externo", idExterno)
                .query(Long.class)
                .optional();
    }

    /** A última coisa que a clínica perguntou a este número — é o que dá sentido ao "1". */
    public Optional<Mensagem> ultimaPergunta(String telefone) {
        return jdbc.sql("""
                        SELECT * FROM comunicacao.mensagens
                        WHERE telefone = :telefone AND sentido = 'saida' AND tipo <> 'resposta'
                          AND status IN ('enviada','entregue','lida')
                          AND enviada_em > now() - interval '72 hours'
                        ORDER BY enviada_em DESC
                        LIMIT 1
                        """)
                .param("telefone", telefone)
                .query(ComunicacaoRepositorio::mapear)
                .optional();
    }

    public void classificar(long idMensagem, String classificacao, String escalada) {
        jdbc.sql("""
                        UPDATE comunicacao.mensagens SET classificacao = :classificacao,
                                                         escalada = :escalada
                        WHERE id_mensagem = :id
                        """)
                .param("id", idMensagem)
                .param("classificacao", classificacao)
                .param("escalada", escalada)
                .update();
    }

    public void registrarCiencia(long idMensagem) {
        jdbc.sql("""
                        UPDATE comunicacao.mensagens
                        SET lida_em = COALESCE(lida_em, now()), status = 'lida'
                        WHERE id_mensagem = :id
                        """)
                .param("id", idMensagem)
                .update();
    }

    // ─── Tela ────────────────────────────────────────────────────────────────

    /** Urgente primeiro; dentro de cada nível, a mais antiga primeiro. */
    public List<Mensagem> escaladas() {
        return jdbc.sql("""
                        SELECT * FROM comunicacao.mensagens
                        WHERE escalada IS NOT NULL AND resolvida_em IS NULL
                        ORDER BY (escalada = 'urgente') DESC, created_at
                        LIMIT 200
                        """)
                .query(ComunicacaoRepositorio::mapear)
                .list();
    }

    public int resolver(long idMensagem, long idUsuario) {
        return jdbc.sql("""
                        UPDATE comunicacao.mensagens SET resolvida_em = now(), resolvida_por = :usuario
                        WHERE id_mensagem = :id AND escalada IS NOT NULL AND resolvida_em IS NULL
                        """)
                .param("id", idMensagem)
                .param("usuario", idUsuario)
                .update();
    }

    public List<Mensagem> doPaciente(long idPaciente) {
        return jdbc.sql("""
                        SELECT * FROM comunicacao.mensagens
                        WHERE id_paciente = :paciente
                        ORDER BY created_at DESC
                        LIMIT 200
                        """)
                .param("paciente", idPaciente)
                .query(ComunicacaoRepositorio::mapear)
                .list();
    }

    // ─── Lista de espera e ofertas (IA-18) ───────────────────────────────────

    public List<Espera> listaDeEspera() {
        return jdbc.sql("""
                        SELECT id_espera, id_paciente, id_dentista, periodo, urgencia, observacao,
                               created_at
                        FROM comunicacao.lista_espera
                        WHERE atendida_em IS NULL
                        ORDER BY urgencia DESC, created_at
                        """)
                .query(ComunicacaoRepositorio::mapearEspera)
                .list();
    }

    public long entrarNaEspera(long idPaciente, Long idDentista, String periodo, int urgencia,
                               String observacao) {
        return jdbc.sql("""
                        INSERT INTO comunicacao.lista_espera
                            (id_clinica, id_paciente, id_dentista, periodo, urgencia, observacao)
                        VALUES (:clinica, :paciente, :dentista, :periodo, :urgencia, :observacao)
                        RETURNING id_espera
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("paciente", idPaciente)
                .param("dentista", idDentista)
                .param("periodo", periodo)
                .param("urgencia", urgencia)
                .param("observacao", observacao)
                .query(Long.class)
                .single();
    }

    public int encerrarEspera(long idEspera) {
        return jdbc.sql("""
                        UPDATE comunicacao.lista_espera SET atendida_em = now()
                        WHERE id_espera = :id AND atendida_em IS NULL
                        """)
                .param("id", idEspera)
                .update();
    }

    /** Quem espera por esta vaga, na ordem de aderência: urgência, depois antiguidade. */
    public List<Espera> candidatos(long idDentista, String periodo, long excetoPaciente, int limite) {
        return jdbc.sql("""
                        SELECT id_espera, id_paciente, id_dentista, periodo, urgencia, observacao,
                               created_at
                        FROM comunicacao.lista_espera
                        WHERE atendida_em IS NULL
                          AND (id_dentista IS NULL OR id_dentista = :dentista)
                          AND periodo IN ('qualquer', :periodo)
                          AND id_paciente <> :exceto
                        ORDER BY urgencia DESC, created_at
                        LIMIT :limite
                        """)
                .param("dentista", idDentista)
                .param("periodo", periodo)
                .param("exceto", excetoPaciente)
                .param("limite", limite)
                .query(ComunicacaoRepositorio::mapearEspera)
                .list();
    }

    public record Oferta(long idOferta, long idEspera, long idPaciente, long idDentista,
                         Instant inicioEm, Instant terminoEm) {
    }

    public long criarOferta(long idEspera, long idPaciente, long idDentista, Instant inicio,
                            Instant termino, Instant expira) {
        return jdbc.sql("""
                        INSERT INTO comunicacao.ofertas
                            (id_clinica, id_espera, id_paciente, id_dentista, inicio_em,
                             termino_em, expira_em)
                        VALUES (:clinica, :espera, :paciente, :dentista, :inicio, :termino, :expira)
                        RETURNING id_oferta
                        """)
                .param("clinica", ContextoAtual.clinicaObrigatoria())
                .param("espera", idEspera)
                .param("paciente", idPaciente)
                .param("dentista", idDentista)
                .param("inicio", Timestamp.from(inicio))
                .param("termino", Timestamp.from(termino))
                .param("expira", Timestamp.from(expira))
                .query(Long.class)
                .single();
    }

    public Optional<Oferta> ofertaAberta(List<Long> idsPaciente) {
        if (idsPaciente.isEmpty()) {
            return Optional.empty();
        }
        return jdbc.sql("""
                        SELECT id_oferta, id_espera, id_paciente, id_dentista, inicio_em, termino_em
                        FROM comunicacao.ofertas
                        WHERE id_paciente IN (:pacientes) AND status = 'aberta' AND expira_em > now()
                        ORDER BY created_at DESC
                        LIMIT 1
                        """)
                .param("pacientes", idsPaciente)
                .query((rs, n) -> new Oferta(rs.getLong("id_oferta"), rs.getLong("id_espera"),
                        rs.getLong("id_paciente"), rs.getLong("id_dentista"),
                        rs.getTimestamp("inicio_em").toInstant(),
                        rs.getTimestamp("termino_em").toInstant()))
                .optional();
    }

    /** Zero linhas = outro paciente chegou antes, ou a oferta venceu. */
    public int fecharOferta(long idOferta, String status) {
        return jdbc.sql("""
                        UPDATE comunicacao.ofertas SET status = :status
                        WHERE id_oferta = :id AND status = 'aberta'
                        """)
                .param("id", idOferta)
                .param("status", status)
                .update();
    }

    public void expirarConcorrentes(Oferta aceita) {
        jdbc.sql("""
                        UPDATE comunicacao.ofertas SET status = 'expirada'
                        WHERE inicio_em = :inicio AND id_dentista = :dentista AND status = 'aberta'
                          AND id_oferta <> :id
                        """)
                .param("inicio", Timestamp.from(aceita.inicioEm()))
                .param("dentista", aceita.idDentista())
                .param("id", aceita.idOferta())
                .update();
    }

    // ─── Mapeamento ──────────────────────────────────────────────────────────

    private static Mensagem mapear(ResultSet rs, int n) throws SQLException {
        long paciente = rs.getLong("id_paciente");
        Long idPaciente = rs.wasNull() ? null : paciente;
        long consulta = rs.getLong("id_consulta");
        Long idConsulta = rs.wasNull() ? null : consulta;
        return new Mensagem(rs.getLong("id_mensagem"), idPaciente, rs.getString("telefone"),
                rs.getString("sentido"), rs.getString("tipo"), idConsulta, rs.getString("texto"),
                rs.getString("status"), instante(rs, "enviar_em"), instante(rs, "enviada_em"),
                instante(rs, "lida_em"), rs.getString("motivo"), rs.getString("classificacao"),
                rs.getString("escalada"), instante(rs, "resolvida_em"),
                instante(rs, "created_at"));
    }

    private static Espera mapearEspera(ResultSet rs, int n) throws SQLException {
        long dentista = rs.getLong("id_dentista");
        Long idDentista = rs.wasNull() ? null : dentista;
        return new Espera(rs.getLong("id_espera"), rs.getLong("id_paciente"), null, idDentista, rs.getString("periodo"), rs.getInt("urgencia"),
                rs.getString("observacao"), instante(rs, "created_at"));
    }

    private static Instant instante(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toInstant();
    }

    private static List<String> texto(Array array) throws SQLException {
        return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
    }
}
