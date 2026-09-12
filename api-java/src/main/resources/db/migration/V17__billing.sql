-- ─────────────────────────────────────────────────────────────────────────────
-- V17 — Módulo billing: a CLÍNICA pagando o DentiBot (Stripe).
-- Não confundir com `financeiro`, que é o PACIENTE pagando a clínica (PSP
-- brasileiro). São dois provedores atrás do mesmo PaymentPort, decisão #6.
--
-- Aqui também mora o gate de plano — a coisa que a landing vende e que hoje
-- nada impõe: Solo = 1 profissional e 500 mensagens/mês; Clínica = até 8.
-- ─────────────────────────────────────────────────────────────────────────────

-- Tabela de referência, sem tenant: descreve os planos do produto, não de uma
-- clínica. Leitura liberada porque são os mesmos números da página de preços.
CREATE TABLE billing.planos (
    codigo              TEXT          PRIMARY KEY CHECK (codigo IN ('solo','clinica')),
    nome                VARCHAR(60)   NOT NULL,
    preco_mensal_centavos INTEGER     NOT NULL CHECK (preco_mensal_centavos >= 0),
    max_profissionais   SMALLINT      NOT NULL CHECK (max_profissionais > 0),
    max_mensagens_mes   INTEGER       NOT NULL CHECK (max_mensagens_mes >= 0),
    permite_permissao_por_perfil BOOLEAN NOT NULL DEFAULT FALSE
);

-- Preço em centavos inteiros (invariante 6). R$ 97,00 = 9700.
INSERT INTO billing.planos (codigo, nome, preco_mensal_centavos, max_profissionais,
                            max_mensagens_mes, permite_permissao_por_perfil)
VALUES ('solo',    'Solo',    9700, 1, 500,  FALSE),
       ('clinica', 'Clínica', 19700, 8, 2000, TRUE);

GRANT SELECT ON billing.planos TO dentibot_app;

CREATE TABLE billing.assinaturas (
    id_assinatura   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),

    plano           TEXT         NOT NULL REFERENCES billing.planos(codigo),
    provedor        TEXT         NOT NULL DEFAULT 'stripe' CHECK (provedor IN ('stripe','cortesia')),
    id_cliente_externo    VARCHAR(120) NULL,
    id_assinatura_externa VARCHAR(120) NULL,

    status          TEXT         NOT NULL DEFAULT 'trial'
                                 CHECK (status IN ('trial','ativa','inadimplente',
                                                   'cancelada','encerrada')),
    periodo_inicio  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    periodo_fim     TIMESTAMPTZ  NULL,
    cancelar_no_fim BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Uma assinatura viva por clínica. Histórico fica nas canceladas/encerradas.
CREATE UNIQUE INDEX uq_assinatura_viva
    ON billing.assinaturas (id_clinica)
    WHERE status IN ('trial','ativa','inadimplente');

CREATE UNIQUE INDEX uq_assinatura_externa
    ON billing.assinaturas (id_assinatura_externa)
    WHERE id_assinatura_externa IS NOT NULL;

CREATE TRIGGER trg_assinaturas_updated_at
    BEFORE UPDATE ON billing.assinaturas
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Consumo do mês, para o gate de plano ────────────────────────────────────
-- O Redis conta em tempo real (camada 12); esta tabela é o fechamento durável,
-- porque "quantas mensagens a clínica mandou em outubro" precisa sobreviver a
-- um flush do Redis para virar cobrança ou bloqueio.
CREATE TABLE billing.uso_mensal (
    id_clinica          BIGINT   NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    competencia         DATE     NOT NULL,   -- sempre dia 1 do mês
    mensagens_enviadas  INTEGER  NOT NULL DEFAULT 0 CHECK (mensagens_enviadas >= 0),
    profissionais_ativos SMALLINT NOT NULL DEFAULT 0 CHECK (profissionais_ativos >= 0),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (id_clinica, competencia),
    CONSTRAINT ck_competencia_dia_1 CHECK (EXTRACT(DAY FROM competencia) = 1)
);

-- ─── Faturas do SaaS ─────────────────────────────────────────────────────────
CREATE TABLE billing.faturas (
    id_fatura       BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_externo      VARCHAR(120) NULL,
    valor_centavos  INTEGER      NOT NULL CHECK (valor_centavos >= 0),
    status          TEXT         NOT NULL CHECK (status IN ('aberta','paga','vencida',
                                                            'estornada','incobravel')),
    competencia     DATE         NOT NULL,
    vence_em        DATE         NULL,
    paga_em         TIMESTAMPTZ  NULL,
    url_fatura      VARCHAR(500) NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_fatura_externa
    ON billing.faturas (id_externo) WHERE id_externo IS NOT NULL;
CREATE INDEX idx_faturas_clinica
    ON billing.faturas (id_clinica, competencia DESC);

-- ─── RLS ─────────────────────────────────────────────────────────────────────
-- Billing não é dado clínico: staff de ops/billing PODE ler. É exatamente o
-- caso que justifica ter dois eixos — suporte resolve cobrança sem nunca
-- alcançar prontuário.
ALTER TABLE billing.assinaturas ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing.assinaturas FORCE  ROW LEVEL SECURITY;
ALTER TABLE billing.uso_mensal  ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing.uso_mensal  FORCE  ROW LEVEL SECURITY;
ALTER TABLE billing.faturas     ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing.faturas     FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON billing.assinaturas
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON billing.uso_mensal
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON billing.faturas
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY staff_billing ON billing.assinaturas
    FOR ALL TO dentibot_app
    USING      (plataforma.staff_atual() IN ('ops_billing','engenharia'))
    WITH CHECK (plataforma.staff_atual() IN ('ops_billing','engenharia'));
CREATE POLICY staff_billing ON billing.faturas
    FOR ALL TO dentibot_app
    USING      (plataforma.staff_atual() IN ('ops_billing','engenharia'))
    WITH CHECK (plataforma.staff_atual() IN ('ops_billing','engenharia'));
CREATE POLICY staff_billing_leitura ON billing.uso_mensal
    FOR SELECT TO dentibot_app
    USING (plataforma.staff_atual() IN ('ops_billing','engenharia'));

-- O worker de faturamento e o de fechamento de uso varrem todos os tenants.
CREATE POLICY worker ON billing.uso_mensal
    FOR ALL TO dentibot_app
    USING (plataforma.modo_worker()) WITH CHECK (plataforma.modo_worker());
CREATE POLICY worker ON billing.assinaturas
    FOR ALL TO dentibot_app
    USING (plataforma.modo_worker()) WITH CHECK (plataforma.modo_worker());
CREATE POLICY worker ON billing.faturas
    FOR ALL TO dentibot_app
    USING (plataforma.modo_worker()) WITH CHECK (plataforma.modo_worker());
