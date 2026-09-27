-- ─────────────────────────────────────────────────────────────────────────────
-- V10 — Módulo agenda.
--
-- A correção mais importante deste porte está aqui. A V1 impedia horário duplo
-- com um COUNT(*) dentro de uma procedure:
--
--     SELECT COUNT(*) INTO v_conflito FROM agenda WHERE ... && ...;
--     IF v_conflito > 0 THEN RAISE EXCEPTION ...
--
-- Isso é um check-then-act: duas requisições simultâneas contam zero, as duas
-- passam, e o dentista fica com dois pacientes no mesmo horário. Não aparece em
-- teste manual e aparece na recepção movimentada de segunda-feira. A garantia
-- aqui é uma EXCLUDE constraint — o banco recusa a segunda linha, com qualquer
-- nível de concorrência.
--
-- (A V1 ainda comparava com tsrange() sobre colunas TIMESTAMPTZ: o cast
--  implícito descarta o fuso e a comparação erra na virada do horário de verão.)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE agenda.consultas (
    id_consulta      BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica       BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente      BIGINT      NOT NULL,
    id_dentista      BIGINT      NOT NULL,
    id_procedimento  BIGINT      NULL,

    -- Invariante 7: TIMESTAMPTZ. O fuso da clínica (clinicas.timezone) é usado
    -- para RENDERIZAR, nunca para armazenar.
    inicio_em        TIMESTAMPTZ NOT NULL,
    termino_em       TIMESTAMPTZ NOT NULL,
    termino_real_em  TIMESTAMPTZ NULL,

    -- Coluna gerada: existe só para a EXCLUDE constraint poder trabalhar sobre
    -- um range, mantendo inicio_em/termino_em como colunas normais para o JPA.
    -- '[)' — fim exclusivo: consulta que termina 10:00 não conflita com a que
    -- começa 10:00.
    periodo          TSTZRANGE   GENERATED ALWAYS AS (tstzrange(inicio_em, termino_em, '[)')) STORED,

    status           TEXT        NOT NULL DEFAULT 'agendada'
                                 CHECK (status IN ('agendada','confirmada','em_atendimento',
                                                   'realizada','cancelada','faltou')),
    motivo_cancelamento VARCHAR(255) NULL,
    observacoes      TEXT        NULL,

    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_periodo_valido CHECK (termino_em > inicio_em),

    CONSTRAINT fk_consulta_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT fk_consulta_dentista
        FOREIGN KEY (id_dentista, id_clinica)
        REFERENCES identidade.dentistas (id_dentista, id_clinica),
    CONSTRAINT fk_consulta_procedimento
        FOREIGN KEY (id_procedimento, id_clinica)
        REFERENCES clinicas.procedimentos (id_procedimento, id_clinica),

    CONSTRAINT uq_consultas_tenant UNIQUE (id_consulta, id_clinica),

    -- O coração da agenda. Parcial: consulta cancelada ou falta libera o horário.
    CONSTRAINT ex_dentista_sem_sobreposicao
        EXCLUDE USING gist (
            id_dentista WITH =,
            periodo     WITH &&
        ) WHERE (status IN ('agendada','confirmada','em_atendimento','realizada'))
);

CREATE INDEX idx_consultas_dentista_dia
    ON agenda.consultas (id_clinica, id_dentista, inicio_em);
CREATE INDEX idx_consultas_paciente
    ON agenda.consultas (id_clinica, id_paciente, inicio_em DESC);
CREATE INDEX idx_consultas_status_dia
    ON agenda.consultas (id_clinica, status, inicio_em);

CREATE TRIGGER trg_consultas_updated_at
    BEFORE UPDATE ON agenda.consultas
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Bloqueios de agenda ─────────────────────────────────────────────────────
-- Férias, almoço, folga semanal, feriado da clínica.
-- Invariante 7: recorrência em RRULE (RFC 5545), NUNCA uma linha por ocorrência.
-- Gerar 52 linhas para "toda terça o dentista não atende" significa que mudar o
-- horário exige reescrever 52 linhas, e que a agenda de 2030 ainda não existe.
CREATE TABLE agenda.bloqueios (
    id_bloqueio  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica   BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    -- NULL = bloqueio de toda a clínica (feriado).
    id_dentista  BIGINT      NULL,

    inicio_em    TIMESTAMPTZ NOT NULL,
    termino_em   TIMESTAMPTZ NOT NULL,

    -- RRULE da RFC 5545, ex.: 'FREQ=WEEKLY;BYDAY=TU'. NULL = bloqueio único.
    -- inicio_em/termino_em descrevem a PRIMEIRA ocorrência; a regra expande as
    -- demais, e a expansão acontece na aplicação, em memória.
    rrule        TEXT        NULL,
    -- Até quando a regra vale. NULL com rrule preenchido = para sempre.
    repete_ate   TIMESTAMPTZ NULL,

    motivo       VARCHAR(200) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_bloqueio_periodo CHECK (termino_em > inicio_em),
    CONSTRAINT ck_repete_exige_rrule CHECK (repete_ate IS NULL OR rrule IS NOT NULL),
    CONSTRAINT fk_bloqueio_dentista
        FOREIGN KEY (id_dentista, id_clinica)
        REFERENCES identidade.dentistas (id_dentista, id_clinica)
);

CREATE INDEX idx_bloqueios_dentista
    ON agenda.bloqueios (id_clinica, id_dentista, inicio_em);

-- ─── RLS ─────────────────────────────────────────────────────────────────────
-- Só tenancy. A regra "dentista vê apenas a própria agenda" é RBAC e vive no
-- PermissionEvaluator (invariante 3) — a V1 a codificava aqui em policy
-- RESTRICTIVE, e o efeito colateral foi negar tudo para todo mundo, porque
-- política RESTRICTIVE sem uma PERMISSIVE ao lado nega por definição.
ALTER TABLE agenda.consultas ENABLE ROW LEVEL SECURITY;
ALTER TABLE agenda.consultas FORCE  ROW LEVEL SECURITY;
ALTER TABLE agenda.bloqueios ENABLE ROW LEVEL SECURITY;
ALTER TABLE agenda.bloqueios FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON agenda.consultas
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON agenda.bloqueios
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
