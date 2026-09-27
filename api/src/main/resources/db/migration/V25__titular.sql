-- ─────────────────────────────────────────────────────────────────────────────
-- V25 — O titular no controle (Doc 03-E; IA-52, IA-53).
--
--   · preferencias: finalidade por finalidade, com histórico. O estado atual é
--     a linha mais recente; mudar de ideia é outra linha, nunca um UPDATE —
--     "revogou em tal dia" precisa continuar demonstrável (art. 8º §5º);
--   · acessos_titular: o link que a clínica entrega ao paciente para ver o
--     extrato de acesso e mexer nas preferências. Não existe conta de
--     paciente: o link É a credencial, então só o hash fica no banco, expira,
--     e a clínica revoga;
--   · oposicao: o paciente contesta um acesso do extrato (art. 18 §2º).
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE lgpd.preferencias (
    id_preferencia BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente    BIGINT      NOT NULL,
    finalidade     TEXT        NOT NULL CHECK (finalidade IN ('whatsapp','imagem_ensino',
                                                              'dados_anonimizados')),
    permitido      BOOLEAN     NOT NULL,
    -- 'titular' pelo link, 'whatsapp' quando respondeu PARAR, 'clinica' pela
    -- recepção (com id_usuario).
    origem         TEXT        NOT NULL CHECK (origem IN ('titular','whatsapp','clinica')),
    id_usuario     BIGINT      NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT fk_preferencia_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica)
);

CREATE INDEX idx_preferencias_atual
    ON lgpd.preferencias (id_clinica, id_paciente, finalidade, id_preferencia DESC);

CREATE TABLE lgpd.acessos_titular (
    id_acesso     BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica    BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente   BIGINT      NOT NULL,
    token_sha256  CHAR(64)    NOT NULL UNIQUE,
    expira_em     TIMESTAMPTZ NOT NULL,
    criado_por    BIGINT      NOT NULL,
    revogado_em   TIMESTAMPTZ NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_acesso_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica)
);

ALTER TABLE lgpd.solicitacoes_titular
    DROP CONSTRAINT solicitacoes_titular_direito_check,
    ADD CONSTRAINT solicitacoes_titular_direito_check
        CHECK (direito IN ('confirmacao','acesso','correcao','anonimizacao','portabilidade',
                           'eliminacao','revogacao_consentimento','oposicao')),
    -- O que o titular contesta, nas palavras dele e com o evento do extrato.
    ADD COLUMN detalhe VARCHAR(500) NULL;

REVOKE UPDATE, DELETE ON lgpd.preferencias FROM dentibot_app;
REVOKE DELETE ON lgpd.acessos_titular FROM dentibot_app;

ALTER TABLE lgpd.preferencias    ENABLE ROW LEVEL SECURITY;
ALTER TABLE lgpd.preferencias    FORCE  ROW LEVEL SECURITY;
ALTER TABLE lgpd.acessos_titular ENABLE ROW LEVEL SECURITY;
ALTER TABLE lgpd.acessos_titular FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON lgpd.preferencias
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON lgpd.acessos_titular
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

-- ─── Link → paciente ─────────────────────────────────────────────────────────
-- Travessia sem tenant, pelo mesmo desenho da V18: sujeito próprio, política
-- explícita, privilégio por coluna. Recebe o HASH — o token em claro não chega
-- ao banco nem como parâmetro.
GRANT USAGE, CREATE ON SCHEMA lgpd TO dentibot_autenticador;
GRANT SELECT (id_clinica, id_paciente, token_sha256, expira_em, revogado_em)
    ON lgpd.acessos_titular TO dentibot_autenticador;

CREATE POLICY resolucao_do_titular ON lgpd.acessos_titular
    FOR SELECT TO dentibot_autenticador
    USING (true);

CREATE OR REPLACE FUNCTION lgpd.titular_do_token(p_token_sha256 TEXT)
    RETURNS TABLE (id_clinica BIGINT, id_paciente BIGINT)
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = lgpd, pg_catalog
AS $$
    SELECT a.id_clinica, a.id_paciente FROM lgpd.acessos_titular a
    WHERE a.token_sha256 = p_token_sha256
      AND a.revogado_em IS NULL
      AND a.expira_em > now()
$$;

ALTER FUNCTION lgpd.titular_do_token(TEXT) OWNER TO dentibot_autenticador;
REVOKE EXECUTE ON FUNCTION lgpd.titular_do_token(TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION lgpd.titular_do_token(TEXT) TO dentibot_app;

REVOKE CREATE ON SCHEMA lgpd FROM dentibot_autenticador;
