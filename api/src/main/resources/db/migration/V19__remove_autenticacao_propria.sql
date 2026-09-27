-- ─────────────────────────────────────────────────────────────────────────────
-- V19 — Fecha a superfície que o login próprio deixou para trás.
--
-- A V18 trocou o provedor de identidade e a mesma leva apagou o
-- `AutenticacaoServico`. Ficaram no banco duas funções SECURITY DEFINER sem
-- nenhum chamador — e uma delas devolve `senha_hash` e `totp_secret` de
-- qualquer staff da plataforma, para qualquer e-mail, ainda com
-- `GRANT EXECUTE ... TO dentibot_app`.
--
-- Isso não é uma vulnerabilidade nova: o grant é da V3 e a app sempre o teve.
-- É pior de outro jeito — é uma defesa que parece ativa numa revisão e não
-- protege nada, porque o código que a justificava não existe mais. O
-- `UsuarioRepositorio` diz isso sobre o Java ("código de autenticação que
-- continua existindo sem ser chamado é pior que código ausente"); a mesma regra
-- vale para o schema, e é o que esta migration cobra.
--
-- Recuperar qualquer uma delas é copiar a definição da V3 — a migration
-- continua no histórico.
-- ─────────────────────────────────────────────────────────────────────────────

-- ─── As funções ──────────────────────────────────────────────────────────────
-- Ordem importa: DROP FUNCTION derruba os GRANTs junto, então o REVOKE
-- explícito viria depois de o alvo já não existir.
DROP FUNCTION IF EXISTS identidade.autenticacao_staff(TEXT);
DROP FUNCTION IF EXISTS identidade.resolver_clinica_por_email(TEXT);

-- ─── Os privilégios por coluna que só elas usavam ────────────────────────────
-- O que sobra para o `dentibot_autenticador` é o mínimo das três funções da
-- V18: (id_usuario, id_clinica, clerk_user_id, papel, status) em usuarios —
-- mais `email`, que o resolvedor de vínculo pendente ainda lê — e
-- (id_staff, clerk_user_id, papel, status) em staff_plataforma.
--
-- Material de credencial sai da lista inteiramente. Depois disto, o sujeito que
-- atravessa o RLS não alcança hash de senha nem segredo de TOTP nem por engano:
-- quem tentar ler recebe recusa do Postgres, não uma revisão de código.
REVOKE SELECT (senha_hash, totp_secret, metodo_2fa, email)
    ON identidade.staff_plataforma FROM dentibot_autenticador;

-- ─── A tabela de sessões próprias ────────────────────────────────────────────
-- Continua existindo e continua sem escritor — o comentário da V18 explica por
-- quê (a trilha de quem esteve logado é a resposta para "quem estava dentro
-- quando aquilo aconteceu"). O que muda é que a app perde o direito de escrever
-- nela: sem emissor de token, um INSERT aqui só poderia vir de código errado.
REVOKE INSERT, UPDATE, DELETE ON identidade.sessoes       FROM dentibot_app;
REVOKE INSERT, UPDATE, DELETE ON identidade.sessoes_staff FROM dentibot_app;
REVOKE INSERT, UPDATE, DELETE ON identidade.codigos_recuperacao FROM dentibot_app;

-- `plataforma.tokens_revogados` era a blocklist de JTI do emissor próprio. Sem
-- emissor não há JTI nosso para revogar — a revogação de sessão agora é do
-- Clerk. A tabela fica pelo mesmo motivo das sessões; a escrita, não.
REVOKE INSERT, UPDATE, DELETE ON plataforma.tokens_revogados FROM dentibot_app;
