-- ─────────────────────────────────────────────────────────────────────────────
-- V14 — Módulo financeiro. Recebíveis, ledger, cobranças no PSP, comissões,
-- despesas e NFS-e.
--
-- A regra que organiza tudo aqui (camada 14): NÃO EXISTE COLUNA `saldo`.
-- Saldo é SUM sobre um ledger append-only. Coluna de saldo mutável é a origem
-- clássica do "o sistema diz que o paciente deve R$ 300 e ninguém sabe por quê":
-- basta um UPDATE perdido, um retry, uma corrida entre dois recebimentos.
-- Com ledger, o saldo é sempre reconstruível e sempre explicável linha a linha.
--
-- `contas_receber.status` existe e É mutável — mas status é estado de workflow
-- ("em aberto" / "paga" / "cancelada"), não dinheiro. O dinheiro está no ledger.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE financeiro.formas_pagamento (
    id_forma        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    nome            VARCHAR(80)   NOT NULL,
    tipo            TEXT          NOT NULL CHECK (tipo IN ('dinheiro','pix','boleto',
                                                           'cartao_credito','cartao_debito',
                                                           'transferencia','convenio')),
    taxa_percentual NUMERIC(5,2)  NOT NULL DEFAULT 0 CHECK (taxa_percentual BETWEEN 0 AND 100),
    dias_liquidacao SMALLINT      NOT NULL DEFAULT 0 CHECK (dias_liquidacao >= 0),
    ativo           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_formas_tenant UNIQUE (id_forma, id_clinica)
);

-- ─── Recebíveis (a obrigação) ────────────────────────────────────────────────
CREATE TABLE financeiro.contas_receber (
    id_recebivel   BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_orcamento   BIGINT        NULL,
    id_paciente    BIGINT        NULL,
    id_convenio    BIGINT        NULL,

    parcela_numero SMALLINT      NOT NULL DEFAULT 1 CHECK (parcela_numero >= 1),
    parcela_total  SMALLINT      NOT NULL DEFAULT 1 CHECK (parcela_total  >= 1),
    valor_parcela  NUMERIC(14,2) NOT NULL CHECK (valor_parcela > 0),
    vencimento_em  DATE          NOT NULL,

    status         TEXT          NOT NULL DEFAULT 'aberta'
                                 CHECK (status IN ('aberta','paga','parcial','vencida','cancelada')),

    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_parcela_coerente CHECK (parcela_numero <= parcela_total),
    CONSTRAINT fk_receber_orcamento
        FOREIGN KEY (id_orcamento, id_clinica)
        REFERENCES orcamento.orcamentos (id_orcamento, id_clinica),
    CONSTRAINT fk_receber_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT fk_receber_convenio
        FOREIGN KEY (id_convenio, id_clinica)
        REFERENCES pacientes.convenios (id_convenio, id_clinica),
    CONSTRAINT uq_receber_tenant UNIQUE (id_recebivel, id_clinica)
);

CREATE INDEX idx_receber_vencimento
    ON financeiro.contas_receber (id_clinica, status, vencimento_em);
CREATE INDEX idx_receber_paciente
    ON financeiro.contas_receber (id_clinica, id_paciente, vencimento_em DESC);

CREATE TRIGGER trg_receber_updated_at
    BEFORE UPDATE ON financeiro.contas_receber
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── LEDGER — append-only, a única fonte de verdade sobre dinheiro ───────────
CREATE TABLE financeiro.lancamentos (
    id_lancamento  BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_recebivel   BIGINT        NULL,

    tipo           TEXT          NOT NULL CHECK (tipo IN ('cobranca','recebimento','estorno',
                                                          'desconto','juros','multa','glosa','taxa')),

    -- Com sinal: recebimento é positivo, estorno é negativo. Assim o saldo é
    -- literalmente SUM(valor) e não depende de ninguém lembrar da regra de sinal
    -- de cada tipo na hora de somar.
    valor          NUMERIC(14,2) NOT NULL CHECK (valor <> 0),

    id_forma       BIGINT        NULL,
    -- Id da transação no PSP. Torna a conciliação possível.
    referencia_externa VARCHAR(120) NULL,
    descricao      VARCHAR(300)  NULL,

    -- Estorno aponta para o lançamento que anula. Nunca se apaga um lançamento.
    estorna_lancamento BIGINT    NULL,

    ocorrido_em    TIMESTAMPTZ   NOT NULL DEFAULT clock_timestamp(),
    registrado_por BIGINT        NULL,
    correlacao_id  UUID          NULL,

    CONSTRAINT fk_lancamento_recebivel
        FOREIGN KEY (id_recebivel, id_clinica)
        REFERENCES financeiro.contas_receber (id_recebivel, id_clinica),
    CONSTRAINT fk_lancamento_forma
        FOREIGN KEY (id_forma, id_clinica)
        REFERENCES financeiro.formas_pagamento (id_forma, id_clinica),
    CONSTRAINT fk_lancamento_estorno
        FOREIGN KEY (estorna_lancamento) REFERENCES financeiro.lancamentos(id_lancamento),
    CONSTRAINT ck_estorno_negativo
        CHECK (tipo <> 'estorno' OR valor < 0)
);

CREATE INDEX idx_lancamentos_recebivel
    ON financeiro.lancamentos (id_clinica, id_recebivel, ocorrido_em);
CREATE INDEX idx_lancamentos_periodo
    ON financeiro.lancamentos (id_clinica, ocorrido_em DESC);
CREATE UNIQUE INDEX uq_lancamento_referencia_externa
    ON financeiro.lancamentos (id_clinica, referencia_externa)
    WHERE referencia_externa IS NOT NULL;

REVOKE UPDATE, DELETE, TRUNCATE ON financeiro.lancamentos FROM dentibot_app;

CREATE OR REPLACE FUNCTION financeiro.bloquear_alteracao_ledger()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'financeiro.lancamentos é append-only. Para desfazer, insira um estorno '
        'com estorna_lancamento = %.', OLD.id_lancamento
        USING ERRCODE = 'insufficient_privilege';
END
$$;

CREATE TRIGGER trg_ledger_append_only
    BEFORE UPDATE OR DELETE ON financeiro.lancamentos
    FOR EACH ROW EXECUTE FUNCTION financeiro.bloquear_alteracao_ledger();

-- Saldo derivado. Nunca materializado.
CREATE VIEW financeiro.vw_saldo_recebivel AS
SELECT cr.id_clinica,
       cr.id_recebivel,
       cr.valor_parcela,
       COALESCE(SUM(l.valor) FILTER (WHERE l.tipo <> 'cobranca'), 0) AS valor_liquidado,
       cr.valor_parcela - COALESCE(SUM(l.valor) FILTER (WHERE l.tipo <> 'cobranca'), 0) AS saldo_devedor
  FROM financeiro.contas_receber cr
  LEFT JOIN financeiro.lancamentos l
         ON l.id_recebivel = cr.id_recebivel AND l.id_clinica = cr.id_clinica
 GROUP BY cr.id_clinica, cr.id_recebivel, cr.valor_parcela;

GRANT SELECT ON financeiro.vw_saldo_recebivel TO dentibot_app;

-- ─── Cobranças no PSP (Pix, boleto, cartão) ──────────────────────────────────
CREATE TABLE financeiro.cobrancas (
    id_cobranca    BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_recebivel   BIGINT        NOT NULL,

    provedor       TEXT          NOT NULL CHECK (provedor IN ('asaas','stripe','manual')),
    -- Id da cobrança no provedor. Único por provedor: é a chave de conciliação.
    id_externo     VARCHAR(120)  NULL,

    meio           TEXT          NOT NULL CHECK (meio IN ('pix','boleto','cartao_credito',
                                                          'cartao_debito','dinheiro')),
    valor          NUMERIC(14,2) NOT NULL CHECK (valor > 0),
    status         TEXT          NOT NULL DEFAULT 'pendente'
                                 CHECK (status IN ('pendente','emitida','paga','vencida',
                                                   'cancelada','estornada','falhou')),

    -- Pix copia-e-cola / linha digitável. Não é segredo, é o que o paciente copia.
    pix_copia_cola TEXT          NULL,
    linha_digitavel VARCHAR(60)  NULL,
    url_documento  VARCHAR(500)  NULL,
    vence_em       DATE          NULL,

    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cobranca_recebivel
        FOREIGN KEY (id_recebivel, id_clinica)
        REFERENCES financeiro.contas_receber (id_recebivel, id_clinica)
);

CREATE UNIQUE INDEX uq_cobranca_provedor_externo
    ON financeiro.cobrancas (provedor, id_externo) WHERE id_externo IS NOT NULL;
CREATE INDEX idx_cobrancas_status
    ON financeiro.cobrancas (id_clinica, status, vence_em);

CREATE TRIGGER trg_cobrancas_updated_at
    BEFORE UPDATE ON financeiro.cobrancas
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Contas a pagar ──────────────────────────────────────────────────────────
CREATE TABLE financeiro.contas_pagar (
    id_despesa      BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    descricao       VARCHAR(200)  NOT NULL,
    categoria       VARCHAR(80)   NOT NULL,
    valor_documento NUMERIC(14,2) NOT NULL CHECK (valor_documento > 0),
    vencimento_em   DATE          NOT NULL,
    pago_em         DATE          NULL,
    status          TEXT          NOT NULL DEFAULT 'aberta'
                                  CHECK (status IN ('aberta','paga','vencida','cancelada')),
    observacoes     TEXT          NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_paga_tem_data CHECK (status <> 'paga' OR pago_em IS NOT NULL)
);

CREATE INDEX idx_pagar_vencimento
    ON financeiro.contas_pagar (id_clinica, status, vencimento_em);

CREATE TRIGGER trg_pagar_updated_at
    BEFORE UPDATE ON financeiro.contas_pagar
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Comissões ───────────────────────────────────────────────────────────────
CREATE TABLE financeiro.regras_comissao (
    id_regra         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica       BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_dentista      BIGINT       NOT NULL,
    id_procedimento  BIGINT       NULL,   -- NULL = vale para todos
    -- Sobre o bruto do paciente ou sobre o líquido já descontada a taxa do meio?
    -- A escolha muda o valor e precisa ser explícita.
    base_calculo     TEXT         NOT NULL DEFAULT 'liquido'
                                  CHECK (base_calculo IN ('bruto','liquido')),
    percentual       NUMERIC(5,2) NOT NULL CHECK (percentual BETWEEN 0 AND 100),
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_regra_dentista
        FOREIGN KEY (id_dentista, id_clinica)
        REFERENCES identidade.dentistas (id_dentista, id_clinica),
    CONSTRAINT fk_regra_procedimento
        FOREIGN KEY (id_procedimento, id_clinica)
        REFERENCES clinicas.procedimentos (id_procedimento, id_clinica)
);

CREATE TABLE financeiro.comissoes (
    id_comissao            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica             BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_dentista            BIGINT        NOT NULL,
    id_item                BIGINT        NOT NULL,
    id_recebivel           BIGINT        NULL,

    valor_base             NUMERIC(14,2) NOT NULL CHECK (valor_base >= 0),
    percentual_aplicado    NUMERIC(5,2)  NOT NULL,
    valor_comissao         NUMERIC(14,2) NOT NULL CHECK (valor_comissao >= 0),

    status                 TEXT          NOT NULL DEFAULT 'prevista'
                                         CHECK (status IN ('prevista','liberada','paga','cancelada')),
    previsao_liberacao_em  DATE          NULL,
    pago_em                TIMESTAMPTZ   NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_comissao_dentista
        FOREIGN KEY (id_dentista, id_clinica)
        REFERENCES identidade.dentistas (id_dentista, id_clinica),
    CONSTRAINT fk_comissao_item
        FOREIGN KEY (id_item, id_clinica)
        REFERENCES orcamento.itens (id_item, id_clinica),
    CONSTRAINT fk_comissao_recebivel
        FOREIGN KEY (id_recebivel, id_clinica)
        REFERENCES financeiro.contas_receber (id_recebivel, id_clinica)
);

CREATE INDEX idx_comissoes_dentista
    ON financeiro.comissoes (id_clinica, id_dentista, status);

-- ─── Fiscal / NFS-e ──────────────────────────────────────────────────────────
CREATE TABLE financeiro.configuracao_fiscal (
    id_clinica          BIGINT       PRIMARY KEY REFERENCES clinicas.clinicas(id_clinica),
    regime_tributacao   TEXT         NOT NULL DEFAULT 'simples_nacional'
                                     CHECK (regime_tributacao IN ('simples_nacional','lucro_presumido','lucro_real')),
    inscricao_municipal VARCHAR(30)  NULL,
    cnae_servico        VARCHAR(10)  NULL,
    aliquota_iss        NUMERIC(5,2) NOT NULL DEFAULT 2.00 CHECK (aliquota_iss BETWEEN 0 AND 100),
    codigo_municipio    CHAR(7)      NULL,   -- IBGE

    -- O certificado A1 NÃO fica aqui. Guarda-se a referência ao segredo no
    -- gerenciador (camada 19 + invariante 12); bytes de certificado e senha em
    -- coluna são um vazamento esperando um dump de banco.
    referencia_certificado VARCHAR(200) NULL,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE financeiro.notas_fiscais (
    id_nota            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica         BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_recebivel       BIGINT       NOT NULL,

    numero_nfse        VARCHAR(30)  NULL,
    codigo_verificacao VARCHAR(50)  NULL,
    status             TEXT         NOT NULL DEFAULT 'pendente'
                                    CHECK (status IN ('pendente','processando','emitida',
                                                      'rejeitada','cancelada')),
    motivo_rejeicao    TEXT         NULL,
    -- Ponteiro para o R2, nunca o XML inteiro no banco.
    chave_objeto_pdf   VARCHAR(500) NULL,
    chave_objeto_xml   VARCHAR(500) NULL,
    emitida_em         TIMESTAMPTZ  NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_nota_recebivel
        FOREIGN KEY (id_recebivel, id_clinica)
        REFERENCES financeiro.contas_receber (id_recebivel, id_clinica)
);

CREATE INDEX idx_notas_status ON financeiro.notas_fiscais (id_clinica, status);

CREATE TRIGGER trg_notas_updated_at
    BEFORE UPDATE ON financeiro.notas_fiscais
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

CREATE TRIGGER trg_config_fiscal_updated_at
    BEFORE UPDATE ON financeiro.configuracao_fiscal
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE financeiro.formas_pagamento    ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.formas_pagamento    FORCE  ROW LEVEL SECURITY;
ALTER TABLE financeiro.contas_receber      ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.contas_receber      FORCE  ROW LEVEL SECURITY;
ALTER TABLE financeiro.lancamentos         ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.lancamentos         FORCE  ROW LEVEL SECURITY;
ALTER TABLE financeiro.cobrancas           ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.cobrancas           FORCE  ROW LEVEL SECURITY;
ALTER TABLE financeiro.contas_pagar        ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.contas_pagar        FORCE  ROW LEVEL SECURITY;
ALTER TABLE financeiro.regras_comissao     ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.regras_comissao     FORCE  ROW LEVEL SECURITY;
ALTER TABLE financeiro.comissoes           ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.comissoes           FORCE  ROW LEVEL SECURITY;
ALTER TABLE financeiro.configuracao_fiscal ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.configuracao_fiscal FORCE  ROW LEVEL SECURITY;
ALTER TABLE financeiro.notas_fiscais       ENABLE ROW LEVEL SECURITY;
ALTER TABLE financeiro.notas_fiscais       FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON financeiro.formas_pagamento
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON financeiro.contas_receber
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON financeiro.lancamentos
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON financeiro.cobrancas
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON financeiro.contas_pagar
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON financeiro.regras_comissao
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON financeiro.comissoes
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON financeiro.configuracao_fiscal
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON financeiro.notas_fiscais
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

-- O worker de conciliação (camada 14) varre cobranças de todos os tenants.
CREATE POLICY worker_concilia ON financeiro.cobrancas
    FOR ALL TO dentibot_app
    USING (plataforma.modo_worker()) WITH CHECK (plataforma.modo_worker());
