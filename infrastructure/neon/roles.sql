-- ─────────────────────────────────────────────────────────────────────────────
-- Provisionamento dos roles no Neon. Roda UMA vez, no SQL Editor do console,
-- com o banco "dentibot" selecionado e como o dono dele (neondb_owner).
--
-- É o equivalente de postgres/init/01-roles.sql para o banco de produção, e o
-- ponto é o mesmo: `dentibot_app` NÃO é superusuária e NÃO é dona de nenhuma
-- tabela. Qualquer uma das duas faz o Postgres ignorar o RLS, e o isolamento
-- entre clínicas vira decoração sem erro nenhum. Ver docs/architecture/tenancy.md.
--
-- Os dois roles nascem aqui, por SQL, e não pela aba Roles do console: role
-- criado pelo console entra em neon_superuser e ganha CREATEROLE e CREATEDB.
--
-- Antes de rodar, troque as duas senhas por valores longos e aleatórios (o
-- Neon recusa senha fraca) e guarde-as: vão para a Render como
-- DENTIBOT_DB_MIGRADOR_PASSWORD e DENTIBOT_DB_PASSWORD. Nenhuma senha volta
-- para este arquivo — o gitleaks é bloqueante.
-- ─────────────────────────────────────────────────────────────────────────────

-- Dono do schema. A API roda o Flyway com ele no start.
-- CREATEROLE porque a V1__plataforma_base.sql cria os roles sem login
-- (provisionador, autenticador) que são donos das funções SECURITY DEFINER.
CREATE ROLE dentibot_migrador
    LOGIN
    NOSUPERUSER
    CREATEROLE
    NOCREATEDB
    PASSWORD 'TROQUE-PELA-SENHA-DO-MIGRADOR';

-- A aplicação. Sem posse, sem superusuário, sem criar nada.
CREATE ROLE dentibot_app
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    PASSWORD 'TROQUE-PELA-SENHA-DA-APP';

-- CREATE no banco é o que o migrador precisa para criar os schemas e as
-- extensões confiáveis (pgcrypto, btree_gist). A posse do banco continua com
-- neondb_owner: o migrador não precisa dela, e trocá-la no PG16 pede SET ROLE.
GRANT CREATE, CONNECT ON DATABASE dentibot TO dentibot_migrador;
GRANT CONNECT         ON DATABASE dentibot TO dentibot_app;

-- Sem isto, qualquer role conectado poderia criar tabela no schema public.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
