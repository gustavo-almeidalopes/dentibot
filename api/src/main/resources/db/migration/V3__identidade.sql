-- ─────────────────────────────────────────────────────────────────────────────
-- V3 — Módulo identidade. Pessoas, usuários da clínica (eixo A) e staff da
-- plataforma (eixo B), em tabelas separadas como manda a camada 5.
--
-- Diferenças deliberadas em relação ao schema da V1:
--   · `papeis_permissoes` com JSONB de privilégios saiu. O papel é um TEXT com
--     CHECK e a matriz vive num PermissionEvaluator só (invariante 3). JSONB de
--     privilégio por clínica é um segundo lugar que decide permissão.
--   · `funcionarios` saiu. Um usuário com papel='recepcionista' É o funcionário;
--     a tabela só duplicava `cargo`. `dentistas` ficou porque CRO/UF é dado real.
--   · staff da plataforma ganhou tabela própria, sem id_clinica — era
--     irrepresentável na V1, onde usuarios_sistema.id_clinica é NOT NULL.
--
-- Padrão de FK composta: toda referência entre tabelas do mesmo tenant carrega
-- id_clinica junto, contra a chave única (id_pai, id_clinica) do pai. Assim um
-- usuário da clínica A apontando para uma pessoa da clínica B é rejeitado pelo
-- banco, e não depende de ninguém lembrar de conferir. O RLS protege a leitura;
-- a FK composta protege a escrita.
-- ─────────────────────────────────────────────────────────────────────────────

-- ─── Pessoas: o dado pessoal de qualquer pessoa física da clínica ────────────
CREATE TABLE identidade.pessoas (
    id_pessoa        BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica       BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),

    nome_completo    VARCHAR(150) NOT NULL,
    cpf              CHAR(11)     NULL CHECK (cpf ~ '^[0-9]{11}$'),
    data_nascimento  DATE         NULL,
    sexo             TEXT         NULL CHECK (sexo IN ('F','M','OUTRO','NAO_INFORMADO')),

    telefone_celular VARCHAR(20)  NULL,
    telefone_fixo    VARCHAR(20)  NULL,
    email            VARCHAR(254) NULL,

    cep              CHAR(8)      NULL CHECK (cep ~ '^[0-9]{8}$'),
    logradouro       VARCHAR(255) NULL,
    numero           VARCHAR(20)  NULL,
    complemento      VARCHAR(100) NULL,
    bairro           VARCHAR(100) NULL,
    cidade           VARCHAR(100) NULL,
    uf               CHAR(2)      NULL,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ  NULL,

    -- Alvo das FKs compostas. Redundante como chave, essencial como guarda.
    CONSTRAINT uq_pessoas_tenant UNIQUE (id_pessoa, id_clinica)
);

-- CPF é único dentro da clínica, não globalmente: a mesma pessoa pode ser
-- paciente em duas clínicas e são dois cadastros, de dois controladores
-- diferentes. Índice parcial porque CPF é opcional (menor de idade sem CPF).
CREATE UNIQUE INDEX uq_pessoas_clinica_cpf
    ON identidade.pessoas (id_clinica, cpf)
    WHERE cpf IS NOT NULL AND deleted_at IS NULL;

-- Todo índice começa por id_clinica: depois do RLS, toda query ganha
-- `WHERE id_clinica = ...` e um índice que não começa por ele vira Seq Scan.
CREATE INDEX idx_pessoas_clinica_nome ON identidade.pessoas (id_clinica, nome_completo)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_pessoas_updated_at
    BEFORE UPDATE ON identidade.pessoas
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Eixo A: usuários da clínica ─────────────────────────────────────────────
CREATE TABLE identidade.usuarios (
    id_usuario        BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica        BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_pessoa         BIGINT       NOT NULL,

    -- Global, não por clínica: sem isso o login precisaria perguntar "qual
    -- clínica?" antes da senha. Limitação conhecida — dentista que atende em
    -- duas clínicas precisa de dois e-mails. Ver docs/architecture/identidade.md.
    email             VARCHAR(254) NOT NULL UNIQUE,

    -- Argon2id, emitido pelo Spring Security. Nunca crypt()/bcrypt do pgcrypto:
    -- o banco não é o lugar de decidir política de senha, e o cost 6 default do
    -- gen_salt('bf') é fraco.
    senha_hash        VARCHAR(255) NOT NULL,

    papel             TEXT         NOT NULL
                                   CHECK (papel IN ('admin','dentista','recepcionista','financeiro','auxiliar')),
    status            TEXT         NOT NULL DEFAULT 'ativo'
                                   CHECK (status IN ('ativo','bloqueado','desativado')),

    metodo_2fa        TEXT         NOT NULL DEFAULT 'nenhum'
                                   CHECK (metodo_2fa IN ('nenhum','email','totp')),
    totp_secret       VARCHAR(255) NULL,
    codigo_2fa_hash   VARCHAR(255) NULL,
    codigo_2fa_expira_em  TIMESTAMPTZ NULL,
    codigo_2fa_tentativas SMALLINT NOT NULL DEFAULT 0,

    ultimo_login_em   TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_usuario_pessoa
        FOREIGN KEY (id_pessoa, id_clinica)
        REFERENCES identidade.pessoas (id_pessoa, id_clinica),
    CONSTRAINT uq_usuarios_tenant UNIQUE (id_usuario, id_clinica)
);

CREATE INDEX idx_usuarios_clinica ON identidade.usuarios (id_clinica, status);

CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON identidade.usuarios
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Dentistas ───────────────────────────────────────────────────────────────
CREATE TABLE identidade.dentistas (
    id_dentista   BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica    BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_pessoa     BIGINT      NOT NULL,
    -- Nem todo dentista tem login (o que faz plantão e não usa o sistema).
    id_usuario    BIGINT      NULL,

    cro_numero    VARCHAR(20) NOT NULL,
    cro_uf        CHAR(2)     NOT NULL,
    especialidade VARCHAR(80) NULL,

    status        TEXT        NOT NULL DEFAULT 'ativo'
                              CHECK (status IN ('ativo','inativo')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_dentista_pessoa
        FOREIGN KEY (id_pessoa, id_clinica)
        REFERENCES identidade.pessoas (id_pessoa, id_clinica),
    CONSTRAINT fk_dentista_usuario
        FOREIGN KEY (id_usuario, id_clinica)
        REFERENCES identidade.usuarios (id_usuario, id_clinica),
    CONSTRAINT uq_dentistas_tenant UNIQUE (id_dentista, id_clinica)
);

-- CRO é único por UF em todo o país; dentro da clínica, nunca duplicado.
CREATE UNIQUE INDEX uq_dentistas_clinica_cro
    ON identidade.dentistas (id_clinica, cro_uf, cro_numero);

CREATE UNIQUE INDEX uq_dentistas_usuario
    ON identidade.dentistas (id_usuario) WHERE id_usuario IS NOT NULL;

CREATE TRIGGER trg_dentistas_updated_at
    BEFORE UPDATE ON identidade.dentistas
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Bloqueio por tentativa de login ─────────────────────────────────────────
CREATE TABLE identidade.bloqueio_login (
    id_usuario          BIGINT      PRIMARY KEY REFERENCES identidade.usuarios(id_usuario),
    id_clinica          BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    falhas_consecutivas SMALLINT    NOT NULL DEFAULT 0,
    bloqueado_ate       TIMESTAMPTZ NULL,
    ultima_falha_em     TIMESTAMPTZ NULL
);

-- ─── Eixo B: staff da plataforma. Sem id_clinica, por definição. ─────────────
CREATE TABLE identidade.staff_plataforma (
    id_staff        BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(254) NOT NULL UNIQUE,
    nome            VARCHAR(150) NOT NULL,
    senha_hash      VARCHAR(255) NOT NULL,

    papel           TEXT         NOT NULL
                                 CHECK (papel IN ('suporte_n1','ops_billing','engenharia')),
    status          TEXT         NOT NULL DEFAULT 'ativo'
                                 CHECK (status IN ('ativo','bloqueado','desativado')),

    -- 2FA é obrigatório para staff: essa conta vê dado de todas as clínicas.
    metodo_2fa      TEXT         NOT NULL DEFAULT 'totp'
                                 CHECK (metodo_2fa IN ('email','totp')),
    totp_secret     VARCHAR(255) NULL,
    codigo_2fa_hash VARCHAR(255) NULL,
    codigo_2fa_expira_em  TIMESTAMPTZ NULL,
    codigo_2fa_tentativas SMALLINT NOT NULL DEFAULT 0,

    ultimo_login_em TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_staff_updated_at
    BEFORE UPDATE ON identidade.staff_plataforma
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Resolução de contexto antes do login ────────────────────────────────────
-- No momento do login ainda não existe tenant: o cliente mandou só e-mail e
-- senha, e a tabela de usuários está protegida por RLS. Alguém precisa
-- atravessar o isolamento para descobrir de qual clínica é aquele e-mail.
--
-- ARMADILHA, e custou uma sessão de depuração aqui: SECURITY DEFINER **não
-- contorna RLS**. Ele só troca QUEM a função é. Com FORCE ROW LEVEL SECURITY
-- ligado, nem o dono da tabela escapa — e um role que não é nomeado por nenhuma
-- política (o caso do migrador, já que as políticas são TO dentibot_app) enxerga
-- ZERO linhas. A função "funcionava", devolvia NULL, e o login respondia
-- "credenciais inválidas" para a senha certa.
--
-- A solução é a mesma do provisionamento: dar à travessia um SUJEITO próprio,
-- com política explícita e privilégio POR COLUNA. `dentibot_autenticador` lê
-- exatamente duas colunas de identidade.usuarios — nem o hash da senha ele
-- alcança.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'dentibot_autenticador') THEN
        CREATE ROLE dentibot_autenticador NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;

GRANT dentibot_autenticador TO dentibot_migrador;  -- para transferir a posse
GRANT USAGE, CREATE ON SCHEMA identidade TO dentibot_autenticador;

-- Privilégio por coluna: o resolvedor de clínica lê id_clinica e email, e nada
-- mais. Se algum dia esta função crescer e tentar ler senha_hash, o Postgres
-- recusa.
GRANT SELECT (id_clinica, email) ON identidade.usuarios TO dentibot_autenticador;

CREATE POLICY autenticacao ON identidade.usuarios
    FOR SELECT TO dentibot_autenticador
    USING (true);

CREATE OR REPLACE FUNCTION identidade.resolver_clinica_por_email(p_email TEXT)
    RETURNS BIGINT
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = identidade, pg_catalog
AS $$
    SELECT id_clinica FROM identidade.usuarios WHERE email = lower(p_email)
$$;

ALTER FUNCTION identidade.resolver_clinica_por_email(TEXT) OWNER TO dentibot_autenticador;
REVOKE EXECUTE ON FUNCTION identidade.resolver_clinica_por_email(TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION identidade.resolver_clinica_por_email(TEXT) TO dentibot_app;

-- Staff não tem clínica, então não há GUC de tenant para destravar o RLS no
-- login. Aqui o hash É necessário — é o fluxo de autenticação inteiro — mas a
-- função devolve só as colunas dele, nada de nome, histórico ou qualquer outra
-- coisa da tabela.
GRANT SELECT (id_staff, email, senha_hash, papel, status, metodo_2fa, totp_secret)
    ON identidade.staff_plataforma TO dentibot_autenticador;

CREATE POLICY autenticacao ON identidade.staff_plataforma
    FOR SELECT TO dentibot_autenticador
    USING (true);

CREATE OR REPLACE FUNCTION identidade.autenticacao_staff(p_email TEXT)
    RETURNS TABLE (
        id_staff    BIGINT,
        senha_hash  VARCHAR(255),
        papel       TEXT,
        status      TEXT,
        metodo_2fa  TEXT,
        totp_secret VARCHAR(255)
    )
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = identidade, pg_catalog
AS $$
    SELECT s.id_staff, s.senha_hash, s.papel, s.status, s.metodo_2fa, s.totp_secret
    FROM identidade.staff_plataforma s
    WHERE s.email = lower(p_email)
$$;

ALTER FUNCTION identidade.autenticacao_staff(TEXT) OWNER TO dentibot_autenticador;
REVOKE EXECUTE ON FUNCTION identidade.autenticacao_staff(TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION identidade.autenticacao_staff(TEXT) TO dentibot_app;

-- Trabalho do autenticador terminado: ele volta a não poder criar nada.
REVOKE CREATE ON SCHEMA identidade FROM dentibot_autenticador;

-- ─── RLS ─────────────────────────────────────────────────────────────────────
ALTER TABLE identidade.pessoas         ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidade.pessoas         FORCE  ROW LEVEL SECURITY;
ALTER TABLE identidade.usuarios        ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidade.usuarios        FORCE  ROW LEVEL SECURITY;
ALTER TABLE identidade.dentistas       ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidade.dentistas       FORCE  ROW LEVEL SECURITY;
ALTER TABLE identidade.bloqueio_login  ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidade.bloqueio_login  FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON identidade.pessoas
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON identidade.usuarios
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON identidade.dentistas
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON identidade.bloqueio_login
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

-- staff_plataforma não tem id_clinica e não pertence a tenant nenhum.
-- RLS fecha a tabela: só quem está autenticado como staff a enxerga.
ALTER TABLE identidade.staff_plataforma ENABLE ROW LEVEL SECURITY;
ALTER TABLE identidade.staff_plataforma FORCE  ROW LEVEL SECURITY;

CREATE POLICY staff_somente ON identidade.staff_plataforma
    FOR ALL TO dentibot_app
    USING      (plataforma.staff_atual() IS NOT NULL)
    WITH CHECK (plataforma.staff_atual() IS NOT NULL);
