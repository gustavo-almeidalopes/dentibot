-- ─────────────────────────────────────────────────────────────────────────────
-- V15 — Módulo estoque.
-- Mesmo princípio do ledger financeiro: não existe coluna `quantidade_atual`.
-- Estoque é SUM sobre movimentações append-only. Uma clínica tem centenas de
-- itens, não milhões — o SUM é barato, e não existe o bug clássico de a coluna
-- e o extrato discordarem depois de um retry.
-- Correção em relação à V1: movimentacao_estoque não tinha id_clinica.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE estoque.fornecedores (
    id_fornecedor  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    razao_social   VARCHAR(144) NOT NULL,
    cnpj           CHAR(14)     NULL CHECK (cnpj ~ '^[0-9]{14}$'),
    telefone       VARCHAR(20)  NULL,
    email_vendedor VARCHAR(254) NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- CNPJ único POR CLÍNICA: duas clínicas podem comprar do mesmo fornecedor.
    -- Na V1 era UNIQUE global, o que impedia isso.
    CONSTRAINT uq_fornecedores_tenant UNIQUE (id_fornecedor, id_clinica)
);

CREATE UNIQUE INDEX uq_fornecedor_clinica_cnpj
    ON estoque.fornecedores (id_clinica, cnpj) WHERE cnpj IS NOT NULL;

CREATE TABLE estoque.produtos (
    id_produto           BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica           BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_fornecedor_padrao BIGINT        NULL,

    nome_produto         VARCHAR(120)  NOT NULL,
    unidade_medida       VARCHAR(30)   NOT NULL,
    ponto_pedido         NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (ponto_pedido >= 0),
    -- Material odontológico vence. Controle de lote é o que permite avisar.
    controla_lote        BOOLEAN       NOT NULL DEFAULT FALSE,

    ativo                BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_produto_fornecedor
        FOREIGN KEY (id_fornecedor_padrao, id_clinica)
        REFERENCES estoque.fornecedores (id_fornecedor, id_clinica),
    CONSTRAINT uq_produtos_tenant UNIQUE (id_produto, id_clinica)
);

CREATE TRIGGER trg_produtos_updated_at
    BEFORE UPDATE ON estoque.produtos
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

CREATE TABLE estoque.movimentacoes (
    id_movimentacao BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      BIGINT        NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_produto      BIGINT        NOT NULL,
    id_usuario      BIGINT        NULL,

    tipo            TEXT          NOT NULL CHECK (tipo IN ('entrada','saida','ajuste','perda','vencimento')),
    -- Com sinal, como o ledger: entrada positiva, saída negativa.
    -- Estoque = SUM(quantidade).
    quantidade      NUMERIC(14,3) NOT NULL CHECK (quantidade <> 0),

    lote            VARCHAR(60)   NULL,
    validade        DATE          NULL,
    custo_unitario  NUMERIC(14,2) NULL CHECK (custo_unitario IS NULL OR custo_unitario >= 0),
    observacao      VARCHAR(300)  NULL,

    movimentado_em  TIMESTAMPTZ   NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT fk_mov_produto
        FOREIGN KEY (id_produto, id_clinica)
        REFERENCES estoque.produtos (id_produto, id_clinica),
    CONSTRAINT fk_mov_usuario
        FOREIGN KEY (id_usuario, id_clinica)
        REFERENCES identidade.usuarios (id_usuario, id_clinica),
    CONSTRAINT ck_sinal_coerente CHECK (
        (tipo = 'entrada' AND quantidade > 0) OR
        (tipo IN ('saida','perda','vencimento') AND quantidade < 0) OR
        (tipo = 'ajuste')
    )
);

CREATE INDEX idx_movimentacoes_produto
    ON estoque.movimentacoes (id_clinica, id_produto, movimentado_em DESC);
CREATE INDEX idx_movimentacoes_validade
    ON estoque.movimentacoes (id_clinica, validade)
    WHERE validade IS NOT NULL;

REVOKE UPDATE, DELETE, TRUNCATE ON estoque.movimentacoes FROM dentibot_app;

CREATE TRIGGER trg_movimentacoes_append_only
    BEFORE UPDATE OR DELETE ON estoque.movimentacoes
    FOR EACH ROW EXECUTE FUNCTION auditoria.bloquear_alteracao();

CREATE VIEW estoque.vw_posicao AS
SELECT p.id_clinica,
       p.id_produto,
       p.nome_produto,
       p.unidade_medida,
       p.ponto_pedido,
       COALESCE(SUM(m.quantidade), 0) AS quantidade_atual,
       COALESCE(SUM(m.quantidade), 0) <= p.ponto_pedido AS abaixo_do_ponto_pedido
  FROM estoque.produtos p
  LEFT JOIN estoque.movimentacoes m
         ON m.id_produto = p.id_produto AND m.id_clinica = p.id_clinica
 WHERE p.ativo
 GROUP BY p.id_clinica, p.id_produto, p.nome_produto, p.unidade_medida, p.ponto_pedido;

GRANT SELECT ON estoque.vw_posicao TO dentibot_app;

ALTER TABLE estoque.fornecedores   ENABLE ROW LEVEL SECURITY;
ALTER TABLE estoque.fornecedores   FORCE  ROW LEVEL SECURITY;
ALTER TABLE estoque.produtos       ENABLE ROW LEVEL SECURITY;
ALTER TABLE estoque.produtos       FORCE  ROW LEVEL SECURITY;
ALTER TABLE estoque.movimentacoes  ENABLE ROW LEVEL SECURITY;
ALTER TABLE estoque.movimentacoes  FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON estoque.fornecedores
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON estoque.produtos
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON estoque.movimentacoes
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
