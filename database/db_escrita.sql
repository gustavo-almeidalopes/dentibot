CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pgaudit;
CREATE EXTENSION IF NOT EXISTS pg_cron;

DROP ROLE IF EXISTS dentibot_superadmin;
DROP ROLE IF EXISTS dentibot_app;
DROP ROLE IF EXISTS dentibot_tokenizer;
DROP ROLE IF EXISTS dentibot_replicator;

CREATE ROLE dentibot_superadmin
    WITH LOGIN CREATEDB CREATEROLE REPLICATION
         PASSWORD 'TROQUE_DEPLOY_SA'
         CONNECTION LIMIT 5;

CREATE ROLE dentibot_app
    WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
         PASSWORD 'TROQUE_DEPLOY_APP'
         CONNECTION LIMIT 80;

CREATE ROLE dentibot_tokenizer
    WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
         PASSWORD 'TROQUE_DEPLOY_TOK'
         CONNECTION LIMIT 5;

CREATE ROLE dentibot_replicator
    WITH LOGIN REPLICATION NOCREATEDB NOSUPERUSER
         PASSWORD 'TROQUE_DEPLOY_REP'
         CONNECTION LIMIT 3;

GRANT CONNECT ON DATABASE dentibot_escrita TO dentibot_app;
GRANT USAGE   ON SCHEMA public             TO dentibot_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES    IN SCHEMA public TO dentibot_app;
GRANT USAGE, SELECT                  ON ALL SEQUENCES IN SCHEMA public TO dentibot_app;
REVOKE CREATE ON SCHEMA public FROM dentibot_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES    TO dentibot_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT                  ON SEQUENCES TO dentibot_app;

ALTER ROLE dentibot_app       VALID UNTIL (NOW() + INTERVAL '90 days')::TEXT;
ALTER ROLE dentibot_tokenizer VALID UNTIL (NOW() + INTERVAL '90 days')::TEXT;

CREATE SCHEMA IF NOT EXISTS token_vault;
REVOKE ALL ON SCHEMA token_vault FROM PUBLIC;
REVOKE ALL ON SCHEMA token_vault FROM dentibot_app;
GRANT  USAGE ON SCHEMA token_vault TO dentibot_tokenizer;
GRANT  USAGE ON SCHEMA token_vault TO dentibot_superadmin;

CREATE TABLE clinicas (
    id_clinica      INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cnpj            CHAR(14)      NOT NULL UNIQUE,
    razao_social    VARCHAR(144)  NOT NULL,
    nome_fantasia   VARCHAR(60)   NOT NULL,

    status          SMALLINT      NOT NULL DEFAULT 4,
    data_inicio     DATE          NOT NULL DEFAULT CURRENT_DATE,
    data_renovacao  DATE          NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NULL
);

CREATE TABLE configuracoes_clinica (
    id_configuracao         INT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica              INT      NOT NULL UNIQUE REFERENCES clinicas(id_clinica),
    ativar_comissao         BOOLEAN  NOT NULL DEFAULT FALSE,

    regra_comissao_padrao   SMALLINT NOT NULL DEFAULT 1,

    emissao_nf              SMALLINT NOT NULL DEFAULT 1,
    exigir_mfa_funcionarios BOOLEAN  NOT NULL DEFAULT FALSE
);

CREATE TABLE pessoas (
    id_pessoa         INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica        INT           NOT NULL REFERENCES clinicas(id_clinica),
    nome_completo     VARCHAR(150)  NOT NULL,
    cpf               VARCHAR(255)  NOT NULL,
    data_nascimento   DATE          NULL,
    sexo              SMALLINT      NULL,
    telefone_celular  VARCHAR(255)  NULL,
    telefone_fixo     VARCHAR(255)  NULL,
    email             VARCHAR(255)  NULL,
    logradouro        VARCHAR(255)  NULL,
    numero            VARCHAR(20)   NULL,
    complemento       VARCHAR(100)  NULL,
    cep               VARCHAR(255)  NULL,
    cidade            VARCHAR(100)  NULL,
    uf                CHAR(2)       NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NULL,
    deleted_at        TIMESTAMPTZ   NULL
);

CREATE TABLE papeis_permissoes (
    id_papel    INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica  INT           NOT NULL REFERENCES clinicas(id_clinica),
    nome_perfil VARCHAR(80)   NOT NULL,

    privilegios JSONB         NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE usuarios_sistema (
    id_usuario      INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pessoa       INT           NOT NULL REFERENCES pessoas(id_pessoa),
    id_clinica      INT           NOT NULL REFERENCES clinicas(id_clinica),
    id_papel        INT           NOT NULL REFERENCES papeis_permissoes(id_papel),
    login_email     VARCHAR(150)  NOT NULL UNIQUE,
    senha_hash      VARCHAR(255)  NOT NULL,
    mfa_secret      VARCHAR(255)  NULL,
    ultimo_login_em TIMESTAMPTZ   NULL,

    status          SMALLINT      NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE pacientes (
    id_paciente     INT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pessoa       INT          NOT NULL REFERENCES pessoas(id_pessoa),
    id_clinica      INT          NOT NULL REFERENCES clinicas(id_clinica),
    id_plano        INT          NULL,
    num_carteirinha VARCHAR(255) NULL,

    status          SMALLINT     NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE dentistas (
    id_dentista   INT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pessoa     INT          NOT NULL REFERENCES pessoas(id_pessoa),
    id_clinica    INT          NOT NULL REFERENCES clinicas(id_clinica),
    cro_numero    VARCHAR(20)  NOT NULL,
    cro_uf        CHAR(2)      NOT NULL,
    especialidade VARCHAR(80)  NULL,

    status        SMALLINT     NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE funcionarios (
    id_funcionario INT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pessoa      INT          NOT NULL REFERENCES pessoas(id_pessoa),
    id_clinica     INT          NOT NULL REFERENCES clinicas(id_clinica),
    cargo          VARCHAR(80)  NOT NULL,

    status         SMALLINT     NOT NULL DEFAULT 1,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE procedimentos (
    id_procedimento INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      INT            NOT NULL REFERENCES clinicas(id_clinica),
    nome_servico    VARCHAR(120)   NOT NULL,
    preco_particular DECIMAL(10,2) NOT NULL DEFAULT 0,
    duracao_minutos SMALLINT       NOT NULL DEFAULT 30,
    ativo           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE agenda (
    id_agendamento       INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica           INT           NOT NULL REFERENCES clinicas(id_clinica),
    id_paciente          INT           NOT NULL REFERENCES pacientes(id_paciente),
    id_dentista          INT           NOT NULL REFERENCES dentistas(id_dentista),
    inicio_em            TIMESTAMPTZ   NOT NULL,
    termino_previsto_em  TIMESTAMPTZ   NOT NULL,
    termino_real_em      TIMESTAMPTZ   NULL,

    status               SMALLINT      NOT NULL DEFAULT 1,
    motivo_cancelamento  VARCHAR(255)  NULL,
    observacoes          TEXT          NULL,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE orcamentos (
    id_orcamento   INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     INT            NOT NULL REFERENCES clinicas(id_clinica),
    id_paciente    INT            NOT NULL REFERENCES pacientes(id_paciente),
    id_dentista    INT            NOT NULL REFERENCES dentistas(id_dentista),

    status         SMALLINT       NOT NULL DEFAULT 1,
    valor_bruto    DECIMAL(10,2)  NOT NULL DEFAULT 0,
    valor_desconto DECIMAL(10,2)  NOT NULL DEFAULT 0,
    valor_final    DECIMAL(10,2)  NOT NULL DEFAULT 0,
    validade_em    DATE           NULL,
    observacoes    TEXT           NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NULL
);

CREATE TABLE itens_orcamento (
    id_item          INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_orcamento     INT            NOT NULL REFERENCES orcamentos(id_orcamento),
    id_procedimento  INT            NOT NULL REFERENCES procedimentos(id_procedimento),
    regiao_dente     VARCHAR(80)    NULL,
    valor_cobrado    DECIMAL(10,2)  NOT NULL,

    status_execucao  SMALLINT       NOT NULL DEFAULT 1,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE prontuarios_anamnese (
    id_prontuario       INT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_paciente         INT         NOT NULL REFERENCES pacientes(id_paciente),
    id_clinica          INT         NOT NULL REFERENCES clinicas(id_clinica),
    questionario_medico JSONB       NULL,
    alergias_relatadas  TEXT        NULL,
    historico_anterior  TEXT        NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NULL
);

CREATE TABLE evolucao_clinica (
    id_evolucao      INT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_paciente      INT         NOT NULL REFERENCES pacientes(id_paciente),
    id_dentista      INT         NOT NULL REFERENCES dentistas(id_dentista),
    id_agendamento   INT         NULL REFERENCES agenda(id_agendamento),
    descricao_sessao TEXT        NOT NULL,
    registrado_em    TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()

);

CREATE TABLE convenios (
    id_convenio          INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica           INT           NOT NULL REFERENCES clinicas(id_clinica),
    nome_operadora       VARCHAR(100)  NOT NULL,
    registro_ans         VARCHAR(20)   NULL,
    prazo_reembolso_dias SMALLINT      NOT NULL DEFAULT 30,
    ativo                BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE planos_convenio (
    id_plano    INT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_convenio INT          NOT NULL REFERENCES convenios(id_convenio),
    nome_plano  VARCHAR(100) NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE pacientes
    ADD CONSTRAINT fk_paciente_plano
    FOREIGN KEY (id_plano) REFERENCES planos_convenio(id_plano);

CREATE TABLE precos_convenio (
    id_tarifa       INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_plano        INT            NOT NULL REFERENCES planos_convenio(id_plano),
    id_procedimento INT            NOT NULL REFERENCES procedimentos(id_procedimento),
    codigo_tuss     VARCHAR(20)    NULL,
    valor_repasse   DECIMAL(10,2)  NOT NULL,
    vigente_desde   DATE           NOT NULL,
    vigente_ate     DATE           NULL,
    UNIQUE (id_plano, id_procedimento)
);

CREATE TABLE guias_autorizacao (
    id_guia            INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica         INT            NOT NULL REFERENCES clinicas(id_clinica),
    id_agendamento     INT            NULL REFERENCES agenda(id_agendamento),
    id_item_orcamento  INT            NULL REFERENCES itens_orcamento(id_item),
    numero_guia        VARCHAR(50)    NULL,

    status             SMALLINT       NOT NULL DEFAULT 1,
    valor_glosado      DECIMAL(10,2)  NOT NULL DEFAULT 0,
    motivo_glosa       TEXT           NULL,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ    NULL
);

CREATE TABLE regras_comissao (
    id_regra        INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      INT            NOT NULL REFERENCES clinicas(id_clinica),
    id_dentista     INT            NOT NULL REFERENCES dentistas(id_dentista),
    id_procedimento INT            NULL REFERENCES procedimentos(id_procedimento),

    metrica_calculo SMALLINT       NOT NULL DEFAULT 1,
    valor_percentual DECIMAL(5,2)  NOT NULL,
    ativo           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE formas_pagamento (
    id_forma         INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica       INT            NOT NULL REFERENCES clinicas(id_clinica),
    nome             VARCHAR(80)    NOT NULL,
    taxa_percentual  DECIMAL(5,2)   NOT NULL DEFAULT 0,
    dias_liquidacao  SMALLINT       NOT NULL DEFAULT 0,
    ativo            BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE contas_receber (
    id_recebivel           INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica             INT            NOT NULL REFERENCES clinicas(id_clinica),
    id_orcamento           INT            NOT NULL REFERENCES orcamentos(id_orcamento),
    id_paciente            INT            NULL REFERENCES pacientes(id_paciente),
    id_convenio            INT            NULL REFERENCES convenios(id_convenio),
    id_forma_pagamento     INT            NULL REFERENCES formas_pagamento(id_forma),
    parcela_numero         SMALLINT       NOT NULL DEFAULT 1,
    parcela_total          SMALLINT       NOT NULL DEFAULT 1,
    valor_parcela          DECIMAL(10,2)  NOT NULL,
    vencimento_em          DATE           NOT NULL,
    recebido_em            DATE           NULL,
    valor_liquido_recebido DECIMAL(10,2)  NULL,

    status                 SMALLINT       NOT NULL DEFAULT 1,
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ    NULL
);

CREATE TABLE comissoes_geradas (
    id_comissao              INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica               INT            NOT NULL REFERENCES clinicas(id_clinica),
    id_dentista              INT            NOT NULL REFERENCES dentistas(id_dentista),
    id_item_orcamento        INT            NOT NULL REFERENCES itens_orcamento(id_item),
    id_conta_receber         INT            NULL REFERENCES contas_receber(id_recebivel),
    valor_bruto_paciente     DECIMAL(10,2)  NOT NULL,
    valor_liquido_dentista   DECIMAL(10,2)  NOT NULL,

    status                   SMALLINT       NOT NULL DEFAULT 1,
    previsao_liberacao_em    DATE           NULL,
    pago_em                  TIMESTAMPTZ    NULL,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE contas_pagar (
    id_despesa      INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      INT            NOT NULL REFERENCES clinicas(id_clinica),
    descricao       VARCHAR(200)   NOT NULL,
    categoria       VARCHAR(80)    NOT NULL,
    valor_documento DECIMAL(10,2)  NOT NULL,
    vencimento_em   DATE           NOT NULL,
    pago_em         DATE           NULL,

    status          SMALLINT       NOT NULL DEFAULT 1,
    observacoes     TEXT           NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE fornecedores (
    id_fornecedor  INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     INT           NOT NULL REFERENCES clinicas(id_clinica),
    razao_social   VARCHAR(144)  NOT NULL,
    cnpj           CHAR(14)      NULL UNIQUE,
    telefone       VARCHAR(20)   NULL,
    email_vendedor VARCHAR(150)  NULL,
    ativo          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE produtos_materiais (
    id_produto           INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica           INT            NOT NULL REFERENCES clinicas(id_clinica),
    id_fornecedor_padrao INT            NULL REFERENCES fornecedores(id_fornecedor),
    nome_produto         VARCHAR(120)   NOT NULL,
    unidade_medida       VARCHAR(30)    NOT NULL,
    quantidade_atual     DECIMAL(10,3)  NOT NULL DEFAULT 0,
    ponto_pedido         DECIMAL(10,3)  NOT NULL DEFAULT 0,
    ativo                BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE movimentacao_estoque (
    id_movimentacao INT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_produto      INT            NOT NULL REFERENCES produtos_materiais(id_produto),
    id_funcionario  INT            NOT NULL REFERENCES funcionarios(id_funcionario),

    tipo_operacao   SMALLINT       NOT NULL,
    quantidade      DECIMAL(10,3)  NOT NULL,
    observacao      VARCHAR(200)   NULL,
    movimentado_em  TIMESTAMPTZ    NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE configuracoes_fiscais_clinica (
    id_fiscal           INT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica          INT          NOT NULL UNIQUE REFERENCES clinicas(id_clinica),
    certificado_a1      BYTEA        NULL,
    senha_certificado   VARCHAR(255) NULL,

    regime_tributacao   SMALLINT     NOT NULL DEFAULT 1,
    inscricao_municipal VARCHAR(30)  NULL,
    cnae_servico        VARCHAR(10)  NULL,
    aliquota_iss        DECIMAL(5,2) NOT NULL DEFAULT 2.00,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NULL
);

CREATE TABLE notas_fiscais_emitidas (
    id_nota            INT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica         INT          NOT NULL REFERENCES clinicas(id_clinica),
    id_conta_receber   INT          NOT NULL REFERENCES contas_receber(id_recebivel),
    numero_nfse        VARCHAR(30)  NULL,
    codigo_verificacao VARCHAR(50)  NULL,

    status             SMALLINT     NOT NULL DEFAULT 1,
    motivo_rejeicao    TEXT         NULL,
    link_pdf           VARCHAR(500) NULL,
    xml_conteudo       TEXT         NULL,
    emitida_em         TIMESTAMPTZ  NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE termos_politicas_lgpd (
    id_termo        INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      INT           NOT NULL REFERENCES clinicas(id_clinica),
    versao          VARCHAR(10)   NOT NULL,

    tipo_documento  SMALLINT      NOT NULL,
    texto_integral  TEXT          NOT NULL,
    ativo_desde     DATE          NOT NULL,
    ativo           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE consentimentos_paciente (
    id_consentimento   INT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_paciente        INT         NOT NULL REFERENCES pacientes(id_paciente),
    id_termo           INT         NOT NULL REFERENCES termos_politicas_lgpd(id_termo),
    finalidades_aceitas JSONB      NOT NULL,
    aceito_em          TIMESTAMPTZ NOT NULL,
    ip_origem          VARCHAR(45) NOT NULL,
    user_agent         VARCHAR(500) NOT NULL,
    revogado           BOOLEAN     NOT NULL DEFAULT FALSE,
    revogado_em        TIMESTAMPTZ NULL
);

CREATE TABLE solicitacoes_titulares (
    id_solicitacao      INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica          INT           NOT NULL REFERENCES clinicas(id_clinica),
    id_paciente         INT           NOT NULL REFERENCES pacientes(id_paciente),

    tipo_direito        SMALLINT      NOT NULL,
    aberta_em           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    prazo_resposta_em   DATE          NOT NULL,

    status              SMALLINT      NOT NULL DEFAULT 1,
    justificativa_recusa TEXT         NULL,
    respondida_em       TIMESTAMPTZ   NULL
);

CREATE TABLE controle_bloqueio_usuario (
    id_usuario           INT         NOT NULL REFERENCES usuarios_sistema(id_usuario) PRIMARY KEY,
    falhas_consecutivas  SMALLINT    NOT NULL DEFAULT 0,
    bloqueado_ate        TIMESTAMPTZ NULL,
    ultima_falha_em      TIMESTAMPTZ NULL
);

CREATE TABLE controle_mudancas_schema (
    id_mudanca        INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descricao         TEXT          NOT NULL,
    script_migracao   TEXT          NOT NULL,
    solicitado_por    VARCHAR(80)   NOT NULL,
    aprovado_por      VARCHAR(80)   NULL,

    status            SMALLINT      NOT NULL DEFAULT 1,
    solicitado_em     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    aprovado_em       TIMESTAMPTZ   NULL,
    aplicado_em       TIMESTAMPTZ   NULL,
    rollback_script   TEXT          NULL,
    ticket_referencia VARCHAR(50)   NULL
);

CREATE TABLE dispositivos_confiaveis (
    id_dispositivo       INT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario           INT          NOT NULL REFERENCES usuarios_sistema(id_usuario),
    id_clinica           INT          NOT NULL REFERENCES clinicas(id_clinica),
    fingerprint_cert     VARCHAR(64)  NOT NULL UNIQUE,
    nome_dispositivo     VARCHAR(100) NOT NULL,
    sistema_operacional  VARCHAR(50)  NULL,
    antivirus_ativo      BOOLEAN      NULL,
    disco_criptografado  BOOLEAN      NULL,
    so_atualizado        BOOLEAN      NULL,
    mdm_gerenciado       BOOLEAN      NOT NULL DEFAULT FALSE,

    status               SMALLINT     NOT NULL DEFAULT 1,
    primeiro_uso_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ultimo_uso_em        TIMESTAMPTZ  NULL,
    revogado_em          TIMESTAMPTZ  NULL,
    motivo_revogacao     TEXT         NULL
);

CREATE TABLE solicitacoes_acesso_jit (
    id_solicitacao     INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_solicitante     INT           NOT NULL REFERENCES usuarios_sistema(id_usuario),
    id_aprovador       INT           NULL REFERENCES usuarios_sistema(id_usuario),
    id_clinica         INT           NOT NULL REFERENCES clinicas(id_clinica),
    papel_solicitado   VARCHAR(60)   NOT NULL,
    motivo_acesso      TEXT          NOT NULL,
    ticket_referencia  VARCHAR(50)   NULL,

    status             SMALLINT      NOT NULL DEFAULT 1,
    solicitado_em      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    aprovado_em        TIMESTAMPTZ   NULL,
    valido_de          TIMESTAMPTZ   NULL,
    valido_ate         TIMESTAMPTZ   NULL,
    role_temporaria    VARCHAR(63)   NULL,
    revogado_em        TIMESTAMPTZ   NULL,
    ip_solicitante     VARCHAR(45)   NOT NULL DEFAULT '0.0.0.0',
    justificativa_recusa TEXT        NULL
);

CREATE TABLE honeytokens (
    id_honeytoken     INT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica        INT           NOT NULL REFERENCES clinicas(id_clinica),
    tabela_alvo       VARCHAR(80)   NOT NULL,
    id_registro_isca  VARCHAR(50)   NOT NULL,
    descricao_isca    VARCHAR(200)  NOT NULL,
    criado_em         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE token_vault.mapa_tokens (
    id_token       BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token          VARCHAR(40)  NOT NULL UNIQUE,
    dado_cifrado   BYTEA        NOT NULL,
    tipo_dado      VARCHAR(30)  NOT NULL,
    hash_busca     CHAR(64)     NOT NULL,
    versao_chave   SMALLINT     NOT NULL DEFAULT 1,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    rotacionado_em TIMESTAMPTZ  NULL,
    revogado       BOOLEAN      NOT NULL DEFAULT FALSE,
    revogado_em    TIMESTAMPTZ  NULL
);

GRANT SELECT, INSERT, UPDATE ON token_vault.mapa_tokens TO dentibot_tokenizer;
GRANT USAGE, SELECT ON token_vault.mapa_tokens_id_token_seq TO dentibot_tokenizer;

CREATE INDEX idx_pessoas_clinica        ON pessoas        (id_clinica, deleted_at);
CREATE INDEX idx_pacientes_clinica      ON pacientes      (id_clinica, status);
CREATE INDEX idx_agenda_dentista_data   ON agenda         (id_dentista, inicio_em, status);
CREATE INDEX idx_agenda_paciente        ON agenda         (id_paciente, inicio_em DESC);
CREATE INDEX idx_contas_receber_status  ON contas_receber (id_clinica, status, vencimento_em);
CREATE INDEX idx_usuarios_login         ON usuarios_sistema (login_email, status);
CREATE INDEX idx_token_hash             ON token_vault.mapa_tokens (hash_busca, revogado);

DO $$
DECLARE
    tbl TEXT;
    tabelas_tenant TEXT[] := ARRAY[
        'configuracoes_clinica','pessoas','pacientes','dentistas','funcionarios',
        'usuarios_sistema','papeis_permissoes','procedimentos','agenda','orcamentos',
        'itens_orcamento','prontuarios_anamnese','evolucao_clinica','convenios',
        'planos_convenio','guias_autorizacao','regras_comissao','comissoes_geradas',
        'formas_pagamento','contas_receber','contas_pagar','fornecedores',
        'produtos_materiais','configuracoes_fiscais_clinica','notas_fiscais_emitidas',
        'termos_politicas_lgpd','consentimentos_paciente','solicitacoes_titulares',
        'dispositivos_confiaveis','solicitacoes_acesso_jit','honeytokens'
    ];
BEGIN
    FOREACH tbl IN ARRAY tabelas_tenant LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tbl);

        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I
             TO dentibot_app
             USING (id_clinica = current_setting(''app.clinica_id'')::INTEGER)',
            tbl
        );

        EXECUTE format(
            'CREATE POLICY superadmin_bypass ON %I
             TO dentibot_superadmin USING (TRUE)',
            tbl
        );
    END LOOP;
END;
$$;

ALTER TABLE agenda           ENABLE ROW LEVEL SECURITY;
ALTER TABLE evolucao_clinica ENABLE ROW LEVEL SECURITY;
ALTER TABLE contas_receber   ENABLE ROW LEVEL SECURITY;
ALTER TABLE contas_pagar     ENABLE ROW LEVEL SECURITY;
ALTER TABLE prontuarios_anamnese ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_agenda_dentista ON agenda AS RESTRICTIVE TO dentibot_app
    USING (
        current_setting('app.papel', TRUE) IN ('admin','recepcionista','financeiro')
        OR (current_setting('app.papel', TRUE) = 'dentista'
            AND id_dentista = current_setting('app.dentista_id', TRUE)::INTEGER)
    );

CREATE POLICY rls_prontuario_clinico ON prontuarios_anamnese AS RESTRICTIVE TO dentibot_app
    USING (current_setting('app.papel', TRUE) IN ('admin','dentista'));

CREATE POLICY rls_evolucao_clinico ON evolucao_clinica AS RESTRICTIVE TO dentibot_app
    USING (
        current_setting('app.papel', TRUE) = 'admin'
        OR (current_setting('app.papel', TRUE) = 'dentista'
            AND id_dentista = current_setting('app.dentista_id', TRUE)::INTEGER)
    );

CREATE POLICY rls_financeiro_receber ON contas_receber AS RESTRICTIVE TO dentibot_app
    USING (current_setting('app.papel', TRUE) IN ('admin','financeiro'));

CREATE POLICY rls_financeiro_pagar ON contas_pagar AS RESTRICTIVE TO dentibot_app
    USING (current_setting('app.papel', TRUE) IN ('admin','financeiro'));

CREATE OR REPLACE FUNCTION sp_autenticar_usuario(
    p_login_email TEXT, p_senha_plain TEXT,
    p_ip TEXT,         p_user_agent TEXT,
    p_clinica_id  INTEGER
)
RETURNS TABLE (autenticado BOOLEAN, id_usuario INTEGER,
               id_papel INTEGER, mfa_habilitado BOOLEAN, motivo_falha TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_u   usuarios_sistema%ROWTYPE;
    v_ok  BOOLEAN := FALSE;
    v_mot TEXT    := NULL;
    v_res SMALLINT := 2;
BEGIN
    SELECT * INTO v_u FROM usuarios_sistema
    WHERE login_email = p_login_email AND id_clinica = p_clinica_id AND status = 1;

    IF NOT FOUND THEN
        v_mot := 'Usuário não encontrado ou inativo';
    ELSIF v_u.status = 2 THEN
        v_res := 4; v_mot := 'Conta bloqueada';
    ELSIF v_u.senha_hash = crypt(p_senha_plain, v_u.senha_hash) THEN
        v_ok := TRUE; v_res := 1;
        UPDATE usuarios_sistema SET ultimo_login_em = NOW() WHERE id_usuario = v_u.id_usuario;
        CALL sp_resetar_falhas_login(v_u.id_usuario);
    ELSE
        v_mot := 'Credenciais inválidas';
        CALL sp_registrar_falha_login(v_u.id_usuario);
    END IF;

    PERFORM pg_notify('auditoria_login', json_build_object(
        'id_clinica', p_clinica_id, 'id_usuario', v_u.id_usuario,
        'resultado', v_res, 'ip', p_ip, 'user_agent', p_user_agent,
        'evento_em', NOW()
    )::TEXT);

    RETURN QUERY SELECT v_ok, v_u.id_usuario, v_u.id_papel,
                        (v_u.mfa_secret IS NOT NULL), v_mot;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_registrar_falha_login(
    p_id_usuario INTEGER, p_limite SMALLINT DEFAULT 5, p_minutos INTEGER DEFAULT 30
)
LANGUAGE plpgsql AS $$
DECLARE v_falhas SMALLINT;
BEGIN
    INSERT INTO controle_bloqueio_usuario (id_usuario, falhas_consecutivas, ultima_falha_em)
    VALUES (p_id_usuario, 1, NOW())
    ON CONFLICT (id_usuario) DO UPDATE
        SET falhas_consecutivas = controle_bloqueio_usuario.falhas_consecutivas + 1,
            ultima_falha_em = NOW();

    SELECT falhas_consecutivas INTO v_falhas
    FROM controle_bloqueio_usuario WHERE id_usuario = p_id_usuario;

    IF v_falhas >= p_limite THEN
        UPDATE usuarios_sistema SET status = 2 WHERE id_usuario = p_id_usuario;
        UPDATE controle_bloqueio_usuario
        SET bloqueado_ate = NOW() + (p_minutos || ' minutes')::INTERVAL
        WHERE id_usuario = p_id_usuario;
    END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_resetar_falhas_login(p_id_usuario INTEGER)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE controle_bloqueio_usuario
    SET falhas_consecutivas = 0, bloqueado_ate = NULL, ultima_falha_em = NULL
    WHERE id_usuario = p_id_usuario;
END;
$$;

SELECT cron.schedule('desbloquear-contas', '*/5 * * * *',
    'UPDATE usuarios_sistema u SET status = 1
     FROM controle_bloqueio_usuario c
     WHERE u.id_usuario = c.id_usuario AND u.status = 2 AND c.bloqueado_ate < NOW()');

CREATE OR REPLACE PROCEDURE sp_criar_agendamento(
    p_clinica_id INTEGER, p_paciente_id INTEGER, p_dentista_id INTEGER,
    p_inicio_em TIMESTAMPTZ, p_termino_previsto TIMESTAMPTZ, p_usuario_acao INTEGER
)
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_conflito INTEGER; v_novo_id INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_conflito FROM agenda
    WHERE id_dentista = p_dentista_id AND id_clinica = p_clinica_id
      AND status NOT IN (5,6)
      AND tsrange(inicio_em, termino_previsto_em, '[)')
          && tsrange(p_inicio_em, p_termino_previsto, '[)');

    IF v_conflito > 0 THEN
        RAISE EXCEPTION 'Conflito de horário detectado' USING ERRCODE = 'P0001';
    END IF;

    INSERT INTO agenda (id_clinica,id_paciente,id_dentista,inicio_em,termino_previsto_em,status)
    VALUES (p_clinica_id,p_paciente_id,p_dentista_id,p_inicio_em,p_termino_previsto,1)
    RETURNING id_agendamento INTO v_novo_id;

    PERFORM pg_notify('auditoria_escrita', json_build_object(
        'tipo','INSERT','tabela','agenda','id_registro',v_novo_id,
        'id_clinica',p_clinica_id,'id_usuario',p_usuario_acao,'evento_em',NOW()
    )::TEXT);
END;
$$;

CREATE OR REPLACE PROCEDURE sp_inserir_evolucao_clinica(
    p_paciente_id INTEGER, p_dentista_id INTEGER, p_agendamento_id INTEGER,
    p_descricao TEXT, p_usuario_acao INTEGER, p_clinica_id INTEGER
)
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_novo_id INTEGER;
BEGIN
    IF TRIM(COALESCE(p_descricao,'')) = '' THEN
        RAISE EXCEPTION 'Descrição não pode ser vazia' USING ERRCODE = 'P0002';
    END IF;

    INSERT INTO evolucao_clinica (id_paciente,id_dentista,id_agendamento,descricao_sessao)
    VALUES (p_paciente_id,p_dentista_id,p_agendamento_id,p_descricao)
    RETURNING id_evolucao INTO v_novo_id;

    PERFORM pg_notify('auditoria_escrita', json_build_object(
        'tipo','INSERT','tabela','evolucao_clinica','id_registro',v_novo_id,
        'id_clinica',p_clinica_id,'id_usuario',p_usuario_acao,'evento_em',NOW(),
        'contexto','Inserção de evolução clínica'
    )::TEXT);
END;
$$;

CREATE OR REPLACE PROCEDURE sp_baixar_conta_receber(
    p_id_recebivel INTEGER, p_clinica_id INTEGER, p_recebido_em DATE,
    p_valor_liquido NUMERIC(10,2), p_id_forma INTEGER, p_usuario_acao INTEGER
)
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_antes JSONB;
BEGIN
    SELECT to_jsonb(c) INTO v_antes FROM contas_receber c
    WHERE id_recebivel = p_id_recebivel AND id_clinica = p_clinica_id AND status = 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Recebível % não encontrado ou já baixado', p_id_recebivel;
    END IF;

    UPDATE contas_receber
    SET status = 2, recebido_em = p_recebido_em,
        valor_liquido_recebido = p_valor_liquido,
        id_forma_pagamento = p_id_forma, updated_at = NOW()
    WHERE id_recebivel = p_id_recebivel AND id_clinica = p_clinica_id;

    PERFORM pg_notify('auditoria_escrita', json_build_object(
        'tipo','UPDATE','tabela','contas_receber','id_registro',p_id_recebivel,
        'id_clinica',p_clinica_id,'id_usuario',p_usuario_acao,'evento_em',NOW(),
        'dados_anteriores',v_antes,
        'dados_posteriores',json_build_object('status',2,'recebido_em',p_recebido_em)
    )::TEXT);
END;
$$;

CREATE OR REPLACE FUNCTION token_vault.fn_tokenizar(
    p_dado_real TEXT, p_tipo TEXT, p_chave_aes TEXT
)
RETURNS VARCHAR(40) LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_token VARCHAR(40); v_hash CHAR(64); v_existente VARCHAR(40);
BEGIN
    v_hash := encode(digest(p_tipo || ':' || p_dado_real, 'sha256'), 'hex');
    SELECT token INTO v_existente FROM token_vault.mapa_tokens
    WHERE hash_busca = v_hash AND revogado = FALSE LIMIT 1;
    IF FOUND THEN RETURN v_existente; END IF;

    v_token := 'tok_' || LEFT(replace(gen_random_uuid()::TEXT,'-',''), 12);
    INSERT INTO token_vault.mapa_tokens (token, dado_cifrado, tipo_dado, hash_busca)
    VALUES (v_token, pgp_sym_encrypt(p_dado_real, p_chave_aes, 'cipher-algo=aes256'),
            p_tipo, v_hash);
    RETURN v_token;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_aprovar_acesso_jit(
    p_id_solicitacao INTEGER, p_id_aprovador INTEGER,
    p_duracao_horas SMALLINT DEFAULT 2
)
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_sol solicitacoes_acesso_jit%ROWTYPE;
        v_role VARCHAR(63); v_expira TIMESTAMPTZ; v_senha TEXT;
BEGIN
    SELECT * INTO v_sol FROM solicitacoes_acesso_jit
    WHERE id_solicitacao = p_id_solicitacao AND status = 1;
    IF NOT FOUND THEN RAISE EXCEPTION 'Solicitação % inválida', p_id_solicitacao; END IF;

    v_expira := NOW() + (p_duracao_horas || ' hours')::INTERVAL;
    v_role   := 'jit_' || v_sol.id_solicitante || '_' || TO_CHAR(NOW(),'YYYYMMDD_HH24MI');
    v_senha  := encode(gen_random_bytes(16), 'hex');

    EXECUTE format('CREATE ROLE %I WITH LOGIN PASSWORD %L VALID UNTIL %L IN ROLE %I',
                   v_role, v_senha, v_expira::TEXT, v_sol.papel_solicitado);

    UPDATE solicitacoes_acesso_jit
    SET status=4, aprovado_em=NOW(), id_aprovador=p_id_aprovador,
        valido_de=NOW(), valido_ate=v_expira, role_temporaria=v_role
    WHERE id_solicitacao = p_id_solicitacao;
END;
$$;

SELECT cron.schedule('revogar-jit','*/5 * * * *', $$
    DO $$DECLARE v RECORD;
    BEGIN
        FOR v IN SELECT role_temporaria,id_solicitacao FROM solicitacoes_acesso_jit
                 WHERE status=4 AND valido_ate < NOW()
        LOOP
            PERFORM pg_terminate_backend(pid) FROM pg_stat_activity WHERE usename=v.role_temporaria;
            EXECUTE format('DROP ROLE IF EXISTS %I', v.role_temporaria);
            UPDATE solicitacoes_acesso_jit SET status=5, revogado_em=NOW()
            WHERE id_solicitacao=v.id_solicitacao;
        END LOOP;
    END$$;
$$);

CREATE OR REPLACE FUNCTION fn_bloquear_edicao_evolucao()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Evoluções clínicas são imutáveis após inserção (CFO/LGPD)'
        USING ERRCODE = 'P0011';
END;
$$;
CREATE TRIGGER trg_imutavel_evolucao
    BEFORE UPDATE OR DELETE ON evolucao_clinica
    FOR EACH ROW EXECUTE FUNCTION fn_bloquear_edicao_evolucao();

CREATE OR REPLACE FUNCTION fn_detectar_acesso_isca()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM honeytokens
               WHERE tabela_alvo = TG_TABLE_NAME
                 AND id_registro_isca = NEW.id_paciente::TEXT) THEN
        PERFORM pg_notify('honeytoken_alert', json_build_object(
            'tabela', TG_TABLE_NAME,
            'id_registro', NEW.id_paciente,
            'evento_em', NOW()
        )::TEXT);
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_isca_pacientes
    AFTER SELECT ON pacientes FOR EACH ROW
    EXECUTE FUNCTION fn_detectar_acesso_isca();

CREATE PUBLICATION pub_dentibot_negocio FOR TABLE
    clinicas, configuracoes_clinica,
    pessoas, pacientes, dentistas, funcionarios,
    usuarios_sistema, papeis_permissoes,
    procedimentos, agenda, orcamentos, itens_orcamento,
    prontuarios_anamnese, evolucao_clinica,
    convenios, planos_convenio, precos_convenio, guias_autorizacao,
    regras_comissao, comissoes_geradas,
    formas_pagamento, contas_receber, contas_pagar,
    fornecedores, produtos_materiais, movimentacao_estoque,
    configuracoes_fiscais_clinica, notas_fiscais_emitidas,
    termos_politicas_lgpd, consentimentos_paciente, solicitacoes_titulares,
    solicitacoes_acesso_jit, dispositivos_confiaveis
WITH (publish = 'insert, update, delete, truncate');
