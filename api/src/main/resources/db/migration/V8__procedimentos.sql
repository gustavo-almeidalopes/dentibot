-- ─────────────────────────────────────────────────────────────────────────────
-- V8 — Catálogo de procedimentos da clínica.
-- Mora em `clinicas` porque é configuração que a clínica mantém, não transação:
-- agenda lê a duração, orçamento lê o preço, convênio lê o código TUSS.
-- Nenhum dos três é dono dele.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE clinicas.procedimentos (
    id_procedimento  BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica       BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),

    nome_servico     VARCHAR(120)  NOT NULL,
    -- Código TUSS da ANS. Opcional: procedimento particular pode não ter.
    codigo_tuss      VARCHAR(20)   NULL,

    -- Invariante 6: dinheiro em NUMERIC. Nunca float — 0.1 + 0.2 em ponto
    -- flutuante não é 0.3, e num orçamento de 12 parcelas isso vira centavo
    -- perdido que ninguém consegue explicar ao paciente.
    preco_particular NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (preco_particular >= 0),
    duracao_minutos  SMALLINT      NOT NULL DEFAULT 30 CHECK (duracao_minutos > 0),

    ativo            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_procedimentos_tenant UNIQUE (id_procedimento, id_clinica)
);

CREATE INDEX idx_procedimentos_clinica
    ON clinicas.procedimentos (id_clinica, nome_servico) WHERE ativo;

CREATE TRIGGER trg_procedimentos_updated_at
    BEFORE UPDATE ON clinicas.procedimentos
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

ALTER TABLE clinicas.procedimentos ENABLE ROW LEVEL SECURITY;
ALTER TABLE clinicas.procedimentos FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON clinicas.procedimentos
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
