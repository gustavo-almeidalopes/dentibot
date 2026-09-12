-- Provisionamento do banco de teste. Espelha infrastructure/postgres/init/ e o
-- que se faz uma vez no console do Neon.
--
-- O ponto que faz este arquivo existir: `dentibot_migrador` NÃO é superusuário.
-- Se fosse, ignoraria RLS e os testes de isolamento passariam por engano —
-- exatamente o erro que a V1 do projeto cometeu ao rodar a aplicação com o role
-- dono das tabelas.

CREATE ROLE dentibot_migrador LOGIN NOSUPERUSER CREATEROLE PASSWORD 'migrador_teste';
CREATE ROLE dentibot_app      LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD 'app_teste';

GRANT CREATE, CONNECT ON DATABASE dentibot TO dentibot_migrador;
GRANT CONNECT           ON DATABASE dentibot TO dentibot_app;
