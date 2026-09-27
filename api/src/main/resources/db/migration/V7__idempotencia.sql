-- ─────────────────────────────────────────────────────────────────────────────
-- V7 — Idempotency-Key (camada 13).
--
-- Toque duplo no celular não pode gerar duas cobranças nem dois agendamentos.
-- O Redis é o caminho rápido; esta tabela é a verdade durável, porque Redis
-- nunca é fonte de verdade (camada 11) e "o Redis caiu" não pode virar
-- "cobrou duas vezes".
--
-- A segunda chamada com a MESMA chave e o MESMO corpo devolve a primeira
-- resposta. Com a mesma chave e corpo DIFERENTE devolve 422: é um bug do
-- cliente, e responder 200 com o resultado do outro pedido seria pior.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE plataforma.idempotencia (
    id_clinica   BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    chave        TEXT        NOT NULL,

    endpoint     TEXT        NOT NULL,
    -- SHA-256 do corpo. Distingue "reenvio do mesmo pedido" de "chave reusada
    -- para outro pedido".
    request_hash CHAR(64)    NOT NULL,

    estado       TEXT        NOT NULL DEFAULT 'em_andamento'
                             CHECK (estado IN ('em_andamento','concluida')),
    status_http  SMALLINT    NULL,
    resposta     JSONB       NULL,

    criado_em    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    concluido_em TIMESTAMPTZ NULL,
    expira_em    TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '24 hours',

    PRIMARY KEY (id_clinica, chave),

    -- Concluída sem resposta guardada seria um replay devolvendo vazio.
    CONSTRAINT ck_concluida_tem_resposta
        CHECK (estado <> 'concluida' OR (status_http IS NOT NULL AND concluido_em IS NOT NULL))
);

CREATE INDEX idx_idempotencia_expiracao ON plataforma.idempotencia (expira_em);

ALTER TABLE plataforma.idempotencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE plataforma.idempotencia FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON plataforma.idempotencia
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY worker_limpa ON plataforma.idempotencia
    FOR ALL TO dentibot_app
    USING      (plataforma.modo_worker())
    WITH CHECK (plataforma.modo_worker());
