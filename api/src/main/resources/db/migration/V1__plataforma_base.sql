-- ─────────────────────────────────────────────────────────────────────────────
-- V1 — Base da plataforma: extensões, roles, schemas de módulo, contexto de tenant.
--
-- Roda como `dentibot_migrador`, que é DONO de tudo. A aplicação conecta como
-- `dentibot_app`, que não tem posse e não é superusuário — sem isso o Postgres
-- deixa o dono ignorar RLS e as políticas viram decoração (foi o que aconteceu
-- na V1 do projeto: 31 políticas pretendidas, 0 criadas, vazamento entre clínicas
-- comprovado). Ver docs/architecture/tenancy.md.
--
-- Nenhuma senha aqui: roles nascem NOLOGIN e o provisionamento
-- (infrastructure/init.ps1 ou o console do Neon) faz o ALTER ROLE ... PASSWORD
-- a partir de variável de ambiente. Invariante 12.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- digest() para o encadeamento da auditoria
CREATE EXTENSION IF NOT EXISTS btree_gist; -- EXCLUDE de agenda: (id_dentista =, periodo &&)

-- ─── Roles ───────────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'dentibot_app') THEN
        CREATE ROLE dentibot_app NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'dentibot_migrador') THEN
        CREATE ROLE dentibot_migrador NOLOGIN NOSUPERUSER NOCREATEDB;
    END IF;
    -- Role sem login, que existe só para ser o dono da função de provisionamento
    -- de clínica (V11). Criar tenant é a única escrita do sistema que não pode
    -- ser feita de dentro de um tenant; em vez de abrir exceção na política,
    -- ela ganha um sujeito próprio. Ver V11__onboarding.sql.
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'dentibot_provisionador') THEN
        CREATE ROLE dentibot_provisionador NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;

-- O migrador precisa ser membro para poder transferir a posse da função.
GRANT dentibot_provisionador TO dentibot_migrador;

-- ─── Schemas: um por módulo (camada 7) ───────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS plataforma;
CREATE SCHEMA IF NOT EXISTS clinicas;
CREATE SCHEMA IF NOT EXISTS identidade;
CREATE SCHEMA IF NOT EXISTS pacientes;
CREATE SCHEMA IF NOT EXISTS agenda;
CREATE SCHEMA IF NOT EXISTS prontuario;
CREATE SCHEMA IF NOT EXISTS orcamento;
CREATE SCHEMA IF NOT EXISTS financeiro;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS estoque;
CREATE SCHEMA IF NOT EXISTS lgpd;
CREATE SCHEMA IF NOT EXISTS auditoria;

GRANT USAGE ON SCHEMA plataforma, clinicas, identidade, pacientes, agenda,
                      prontuario, orcamento, financeiro, billing, estoque,
                      lgpd, auditoria
    TO dentibot_app;

-- A app nunca cria objeto. Migration é o único caminho para mudar schema (invariante 13).
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- Toda tabela criada daqui em diante pelo migrador já nasce acessível à app.
-- Sem isto, cada migration precisaria lembrar de dar GRANT — e a que esquecer
-- falha só em produção, no primeiro request.
ALTER DEFAULT PRIVILEGES FOR ROLE dentibot_migrador
    IN SCHEMA plataforma, clinicas, identidade, pacientes, agenda, prontuario,
              orcamento, financeiro, billing, estoque, lgpd, auditoria
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO dentibot_app;

ALTER DEFAULT PRIVILEGES FOR ROLE dentibot_migrador
    IN SCHEMA plataforma, clinicas, identidade, pacientes, agenda, prontuario,
              orcamento, financeiro, billing, estoque, lgpd, auditoria
    GRANT USAGE, SELECT ON SEQUENCES TO dentibot_app;

-- ─── Contexto de tenant ──────────────────────────────────────────────────────
-- Lida por TODA política de RLS. Três detalhes, cada um já custou vazamento em
-- algum projeto:
--
--   1. current_setting(..., true) — o `true` é missing_ok. Sem ele, requisição
--      sem tenant definido levanta exceção em vez de filtrar.
--   2. NULLIF(..., '') — GUC não definida devolve string vazia, e ''::BIGINT
--      estoura. Com NULLIF vira NULL.
--   3. NULL não casa com nada, nem com NULL. Logo `id_clinica = NULL` devolve
--      zero linhas: falha fechando, que é o comportamento certo.
--
-- SET search_path = pg_catalog impede que alguém plante uma função homônima
-- num schema anterior do search_path e sequestre a decisão.
CREATE OR REPLACE FUNCTION plataforma.clinica_atual()
    RETURNS BIGINT
    LANGUAGE sql
    STABLE
    SET search_path = pg_catalog
AS $$
    SELECT NULLIF(current_setting('app.clinica', true), '')::BIGINT
$$;

COMMENT ON FUNCTION plataforma.clinica_atual() IS
    'Tenant da transação corrente. Definido por set_config(''app.clinica'', ?, true) '
    '— o terceiro argumento é is_local: some no fim da transação. Nunca use SET: '
    'com pool de conexões o valor sobrevive e a próxima requisição herda o tenant '
    'da anterior, em silêncio.';

GRANT EXECUTE ON FUNCTION plataforma.clinica_atual() TO dentibot_app;

-- Usuário da transação corrente. Usado pela auditoria para saber quem leu/escreveu.
CREATE OR REPLACE FUNCTION plataforma.usuario_atual()
    RETURNS BIGINT
    LANGUAGE sql
    STABLE
    SET search_path = pg_catalog
AS $$
    SELECT NULLIF(current_setting('app.usuario', true), '')::BIGINT
$$;

GRANT EXECUTE ON FUNCTION plataforma.usuario_atual() TO dentibot_app;

-- Papel de staff da plataforma (eixo B), quando houver. O token carrega OU
-- clinica_id+papel, OU staff_role — nunca os dois (camada 5). Por construção,
-- nenhuma tabela clínica (pacientes, prontuario, agenda, lgpd) ganha política
-- que consulte esta função: staff da plataforma não lê dado clínico nem se
-- quiser, e isso é verificado por teste, não por revisão de código.
CREATE OR REPLACE FUNCTION plataforma.staff_atual()
    RETURNS TEXT
    LANGUAGE sql
    STABLE
    SET search_path = pg_catalog
AS $$
    SELECT NULLIF(current_setting('app.staff', true), '')
$$;

GRANT EXECUTE ON FUNCTION plataforma.staff_atual() TO dentibot_app;

-- Correlation-id da requisição corrente. Atravessa request → evento → worker → banco.
CREATE OR REPLACE FUNCTION plataforma.correlacao_atual()
    RETURNS UUID
    LANGUAGE sql
    STABLE
    SET search_path = pg_catalog
AS $$
    SELECT NULLIF(current_setting('app.correlacao', true), '')::UUID
$$;

GRANT EXECUTE ON FUNCTION plataforma.correlacao_atual() TO dentibot_app;

-- ─── Gatilho de updated_at ───────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION plataforma.tocar_updated_at()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END
$$;
