-- ─────────────────────────────────────────────────────────────────────────────
-- V24 — Comunicação com o paciente pelo WhatsApp (Doc 03-D).
--
--   · canais: o número de WhatsApp Business de cada clínica. O webhook da Meta
--     chega sem tenant e diz só o phone_number_id; a travessia até a clínica é
--     uma função SECURITY DEFINER de uma coluna, como a do login (V3/V18);
--   · mensagens: o que saiu e o que chegou, com o estado da entrega. "Lida"
--     é a prova de que a orientação foi prestada (IA-38);
--   · lista_espera e ofertas: a vaga liberada vai para quem espera (IA-18).
-- O conteúdo clínico que o paciente escreve fica aqui, sob RLS, e não sai para
-- provedor de IA nenhum: a triagem de urgência é regra, não modelo (IA-31).
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SCHEMA comunicacao;
GRANT USAGE ON SCHEMA comunicacao TO dentibot_app;
ALTER DEFAULT PRIVILEGES FOR ROLE dentibot_migrador IN SCHEMA comunicacao
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO dentibot_app;
ALTER DEFAULT PRIVILEGES FOR ROLE dentibot_migrador IN SCHEMA comunicacao
    GRANT USAGE, SELECT ON SEQUENCES TO dentibot_app;

CREATE TABLE comunicacao.canais (
    id_clinica        BIGINT      PRIMARY KEY REFERENCES clinicas.clinicas(id_clinica),
    -- Um número da Meta pertence a uma clínica só: é o que torna a resolução
    -- do webhook inequívoca.
    phone_number_id   TEXT        NOT NULL UNIQUE CHECK (phone_number_id ~ '^[0-9]{5,30}$'),
    ativo             BOOLEAN     NOT NULL DEFAULT true,
    tipos_desligados  TEXT[]      NOT NULL DEFAULT '{}',
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE comunicacao.mensagens (
    id_mensagem    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente    BIGINT      NULL,
    -- Dígitos, como a Meta manda e recebe. Número desconhecido também conversa.
    telefone       TEXT        NOT NULL CHECK (telefone ~ '^[0-9]{10,15}$'),
    sentido        TEXT        NOT NULL CHECK (sentido IN ('saida','entrada')),
    tipo           TEXT        NOT NULL CHECK (tipo IN ('lembrete','confirmacao','reforco',
                                   'oferta_vaga','pos_procedimento','orientacao',
                                   'resposta','recebida')),
    id_consulta    BIGINT      NULL,
    texto          TEXT        NOT NULL CHECK (length(texto) BETWEEN 1 AND 4096),
    status         TEXT        NOT NULL CHECK (status IN ('agendada','enviando','enviada',
                                   'entregue','lida','falhou','cancelada','recebida')),
    enviar_em      TIMESTAMPTZ NULL,
    enviada_em     TIMESTAMPTZ NULL,
    lida_em        TIMESTAMPTZ NULL,
    -- wamid da Meta: a chave que amarra status e resposta à mensagem, e a
    -- idempotência do webhook (a Meta reenvia).
    id_externo     TEXT        NULL UNIQUE,
    motivo         TEXT        NULL,
    -- Entrada: o que a triagem entendeu, e se precisa de gente.
    classificacao  TEXT        NULL,
    escalada       TEXT        NULL CHECK (escalada IN ('urgente','normal')),
    resolvida_por  BIGINT      NULL,
    resolvida_em   TIMESTAMPTZ NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CHECK ((sentido = 'entrada') = (tipo = 'recebida'))
);

-- O despachante varre o que venceu; o índice parcial encolhe com a fila.
CREATE INDEX idx_mensagens_a_enviar ON comunicacao.mensagens (enviar_em)
    WHERE status = 'agendada';
CREATE INDEX idx_mensagens_telefone ON comunicacao.mensagens (id_clinica, telefone, created_at);
CREATE INDEX idx_mensagens_escaladas ON comunicacao.mensagens (id_clinica, created_at)
    WHERE escalada IS NOT NULL AND resolvida_em IS NULL;

CREATE TABLE comunicacao.lista_espera (
    id_espera    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica   BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente  BIGINT      NOT NULL,
    id_dentista  BIGINT      NULL,
    periodo      TEXT        NOT NULL DEFAULT 'qualquer'
                             CHECK (periodo IN ('manha','tarde','qualquer')),
    urgencia     SMALLINT    NOT NULL DEFAULT 1 CHECK (urgencia BETWEEN 1 AND 3),
    observacao   VARCHAR(200) NULL,
    atendida_em  TIMESTAMPTZ NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_espera_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica)
);

-- Um paciente espera uma vez: duas entradas abertas receberiam duas ofertas.
CREATE UNIQUE INDEX uq_espera_aberta ON comunicacao.lista_espera (id_clinica, id_paciente)
    WHERE atendida_em IS NULL;

CREATE TABLE comunicacao.ofertas (
    id_oferta    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica   BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_espera    BIGINT      NOT NULL REFERENCES comunicacao.lista_espera(id_espera),
    id_paciente  BIGINT      NOT NULL,
    id_dentista  BIGINT      NOT NULL,
    inicio_em    TIMESTAMPTZ NOT NULL,
    termino_em   TIMESTAMPTZ NOT NULL,
    expira_em    TIMESTAMPTZ NOT NULL,
    status       TEXT        NOT NULL DEFAULT 'aberta'
                             CHECK (status IN ('aberta','aceita','recusada','expirada')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ofertas_abertas ON comunicacao.ofertas (id_clinica, id_paciente)
    WHERE status = 'aberta';

ALTER TABLE comunicacao.canais       ENABLE ROW LEVEL SECURITY;
ALTER TABLE comunicacao.canais       FORCE  ROW LEVEL SECURITY;
ALTER TABLE comunicacao.mensagens    ENABLE ROW LEVEL SECURITY;
ALTER TABLE comunicacao.mensagens    FORCE  ROW LEVEL SECURITY;
ALTER TABLE comunicacao.lista_espera ENABLE ROW LEVEL SECURITY;
ALTER TABLE comunicacao.lista_espera FORCE  ROW LEVEL SECURITY;
ALTER TABLE comunicacao.ofertas      ENABLE ROW LEVEL SECURITY;
ALTER TABLE comunicacao.ofertas      FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON comunicacao.canais
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON comunicacao.mensagens
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON comunicacao.lista_espera
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
CREATE POLICY tenant_isolation ON comunicacao.ofertas
    FOR ALL TO dentibot_app USING (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

-- O despachante roda sem tenant, como o worker do outbox: vê a fila de todas
-- as clínicas só para travar e marcar. O envio em si acontece com a clínica
-- promovida.
CREATE POLICY worker_despacha ON comunicacao.mensagens
    FOR ALL TO dentibot_app
    USING      (plataforma.modo_worker())
    WITH CHECK (plataforma.modo_worker());

-- ─── Webhook → clínica ───────────────────────────────────────────────────────
-- Mesmo desenho da V18: SECURITY DEFINER não contorna FORCE RLS, então a
-- travessia tem sujeito próprio, política explícita e privilégio de UMA coluna
-- além da chave. O webhook já passou pela assinatura HMAC quando chama isto.
GRANT USAGE, CREATE ON SCHEMA comunicacao TO dentibot_autenticador;
GRANT SELECT (id_clinica, phone_number_id, ativo) ON comunicacao.canais TO dentibot_autenticador;

CREATE POLICY resolucao_do_webhook ON comunicacao.canais
    FOR SELECT TO dentibot_autenticador
    USING (true);

CREATE OR REPLACE FUNCTION comunicacao.clinica_do_canal(p_phone_number_id TEXT)
    RETURNS BIGINT
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = comunicacao, pg_catalog
AS $$
    SELECT c.id_clinica FROM comunicacao.canais c
    WHERE c.phone_number_id = p_phone_number_id AND c.ativo
$$;

ALTER FUNCTION comunicacao.clinica_do_canal(TEXT) OWNER TO dentibot_autenticador;
REVOKE EXECUTE ON FUNCTION comunicacao.clinica_do_canal(TEXT) FROM PUBLIC;
GRANT  EXECUTE ON FUNCTION comunicacao.clinica_do_canal(TEXT) TO dentibot_app;

REVOKE CREATE ON SCHEMA comunicacao FROM dentibot_autenticador;
