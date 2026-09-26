-- ─────────────────────────────────────────────────────────────────────────────
-- V12 — Módulo prontuário. O núcleo regulado: Resolução CFO-226/2020 e LGPD
-- art. 11 (dado de saúde é dado pessoal sensível).
--
-- Três regras que o banco impõe, porque norma não se confia a revisão de código:
--   · evolução clínica é IMUTÁVEL depois de inserida. Correção é adendo que
--     referencia o registro corrigido, nunca UPDATE — o prontuário precisa
--     mostrar o que foi escrito E que foi corrigido depois;
--   · nada de hard delete (invariante 10): guarda mínima legal;
--   · odontograma é append-only de lançamentos, e o estado atual é a última
--     linha por dente/face. Assim o histórico existe sem custar nada.
--
-- O odontograma não existia na V1 — e a landing o vende. `web/src/components/
-- Odontograma.jsx` já desenha um.
-- ─────────────────────────────────────────────────────────────────────────────

-- ─── Anamnese ────────────────────────────────────────────────────────────────
-- Documento vivo: atualiza. Toda alteração vai para auditoria.eventos com
-- dados_anteriores/dados_posteriores, na mesma transação.
CREATE TABLE prontuario.anamneses (
    id_anamnese         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica          BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente         BIGINT      NOT NULL,

    questionario        JSONB       NOT NULL DEFAULT '{}'::JSONB,
    alergias_relatadas  TEXT        NULL,
    historico_anterior  TEXT        NULL,
    medicacoes_em_uso   TEXT        NULL,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_anamnese_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT uq_anamnese_paciente UNIQUE (id_clinica, id_paciente)
);

CREATE TRIGGER trg_anamneses_updated_at
    BEFORE UPDATE ON prontuario.anamneses
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Evolução clínica ────────────────────────────────────────────────────────
CREATE TABLE prontuario.evolucoes (
    id_evolucao      BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica       BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente      BIGINT      NOT NULL,
    -- Quem assina. CFO-226 exige identificação do profissional responsável.
    id_dentista      BIGINT      NOT NULL,
    id_consulta      BIGINT      NULL,

    descricao_sessao TEXT        NOT NULL CHECK (length(trim(descricao_sessao)) > 0),

    -- Adendo de retificação. Preenchido, esta linha CORRIGE aquela — e as duas
    -- continuam visíveis. É assim que se corrige prontuário: somando, não
    -- apagando.
    retifica_evolucao BIGINT     NULL,
    motivo_retificacao TEXT      NULL,

    registrado_em    TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT fk_evolucao_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT fk_evolucao_dentista
        FOREIGN KEY (id_dentista, id_clinica)
        REFERENCES identidade.dentistas (id_dentista, id_clinica),
    CONSTRAINT fk_evolucao_consulta
        FOREIGN KEY (id_consulta, id_clinica)
        REFERENCES agenda.consultas (id_consulta, id_clinica),
    CONSTRAINT fk_evolucao_retificada
        FOREIGN KEY (retifica_evolucao) REFERENCES prontuario.evolucoes(id_evolucao),
    CONSTRAINT ck_retificacao_tem_motivo
        CHECK (retifica_evolucao IS NULL OR length(trim(coalesce(motivo_retificacao,''))) > 0)
);

CREATE INDEX idx_evolucoes_paciente
    ON prontuario.evolucoes (id_clinica, id_paciente, registrado_em DESC);
CREATE INDEX idx_evolucoes_dentista
    ON prontuario.evolucoes (id_clinica, id_dentista, registrado_em DESC);

-- Imutabilidade em duas camadas, como a auditoria: privilégio e trigger.
REVOKE UPDATE, DELETE, TRUNCATE ON prontuario.evolucoes FROM dentibot_app;

CREATE OR REPLACE FUNCTION prontuario.bloquear_alteracao_evolucao()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Evolução clínica é imutável após o registro (CFO-226/2020). '
        'Para corrigir, insira um adendo com retifica_evolucao = %.',
        OLD.id_evolucao
        USING ERRCODE = 'insufficient_privilege';
END
$$;

CREATE TRIGGER trg_evolucao_imutavel
    BEFORE UPDATE OR DELETE ON prontuario.evolucoes
    FOR EACH ROW EXECUTE FUNCTION prontuario.bloquear_alteracao_evolucao();

-- ─── Odontograma ─────────────────────────────────────────────────────────────
-- Append-only de lançamentos. O estado atual do dente 36, face oclusal, é o
-- último lançamento para aquele par — e o histórico vem de graça.
CREATE TABLE prontuario.odontograma_lancamentos (
    id_lancamento BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica    BIGINT      NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente   BIGINT      NOT NULL,
    id_dentista   BIGINT      NOT NULL,

    -- Notação FDI (ISO 3950). 11-18/21-28/31-38/41-48 permanentes,
    -- 51-55/61-65/71-75/81-85 decíduos.
    dente         SMALLINT    NOT NULL CHECK (
                      dente BETWEEN 11 AND 18 OR dente BETWEEN 21 AND 28 OR
                      dente BETWEEN 31 AND 38 OR dente BETWEEN 41 AND 48 OR
                      dente BETWEEN 51 AND 55 OR dente BETWEEN 61 AND 65 OR
                      dente BETWEEN 71 AND 75 OR dente BETWEEN 81 AND 85),

    -- NULL = condição do dente inteiro (ausente, implante, extraído).
    face          TEXT        NULL CHECK (face IN ('V','L','M','D','O','I','P')),

    condicao      TEXT        NOT NULL CHECK (condicao IN (
                      'higido','carie','restauracao','ausente','implante',
                      'coroa','canal','fratura','extraido','selante','protese')),
    observacao    VARCHAR(300) NULL,

    registrado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT fk_odonto_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT fk_odonto_dentista
        FOREIGN KEY (id_dentista, id_clinica)
        REFERENCES identidade.dentistas (id_dentista, id_clinica),
    -- Condição de dente inteiro não tem face; condição de face exige face.
    CONSTRAINT ck_face_coerente CHECK (
        (condicao IN ('ausente','implante','extraido','coroa','canal','protese') AND face IS NULL)
        OR (condicao IN ('higido','carie','restauracao','selante') AND face IS NOT NULL)
    )
);

CREATE INDEX idx_odonto_estado_atual
    ON prontuario.odontograma_lancamentos (id_clinica, id_paciente, dente, face, registrado_em DESC);

REVOKE UPDATE, DELETE, TRUNCATE ON prontuario.odontograma_lancamentos FROM dentibot_app;

CREATE TRIGGER trg_odonto_imutavel
    BEFORE UPDATE OR DELETE ON prontuario.odontograma_lancamentos
    FOR EACH ROW EXECUTE FUNCTION auditoria.bloquear_alteracao();

-- ─── Anexos (radiografia, foto, laudo) ───────────────────────────────────────
-- Camada 19: nada de binário no Postgres. Aqui fica só o ponteiro para o R2 e
-- os metadados; o acesso é sempre por URL assinada de expiração curta.
CREATE TABLE prontuario.anexos (
    id_anexo       BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_clinica     BIGINT       NOT NULL REFERENCES clinicas.clinicas(id_clinica),
    id_paciente    BIGINT       NOT NULL,
    id_consulta    BIGINT       NULL,

    tipo           TEXT         NOT NULL CHECK (tipo IN ('radiografia','foto_intraoral',
                                                         'documento','laudo','modelo_3d')),
    -- Chave do objeto no bucket. Nunca URL pública.
    chave_objeto   VARCHAR(500) NOT NULL UNIQUE,
    nome_arquivo   VARCHAR(255) NOT NULL,
    content_type   VARCHAR(120) NOT NULL,
    tamanho_bytes  BIGINT       NOT NULL CHECK (tamanho_bytes > 0),
    -- Integridade: o agente Windows calcula na origem, a API confere.
    hash_sha256    CHAR(64)     NOT NULL,

    enviado_por    BIGINT       NULL,
    origem         TEXT         NOT NULL DEFAULT 'upload_web'
                                CHECK (origem IN ('upload_web','agente_windows','api')),

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ  NULL,

    CONSTRAINT fk_anexo_paciente
        FOREIGN KEY (id_paciente, id_clinica)
        REFERENCES pacientes.pacientes (id_paciente, id_clinica),
    CONSTRAINT fk_anexo_consulta
        FOREIGN KEY (id_consulta, id_clinica)
        REFERENCES agenda.consultas (id_consulta, id_clinica)
);

CREATE INDEX idx_anexos_paciente
    ON prontuario.anexos (id_clinica, id_paciente, created_at DESC)
    WHERE deleted_at IS NULL;

-- ─── RLS ─────────────────────────────────────────────────────────────────────
-- Tenancy apenas. "Dentista vê só os pacientes dele" e "recepcionista não vê
-- prontuário" são RBAC e vivem no PermissionEvaluator (invariante 3).
-- Nenhuma política de staff aqui: staff da plataforma não lê dado clínico,
-- nem com JIT — o JIT da Fase 4 concede acesso dentro de um tenant, definindo
-- app.clinica, e fica registrado em auditoria.eventos.
ALTER TABLE prontuario.anamneses               ENABLE ROW LEVEL SECURITY;
ALTER TABLE prontuario.anamneses               FORCE  ROW LEVEL SECURITY;
ALTER TABLE prontuario.evolucoes               ENABLE ROW LEVEL SECURITY;
ALTER TABLE prontuario.evolucoes               FORCE  ROW LEVEL SECURITY;
ALTER TABLE prontuario.odontograma_lancamentos ENABLE ROW LEVEL SECURITY;
ALTER TABLE prontuario.odontograma_lancamentos FORCE  ROW LEVEL SECURITY;
ALTER TABLE prontuario.anexos                  ENABLE ROW LEVEL SECURITY;
ALTER TABLE prontuario.anexos                  FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON prontuario.anamneses
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON prontuario.evolucoes
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON prontuario.odontograma_lancamentos
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON prontuario.anexos
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
