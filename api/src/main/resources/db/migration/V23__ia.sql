-- ─────────────────────────────────────────────────────────────────────────────
-- V23 — Gateway de IA (Doc 03-B; ST-61, IA-59, IA-63).
--
-- Os seis controles do Doc 03 que viram banco:
--   · rastreabilidade: toda chamada fica em ia.chamadas — modelo, entrada JÁ
--     redigida, saída, tokens e custo; append-only;
--   · confirmação humana: ia.confirmacoes, uma por chamada, com quem e onde o
--     aceito entrou; append-only;
--   · desligamento por funcionalidade e cota: ia.configuracao, por clínica.
-- A redação de PII antes do envio é da aplicação (GatewayDeIa); o que chega
-- aqui já saiu redigido — é o mesmo texto que o provedor viu.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SCHEMA ia;
GRANT USAGE ON SCHEMA ia TO dentibot_app;
-- O ALTER DEFAULT PRIVILEGES da V1 lista os schemas um a um: tabela em schema
-- novo nasceria sem GRANT nenhum e a app levaria "permission denied" no
-- primeiro request. Declarado ANTES dos CREATE TABLE, para valer para eles.
ALTER DEFAULT PRIVILEGES FOR ROLE dentibot_migrador IN SCHEMA ia
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO dentibot_app;
ALTER DEFAULT PRIVILEGES FOR ROLE dentibot_migrador IN SCHEMA ia
    GRANT USAGE, SELECT ON SEQUENCES TO dentibot_app;

CREATE TABLE ia.chamadas (
    id_chamada        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica        BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_usuario        BIGINT        NULL,
    recurso           TEXT          NOT NULL CHECK (recurso ~ '^[a-z_]{3,40}$'),
    provedor          TEXT          NOT NULL,
    modelo            TEXT          NOT NULL,
    entrada_redigida  TEXT          NOT NULL,
    entrada_sha256    CHAR(64)      NOT NULL,
    saida             TEXT          NULL,
    tokens_entrada    INTEGER       NOT NULL DEFAULT 0 CHECK (tokens_entrada >= 0),
    tokens_saida      INTEGER       NOT NULL DEFAULT 0 CHECK (tokens_saida >= 0),
    -- NUMERIC e não float: custo somado por mês vira cobrança (IA-59).
    custo_usd         NUMERIC(12,6) NOT NULL DEFAULT 0 CHECK (custo_usd >= 0),
    status            TEXT          NOT NULL CHECK (status IN ('sucesso','erro')),
    erro              TEXT          NULL,
    correlacao_id     UUID          NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT uq_chamada_tenant UNIQUE (id_chamada, id_clinica)
);

-- Cota e painel de consumo somam o mês da clínica.
CREATE INDEX idx_ia_chamadas_mes ON ia.chamadas (id_clinica, created_at);

CREATE TABLE ia.confirmacoes (
    id_confirmacao    BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica        BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_chamada        BIGINT        NOT NULL,
    id_usuario        BIGINT        NOT NULL,
    aceita            BOOLEAN       NOT NULL,
    -- Onde o aceito entrou, quando entrou: 'prontuario.evolucao:123'.
    registro          VARCHAR(120)  NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT clock_timestamp(),

    -- Uma decisão por sugestão. Mudar de ideia é outra chamada, não um UPDATE.
    CONSTRAINT uq_confirmacao_unica UNIQUE (id_chamada),
    CONSTRAINT fk_confirmacao_chamada
        FOREIGN KEY (id_chamada, id_clinica) REFERENCES ia.chamadas (id_chamada, id_clinica)
);

CREATE TABLE ia.configuracao (
    id_clinica          BIGINT        PRIMARY KEY REFERENCES clinicas.clinicas(id_clinica),
    recursos_desligados TEXT[]        NOT NULL DEFAULT '{}',
    cota_mensal_usd     NUMERIC(10,2) NOT NULL DEFAULT 20.00 CHECK (cota_mensal_usd >= 0),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Trilha é trilha: a aplicação insere e lê, não reescreve.
REVOKE UPDATE, DELETE ON ia.chamadas, ia.confirmacoes FROM dentibot_app;

ALTER TABLE ia.chamadas     ENABLE ROW LEVEL SECURITY;
ALTER TABLE ia.chamadas     FORCE  ROW LEVEL SECURITY;
ALTER TABLE ia.confirmacoes ENABLE ROW LEVEL SECURITY;
ALTER TABLE ia.confirmacoes FORCE  ROW LEVEL SECURITY;
ALTER TABLE ia.configuracao ENABLE ROW LEVEL SECURITY;
ALTER TABLE ia.configuracao FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON ia.chamadas
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON ia.confirmacoes
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON ia.configuracao
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
