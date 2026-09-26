-- ─────────────────────────────────────────────────────────────────────────────
-- V11 — Provisionamento de clínica (o problema do ovo e da galinha).
--
-- Com FORCE ROW LEVEL SECURITY, a política de clinicas.clinicas é
-- `id_clinica = plataforma.clinica_atual()`. Para inserir a clínica 7 seria
-- preciso já estar no tenant 7 — que ainda não existe. Nem o dono da tabela
-- escapa: FORCE significa FORCE.
--
-- Isso é bom: criar tenant deixa de ser um INSERT que um endpoint distraído
-- possa fazer. A solução NÃO é uma flag de sessão — a aplicação poderia ligá-la
-- sozinha, e aí a política seria convenção, não fronteira. A solução é dar à
-- operação um SUJEITO próprio:
--
--   · `dentibot_provisionador` é o único role com INSERT em clinicas.clinicas;
--   · `clinicas.provisionar()` é SECURITY DEFINER e pertence a esse role;
--   · `dentibot_app` recebe EXECUTE na função e NADA de INSERT na tabela.
--
-- Logo o único caminho para nascer uma clínica é o corpo desta função. A app não
-- consegue contornar porque não é membro do role — não é uma GUC que ela mesma
-- possa definir.
--
-- A função faz UMA coisa: insere a linha da clínica. Configurações, pessoa e
-- usuário admin vêm depois, pelo Java, já com app.clinica definido. Assim a
-- superfície SECURITY DEFINER tem o tamanho de um INSERT e a regra de negócio
-- fica fora do banco — o erro da V1 foi o contrário, com a autenticação inteira
-- dentro de uma procedure SECURITY DEFINER.
-- ─────────────────────────────────────────────────────────────────────────────

-- A app perde INSERT direto (o ALTER DEFAULT PRIVILEGES da V1 havia concedido).
REVOKE INSERT ON clinicas.clinicas FROM dentibot_app;

-- CREATE é temporário: transferir a posse de uma função exige que o novo dono
-- possa criar no schema. Revogado logo após o ALTER FUNCTION, no fim do arquivo.
GRANT USAGE, CREATE ON SCHEMA clinicas  TO dentibot_provisionador;
GRANT USAGE  ON SCHEMA plataforma       TO dentibot_provisionador;
GRANT EXECUTE ON FUNCTION plataforma.clinica_atual() TO dentibot_provisionador;

-- SELECT entra junto com INSERT porque `INSERT ... RETURNING id_clinica` lê a
-- coluna que devolve. Note que UPDATE e DELETE ficam de fora: privilégio e
-- política são avaliados em conjunto, então é este GRANT — e não a política
-- abaixo — que define o alcance real do provisionador.
GRANT INSERT, SELECT ON clinicas.clinicas TO dentibot_provisionador;

CREATE POLICY provisionamento ON clinicas.clinicas
    FOR ALL TO dentibot_provisionador
    USING      (true)
    WITH CHECK (true);

CREATE OR REPLACE FUNCTION clinicas.provisionar(
    p_cnpj          CHAR(14),
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
    VALUES (p_cnpj, p_razao_social, p_nome_fantasia, p_timezone, p_plano)
    RETURNING id_clinica INTO v_id;

    RETURN v_id;
END
$$;

-- A posse define quem a função "é" quando roda. Sem este ALTER ela rodaria como
-- o migrador, que FORCE RLS também barra.
ALTER FUNCTION clinicas.provisionar(CHAR, VARCHAR, VARCHAR, TEXT, TEXT)
    OWNER TO dentibot_provisionador;

COMMENT ON FUNCTION clinicas.provisionar IS
    'Única porta de criação de tenant. Devolve o id_clinica; o chamador faz '
    'set_config(''app.clinica'', <id>, true) e segue inserindo configurações, '
    'pessoa e usuário admin na MESMA transação.';

REVOKE EXECUTE ON FUNCTION clinicas.provisionar(CHAR, VARCHAR, VARCHAR, TEXT, TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION clinicas.provisionar(CHAR, VARCHAR, VARCHAR, TEXT, TEXT) TO dentibot_app;

-- Fim do trabalho do provisionador: ele volta a não poder criar nada. Sobra só
-- ser dono daquela função e ter INSERT naquela tabela.
REVOKE CREATE ON SCHEMA clinicas FROM dentibot_provisionador;
