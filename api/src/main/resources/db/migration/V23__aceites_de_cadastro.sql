-- ─────────────────────────────────────────────────────────────────────────────
-- V23 — Prova do aceite feito no cadastro (LGPD art. 8º §2º).
--
-- O ônus de provar o consentimento é de quem coleta. O cadastro — de clínica e
-- de paciente — roda SEM tenant, então lgpd.consentimentos (que exige
-- id_clinica e id_paciente) não serve: aqui o titular é o `sub` do Clerk.
--
-- Uma linha por envio, nunca editada: aceitar a versão nova dos termos é outra
-- linha, e a anterior continua provando o que a pessoa aceitou naquela época.
-- aceito_em é a hora do SERVIDOR; a hora que o navegador mandaria é a que o
-- navegador escolheu.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE lgpd.aceites_cadastro (
    id_aceite            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clerk_user_id        VARCHAR(255) NOT NULL,
    origem               TEXT         NOT NULL CHECK (origem IN ('clinica', 'paciente')),
    versao_termos        VARCHAR(10)  NOT NULL,

    -- Sem aceite dos termos não há cadastro: o CHECK impede gravar "recusou".
    termos_e_privacidade BOOLEAN      NOT NULL CHECK (termos_e_privacidade),
    -- Específico e destacado (art. 11 I). NULL = não se aplica à origem.
    dados_de_saude       BOOLEAN      NULL,
    -- Art. 14 §1º. NULL quando o paciente é maior de idade.
    responsavel_legal    BOOLEAN      NULL,
    -- Quem cadastra a clínica declara poder contratar por ela.
    representacao        BOOLEAN      NULL,
    novidades            BOOLEAN      NOT NULL DEFAULT FALSE,

    aceito_em            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ip_origem            INET         NOT NULL,
    user_agent           VARCHAR(500) NOT NULL,

    CONSTRAINT ck_aceite_clinica_representa
        CHECK (origem <> 'clinica' OR representacao IS TRUE)
);

-- Escrita apenas, como pacientes.pre_cadastros (V20): o leitor legítimo — a
-- tela de direitos do titular — traz junto o SELECT e a regra de quem lê.
ALTER TABLE lgpd.aceites_cadastro ENABLE ROW LEVEL SECURITY;
ALTER TABLE lgpd.aceites_cadastro FORCE  ROW LEVEL SECURITY;

CREATE POLICY somente_insercao ON lgpd.aceites_cadastro
    FOR INSERT TO dentibot_app
    WITH CHECK (true);

GRANT  INSERT ON lgpd.aceites_cadastro TO dentibot_app;
REVOKE SELECT, UPDATE, DELETE ON lgpd.aceites_cadastro FROM dentibot_app;

COMMENT ON TABLE lgpd.aceites_cadastro IS
    'Prova do aceite de termos, privacidade e consentimentos no cadastro sem '
    'tenant. Append-only: IP e user-agent vêm da requisição, a hora do servidor.';
