-- Só no perfil render (application-render.yml). Roda depois de todo migrate,
-- inclusive o que não tinha nada pendente, como dentibot_migrador.
--
-- A V1 criou dentibot_app NOLOGIN e sem senha, e lá quem a criou foi o
-- migrador — que por isso administra o role e pode fazer isto. A senha vem de
-- DENTIBOT_DB_PASSWORD, gerada pela Render em base64 (sem aspas). Repetir a
-- cada start mantém o banco igual à variável: trocou na Render, vale no próximo
-- deploy.

-- Sem a variável (apagada na Render, serviço criado fora do Blueprint), o
-- start falha aqui com o motivo, em vez de subir com uma senha conhecida.
DO $$
BEGIN
    IF length('${senhaapp}') < 32 THEN
        RAISE EXCEPTION 'DENTIBOT_DB_PASSWORD ausente ou curta: o Blueprint a gera (generateValue)';
    END IF;
END
$$;

ALTER ROLE dentibot_app WITH LOGIN PASSWORD '${senhaapp}';
