-- ─────────────────────────────────────────────────────────────────────────────
-- V9 — Módulo pacientes e convênios.
--
-- Correções em relação à V1: planos_convenio e precos_convenio ganharam
-- id_clinica (não tinham, e por isso ficavam fora de qualquer isolamento), e a
-- vigência de preço virou daterange com EXCLUDE — antes duas tarifas podiam
-- valer ao mesmo tempo para o mesmo plano e procedimento, e qual delas o
-- sistema usava dependia da ordem das linhas.
-- ─────────────────────────────────────────────────────────────────────────────

-- ─── Convênios ───────────────────────────────────────────────────────────────
CREATE TABLE pacientes.convenios (
    id_convenio          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica           BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    nome_operadora       VARCHAR(100) NOT NULL,
    registro_ans         VARCHAR(20)  NULL,
    prazo_reembolso_dias SMALLINT     NOT NULL DEFAULT 30 CHECK (prazo_reembolso_dias >= 0),
    ativo                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_convenios_tenant UNIQUE (id_convenio, id_clinica)
);

CREATE TABLE pacientes.planos_convenio (
    id_plano    BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica  BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_convenio BIGINT       NOT NULL,
    nome_plano  VARCHAR(100) NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_plano_convenio
        FOREIGN KEY (id_convenio, id_clinica)
        REFERENCES pacientes.convenios (id_convenio, id_clinica),
    CONSTRAINT uq_planos_tenant UNIQUE (id_plano, id_clinica)
);

CREATE TABLE pacientes.precos_convenio (
    id_tarifa       BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_plano        BIGINT        NOT NULL,
    id_procedimento BIGINT        NOT NULL,

    valor_repasse   NUMERIC(14,2) NOT NULL CHECK (valor_repasse >= 0),

    -- Vigência como intervalo, não como par de colunas soltas: permite ao banco
    -- recusar duas tarifas sobrepostas para o mesmo plano e procedimento.
    -- Limite superior aberto = tarifa vigente sem data de fim.
    vigencia        DATERANGE     NOT NULL,

    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tarifa_plano
        FOREIGN KEY (id_plano, id_clinica)
        REFERENCES pacientes.planos_convenio (id_plano, id_clinica),
    CONSTRAINT fk_tarifa_procedimento
        FOREIGN KEY (id_procedimento, id_clinica)
        REFERENCES clinicas.procedimentos (id_procedimento, id_clinica),

    CONSTRAINT ex_tarifa_sem_sobreposicao
        EXCLUDE USING gist (
            id_plano        WITH =,
            id_procedimento WITH =,
            vigencia        WITH &&
        )
);

CREATE INDEX idx_precos_convenio_lookup
    ON pacientes.precos_convenio (id_clinica, id_plano, id_procedimento);

-- ─── Pacientes ───────────────────────────────────────────────────────────────
CREATE TABLE pacientes.pacientes (
    id_paciente     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),

    -- O dado pessoal mora em identidade.pessoas; aqui fica o que é específico
    -- de ser paciente desta clínica.
    id_pessoa       BIGINT       NOT NULL,

    id_plano        BIGINT       NULL,
    num_carteirinha VARCHAR(60)  NULL,

    -- Número do prontuário como a clínica o conhece (etiqueta da pasta física,
    -- numeração antiga migrada). Livre, único por clínica quando informado.
    numero_prontuario VARCHAR(30) NULL,

    status          TEXT         NOT NULL DEFAULT 'ativo'
                                 CHECK (status IN ('ativo','inativo','obito')),
    observacoes     TEXT         NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- Invariante 10: paciente com prontuário não sofre hard delete. Guarda legal.
    deleted_at      TIMESTAMPTZ  NULL,

    CONSTRAINT fk_paciente_pessoa
        FOREIGN KEY (id_pessoa, id_clinica)
        REFERENCES identidade.pessoas (id_pessoa, id_clinica),
    CONSTRAINT fk_paciente_plano
        FOREIGN KEY (id_plano, id_clinica)
        REFERENCES pacientes.planos_convenio (id_plano, id_clinica),
    CONSTRAINT uq_pacientes_tenant UNIQUE (id_paciente, id_clinica),
    -- Carteirinha sem plano é dado órfão.
    CONSTRAINT ck_carteirinha_exige_plano
        CHECK (num_carteirinha IS NULL OR id_plano IS NOT NULL)
);

CREATE UNIQUE INDEX uq_paciente_pessoa
    ON pacientes.pacientes (id_clinica, id_pessoa) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_paciente_numero_prontuario
    ON pacientes.pacientes (id_clinica, numero_prontuario)
    WHERE numero_prontuario IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_pacientes_clinica_status
    ON pacientes.pacientes (id_clinica, status) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_pacientes_updated_at
    BEFORE UPDATE ON pacientes.pacientes
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

CREATE TRIGGER trg_convenios_updated_at
    BEFORE UPDATE ON pacientes.convenios
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE pacientes.convenios        ENABLE ROW LEVEL SECURITY;
ALTER TABLE pacientes.convenios        FORCE  ROW LEVEL SECURITY;
ALTER TABLE pacientes.planos_convenio  ENABLE ROW LEVEL SECURITY;
ALTER TABLE pacientes.planos_convenio  FORCE  ROW LEVEL SECURITY;
ALTER TABLE pacientes.precos_convenio  ENABLE ROW LEVEL SECURITY;
ALTER TABLE pacientes.precos_convenio  FORCE  ROW LEVEL SECURITY;
ALTER TABLE pacientes.pacientes        ENABLE ROW LEVEL SECURITY;
ALTER TABLE pacientes.pacientes        FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON pacientes.convenios
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON pacientes.planos_convenio
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON pacientes.precos_convenio
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON pacientes.pacientes
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
