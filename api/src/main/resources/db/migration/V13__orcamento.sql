-- ─────────────────────────────────────────────────────────────────────────────
-- V13 — Módulo orçamento (plano de tratamento) e guias de convênio.
--
-- O orçamento é um DOCUMENTO, não uma consulta ao catálogo: `valor_cobrado` é
-- congelado na criação do item. Se a clínica reajusta o preço de uma restauração
-- amanhã, o orçamento que o paciente assinou hoje continua valendo o que dizia.
-- Por isso os totais são colunas, não SUM() sobre itens.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE orcamento.orcamentos (
    id_orcamento   BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente    BIGINT        NOT NULL,
    id_dentista    BIGINT        NOT NULL,

    status         TEXT          NOT NULL DEFAULT 'rascunho'
                                 CHECK (status IN ('rascunho','enviado','aprovado',
                                                   'recusado','expirado','cancelado')),

    valor_bruto    NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (valor_bruto   >= 0),
    valor_desconto NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (valor_desconto >= 0),
    valor_final    NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (valor_final    >= 0),

    validade_em    DATE          NULL,
    observacoes    TEXT          NULL,
    aprovado_em    TIMESTAMPTZ   NULL,

    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    -- Aritmética conferida pelo banco. Um desconto que não bate com o total é o
    -- tipo de erro que só aparece na conversa com o paciente.
    CONSTRAINT ck_total_coerente CHECK (valor_final = valor_bruto - valor_desconto),
    CONSTRAINT ck_desconto_nao_excede CHECK (valor_desconto <= valor_bruto),
    CONSTRAINT ck_aprovado_tem_data
        CHECK (status <> 'aprovado' OR aprovado_em IS NOT NULL),

    CONSTRAINT fk_orcamento_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT fk_orcamento_dentista
        FOREIGN KEY (id_dentista, id_clinica)
        REFERENCES identidade.dentistas (id_dentista, id_clinica),
    CONSTRAINT uq_orcamentos_tenant UNIQUE (id_orcamento, id_clinica)
);

CREATE INDEX idx_orcamentos_paciente
    ON orcamento.orcamentos (id_clinica, id_paciente, created_at DESC);
CREATE INDEX idx_orcamentos_status
    ON orcamento.orcamentos (id_clinica, status, created_at DESC);

CREATE TRIGGER trg_orcamentos_updated_at
    BEFORE UPDATE ON orcamento.orcamentos
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

CREATE TABLE orcamento.itens (
    id_item         BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_orcamento    BIGINT        NOT NULL,
    id_procedimento BIGINT        NOT NULL,

    -- Notação FDI; NULL para procedimento que não é por dente (profilaxia).
    dente           SMALLINT      NULL,
    face            TEXT          NULL CHECK (face IN ('V','L','M','D','O','I','P')),

    -- Preço congelado no momento do orçamento.
    valor_cobrado   NUMERIC(14,2) NOT NULL CHECK (valor_cobrado >= 0),

    status_execucao TEXT          NOT NULL DEFAULT 'pendente'
                                  CHECK (status_execucao IN ('pendente','em_andamento',
                                                             'concluido','cancelado')),
    executado_em    TIMESTAMPTZ   NULL,
    id_consulta     BIGINT        NULL,

    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_item_orcamento
        FOREIGN KEY (id_orcamento, id_clinica)
        REFERENCES orcamento.orcamentos (id_orcamento, id_clinica) ON DELETE CASCADE,
    CONSTRAINT fk_item_procedimento
        FOREIGN KEY (id_procedimento, id_clinica)
        REFERENCES clinicas.procedimentos (id_procedimento, id_clinica),
    CONSTRAINT fk_item_consulta
        FOREIGN KEY (id_consulta, id_clinica)
        REFERENCES agenda.consultas (id_consulta, id_clinica),
    CONSTRAINT uq_itens_tenant UNIQUE (id_item, id_clinica),
    CONSTRAINT ck_concluido_tem_data
        CHECK (status_execucao <> 'concluido' OR executado_em IS NOT NULL)
);

CREATE INDEX idx_itens_orcamento ON orcamento.itens (id_clinica, id_orcamento);

-- ─── Guias de autorização de convênio ────────────────────────────────────────
CREATE TABLE orcamento.guias_autorizacao (
    id_guia       BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica    BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_item       BIGINT        NULL,
    id_consulta   BIGINT        NULL,

    numero_guia   VARCHAR(50)   NULL,
    status        TEXT          NOT NULL DEFAULT 'solicitada'
                                CHECK (status IN ('solicitada','autorizada','negada',
                                                  'executada','glosada','cancelada')),
    valor_glosado NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (valor_glosado >= 0),
    motivo_glosa  TEXT          NULL,

    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_guia_item
        FOREIGN KEY (id_item, id_clinica)
        REFERENCES orcamento.itens (id_item, id_clinica),
    CONSTRAINT fk_guia_consulta
        FOREIGN KEY (id_consulta, id_clinica)
        REFERENCES agenda.consultas (id_consulta, id_clinica),
    CONSTRAINT ck_glosa_tem_motivo
        CHECK (status <> 'glosada' OR length(trim(coalesce(motivo_glosa,''))) > 0)
);

CREATE INDEX idx_guias_status ON orcamento.guias_autorizacao (id_clinica, status);

CREATE TRIGGER trg_guias_updated_at
    BEFORE UPDATE ON orcamento.guias_autorizacao
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE orcamento.orcamentos        ENABLE ROW LEVEL SECURITY;
ALTER TABLE orcamento.orcamentos        FORCE  ROW LEVEL SECURITY;
ALTER TABLE orcamento.itens             ENABLE ROW LEVEL SECURITY;
ALTER TABLE orcamento.itens             FORCE  ROW LEVEL SECURITY;
ALTER TABLE orcamento.guias_autorizacao ENABLE ROW LEVEL SECURITY;
ALTER TABLE orcamento.guias_autorizacao FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON orcamento.orcamentos
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON orcamento.itens
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON orcamento.guias_autorizacao
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
