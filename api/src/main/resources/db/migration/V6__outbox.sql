-- ─────────────────────────────────────────────────────────────────────────────
-- V6 — Outbox transacional.
--
-- Por que não um broker: publicar no broker e commitar a transação não são
-- atômicos. Se o publish acontece e a transação faz rollback, sai evento de
-- consulta que não existe e o paciente recebe confirmação de um horário que
-- ninguém marcou. Se o publish falha depois do commit, a consulta existe e
-- ninguém é avisado. As duas falhas são invisíveis em desenvolvimento e certas
-- em produção. Gravar o evento na MESMA transação da escrita elimina as duas.
--
-- A entrega é at-least-once: o consumidor PRECISA deduplicar por event_id.
-- ─────────────────────────────────────────────────────────────────────────────

-- O worker do outbox varre todos os tenants — não tem um `app.clinica` para
-- chamar de seu. Em vez de abrir a tabela, ele se identifica por GUC própria.
-- Mesma confiança do `app.clinica`: a aplicação define o contexto, o RLS garante
-- que a AUSÊNCIA de contexto nega tudo, e que um endpoint de negócio com bug
-- não enxerga a fila de outra clínica.
CREATE OR REPLACE FUNCTION plataforma.modo_worker()
    RETURNS BOOLEAN
    LANGUAGE sql
    STABLE
    SET search_path = pg_catalog
AS $$
    SELECT COALESCE(NULLIF(current_setting('app.worker', true), ''), 'false') = 'true'
$$;

GRANT EXECUTE ON FUNCTION plataforma.modo_worker() TO dentibot_app;

CREATE TABLE plataforma.outbox (
    -- Contrato de evento versionado, camada 16.
    event_id       UUID        PRIMARY KEY,
    event_type     TEXT        NOT NULL,
    version        SMALLINT    NOT NULL DEFAULT 1,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    clinica_id     BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    correlation_id UUID        NULL,

    -- { "user_id": ..., "acting_as": null }. `acting_as` existe desde já para a
    -- impersonation da Fase 4 não exigir migração de contrato depois.
    actor          JSONB       NOT NULL DEFAULT '{}'::JSONB,
    data           JSONB       NOT NULL DEFAULT '{}'::JSONB,

    -- Estado de publicação.
    publicado_em         TIMESTAMPTZ NULL,
    tentativas           SMALLINT    NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ultimo_erro          TEXT        NULL
);

-- Índice parcial: o worker só enxerga o que falta publicar, e o índice encolhe
-- sozinho conforme a fila drena. Sem o WHERE, o índice cresce para sempre com
-- linhas já publicadas que nunca mais serão lidas por ele.
CREATE INDEX idx_outbox_pendente
    ON plataforma.outbox (proxima_tentativa_em, event_id)
    WHERE publicado_em IS NULL;

CREATE INDEX idx_outbox_clinica_tipo
    ON plataforma.outbox (clinica_id, event_type, occurred_at DESC);

COMMENT ON TABLE plataforma.outbox IS
    'Gravado na mesma transação da escrita de negócio (invariante 8). Lido por '
    'worker com SELECT ... FOR UPDATE SKIP LOCKED. Entrega at-least-once: o '
    'consumidor deduplica por event_id ou o e-mail sai duas vezes.';

-- ─── Deduplicação do lado do consumidor ──────────────────────────────────────
-- O outbox garante que o evento SAI pelo menos uma vez; esta tabela garante que
-- ele é PROCESSADO uma vez só. Sem ela, at-least-once vira cobrança duplicada.
CREATE TABLE plataforma.eventos_processados (
    event_id      UUID        NOT NULL,
    consumidor    TEXT        NOT NULL,
    processado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumidor)
);

-- ─── Webhooks recebidos (Stripe, PSP) ────────────────────────────────────────
-- O mesmo problema pelo outro lado: provedor de pagamento reenvia webhook.
-- INSERT ... ON CONFLICT DO NOTHING é o que torna o reenvio inofensivo.
CREATE TABLE plataforma.webhooks_recebidos (
    id_webhook     BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provedor       TEXT        NOT NULL CHECK (provedor IN ('stripe','asaas')),
    -- Id do evento no provedor. É a chave da idempotência do recebimento.
    evento_externo TEXT        NOT NULL,
    tipo           TEXT        NOT NULL,
    payload        JSONB       NOT NULL,
    recebido_em    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processado_em  TIMESTAMPTZ NULL,
    erro           TEXT        NULL,

    CONSTRAINT uq_webhook_provedor_evento UNIQUE (provedor, evento_externo)
);

CREATE INDEX idx_webhooks_pendentes
    ON plataforma.webhooks_recebidos (recebido_em)
    WHERE processado_em IS NULL;

-- ─── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE plataforma.outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE plataforma.outbox FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON plataforma.outbox
    FOR ALL TO dentibot_app
    USING      (clinica_id = plataforma.clinica_atual())
    WITH CHECK (clinica_id = plataforma.clinica_atual());

CREATE POLICY worker_publica ON plataforma.outbox
    FOR ALL TO dentibot_app
    USING      (plataforma.modo_worker())
    WITH CHECK (plataforma.modo_worker());

-- eventos_processados e webhooks_recebidos são infraestrutura do worker:
-- sem tenant, só acessíveis em modo worker.
ALTER TABLE plataforma.eventos_processados ENABLE ROW LEVEL SECURITY;
ALTER TABLE plataforma.eventos_processados FORCE  ROW LEVEL SECURITY;

CREATE POLICY worker_somente ON plataforma.eventos_processados
    FOR ALL TO dentibot_app
    USING      (plataforma.modo_worker())
    WITH CHECK (plataforma.modo_worker());

ALTER TABLE plataforma.webhooks_recebidos ENABLE ROW LEVEL SECURITY;
ALTER TABLE plataforma.webhooks_recebidos FORCE  ROW LEVEL SECURITY;

CREATE POLICY worker_somente ON plataforma.webhooks_recebidos
    FOR ALL TO dentibot_app
    USING      (plataforma.modo_worker())
    WITH CHECK (plataforma.modo_worker());
