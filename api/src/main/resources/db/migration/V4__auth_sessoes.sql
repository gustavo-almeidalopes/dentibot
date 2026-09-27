-- ─────────────────────────────────────────────────────────────────────────────
-- V4 — Sessões, refresh rotativo e blocklist de token.
-- Porte de database/02-auth-extensions.sql da V1 (o único arquivo SQL daquele
-- repositório que executa sem erro), com três mudanças:
--   · id_clinica adicionado — as três tabelas não tinham, e sem ele não há RLS;
--   · `familia` para detectar reuso de refresh token rotacionado;
--   · sessão de staff em tabela própria, porque staff não tem tenant.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE identidade.sessoes (
    id_sessao    BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica   BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_usuario   BIGINT       NOT NULL,

    -- jti do refresh token CORRENTE. Rotaciona a cada uso.
    jti          UUID         NOT NULL UNIQUE,

    -- Todas as rotações de um mesmo login compartilham a família. Se um refresh
    -- já rotacionado reaparece, é sinal de token roubado: revoga-se a família
    -- inteira, não só aquele token. Sem isto, o atacante que copiou o refresh
    -- continua renovando para sempre em paralelo com o usuário legítimo.
    familia      UUID         NOT NULL,

    refresh_hash VARCHAR(255) NOT NULL,
    user_agent   VARCHAR(300) NULL,
    ip_address   INET         NULL,

    criada_em    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expira_em    TIMESTAMPTZ  NOT NULL,
    revogada_em  TIMESTAMPTZ  NULL,
    motivo_revogacao TEXT     NULL,

    CONSTRAINT fk_sessao_usuario
        FOREIGN KEY (id_usuario, id_clinica)
        REFERENCES identidade.usuarios (id_usuario, id_clinica)
);

CREATE INDEX idx_sessoes_usuario  ON identidade.sessoes (id_clinica, id_usuario)
    WHERE revogada_em IS NULL;
CREATE INDEX idx_sessoes_familia  ON identidade.sessoes (familia);
CREATE INDEX idx_sessoes_expiracao ON identidade.sessoes (expira_em)
    WHERE revogada_em IS NULL;

-- ─── Sessões de staff da plataforma ──────────────────────────────────────────
CREATE TABLE identidade.sessoes_staff (
    id_sessao    BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_staff     BIGINT       NOT NULL REFERENCES identidade.staff_plataforma(id_staff),
    jti          UUID         NOT NULL UNIQUE,
    familia      UUID         NOT NULL,
    refresh_hash VARCHAR(255) NOT NULL,
    user_agent   VARCHAR(300) NULL,
    ip_address   INET         NULL,
    criada_em    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expira_em    TIMESTAMPTZ  NOT NULL,
    revogada_em  TIMESTAMPTZ  NULL,
    motivo_revogacao TEXT     NULL
);

CREATE INDEX idx_sessoes_staff_staff ON identidade.sessoes_staff (id_staff)
    WHERE revogada_em IS NULL;

-- ─── Códigos de recuperação de 2FA (uso único) ───────────────────────────────
CREATE TABLE identidade.codigos_recuperacao (
    id_codigo   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica  BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_usuario  BIGINT       NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    usado_em    TIMESTAMPTZ  NULL,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_codigo_usuario
        FOREIGN KEY (id_usuario, id_clinica)
        REFERENCES identidade.usuarios (id_usuario, id_clinica)
);

CREATE INDEX idx_recuperacao_usuario
    ON identidade.codigos_recuperacao (id_clinica, id_usuario)
    WHERE usado_em IS NULL;

-- ─── Blocklist de access token revogado ──────────────────────────────────────
-- Mora em `plataforma`, não em `identidade`, de propósito: é a única tabela do
-- sistema sem tenant, e isso precisa ser óbvio na leitura do schema em vez de
-- virar a exceção esquecida na lista de RLS. Guarda só um UUID e uma data —
-- nenhum dado pessoal, nada que pertença a uma clínica.
-- Em runtime é espelhada no Redis com TTL = exp do token; esta é a fonte durável,
-- porque Redis nunca é fonte de verdade (camada 11).
CREATE TABLE plataforma.tokens_revogados (
    jti         UUID        PRIMARY KEY,
    revogado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Depois disto o token expirou sozinho e a linha pode sair: a blocklist
    -- só precisa cobrir a janela entre a revogação e a expiração natural.
    expira_em   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tokens_revogados_expiracao ON plataforma.tokens_revogados (expira_em);

-- ─── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE identidade.sessoes              ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidade.sessoes              FORCE  ROW LEVEL SECURITY;
ALTER TABLE identidade.codigos_recuperacao  ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidade.codigos_recuperacao  FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON identidade.sessoes
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON identidade.codigos_recuperacao
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

ALTER TABLE identidade.sessoes_staff ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidade.sessoes_staff FORCE  ROW LEVEL SECURITY;

CREATE POLICY staff_somente ON identidade.sessoes_staff
    FOR ALL TO dentibot_app
    USING      (plataforma.staff_atual() IS NOT NULL)
    WITH CHECK (plataforma.staff_atual() IS NOT NULL);
