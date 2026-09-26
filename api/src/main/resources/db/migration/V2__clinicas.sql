-- ─────────────────────────────────────────────────────────────────────────────
-- V2 — Módulo clinicas. A raiz do tenant: tudo no sistema pendura em id_clinica.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE clinicas.clinicas (
    id_clinica     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cnpj           CHAR(14)     NOT NULL UNIQUE,
    razao_social   VARCHAR(144) NOT NULL,
    nome_fantasia  VARCHAR(60)  NOT NULL,

    -- Invariante 7. Nome IANA, não offset: "America/Sao_Paulo" sabe quando o
    -- horário de verão volta; "-03:00" não sabe, e a agenda inteira erra uma hora.
    timezone       TEXT         NOT NULL DEFAULT 'America/Sao_Paulo',

    -- Plano vendido na landing. É o que o gate de limite consulta (camada 12).
    plano          TEXT         NOT NULL DEFAULT 'solo'
                                CHECK (plano IN ('solo', 'clinica')),
    status         TEXT         NOT NULL DEFAULT 'trial'
                                CHECK (status IN ('trial','ativa','inadimplente','suspensa','encerrada')),

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ  NULL
);

COMMENT ON COLUMN clinicas.clinicas.timezone IS
    'Nome IANA. Validado por trigger contra pg_timezone_names — "America/SaoPaulo" '
    '(sem underscore) é um erro de digitação que quebraria toda a agenda em silêncio.';

-- Validação do timezone. Não dá para fazer em CHECK: exigiria subconsulta.
CREATE OR REPLACE FUNCTION clinicas.validar_timezone()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_timezone_names WHERE name = NEW.timezone) THEN
        RAISE EXCEPTION 'Timezone IANA inválido: %', NEW.timezone
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_validar_timezone
    BEFORE INSERT OR UPDATE OF timezone ON clinicas.clinicas
    FOR EACH ROW EXECUTE FUNCTION clinicas.validar_timezone();

CREATE TRIGGER trg_clinicas_updated_at
    BEFORE UPDATE ON clinicas.clinicas
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── Configurações por clínica ───────────────────────────────────────────────
CREATE TABLE clinicas.configuracoes (
    id_clinica              BIGINT      PRIMARY KEY
                                        REFERENCES clinicas.clinicas(id_clinica),
    ativar_comissao         BOOLEAN     NOT NULL DEFAULT FALSE,
    regra_comissao_padrao   NUMERIC(5,2) NOT NULL DEFAULT 0
                                        CHECK (regra_comissao_padrao BETWEEN 0 AND 100),
    emissao_nfse_automatica BOOLEAN     NOT NULL DEFAULT FALSE,
    exigir_2fa              BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Janela padrão de atendimento, usada pela agenda quando o dentista não
    -- tem horário próprio. Armazenada como hora local da clínica.
    abre_as                 TIME        NOT NULL DEFAULT '08:00',
    fecha_as                TIME        NOT NULL DEFAULT '18:00',

    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_janela_valida CHECK (abre_as < fecha_as)
);

CREATE TRIGGER trg_configuracoes_updated_at
    BEFORE UPDATE ON clinicas.configuracoes
    FOR EACH ROW EXECUTE FUNCTION plataforma.tocar_updated_at();

-- ─── RLS ─────────────────────────────────────────────────────────────────────
-- ENABLE liga a política para quem não é dono. FORCE liga também para o dono —
-- as duas são necessárias, e é a segunda que quase todo mundo esquece.
ALTER TABLE clinicas.clinicas     ENABLE ROW LEVEL SECURITY;
ALTER TABLE clinicas.clinicas     FORCE  ROW LEVEL SECURITY;
ALTER TABLE clinicas.configuracoes ENABLE ROW LEVEL SECURITY;
ALTER TABLE clinicas.configuracoes FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON clinicas.clinicas
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

CREATE POLICY tenant_isolation ON clinicas.configuracoes
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());

-- Staff da plataforma (eixo B) enxerga o cadastro das clínicas — é o que
-- billing e suporte precisam. Política PERMISSIVE: soma com tenant_isolation.
-- Note que é FOR SELECT: staff não altera dado de clínica por esta via.
-- Nenhuma tabela de dado clínico ganha política equivalente.
CREATE POLICY staff_leitura ON clinicas.clinicas
    FOR SELECT TO dentibot_app
    USING (plataforma.staff_atual() IS NOT NULL);

-- ─── Índices ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_clinicas_status ON clinicas.clinicas (status) WHERE deleted_at IS NULL;
