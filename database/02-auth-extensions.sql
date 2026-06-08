-- ──────────────────────────────────────────────────────────────────────────────
-- Extensões de autenticação específicas da aplicação (Fases 1–3).
-- Mantidas separadas do schema canônico (db_escrita.sql) para isolar as
-- necessidades do fluxo de login do app (2FA por e-mail/SMS, sessões/refresh,
-- blocklist de JTI, códigos de recuperação) sem poluir o modelo de domínio.
-- Roda APÓS 01-schema.sql, como superusuário, no banco da aplicação.
-- ──────────────────────────────────────────────────────────────────────────────

-- ─── Método de 2FA preferido + PIN transitório (e-mail/SMS) ─────────────────────
ALTER TABLE usuarios_sistema
    ADD COLUMN IF NOT EXISTS metodo_2fa            VARCHAR(10)  NOT NULL DEFAULT 'email', -- email | sms | app
    ADD COLUMN IF NOT EXISTS codigo_2fa_hash       VARCHAR(255) NULL,                     -- HASH do PIN (nunca em texto)
    ADD COLUMN IF NOT EXISTS codigo_2fa_expira_em  TIMESTAMPTZ  NULL,
    ADD COLUMN IF NOT EXISTS codigo_2fa_tentativas SMALLINT     NOT NULL DEFAULT 0;

-- O mfa_secret (TOTP) já existe em usuarios_sistema; será cifrado em repouso na Fase 3.

-- ─── Sessões / refresh tokens rotativos com JTI (Fase 2) ────────────────────────
CREATE TABLE IF NOT EXISTS sessoes_usuario (
    id_sessao    BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario   INT          NOT NULL REFERENCES usuarios_sistema(id_usuario),
    id_clinica   INT          NOT NULL REFERENCES clinicas(id_clinica),
    jti          UUID         NOT NULL UNIQUE,            -- jti do refresh token corrente
    refresh_hash VARCHAR(255) NOT NULL,                   -- hash do refresh token (nunca em texto)
    user_agent   VARCHAR(300) NULL,
    ip_address   VARCHAR(45)  NULL,
    criada_em    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expira_em    TIMESTAMPTZ  NOT NULL,
    revogada     BOOLEAN      NOT NULL DEFAULT FALSE,
    revogada_em  TIMESTAMPTZ  NULL
);
CREATE INDEX IF NOT EXISTS idx_sessoes_usuario ON sessoes_usuario (id_usuario, revogada);

-- ─── Blocklist de JTI de access tokens revogados (revogação imediata, Fase 2) ──
-- Em runtime também espelhada no Redis com TTL = exp do token; a tabela é a
-- fonte durável.
CREATE TABLE IF NOT EXISTS tokens_revogados (
    jti         UUID        PRIMARY KEY,
    id_usuario  INT         NULL REFERENCES usuarios_sistema(id_usuario),
    revogado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_em   TIMESTAMPTZ NOT NULL                       -- limpeza após expiração natural
);

-- ─── Códigos de recuperação de 2FA (uso único, com hash) (Fase 3) ──────────────
CREATE TABLE IF NOT EXISTS codigos_recuperacao (
    id_codigo   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario  INT          NOT NULL REFERENCES usuarios_sistema(id_usuario),
    codigo_hash VARCHAR(255) NOT NULL,
    usado       BOOLEAN      NOT NULL DEFAULT FALSE,
    usado_em    TIMESTAMPTZ  NULL,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_recuperacao_usuario ON codigos_recuperacao (id_usuario, usado);

-- ─── Permissões para o role da aplicação (privilégio mínimo) ───────────────────
GRANT SELECT, INSERT, UPDATE, DELETE ON sessoes_usuario     TO dentibot_app;
GRANT SELECT, INSERT, DELETE         ON tokens_revogados     TO dentibot_app;
GRANT SELECT, INSERT, UPDATE         ON codigos_recuperacao  TO dentibot_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO dentibot_app;

-- ─── Resolver clínica pelo login_email (RLS-safe) ──────────────────────────────
-- login_email é globalmente único; esta função SECURITY DEFINER resolve o
-- id_clinica antes da autenticação (quando ainda não há contexto RLS), sem
-- expor dados de outras clínicas. Retorna NULL se o e-mail não existir
-- (o chamador responde sempre com "credenciais inválidas", evitando enumeração).
CREATE OR REPLACE FUNCTION fn_resolver_clinica_por_email(p_login_email TEXT)
RETURNS INTEGER LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_clinica INTEGER;
BEGIN
    SELECT id_clinica INTO v_clinica
    FROM usuarios_sistema WHERE login_email = lower(p_login_email);
    RETURN v_clinica;
END;
$$;

-- Resolve a clínica a partir do id_usuario (usado no fluxo de verify-2fa, quando
-- o cliente só devolve o user_id). Também SECURITY DEFINER (bypassa RLS).
CREATE OR REPLACE FUNCTION fn_resolver_clinica_por_usuario(p_id_usuario INTEGER)
RETURNS INTEGER LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_clinica INTEGER;
BEGIN
    SELECT id_clinica INTO v_clinica
    FROM usuarios_sistema WHERE id_usuario = p_id_usuario;
    RETURN v_clinica;
END;
$$;

-- ─── Grants de EXECUTE para o role da aplicação (privilégio mínimo) ────────────
GRANT EXECUTE ON FUNCTION fn_resolver_clinica_por_email(TEXT)                   TO dentibot_app;
GRANT EXECUTE ON FUNCTION fn_resolver_clinica_por_usuario(INTEGER)              TO dentibot_app;
GRANT EXECUTE ON FUNCTION sp_autenticar_usuario(TEXT, TEXT, TEXT, TEXT, INTEGER) TO dentibot_app;

-- ─── Limpeza periódica de tokens revogados expirados (pg_cron) ─────────────────
SELECT cron.schedule('limpar-tokens-revogados', '*/15 * * * *',
    'DELETE FROM tokens_revogados WHERE expira_em < NOW()');
