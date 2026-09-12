-- ─────────────────────────────────────────────────────────────────────────────
-- V16 — Módulo LGPD.
--
-- Papéis: a CLÍNICA é a controladora dos dados do paciente; o DentiBot é
-- operador. Por isso o termo e o consentimento pertencem à clínica, e o registro
-- de sub-processadores (Sentry, PostHog, Resend, R2, Meta...) é documento do
-- repositório — docs/security/subprocessadores.md — e não linha de banco: a
-- clínica precisa lê-lo antes de assinar, não depois de virar cliente.
--
-- Correção em relação à V1: consentimentos_paciente não tinha id_clinica.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE lgpd.termos (
    id_termo       BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),

    tipo           TEXT         NOT NULL CHECK (tipo IN ('politica_privacidade',
                                                         'termo_consentimento',
                                                         'termo_uso_imagem',
                                                         'termo_tratamento')),
    versao         VARCHAR(10)  NOT NULL,
    texto_integral TEXT         NOT NULL,
    ativo_desde    DATE         NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_termo_versao UNIQUE (id_clinica, tipo, versao),
    CONSTRAINT uq_termos_tenant UNIQUE (id_termo, id_clinica)
);

-- Texto de termo não se edita depois de alguém consentir: consentimento aponta
-- para uma versão, e a versão tem que continuar sendo o que a pessoa leu.
-- Nova redação = nova versão.
REVOKE UPDATE, DELETE ON lgpd.termos FROM dentibot_app;
GRANT  UPDATE (ativo) ON lgpd.termos TO dentibot_app;

CREATE TABLE lgpd.consentimentos (
    id_consentimento    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica          BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente         BIGINT      NOT NULL,
    id_termo            BIGINT      NOT NULL,

    finalidades_aceitas JSONB       NOT NULL,
    aceito_em           TIMESTAMPTZ NOT NULL,
    -- Prova do consentimento: quem, quando, de onde. Exigência prática de
    -- demonstração de conformidade (LGPD art. 37).
    ip_origem           INET        NOT NULL,
    user_agent          VARCHAR(500) NOT NULL,

    revogado_em         TIMESTAMPTZ NULL,

    CONSTRAINT fk_consentimento_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT fk_consentimento_termo
        FOREIGN KEY (id_termo, id_clinica)
        REFERENCES lgpd.termos (id_termo, id_clinica)
);

CREATE INDEX idx_consentimentos_paciente
    ON lgpd.consentimentos (id_clinica, id_paciente, aceito_em DESC);

CREATE TABLE lgpd.solicitacoes_titular (
    id_solicitacao       BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica           BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente          BIGINT       NOT NULL,

    -- Direitos do art. 18. `eliminacao` NÃO apaga prontuário: a guarda mínima
    -- legal (CFO-226) prevalece, e a recusa precisa ser justificada por escrito
    -- — daí justificativa_recusa ser obrigatória quando se recusa.
    direito              TEXT         NOT NULL CHECK (direito IN ('confirmacao','acesso',
                                                                  'correcao','anonimizacao',
                                                                  'portabilidade','eliminacao',
                                                                  'revogacao_consentimento')),
    aberta_em            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    prazo_resposta_em    DATE         NOT NULL,
    status               TEXT         NOT NULL DEFAULT 'aberta'
                                      CHECK (status IN ('aberta','em_analise','atendida',
                                                        'recusada','parcialmente_atendida')),
    justificativa_recusa TEXT         NULL,
    -- Export gerado no R2, entregue por URL assinada de expiração curta.
    chave_objeto_resposta VARCHAR(500) NULL,
    respondida_em        TIMESTAMPTZ  NULL,

    CONSTRAINT fk_solicitacao_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT ck_recusa_justificada
        CHECK (status NOT IN ('recusada','parcialmente_atendida')
               OR length(trim(coalesce(justificativa_recusa,''))) > 0)
);

CREATE INDEX idx_solicitacoes_prazo
    ON lgpd.solicitacoes_titular (id_clinica, status, prazo_resposta_em);

ALTER TABLE lgpd.termos               ENABLE ROW LEVEL SECURITY;
ALTER TABLE lgpd.termos               FORCE  ROW LEVEL SECURITY;
ALTER TABLE lgpd.consentimentos       ENABLE ROW LEVEL SECURITY;
ALTER TABLE lgpd.consentimentos       FORCE  ROW LEVEL SECURITY;
ALTER TABLE lgpd.solicitacoes_titular ENABLE ROW LEVEL SECURITY;
ALTER TABLE lgpd.solicitacoes_titular FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON lgpd.termos
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON lgpd.consentimentos
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON lgpd.solicitacoes_titular
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
