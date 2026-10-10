-- Só no perfil render (application-render.yml). Roda depois de todo migrate,
-- inclusive o que não tinha nada pendente, como dentibot_migrador.
--
-- A V1 criou dentibot_app NOLOGIN e sem senha, e lá quem a criou foi o
-- migrador — que por isso administra o role e pode fazer isto. A senha vem de
-- DENTIBOT_DB_PASSWORD, gerada pela Render em base64 (sem aspas). Repetir a
-- cada start mantém o banco igual à variável: trocou na Render, vale no próximo
-- deploy.
ALTER ROLE dentibot_app WITH LOGIN PASSWORD '${senhaapp}';
