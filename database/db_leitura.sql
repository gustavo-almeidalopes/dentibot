CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pgaudit;

DROP ROLE IF EXISTS dentibot_readonly;
DROP ROLE IF EXISTS dentibot_audit;
DROP ROLE IF EXISTS dentibot_monitor;
DROP ROLE IF EXISTS dentibot_dba_leitura;

CREATE ROLE dentibot_dba_leitura
    WITH LOGIN CREATEDB CREATEROLE
         PASSWORD 'TROQUE_DEPLOY_DBA_L'
         CONNECTION LIMIT 5;

CREATE ROLE dentibot_readonly
    WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
         PASSWORD 'TROQUE_DEPLOY_RO'
         CONNECTION LIMIT 40;

CREATE ROLE dentibot_audit
    WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
         PASSWORD 'TROQUE_DEPLOY_AUDIT'
         CONNECTION LIMIT 10;

CREATE ROLE dentibot_monitor
    WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
         PASSWORD 'TROQUE_DEPLOY_MON'
         CONNECTION LIMIT 5;

GRANT CONNECT ON DATABASE dentibot_leitura TO dentibot_readonly;
GRANT CONNECT ON DATABASE dentibot_leitura TO dentibot_audit;
GRANT CONNECT ON DATABASE dentibot_leitura TO dentibot_monitor;

ALTER ROLE dentibot_readonly VALID UNTIL (NOW() + INTERVAL '90 days')::TEXT;
ALTER ROLE dentibot_audit    VALID UNTIL (NOW() + INTERVAL '90 days')::TEXT;

CREATE SCHEMA IF NOT EXISTS negocio;

CREATE SCHEMA IF NOT EXISTS seguranca;

GRANT USAGE  ON SCHEMA negocio   TO dentibot_readonly;
GRANT USAGE  ON SCHEMA seguranca TO dentibot_audit;
GRANT USAGE  ON SCHEMA seguranca TO dentibot_readonly;
GRANT USAGE  ON SCHEMA seguranca TO dentibot_monitor;

ALTER DEFAULT PRIVILEGES IN SCHEMA negocio
    GRANT SELECT ON TABLES TO dentibot_readonly;

ALTER DEFAULT PRIVILEGES IN SCHEMA seguranca
    GRANT INSERT ON TABLES TO dentibot_audit;
ALTER DEFAULT PRIVILEGES IN SCHEMA seguranca
    GRANT USAGE, SELECT ON SEQUENCES TO dentibot_audit;

CREATE TABLE seguranca.logs_auditoria_dados (
    id_log_auditoria  BIGINT        GENERATED ALWAYS AS IDENTITY,
    id_clinica        INTEGER       NOT NULL,
    id_usuario        INTEGER       NOT NULL,
    tipo_transacao    VARCHAR(10)   NOT NULL
                      CHECK (tipo_transacao IN ('SELECT','INSERT','UPDATE','DELETE')),
    tabela_afetada    VARCHAR(80)   NOT NULL,
    id_registro       VARCHAR(50)   NOT NULL,
    dados_anteriores  JSONB         NULL,
    dados_posteriores JSONB         NULL,
    evento_em         TIMESTAMPTZ(3) NOT NULL DEFAULT clock_timestamp(),
    contexto          VARCHAR(300)  NULL,

    hash_linha        CHAR(64)      GENERATED ALWAYS AS (
        encode(digest(
            tipo_transacao || tabela_afetada || id_registro || evento_em::TEXT,
        'sha256'), 'hex')
    ) STORED,
    PRIMARY KEY (id_log_auditoria, evento_em)
) PARTITION BY RANGE (evento_em);

CREATE TABLE seguranca.logs_auditoria_2025
    PARTITION OF seguranca.logs_auditoria_dados
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE seguranca.logs_auditoria_2026
    PARTITION OF seguranca.logs_auditoria_dados
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

CREATE TABLE seguranca.logs_auditoria_2027
    PARTITION OF seguranca.logs_auditoria_dados
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');

CREATE INDEX idx_audit_tabela_reg    ON seguranca.logs_auditoria_2026 (tabela_afetada, id_registro, evento_em DESC);
CREATE INDEX idx_audit_usuario_ev    ON seguranca.logs_auditoria_2026 (id_usuario, evento_em DESC);
CREATE INDEX idx_audit_clinica       ON seguranca.logs_auditoria_2026 (id_clinica, evento_em DESC);

CREATE OR REPLACE FUNCTION seguranca.fn_bloquear_alteracao_log()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION
        'logs_auditoria_dados é append-only. UPDATE e DELETE são proibidos.'
        USING ERRCODE = 'P0010';
END;
$$;

CREATE TRIGGER trg_readonly_audit
    BEFORE UPDATE OR DELETE ON seguranca.logs_auditoria_dados
    FOR EACH ROW EXECUTE FUNCTION seguranca.fn_bloquear_alteracao_log();

GRANT INSERT ON seguranca.logs_auditoria_dados       TO dentibot_audit;
GRANT INSERT ON seguranca.logs_auditoria_2025        TO dentibot_audit;
GRANT INSERT ON seguranca.logs_auditoria_2026        TO dentibot_audit;
GRANT INSERT ON seguranca.logs_auditoria_2027        TO dentibot_audit;
GRANT SELECT ON seguranca.logs_auditoria_dados       TO dentibot_readonly;
GRANT SELECT ON seguranca.logs_auditoria_dados       TO dentibot_monitor;

CREATE TABLE seguranca.logs_acesso_sessoes (
    id_log_acesso   BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      INTEGER       NOT NULL,
    id_usuario      INTEGER       NOT NULL,
    login_em        TIMESTAMPTZ   NOT NULL,
    logout_em       TIMESTAMPTZ   NULL,

    resultado       SMALLINT      NOT NULL,
    ip_origem       VARCHAR(45)   NOT NULL,
    geolocalizacao  VARCHAR(120)  NULL,
    dispositivo_ua  VARCHAR(500)  NULL,
    token_sessao_id VARCHAR(255)  NULL UNIQUE,
    sessao_revogada BOOLEAN       NOT NULL DEFAULT FALSE,
    revogada_em     TIMESTAMPTZ   NULL
);

CREATE INDEX idx_sess_ip_resultado  ON seguranca.logs_acesso_sessoes (ip_origem, resultado, login_em DESC);
CREATE INDEX idx_sess_usuario       ON seguranca.logs_acesso_sessoes (id_usuario, login_em DESC);

GRANT INSERT, UPDATE ON seguranca.logs_acesso_sessoes TO dentibot_audit;
GRANT SELECT         ON seguranca.logs_acesso_sessoes TO dentibot_readonly;
GRANT SELECT         ON seguranca.logs_acesso_sessoes TO dentibot_monitor;

CREATE TABLE seguranca.alertas_anomalia (
    id_alerta     BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica    INTEGER       NOT NULL,
    id_usuario    INTEGER       NULL,
    ip_origem     VARCHAR(45)   NULL,

    severidade    SMALLINT      NOT NULL,
    tipo_anomalia VARCHAR(60)   NOT NULL,
    descricao     TEXT          NOT NULL,
    evidencia     JSONB         NULL,
    detectado_em  TIMESTAMPTZ   NOT NULL DEFAULT clock_timestamp(),

    status        SMALLINT      NOT NULL DEFAULT 1,
    resolvido_em  TIMESTAMPTZ   NULL,
    respondido_por INTEGER      NULL
);

CREATE INDEX idx_alertas_severidade ON seguranca.alertas_anomalia (severidade DESC, detectado_em DESC);
CREATE INDEX idx_alertas_tipo       ON seguranca.alertas_anomalia (tipo_anomalia, detectado_em DESC);

GRANT INSERT         ON seguranca.alertas_anomalia TO dentibot_audit;
GRANT SELECT, UPDATE ON seguranca.alertas_anomalia TO dentibot_readonly;
GRANT SELECT         ON seguranca.alertas_anomalia TO dentibot_monitor;

CREATE TABLE seguranca.atividade_tempo_real (
    id_atividade    BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica      INTEGER      NOT NULL,
    id_usuario      INTEGER      NOT NULL,
    tabela_acessada VARCHAR(80)  NOT NULL,
    tipo_operacao   VARCHAR(10)  NOT NULL,
    linhas_afetadas BIGINT       NOT NULL DEFAULT 0,
    duracao_ms      INTEGER      NOT NULL DEFAULT 0,
    ip_origem       VARCHAR(45)  NOT NULL,
    registrado_em   TIMESTAMPTZ  NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_ativ_usuario_tempo ON seguranca.atividade_tempo_real (id_usuario, registrado_em DESC);

CREATE TABLE seguranca.baseline_atividade_usuario (
    id_usuario           INTEGER      NOT NULL,
    hora_do_dia          SMALLINT     NOT NULL CHECK (hora_do_dia BETWEEN 0 AND 23),
    dia_da_semana        SMALLINT     NOT NULL CHECK (dia_da_semana BETWEEN 0 AND 6),
    media_queries_hora   NUMERIC(8,2) NOT NULL DEFAULT 0,
    desvio_padrao        NUMERIC(8,2) NOT NULL DEFAULT 0,
    max_linhas_retornadas BIGINT      NOT NULL DEFAULT 0,
    media_linhas         NUMERIC(12,2) NOT NULL DEFAULT 0,
    ultima_atualizacao   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id_usuario, hora_do_dia, dia_da_semana)
);

GRANT INSERT, UPDATE ON seguranca.atividade_tempo_real       TO dentibot_audit;
GRANT INSERT, UPDATE ON seguranca.baseline_atividade_usuario TO dentibot_audit;
GRANT SELECT         ON seguranca.atividade_tempo_real       TO dentibot_readonly;
GRANT SELECT         ON seguranca.baseline_atividade_usuario TO dentibot_readonly;

CREATE TABLE seguranca.catalogo_dados_sensiveis (
    id_catalogo           SERIAL        PRIMARY KEY,
    schema_nome           VARCHAR(63)   NOT NULL DEFAULT 'negocio',
    tabela_nome           VARCHAR(63)   NOT NULL,
    coluna_nome           VARCHAR(63)   NOT NULL,

    classificacao         SMALLINT      NOT NULL,
    criptografada         BOOLEAN       NOT NULL DEFAULT FALSE,
    algoritmo_crypto      VARCHAR(20)   NULL,
    exige_auditoria       BOOLEAN       NOT NULL DEFAULT TRUE,
    exige_mascaramento    BOOLEAN       NOT NULL DEFAULT FALSE,
    responsavel_dado      VARCHAR(80)   NOT NULL,
    base_legal_lgpd       VARCHAR(120)  NOT NULL,
    prazo_retencao_anos   SMALLINT      NOT NULL,
    observacoes           TEXT          NULL,
    atualizado_em         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (tabela_nome, coluna_nome)
);

CREATE TABLE seguranca.registros_teste_restauracao (
    id_teste              SERIAL        PRIMARY KEY,
    realizado_em          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    arquivo_backup        VARCHAR(255)  NOT NULL,
    hash_sha256           CHAR(64)      NOT NULL,
    tempo_restauracao     INTERVAL      NOT NULL,
    rpo_minutos           INTEGER       NOT NULL,
    sucesso               BOOLEAN       NOT NULL,
    executado_por         VARCHAR(80)   NOT NULL,
    observacoes           TEXT          NULL
);

CREATE TABLE seguranca.controle_arquivamento_particoes (
    id_arquivamento           SERIAL        PRIMARY KEY,
    nome_particao             VARCHAR(80)   NOT NULL UNIQUE,
    detachada_em              TIMESTAMPTZ   NULL,
    exportada_em              TIMESTAMPTZ   NULL,
    local_arquivo             VARCHAR(500)  NULL,
    hash_export               CHAR(64)      NULL,
    excluida_fisicamente_em   TIMESTAMPTZ   NULL,

    status                    SMALLINT      NOT NULL DEFAULT 1
);

CREATE TABLE seguranca.registros_pentest (
    id_pentest              SERIAL        PRIMARY KEY,
    data_inicio             DATE          NOT NULL,
    data_fim                DATE          NOT NULL,
    empresa_executora       VARCHAR(100)  NOT NULL,
    responsavel_interno     VARCHAR(80)   NOT NULL,
    escopo                  TEXT          NOT NULL,

    modalidade              SMALLINT      NOT NULL DEFAULT 2,
    total_criticos          SMALLINT      NOT NULL DEFAULT 0,
    total_altos             SMALLINT      NOT NULL DEFAULT 0,
    total_medios            SMALLINT      NOT NULL DEFAULT 0,
    total_baixos            SMALLINT      NOT NULL DEFAULT 0,
    relatorio_path          VARCHAR(500)  NULL,

    status_remediacao       SMALLINT      NOT NULL DEFAULT 1,
    proxima_revisao_em      DATE          NOT NULL,
    observacoes             TEXT          NULL
);

CREATE TABLE seguranca.vulnerabilidades_encontradas (
    id_vulnerabilidade      SERIAL        PRIMARY KEY,
    id_pentest              INTEGER       NOT NULL
                            REFERENCES seguranca.registros_pentest(id_pentest),
    titulo                  VARCHAR(200)  NOT NULL,

    severidade              SMALLINT      NOT NULL,
    cvss_score              DECIMAL(3,1)  NULL,
    cwe_id                  VARCHAR(20)   NULL,
    descricao               TEXT          NOT NULL,
    evidencia               TEXT          NULL,
    recomendacao            TEXT          NOT NULL,

    status                  SMALLINT      NOT NULL DEFAULT 1,
    corrigida_em            DATE          NULL,
    validada_em             DATE          NULL,
    responsavel_correcao    VARCHAR(80)   NULL
);

GRANT SELECT, INSERT, UPDATE ON seguranca.catalogo_dados_sensiveis      TO dentibot_readonly;
GRANT SELECT, INSERT, UPDATE ON seguranca.registros_teste_restauracao    TO dentibot_readonly;
GRANT SELECT, INSERT, UPDATE ON seguranca.controle_arquivamento_particoes TO dentibot_readonly;
GRANT SELECT, INSERT, UPDATE ON seguranca.registros_pentest              TO dentibot_readonly;
GRANT SELECT, INSERT, UPDATE ON seguranca.vulnerabilidades_encontradas   TO dentibot_readonly;
GRANT SELECT ON seguranca.catalogo_dados_sensiveis     TO dentibot_monitor;
GRANT SELECT ON seguranca.registros_pentest            TO dentibot_monitor;
GRANT SELECT ON seguranca.vulnerabilidades_encontradas TO dentibot_monitor;

INSERT INTO seguranca.catalogo_dados_sensiveis
    (tabela_nome, coluna_nome, classificacao, criptografada, algoritmo_crypto,
     exige_auditoria, exige_mascaramento, responsavel_dado, base_legal_lgpd, prazo_retencao_anos)
VALUES
    ('pessoas','cpf',              1, TRUE,  'AES-256',  TRUE,  TRUE,  'DPO','Art. 7º, V', 20),
    ('pessoas','telefone_celular', 1, TRUE,  'AES-256',  FALSE, TRUE,  'DPO','Art. 7º, V',  5),
    ('pessoas','email',            1, TRUE,  'AES-256',  FALSE, TRUE,  'DPO','Art. 7º, V',  5),
    ('pessoas','logradouro',       1, TRUE,  'AES-256',  FALSE, FALSE, 'DPO','Art. 7º, V',  5),
    ('pacientes','num_carteirinha',1, TRUE,  'AES-256',  TRUE,  TRUE,  'DPO','Art. 7º, V', 20),
    ('prontuarios_anamnese','questionario_medico',2,FALSE,NULL,TRUE,FALSE,'Médico','Art. 11, II, f',20),
    ('evolucao_clinica','descricao_sessao',       2,FALSE,NULL,TRUE,FALSE,'Médico','Art. 11, II, f',20),
    ('usuarios_sistema','senha_hash',             4,TRUE,'Argon2id',TRUE,FALSE,'TI','Art. 46', 5),
    ('usuarios_sistema','mfa_secret',             4,TRUE,'AES-256', TRUE,FALSE,'TI','Art. 46', 5),
    ('contas_receber','valor_parcela',            3,FALSE,NULL,TRUE,FALSE,'Financeiro','Art. 7º, V',10);

CREATE OR REPLACE PROCEDURE seguranca.sp_registrar_auditoria(
    p_id_clinica      INTEGER,
    p_id_usuario      INTEGER,
    p_tipo            VARCHAR(10),
    p_tabela          VARCHAR(80),
    p_id_registro     VARCHAR(50),
    p_dados_antes     JSONB    DEFAULT NULL,
    p_dados_depois    JSONB    DEFAULT NULL,
    p_contexto        VARCHAR(300) DEFAULT NULL
)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO seguranca.logs_auditoria_dados
        (id_clinica, id_usuario, tipo_transacao, tabela_afetada,
         id_registro, dados_anteriores, dados_posteriores, evento_em, contexto)
    VALUES
        (p_id_clinica, p_id_usuario, p_tipo, p_tabela,
         p_id_registro, p_dados_antes, p_dados_depois, clock_timestamp(), p_contexto);
END;
$$;

GRANT EXECUTE ON PROCEDURE seguranca.sp_registrar_auditoria TO dentibot_audit;

CREATE OR REPLACE PROCEDURE seguranca.sp_registrar_acesso(
    p_id_clinica  INTEGER, p_id_usuario  INTEGER,
    p_resultado   SMALLINT, p_ip          VARCHAR(45),
    p_geo         VARCHAR(120), p_ua      VARCHAR(500),
    p_token_id    VARCHAR(255) DEFAULT NULL
)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO seguranca.logs_acesso_sessoes
        (id_clinica, id_usuario, login_em, resultado,
         ip_origem, geolocalizacao, dispositivo_ua, token_sessao_id)
    VALUES
        (p_id_clinica, p_id_usuario, NOW(), p_resultado,
         p_ip, p_geo, p_ua, p_token_id);
END;
$$;

GRANT EXECUTE ON PROCEDURE seguranca.sp_registrar_acesso TO dentibot_audit;

CREATE OR REPLACE FUNCTION seguranca.fn_detectar_anomalias(
    p_id_usuario  INTEGER, p_id_clinica  INTEGER,
    p_tabela      VARCHAR(80), p_linhas  BIGINT, p_ip VARCHAR(45)
)
RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    v_baseline seguranca.baseline_atividade_usuario%ROWTYPE;
    v_hora  SMALLINT := EXTRACT(HOUR FROM NOW())::SMALLINT;
    v_dia   SMALLINT := EXTRACT(DOW  FROM NOW())::SMALLINT;
    v_threshold NUMERIC;
    v_count_hora BIGINT;
BEGIN
    SELECT * INTO v_baseline FROM seguranca.baseline_atividade_usuario
    WHERE id_usuario = p_id_usuario AND hora_do_dia = v_hora AND dia_da_semana = v_dia;

    IF FOUND AND v_baseline.media_linhas > 0 THEN
        v_threshold := v_baseline.media_linhas + (3 * v_baseline.desvio_padrao);
        IF p_linhas > GREATEST(v_threshold, 500) THEN
            INSERT INTO seguranca.alertas_anomalia
                (id_clinica, id_usuario, ip_origem, severidade, tipo_anomalia, descricao, evidencia)
            VALUES (p_id_clinica, p_id_usuario, p_ip,
                CASE WHEN p_linhas > v_threshold * 10 THEN 4 ELSE 3 END,
                'VOLUME_EXFILTRACAO',
                format('Usuário %s retornou %s linhas da tabela %s (normal: ~%s)',
                    p_id_usuario, p_linhas, p_tabela, round(v_baseline.media_linhas)),
                jsonb_build_object('linhas', p_linhas, 'media', v_baseline.media_linhas,
                                   'threshold', v_threshold, 'tabela', p_tabela));
        END IF;
    END IF;

    IF v_hora NOT BETWEEN 7 AND 20 THEN
        SELECT COUNT(*) INTO v_count_hora FROM seguranca.atividade_tempo_real
        WHERE id_usuario = p_id_usuario AND registrado_em > NOW() - INTERVAL '2 hours';
        IF v_count_hora > 20 THEN
            INSERT INTO seguranca.alertas_anomalia
                (id_clinica, id_usuario, ip_origem, severidade, tipo_anomalia, descricao, evidencia)
            VALUES (p_id_clinica, p_id_usuario, p_ip, 3,
                'ACESSO_HORARIO_ANOMALO',
                format('Usuário %s realizou %s operações fora do horário (%sh)', p_id_usuario, v_count_hora, v_hora),
                jsonb_build_object('hora', v_hora, 'operacoes_2h', v_count_hora));
        END IF;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM seguranca.logs_acesso_sessoes
        WHERE id_usuario = p_id_usuario AND ip_origem = p_ip
          AND resultado = 1 AND login_em < NOW() - INTERVAL '10 minutes'
    ) THEN
        INSERT INTO seguranca.alertas_anomalia
            (id_clinica, id_usuario, ip_origem, severidade, tipo_anomalia, descricao, evidencia)
        VALUES (p_id_clinica, p_id_usuario, p_ip, 3,
            'IP_NUNCA_VISTO',
            format('Usuário %s autenticado de IP desconhecido: %s', p_id_usuario, p_ip),
            jsonb_build_object('ip_novo', p_ip));
    END IF;

    INSERT INTO seguranca.atividade_tempo_real
        (id_clinica, id_usuario, tabela_acessada, tipo_operacao, linhas_afetadas, ip_origem)
    VALUES (p_id_clinica, p_id_usuario, p_tabela, 'SELECT', p_linhas, p_ip);
END;
$$;

CREATE OR REPLACE FUNCTION seguranca.fn_detectar_brute_force(
    p_minutos INTEGER DEFAULT 15, p_limite INTEGER DEFAULT 5
)
RETURNS TABLE (ip_origem TEXT, tentativas_falhas BIGINT,
               primeiro_evento TIMESTAMPTZ, ultimo_evento TIMESTAMPTZ, ids_usuarios TEXT)
LANGUAGE sql STABLE AS $$
    SELECT ip_origem, COUNT(*), MIN(login_em), MAX(login_em),
           STRING_AGG(DISTINCT id_usuario::TEXT, ', ')
    FROM   seguranca.logs_acesso_sessoes
    WHERE  resultado <> 1
      AND  login_em >= NOW() - (p_minutos || ' minutes')::INTERVAL
    GROUP BY ip_origem HAVING COUNT(*) >= p_limite
    ORDER BY 2 DESC;
$$;

CREATE OR REPLACE FUNCTION seguranca.fn_verificar_exfiltracao(
    p_id_usuario INTEGER, p_id_clinica INTEGER,
    p_linhas BIGINT, p_ip VARCHAR(45)
)
RETURNS BOOLEAN LANGUAGE plpgsql AS $$
DECLARE
    v_1min  BIGINT;
    v_5min  BIGINT;
    lim_1m  BIGINT := 10000;
    lim_5m  BIGINT := 30000;
BEGIN
    SELECT COALESCE(SUM(linhas_afetadas),0) INTO v_1min
    FROM seguranca.atividade_tempo_real
    WHERE id_usuario = p_id_usuario AND registrado_em > NOW() - INTERVAL '1 minute';

    SELECT COALESCE(SUM(linhas_afetadas),0) INTO v_5min
    FROM seguranca.atividade_tempo_real
    WHERE id_usuario = p_id_usuario AND registrado_em > NOW() - INTERVAL '5 minutes';

    IF (v_1min + p_linhas) > lim_1m OR (v_5min + p_linhas) > lim_5m THEN
        INSERT INTO seguranca.alertas_anomalia
            (id_clinica, id_usuario, ip_origem, severidade, tipo_anomalia, descricao, evidencia)
        VALUES (p_id_clinica, p_id_usuario, p_ip, 4, 'EXFILTRACAO_BLOQUEADA',
            format('Taxa de exfiltração excedida: %s linhas/min', v_1min + p_linhas),
            jsonb_build_object('total_1min', v_1min, 'total_5min', v_5min,
                               'nova_query', p_linhas, 'limite_1min', lim_1m));
        RETURN FALSE;
    END IF;
    RETURN TRUE;
END;
$$;

CREATE OR REPLACE PROCEDURE seguranca.sp_registrar_honeytoken_alert(
    p_id_clinica  INTEGER,
    p_id_usuario  INTEGER,
    p_tabela      VARCHAR(80),
    p_id_registro VARCHAR(50),
    p_ip          VARCHAR(45)
)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO seguranca.alertas_anomalia
        (id_clinica, id_usuario, ip_origem, severidade, tipo_anomalia, descricao, evidencia)
    VALUES (
        p_id_clinica, p_id_usuario, p_ip, 4,
        'HONEYTOKEN_TRIGGERED',
        format('INTRUSÃO: Usuário %s acessou registro ISCA na tabela %s. '
               'Nenhuma operação legítima acessa este dado.',
               p_id_usuario, p_tabela),
        jsonb_build_object('tabela', p_tabela, 'id_registro', p_id_registro,
                           'usuario', p_id_usuario, 'ip', p_ip)
    );

    PERFORM pg_notify('honeytoken_critical', json_build_object(
        'severidade', 'CRITICO', 'usuario_id', p_id_usuario,
        'clinica_id', p_id_clinica, 'tabela', p_tabela, 'timestamp', NOW()
    )::TEXT);
END;
$$;

GRANT EXECUTE ON PROCEDURE seguranca.sp_registrar_honeytoken_alert TO dentibot_audit;

CREATE OR REPLACE PROCEDURE seguranca.sp_encerrar_sessoes_expiradas(
    p_timeout_minutos INTEGER DEFAULT 60
)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE seguranca.logs_acesso_sessoes
    SET logout_em = NOW(), sessao_revogada = TRUE, revogada_em = NOW()
    WHERE logout_em IS NULL AND sessao_revogada = FALSE
      AND login_em < NOW() - (p_timeout_minutos || ' minutes')::INTERVAL;
END;
$$;

CREATE OR REPLACE VIEW seguranca.vw_ddm_pessoas
    WITH (security_barrier = TRUE)
AS
SELECT
    id_pessoa, id_clinica, data_nascimento, sexo, cidade, uf, created_at,

    CASE current_setting('app.nivel_acesso', TRUE)
        WHEN 'full'    THEN nome_completo
        WHEN 'parcial' THEN SPLIT_PART(nome_completo,' ',1) || ' ' ||
                            LEFT(SPLIT_PART(nome_completo,' ',2),1) || '.'
        ELSE                LEFT(nome_completo,1) || REPEAT('*',5)
    END AS nome_completo,

    CASE current_setting('app.nivel_acesso', TRUE)
        WHEN 'full' THEN cpf
        ELSE             REPEAT('*',3)||'.'||REPEAT('*',3)||'.'||REPEAT('*',3)||'-'||REPEAT('*',2)
    END AS cpf,

    CASE current_setting('app.nivel_acesso', TRUE)
        WHEN 'full' THEN telefone_celular
        ELSE             SUBSTRING(telefone_celular,1,4) || ' ****-****'
    END AS telefone_celular,

    CASE current_setting('app.nivel_acesso', TRUE)
        WHEN 'full'    THEN email
        WHEN 'parcial' THEN LEFT(email,3)||REPEAT('*',4)||'@'||SPLIT_PART(email,'@',2)
        ELSE                '****@****'
    END AS email

FROM negocio.pessoas
WHERE deleted_at IS NULL;

CREATE OR REPLACE VIEW seguranca.vw_ddm_financeiro
    WITH (security_barrier = TRUE)
AS
SELECT
    cr.id_recebivel, cr.id_clinica, cr.id_orcamento,
    cr.parcela_numero, cr.parcela_total, cr.vencimento_em,
    cr.recebido_em, cr.status, fp.nome AS forma_pagamento,

    CASE current_setting('app.nivel_acesso', TRUE)
        WHEN 'full'    THEN cr.valor_parcela
        WHEN 'parcial' THEN cr.valor_parcela
        ELSE NULL
    END AS valor_parcela,

    CASE current_setting('app.nivel_acesso', TRUE)
        WHEN 'full'    THEN cr.valor_liquido_recebido
        WHEN 'parcial' THEN cr.valor_liquido_recebido
        ELSE NULL
    END AS valor_liquido_recebido

FROM   negocio.contas_receber cr
LEFT JOIN negocio.formas_pagamento fp ON fp.id_forma = cr.id_forma_pagamento;

GRANT SELECT ON seguranca.vw_ddm_pessoas    TO dentibot_readonly;
GRANT SELECT ON seguranca.vw_ddm_financeiro TO dentibot_readonly;

CREATE OR REPLACE VIEW seguranca.vw_alertas_recentes AS
SELECT
    a.id_alerta,
    CASE a.severidade WHEN 4 THEN 'CRÍTICO' WHEN 3 THEN 'ALTO'
                      WHEN 2 THEN 'MÉDIO'   ELSE 'BAIXO' END AS severidade,
    a.tipo_anomalia, a.descricao, a.ip_origem,
    a.detectado_em,
    CASE a.status WHEN 1 THEN 'Aberto' WHEN 2 THEN 'Investigando'
                  WHEN 3 THEN 'Falso Positivo' WHEN 4 THEN 'Confirmado'
                  ELSE 'Mitigado' END AS status_alerta,
    a.id_clinica, a.id_usuario
FROM   seguranca.alertas_anomalia a
WHERE  a.detectado_em >= NOW() - INTERVAL '24 hours'
  AND  a.severidade   >= 3
ORDER BY a.severidade DESC, a.detectado_em DESC;

CREATE OR REPLACE VIEW seguranca.vw_resumo_diario AS
SELECT
    NOW()::DATE                                                            AS data,
    COUNT(*) FILTER (WHERE severidade = 4)                                AS criticos,
    COUNT(*) FILTER (WHERE severidade = 3)                                AS altos,
    COUNT(*) FILTER (WHERE tipo_anomalia = 'HONEYTOKEN_TRIGGERED')        AS honeytokens,
    COUNT(*) FILTER (WHERE tipo_anomalia = 'EXFILTRACAO_BLOQUEADA')       AS tentativas_exfiltracao,
    COUNT(*) FILTER (WHERE tipo_anomalia = 'IP_NUNCA_VISTO')              AS ips_novos,
    COUNT(*) FILTER (WHERE tipo_anomalia = 'DISPOSITIVO_DESCONHECIDO')    AS dispositivos_novos,
    COUNT(*) FILTER (WHERE tipo_anomalia = 'ACESSO_HORARIO_ANOMALO')      AS acessos_fora_horario
FROM   seguranca.alertas_anomalia
WHERE  detectado_em >= NOW() - INTERVAL '24 hours';

CREATE OR REPLACE VIEW seguranca.vw_compliance_backup AS
SELECT
    MAX(realizado_em)                                        AS ultimo_teste_em,
    NOW() - MAX(realizado_em)                                AS tempo_sem_teste,
    (NOW() - MAX(realizado_em)) > INTERVAL '30 days'        AS alerta_vencido,
    COUNT(*) FILTER (WHERE sucesso = TRUE)                   AS testes_ok,
    COUNT(*) FILTER (WHERE sucesso = FALSE)                  AS testes_falhos,
    ROUND(AVG(rpo_minutos), 1)                               AS rpo_medio_minutos
FROM   seguranca.registros_teste_restauracao
WHERE  realizado_em >= NOW() - INTERVAL '1 year';

CREATE OR REPLACE VIEW seguranca.vw_gaps_criptografia AS
SELECT tabela_nome, coluna_nome,
    CASE classificacao WHEN 1 THEN 'PII' WHEN 2 THEN 'PHI (Saúde)'
                       WHEN 3 THEN 'Financeiro' WHEN 4 THEN 'Credencial'
                       ELSE 'Jurídico' END AS tipo_dado,
    responsavel_dado, base_legal_lgpd
FROM   seguranca.catalogo_dados_sensiveis
WHERE  criptografada = FALSE AND classificacao IN (1,2,4)
ORDER BY classificacao, tabela_nome;

CREATE OR REPLACE VIEW seguranca.vw_sla_pentest_vencido AS
SELECT
    v.id_vulnerabilidade, v.titulo,
    CASE v.severidade WHEN 1 THEN 'CRÍTICO' WHEN 2 THEN 'ALTO'
                      WHEN 3 THEN 'MÉDIO'   ELSE 'BAIXO' END AS severidade,
    v.cvss_score, p.data_fim AS pentest_em,
    CASE v.severidade WHEN 1 THEN p.data_fim+1 WHEN 2 THEN p.data_fim+7
                      WHEN 3 THEN p.data_fim+30 ELSE p.data_fim+90 END AS deadline,
    NOW()::DATE - CASE v.severidade
        WHEN 1 THEN p.data_fim+1 WHEN 2 THEN p.data_fim+7
        WHEN 3 THEN p.data_fim+30 ELSE p.data_fim+90 END AS dias_atrasado,
    v.responsavel_correcao
FROM   seguranca.vulnerabilidades_encontradas v
JOIN   seguranca.registros_pentest p ON p.id_pentest = v.id_pentest
WHERE  v.status IN (1,2)
  AND  CASE v.severidade WHEN 1 THEN p.data_fim+1 WHEN 2 THEN p.data_fim+7
                         WHEN 3 THEN p.data_fim+30 ELSE p.data_fim+90 END < NOW()::DATE
ORDER BY v.severidade, dias_atrasado DESC;

CREATE OR REPLACE VIEW seguranca.vw_checklist_conformidade AS
SELECT 'Backup testado < 30 dias'     AS controle,
    CASE WHEN EXISTS(SELECT 1 FROM seguranca.vw_compliance_backup WHERE alerta_vencido=FALSE)
         THEN 'OK' ELSE 'FALHA — realizar teste de restauração' END AS status
UNION ALL SELECT 'Gaps de criptografia',
    CASE WHEN EXISTS(SELECT 1 FROM seguranca.vw_gaps_criptografia)
         THEN 'FALHA — ver vw_gaps_criptografia' ELSE 'OK' END
UNION ALL SELECT 'Vulnerabilidades SLA vencido',
    CASE WHEN EXISTS(SELECT 1 FROM seguranca.vw_sla_pentest_vencido)
         THEN 'FALHA — ver vw_sla_pentest_vencido' ELSE 'OK' END
UNION ALL SELECT 'Alertas críticos nas últimas 24h',
    CASE WHEN EXISTS(SELECT 1 FROM seguranca.alertas_anomalia
                     WHERE severidade=4 AND detectado_em > NOW()-INTERVAL '24 hours')
         THEN 'ATENÇÃO — alertas críticos ativos' ELSE 'OK' END
ORDER BY status DESC;

GRANT SELECT ON seguranca.vw_alertas_recentes     TO dentibot_readonly, dentibot_monitor;
GRANT SELECT ON seguranca.vw_resumo_diario        TO dentibot_readonly, dentibot_monitor;
GRANT SELECT ON seguranca.vw_compliance_backup    TO dentibot_readonly, dentibot_monitor;
GRANT SELECT ON seguranca.vw_gaps_criptografia    TO dentibot_readonly, dentibot_monitor;
GRANT SELECT ON seguranca.vw_sla_pentest_vencido  TO dentibot_readonly, dentibot_monitor;
GRANT SELECT ON seguranca.vw_checklist_conformidade TO dentibot_monitor;

SELECT cron.schedule('limpar-atividade-rt',   '0 * * * *',
    'DELETE FROM seguranca.atividade_tempo_real WHERE registrado_em < NOW() - INTERVAL ''24 hours''');

SELECT cron.schedule('atualizar-baseline',    '0 1 * * *',
    $$ INSERT INTO seguranca.baseline_atividade_usuario
           (id_usuario, hora_do_dia, dia_da_semana, media_linhas, desvio_padrao,
            max_linhas_retornadas, media_queries_hora)
       SELECT id_usuario,
           EXTRACT(HOUR FROM registrado_em)::SMALLINT,
           EXTRACT(DOW  FROM registrado_em)::SMALLINT,
           AVG(linhas_afetadas), STDDEV(linhas_afetadas),
           MAX(linhas_afetadas), COUNT(*)/90.0
       FROM   seguranca.atividade_tempo_real
       WHERE  registrado_em > NOW() - INTERVAL '90 days'
       GROUP BY id_usuario, EXTRACT(HOUR FROM registrado_em), EXTRACT(DOW FROM registrado_em)
       ON CONFLICT (id_usuario, hora_do_dia, dia_da_semana) DO UPDATE
           SET media_linhas=EXCLUDED.media_linhas, desvio_padrao=EXCLUDED.desvio_padrao,
               max_linhas_retornadas=EXCLUDED.max_linhas_retornadas,
               media_queries_hora=EXCLUDED.media_queries_hora, ultima_atualizacao=NOW(); $$);

SELECT cron.schedule('encerrar-sessoes',       '*/15 * * * *',
    'CALL seguranca.sp_encerrar_sessoes_expiradas(60)');

SELECT cron.schedule('criar-particao-futura',  '0 0 1 12 *',
    $$ DO $$ DECLARE v_ano INTEGER := EXTRACT(YEAR FROM NOW())::INTEGER+1;
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_class WHERE relname='logs_auditoria_'||v_ano) THEN
            EXECUTE format('CREATE TABLE seguranca.logs_auditoria_%s
                PARTITION OF seguranca.logs_auditoria_dados
                FOR VALUES FROM (''%s-01-01'') TO (''%s-01-01'')',
                v_ano, v_ano, v_ano+1);
        END IF;
    END; $$);

SELECT cron.schedule('arquivar-particoes',     '0 3 1 1 *',
    $$ DO $$ DECLARE v RECORD; lim INTEGER := EXTRACT(YEAR FROM NOW())::INTEGER-2;
    BEGIN
        FOR v IN SELECT relname FROM pg_class
                 WHERE relname LIKE 'logs_auditoria_%' AND relkind='r'
                   AND substring(relname FROM '\d{4}$')::INTEGER < lim
        LOOP
            EXECUTE 'ALTER TABLE seguranca.logs_auditoria_dados DETACH PARTITION '||quote_ident(v.relname)||' CONCURRENTLY';
            EXECUTE 'REVOKE INSERT,UPDATE,DELETE ON '||quote_ident(v.relname)||' FROM PUBLIC';
            INSERT INTO seguranca.controle_arquivamento_particoes (nome_particao,detachada_em,status)
            VALUES (v.relname, NOW(), 2) ON CONFLICT DO NOTHING;
        END LOOP;
    END; $$);
