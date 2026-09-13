-- ─────────────────────────────────────────────────────────────────────────────
-- V20 — Quem se cadastra com CPF, e a ficha que a anamnese exige.
--
-- Duas lacunas que apareceram juntas quando o auto-cadastro passou a ser a
-- porta de entrada de quem cria conta pelo Google, Microsoft ou Apple:
--
--   1. o dentista autônomo não tem CNPJ — e precisa de tenant igual, senão o
--      ResolvedorDeAcessoClerk devolve vazio e ele toma 401 em toda tela;
--   2. a ficha do paciente parava em nome, CPF, telefone e e-mail. Faltava
--      justamente o que muda conduta clínica: data de nascimento (dose de
--      anestésico), alergia, condição sistêmica, gravidez.
-- ─────────────────────────────────────────────────────────────────────────────

-- ─── 1. Documento da clínica: CNPJ (14) ou CPF (11) ──────────────────────────
-- A coluna continua se chamando `cnpj` de propósito. Renomear atinge a função
-- de provisionamento, o domínio, o DTO e o front — e o ganho seria um nome mais
-- bonito para a mesma coluna. O CHECK é que passa a dizer o que ela aceita.
ALTER TABLE clinicas.clinicas
    ALTER COLUMN cnpj TYPE VARCHAR(14);

ALTER TABLE clinicas.clinicas
    ADD CONSTRAINT ck_clinica_documento
    CHECK (cnpj ~ '^([0-9]{11}|[0-9]{14})$');

COMMENT ON COLUMN clinicas.clinicas.cnpj IS
    'Documento do titular: CNPJ (14 dígitos) da clínica ou CPF (11) do dentista '
    'autônomo. Sem pontuação. UNIQUE vale para os dois — a mesma pessoa não abre '
    'duas clínicas com o mesmo documento.';

-- A função de provisionamento tinha CHAR(14) na assinatura, e CHAR completa com
-- espaços: um CPF entraria no banco como '52998224725   ' e nunca mais casaria
-- com a busca por 11 dígitos. Trocar o tipo é criar outra função — CREATE OR
-- REPLACE não muda assinatura —, então é DROP e CREATE, com a mesma dança de
-- posse da V11. O migrador é membro de dentibot_provisionador (V1), que é o que
-- lhe permite dropar e reatribuir a posse.
GRANT CREATE ON SCHEMA clinicas TO dentibot_provisionador;

DROP FUNCTION clinicas.provisionar(CHAR, VARCHAR, VARCHAR, TEXT, TEXT);

CREATE FUNCTION clinicas.provisionar(
    p_documento     VARCHAR(14),
    p_razao_social  VARCHAR(144),
    p_nome_fantasia VARCHAR(60),
    p_timezone      TEXT DEFAULT 'America/Sao_Paulo',
    p_plano         TEXT DEFAULT 'solo'
)
    RETURNS BIGINT
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = clinicas, pg_catalog
AS $$
DECLARE
    v_id BIGINT;
BEGIN
    INSERT INTO clinicas.clinicas (cnpj, razao_social, nome_fantasia, timezone, plano)
    VALUES (p_documento, p_razao_social, p_nome_fantasia, p_timezone, p_plano)
    RETURNING id_clinica INTO v_id;

    RETURN v_id;
END
$$;

ALTER FUNCTION clinicas.provisionar(VARCHAR, VARCHAR, VARCHAR, TEXT, TEXT)
    OWNER TO dentibot_provisionador;

COMMENT ON FUNCTION clinicas.provisionar IS
    'Única porta de criação de tenant. Devolve o id_clinica; o chamador faz '
    'set_config(''app.clinica'', <id>, true) e segue inserindo configurações, '
    'pessoa e usuário admin na MESMA transação.';

REVOKE EXECUTE ON FUNCTION clinicas.provisionar(VARCHAR, VARCHAR, VARCHAR, TEXT, TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION clinicas.provisionar(VARCHAR, VARCHAR, VARCHAR, TEXT, TEXT) TO dentibot_app;

REVOKE CREATE ON SCHEMA clinicas FROM dentibot_provisionador;

-- ─── 2. Ficha: o que faltava em identidade.pessoas ───────────────────────────
-- Endereço, CPF, data de nascimento e telefones já estavam lá desde a V3. Estes
-- três não.
ALTER TABLE identidade.pessoas
    ADD COLUMN rg                VARCHAR(20)  NULL,
    ADD COLUMN profissao         VARCHAR(80)  NULL,
    -- Não é FK para outra pessoa: o responsável costuma não ser paciente da
    -- clínica, e exigir que ele vire linha para poder assinar por um menor é
    -- criar cadastro fantasma a cada criança atendida.
    ADD COLUMN responsavel_legal VARCHAR(150) NULL;

COMMENT ON COLUMN identidade.pessoas.responsavel_legal IS
    'Obrigatório para menor de idade — a regra é da aplicação, e não um CHECK, '
    'porque data_nascimento é nula em cadastro antigo migrado e o CHECK '
    'reprovaria a linha inteira na primeira atualização de telefone.';

-- ─── 3. Triagem de saúde ─────────────────────────────────────────────────────
-- JSONB e não uma coluna por pergunta: o questionário é um FORMULÁRIO, e
-- formulário muda — a cada pergunta nova seria uma migração e um deploy. Nada
-- aqui é consultado por índice hoje.
-- ponytail: JSONB sem índice. Vira coluna (ou GIN) quando alguma tela precisar
-- filtrar por resposta — "quem é alérgico a látex", por exemplo.
ALTER TABLE pacientes.pacientes
    ADD COLUMN anamnese JSONB NULL;

COMMENT ON COLUMN pacientes.pacientes.anamnese IS
    'Triagem inicial: tratamento em curso, medicamento contínuo, alergia, '
    'condição sistêmica, gravidez, motivo da consulta, sensibilidade e '
    'sangramento gengival. Dado de saúde (LGPD art. 11): só a equipe clínica lê.';

-- ─── 4. Pré-cadastro do paciente que ainda não tem clínica ───────────────────
-- Quem cria conta com CPF e se declara paciente não pertence a tenant nenhum
-- ainda — não há id_clinica para pendurar a linha. A ficha fica aqui até a
-- clínica vinculá-la pelo CPF.
--
-- A tabela é DE ESCRITA APENAS para a aplicação: sem política de SELECT, com
-- FORCE ligado, um SELECT devolve zero linhas para todo mundo — inclusive para
-- quem inseriu. É de propósito: enquanto não existe o fluxo de vínculo, não
-- existe leitor legítimo, e ficha de saúde sem dono é exatamente o tipo de
-- tabela que vaza por um endpoint distraído.
CREATE TABLE pacientes.pre_cadastros (
    id_pre_cadastro BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Dono da ficha: o `sub` do Clerk de quem preencheu.
    clerk_user_id   VARCHAR(255) NOT NULL,

    nome_completo   VARCHAR(150) NOT NULL,
    cpf             CHAR(11)     NULL CHECK (cpf ~ '^[0-9]{11}$'),
    rg              VARCHAR(20)  NULL,
    data_nascimento DATE         NULL,
    telefone_celular VARCHAR(20) NULL,
    email           VARCHAR(254) NULL,
    profissao       VARCHAR(80)  NULL,
    responsavel_legal VARCHAR(150) NULL,

    cep             CHAR(8)      NULL CHECK (cep ~ '^[0-9]{8}$'),
    logradouro      VARCHAR(255) NULL,
    numero          VARCHAR(20)  NULL,
    complemento     VARCHAR(100) NULL,
    bairro          VARCHAR(100) NULL,
    cidade          VARCHAR(100) NULL,
    uf              CHAR(2)      NULL,

    anamnese        JSONB        NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Append-only, como auditoria e evolução clínica: reenviar a ficha grava outra
-- linha, e quem for ler pega a mais recente do `sub`. UPSERT exigiria UPDATE, e
-- UPDATE sem SELECT é um privilégio que só serve para sobrescrever ficha alheia
-- às cegas. Sem índice por enquanto — ele entra junto com o primeiro leitor.

ALTER TABLE pacientes.pre_cadastros ENABLE ROW LEVEL SECURITY;
ALTER TABLE pacientes.pre_cadastros FORCE  ROW LEVEL SECURITY;

CREATE POLICY somente_insercao ON pacientes.pre_cadastros
    FOR INSERT TO dentibot_app
    WITH CHECK (true);

-- Privilégio e política são avaliados em conjunto; tirar o privilégio é a
-- camada que não depende de a política estar certa.
REVOKE SELECT, UPDATE, DELETE ON pacientes.pre_cadastros FROM dentibot_app;

COMMENT ON TABLE pacientes.pre_cadastros IS
    'Ficha de quem criou conta como paciente antes de qualquer clínica '
    'vinculá-lo. Sem id_clinica porque não há tenant ainda. Escrita apenas: o '
    'fluxo de vínculo por CPF (e o SELECT que ele vai precisar) ainda não '
    'existe, e liberar leitura antes dele é abrir ficha de saúde sem dono.';
