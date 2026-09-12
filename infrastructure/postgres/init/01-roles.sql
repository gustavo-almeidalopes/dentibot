-- ─────────────────────────────────────────────────────────────────────────────
-- Provisionamento dos roles. Roda uma vez, na criação do volume do Postgres.
-- Em produção (Neon) o equivalente é feito uma vez no console, com senhas vindas
-- do gerenciador de segredos.
--
-- O ponto inteiro deste arquivo: `dentibot_app` NÃO é superusuária e NÃO é dona
-- de nenhuma tabela. Qualquer uma das duas coisas faz o Postgres ignorar as
-- políticas de RLS, e todo o isolamento multi-tenant vira decoração — sem erro,
-- sem aviso, sem teste vermelho, a menos que alguém escreva o teste.
--
-- (Foi exatamente o que aconteceu na V1 deste projeto: a aplicação conectava com
--  o role que criara as tabelas.)
-- ─────────────────────────────────────────────────────────────────────────────

-- Dono do schema. O Flyway conecta com ele.
CREATE ROLE dentibot_migrador
    LOGIN
    NOSUPERUSER
    -- CREATEROLE porque a V1__plataforma_base.sql cria dentibot_app e
    -- dentibot_provisionador se ainda não existirem.
    CREATEROLE
    NOCREATEDB
    PASSWORD 'migrador_local_apenas';

-- A aplicação. Sem posse, sem superusuário, sem criar nada.
CREATE ROLE dentibot_app
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    PASSWORD 'app_local_apenas';

ALTER DATABASE dentibot OWNER TO dentibot_migrador;

GRANT CREATE, CONNECT ON DATABASE dentibot TO dentibot_migrador;
GRANT CONNECT           ON DATABASE dentibot TO dentibot_app;

-- Sem isto, qualquer role conectado poderia criar tabela no schema public.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
