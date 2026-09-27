-- ─────────────────────────────────────────────────────────────────────────────
-- V18 — Clerk como provedor de identidade.
--
-- O front-end (web e app) migrou para o Clerk na V2; o back-end continuou
-- emitindo e validando o próprio JWT. O resultado era 401 em toda rota
-- autenticada: o token chegava assinado por uma chave que o Java não conhece.
--
-- O que muda aqui é só QUEM prova a identidade. O que NÃO muda:
--   · o tenant continua vindo do contexto da requisição, nunca do corpo;
--   · a autorização continua sendo do AvaliadorDePermissao (invariante 3);
--   · o isolamento continua sendo do RLS (invariante 5).
--
-- O Clerk responde "quem é você" e nada mais. "De qual clínica" e "com qual
-- papel" continuam sendo resposta DESTE banco — de propósito. Pôr id_clinica
-- num claim do Clerk moveria a decisão de tenant para fora do sistema que a
-- impõe, e um template de JWT editado no dashboard passaria a ser capaz de
-- trocar a clínica de um usuário sem passar por migration, code review ou
-- auditoria.
-- ─────────────────────────────────────────────────────────────────────────────

-- ─── Eixo A: usuários da clínica ─────────────────────────────────────────────
ALTER TABLE identidade.usuarios
    ADD COLUMN clerk_user_id VARCHAR(255) NULL;

COMMENT ON COLUMN identidade.usuarios.clerk_user_id IS
    'Sujeito (sub) do JWT do Clerk. Global, não por clínica — é o Clerk que o emite.';

-- Único globalmente, e não por clínica: o `sub` do Clerk identifica uma conta
-- em toda a instância. Duas linhas com o mesmo sub significariam que um login
-- resolve para duas clínicas, e o resolvedor abaixo teria de escolher uma —
-- é exatamente o tipo de escolha que não se quer fazer em silêncio.
CREATE UNIQUE INDEX uq_usuarios_clerk
    ON identidade.usuarios (clerk_user_id)
    WHERE clerk_user_id IS NOT NULL;

-- A senha deixa de ser deste banco. A coluna FICA: as linhas existentes ainda a
-- têm, e dropar coluna é a migration que não se desfaz. Ninguém escreve nela a
-- partir daqui — quem quiser conferir tem o grep por senha_hash.
ALTER TABLE identidade.usuarios
    ALTER COLUMN senha_hash DROP NOT NULL;

-- ─── Eixo B: staff da plataforma ─────────────────────────────────────────────
ALTER TABLE identidade.staff_plataforma
    ADD COLUMN clerk_user_id VARCHAR(255) NULL;

CREATE UNIQUE INDEX uq_staff_clerk
    ON identidade.staff_plataforma (clerk_user_id)
    WHERE clerk_user_id IS NOT NULL;

ALTER TABLE identidade.staff_plataforma
    ALTER COLUMN senha_hash DROP NOT NULL;

-- ─── Resolução de contexto a partir do `sub` ─────────────────────────────────
-- Mesma armadilha documentada na V3, e vale repetir porque é contraintuitiva:
-- SECURITY DEFINER **não** contorna RLS. Com FORCE ROW LEVEL SECURITY nem o
-- dono escapa, e um role sem política nomeando-o enxerga ZERO linhas — a função
-- "funciona", devolve vazio, e o login responde 401 para credencial correta.
--
-- Por isso a travessia tem sujeito próprio (`dentibot_autenticador`, criado na
-- V3), política explícita e privilégio POR COLUNA.
GRANT USAGE, CREATE ON SCHEMA identidade TO dentibot_autenticador;

-- Exatamente as colunas que o resolvedor lê. Se um dia esta função crescer e
-- tentar ler telefone, CPF ou o hash de senha, o Postgres recusa.
GRANT SELECT (id_usuario, id_clinica, clerk_user_id, papel, status)
    ON identidade.usuarios TO dentibot_autenticador;

GRANT SELECT (id_staff, clerk_user_id, papel, status)
    ON identidade.staff_plataforma TO dentibot_autenticador;

-- Devolve `status` em vez de filtrar por 'ativo' aqui dentro: quem chama
-- precisa distinguir "não existe" (401) de "existe e está bloqueado" (403).
-- Filtrar na função colapsaria os dois num vazio só.
CREATE OR REPLACE FUNCTION identidade.resolver_acesso_por_clerk(p_sub TEXT)
    RETURNS TABLE (
        id_usuario BIGINT,
        id_clinica BIGINT,
        papel      TEXT,
        status     TEXT
    )
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = identidade, pg_catalog
AS $$
    SELECT u.id_usuario, u.id_clinica, u.papel, u.status
    FROM identidade.usuarios u
    WHERE u.clerk_user_id = p_sub
$$;

ALTER FUNCTION identidade.resolver_acesso_por_clerk(TEXT) OWNER TO dentibot_autenticador;
REVOKE EXECUTE ON FUNCTION identidade.resolver_acesso_por_clerk(TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION identidade.resolver_acesso_por_clerk(TEXT) TO dentibot_app;

-- Vínculo na primeira entrada de quem a clínica cadastrou na tela de equipe: a
-- linha existe, com e-mail e papel, e ainda sem `sub` — ninguém cria conta do
-- Clerk no lugar de outra pessoa.
--
-- `clerk_user_id IS NULL` é a metade que importa da cláusula. Sem ela, um e-mail
-- já vinculado seria devolvido de novo e uma conta nova do Clerk com o mesmo
-- e-mail tomaria posse da conta existente. Com ela, a janela de vínculo fecha na
-- primeira entrada e não reabre.
CREATE OR REPLACE FUNCTION identidade.resolver_acesso_pendente_por_email(p_email TEXT)
    RETURNS TABLE (
        id_usuario BIGINT,
        id_clinica BIGINT,
        papel      TEXT,
        status     TEXT
    )
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = identidade, pg_catalog
AS $$
    SELECT u.id_usuario, u.id_clinica, u.papel, u.status
    FROM identidade.usuarios u
    WHERE u.email = lower(p_email)
      AND u.clerk_user_id IS NULL
$$;

ALTER FUNCTION identidade.resolver_acesso_pendente_por_email(TEXT)
    OWNER TO dentibot_autenticador;
REVOKE EXECUTE ON FUNCTION identidade.resolver_acesso_pendente_por_email(TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION identidade.resolver_acesso_pendente_por_email(TEXT) TO dentibot_app;

CREATE OR REPLACE FUNCTION identidade.resolver_staff_por_clerk(p_sub TEXT)
    RETURNS TABLE (
        id_staff BIGINT,
        papel    TEXT,
        status   TEXT
    )
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = identidade, pg_catalog
AS $$
    SELECT s.id_staff, s.papel, s.status
    FROM identidade.staff_plataforma s
    WHERE s.clerk_user_id = p_sub
$$;

ALTER FUNCTION identidade.resolver_staff_por_clerk(TEXT) OWNER TO dentibot_autenticador;
REVOKE EXECUTE ON FUNCTION identidade.resolver_staff_por_clerk(TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION identidade.resolver_staff_por_clerk(TEXT) TO dentibot_app;

-- ─── Vínculo do sub à conta, no onboarding ───────────────────────────────────
-- O cadastro acontece com o usuário JÁ autenticado no Clerk e ainda sem clínica
-- nenhuma: é o único momento em que um `sub` sem linha correspondente é
-- legítimo. A escrita é da app, dentro do tenant recém-criado, então vai por
-- UPDATE normal e não precisa de travessia.

-- Trabalho do autenticador terminado: ele volta a não poder criar nada.
REVOKE CREATE ON SCHEMA identidade FROM dentibot_autenticador;

-- ─── Sessões próprias deixam de ser escritas ─────────────────────────────────
-- `identidade.sessoes` (V4) guardava o hash do refresh token que o Java emitia.
-- O Clerk passa a ser dono do ciclo de vida da sessão. A tabela fica, sem
-- escritor: apagá-la junto com a troca de provedor tiraria a trilha das sessões
-- que existiram, e essa trilha é o que responde "quem estava logado quando
-- aquilo aconteceu".
COMMENT ON TABLE identidade.sessoes IS
    'Histórico das sessões emitidas pelo Java até a V18. Sem escritor desde a '
    'migração para o Clerk, que passou a ser dono do ciclo de vida da sessão.';
