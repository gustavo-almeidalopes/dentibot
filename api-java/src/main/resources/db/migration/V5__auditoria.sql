-- ─────────────────────────────────────────────────────────────────────────────
-- V5 — Auditoria append-only, NO BANCO DE ESCRITA.
--
-- Mudança de desenho em relação à V1, e a mais importante deste porte:
-- lá a auditoria morava em OUTRO banco (`db_leitura.sql`, schema `seguranca`),
-- alimentado por replicação lógica, e o lado de escrita só fazia pg_notify.
-- Três problemas, todos verificados no repositório original:
--   1. pg_notify é fire-and-forget: sem LISTEN ativo, o evento evapora;
--   2. a PUBLICATION nunca chegou a ser criada (0 publicações após rodar o script);
--   3. "mesma transação da leitura" (invariante 9) é fisicamente impossível
--      quando o destino é outro cluster.
-- Aqui a linha de auditoria é um INSERT comum, na mesma transação do que se leu
-- ou escreveu. Se a transação der rollback, a auditoria some junto — que é o
-- comportamento correto: não aconteceu.
--
-- Append-only tem duas camadas: REVOKE tira UPDATE/DELETE da app, e o trigger
-- pega também quem conecta como dono. Uma só não basta.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE auditoria.eventos (
    id_evento      BIGINT      GENERATED ALWAYS AS IDENTITY,
    id_clinica     BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),

    -- Quem. id_usuario nulo = ação de sistema (worker, job, webhook).
    id_usuario     BIGINT      NULL,
    -- Preenchido quando um staff da plataforma agiu sobre esta clínica
    -- (suporte, JIT, impersonation). Ver `acting_as` no contrato de evento.
    staff_papel    TEXT        NULL,

    -- O quê. `leitura` não é decoração: a CFO-226 e a LGPD exigem registro de
    -- QUEM LEU prontuário, não só de quem escreveu.
    acao           TEXT        NOT NULL
                               CHECK (acao IN ('leitura','criacao','alteracao','exclusao',
                                               'login','logout','falha_login','exportacao')),
    recurso        TEXT        NOT NULL,   -- 'prontuario.evolucao', 'paciente', ...
    id_recurso     TEXT        NULL,

    dados_anteriores  JSONB    NULL,
    dados_posteriores JSONB    NULL,

    ip_origem      INET        NULL,
    user_agent     VARCHAR(500) NULL,
    correlacao_id  UUID        NULL,

    ocorrido_em    TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    PRIMARY KEY (id_evento, ocorrido_em)
) PARTITION BY RANGE (ocorrido_em);

COMMENT ON TABLE auditoria.eventos IS
    'Append-only. UPDATE e DELETE bloqueados por REVOKE (app) e por trigger (dono). '
    'Escrita na MESMA transação do fato auditado — invariante 9.';

-- ─── Partições ───────────────────────────────────────────────────────────────
-- ATENÇÃO, e isto custou um vazamento demonstrado durante este porte:
-- RLS da tabela particionada NÃO desce para as partições. Com a política só no
-- pai, `SELECT * FROM auditoria.eventos` filtra por tenant, mas
-- `SELECT * FROM auditoria.eventos_2026` devolve a auditoria de TODAS as
-- clínicas. Cada partição precisa da própria política.
--
-- Por isso criar partição passa por esta função, e não por CREATE TABLE solto:
-- a política vem junto ou não vem partição nenhuma.
CREATE OR REPLACE FUNCTION auditoria.criar_particao_anual(p_ano INT)
    RETURNS VOID
    LANGUAGE plpgsql
AS $$
DECLARE
    v_nome TEXT := format('eventos_%s', p_ano);
BEGIN
    EXECUTE format(
        'CREATE TABLE auditoria.%I PARTITION OF auditoria.eventos
         FOR VALUES FROM (%L) TO (%L)',
        v_nome, format('%s-01-01', p_ano), format('%s-01-01', p_ano + 1));

    EXECUTE format('ALTER TABLE auditoria.%I ENABLE ROW LEVEL SECURITY', v_nome);
    EXECUTE format('ALTER TABLE auditoria.%I FORCE  ROW LEVEL SECURITY', v_nome);
    EXECUTE format(
        'CREATE POLICY tenant_isolation ON auditoria.%I
         FOR ALL TO dentibot_app
         USING (id_clinica = plataforma.clinica_atual())
         WITH CHECK (id_clinica = plataforma.clinica_atual())', v_nome);
    EXECUTE format('REVOKE UPDATE, DELETE, TRUNCATE ON auditoria.%I FROM dentibot_app', v_nome);
END
$$;

DO $$
BEGIN
    PERFORM auditoria.criar_particao_anual(ano)
    FROM generate_series(2026, 2029) AS ano;
END
$$;

-- A DEFAULT existe para que um INSERT fora das faixas declaradas NÃO falhe:
-- perder auditoria porque ninguém criou a partição de 2031 seria trocar um
-- problema de operação por um de conformidade. Ela também leva política própria.
CREATE TABLE auditoria.eventos_default PARTITION OF auditoria.eventos DEFAULT;
ALTER TABLE auditoria.eventos_default ENABLE ROW LEVEL SECURITY;
ALTER TABLE auditoria.eventos_default FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON auditoria.eventos_default
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE INDEX idx_auditoria_clinica_tempo
    ON auditoria.eventos (id_clinica, ocorrido_em DESC);
CREATE INDEX idx_auditoria_recurso
    ON auditoria.eventos (id_clinica, recurso, id_recurso, ocorrido_em DESC);
CREATE INDEX idx_auditoria_usuario
    ON auditoria.eventos (id_clinica, id_usuario, ocorrido_em DESC);
CREATE INDEX idx_auditoria_correlacao
    ON auditoria.eventos (correlacao_id) WHERE correlacao_id IS NOT NULL;

-- ─── Auditoria de ações da plataforma (eixo B, sem tenant) ───────────────────
CREATE TABLE auditoria.eventos_plataforma (
    id_evento     BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_staff      BIGINT      NULL REFERENCES identidade.staff_plataforma(id_staff),
    acao          TEXT        NOT NULL,
    recurso       TEXT        NOT NULL,
    id_recurso    TEXT        NULL,
    detalhes      JSONB       NULL,
    ip_origem     INET        NULL,
    correlacao_id UUID        NULL,
    ocorrido_em   TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_auditoria_plataforma_tempo
    ON auditoria.eventos_plataforma (ocorrido_em DESC);

-- ─── Append-only, camada 1: privilégio ───────────────────────────────────────
-- O ALTER DEFAULT PRIVILEGES da V1 já deu UPDATE/DELETE à app. Aqui tira-se.
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria.eventos             FROM dentibot_app;
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria.eventos_2026        FROM dentibot_app;
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria.eventos_2027        FROM dentibot_app;
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria.eventos_2028        FROM dentibot_app;
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria.eventos_2029        FROM dentibot_app;
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria.eventos_default     FROM dentibot_app;
REVOKE UPDATE, DELETE, TRUNCATE ON auditoria.eventos_plataforma  FROM dentibot_app;

-- ─── Append-only, camada 2: trigger (pega o dono também) ─────────────────────
CREATE OR REPLACE FUNCTION auditoria.bloquear_alteracao()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'auditoria.% é append-only: UPDATE e DELETE são proibidos (LGPD art. 37, CFO-226).',
        TG_TABLE_NAME
        USING ERRCODE = 'insufficient_privilege';
END
$$;

CREATE TRIGGER trg_auditoria_append_only
    BEFORE UPDATE OR DELETE ON auditoria.eventos
    FOR EACH ROW EXECUTE FUNCTION auditoria.bloquear_alteracao();

CREATE TRIGGER trg_auditoria_plataforma_append_only
    BEFORE UPDATE OR DELETE ON auditoria.eventos_plataforma
    FOR EACH ROW EXECUTE FUNCTION auditoria.bloquear_alteracao();

-- ─── RLS ─────────────────────────────────────────────────────────────────────
-- Uma clínica lê a própria auditoria. Staff da plataforma NÃO tem política aqui:
-- a trilha de auditoria cita recurso clínico, e suporte N1 não vê dado clínico.
ALTER TABLE auditoria.eventos ENABLE ROW LEVEL SECURITY;
ALTER TABLE auditoria.eventos FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON auditoria.eventos
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

ALTER TABLE auditoria.eventos_plataforma ENABLE ROW LEVEL SECURITY;
ALTER TABLE auditoria.eventos_plataforma FORCE  ROW LEVEL SECURITY;

CREATE POLICY staff_somente ON auditoria.eventos_plataforma
    FOR ALL TO dentibot_app
    USING      (plataforma.staff_atual() IS NOT NULL)
    WITH CHECK (plataforma.staff_atual() IS NOT NULL);
