SELECT pubname, puballtables, pubinsert, pubupdate, pubdelete
FROM   pg_publication
WHERE  pubname = 'pub_dentibot_negocio';

GRANT pg_read_all_data TO dentibot_replicator;

SELECT pg_create_logical_replication_slot(
    'slot_dentibot_leitura',
    'pgoutput'
);

CREATE TABLE IF NOT EXISTS negocio.clinicas                     (LIKE public.clinicas                    INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.configuracoes_clinica        (LIKE public.configuracoes_clinica       INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.pessoas                      (LIKE public.pessoas                     INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.papeis_permissoes            (LIKE public.papeis_permissoes           INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.usuarios_sistema             (LIKE public.usuarios_sistema            INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.pacientes                    (LIKE public.pacientes                   INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.dentistas                    (LIKE public.dentistas                   INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.funcionarios                 (LIKE public.funcionarios                INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.procedimentos                (LIKE public.procedimentos               INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.agenda                       (LIKE public.agenda                      INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.orcamentos                   (LIKE public.orcamentos                  INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.itens_orcamento              (LIKE public.itens_orcamento             INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.prontuarios_anamnese         (LIKE public.prontuarios_anamnese        INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.evolucao_clinica             (LIKE public.evolucao_clinica            INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.convenios                    (LIKE public.convenios                   INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.planos_convenio              (LIKE public.planos_convenio             INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.precos_convenio              (LIKE public.precos_convenio             INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.guias_autorizacao            (LIKE public.guias_autorizacao           INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.regras_comissao              (LIKE public.regras_comissao             INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.comissoes_geradas            (LIKE public.comissoes_geradas           INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.formas_pagamento             (LIKE public.formas_pagamento            INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.contas_receber               (LIKE public.contas_receber              INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.contas_pagar                 (LIKE public.contas_pagar                INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.fornecedores                 (LIKE public.fornecedores                INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.produtos_materiais           (LIKE public.produtos_materiais          INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.movimentacao_estoque         (LIKE public.movimentacao_estoque        INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.configuracoes_fiscais_clinica(LIKE public.configuracoes_fiscais_clinica INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.notas_fiscais_emitidas       (LIKE public.notas_fiscais_emitidas      INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.termos_politicas_lgpd        (LIKE public.termos_politicas_lgpd       INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.consentimentos_paciente      (LIKE public.consentimentos_paciente     INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.solicitacoes_titulares       (LIKE public.solicitacoes_titulares      INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.solicitacoes_acesso_jit      (LIKE public.solicitacoes_acesso_jit     INCLUDING ALL);
CREATE TABLE IF NOT EXISTS negocio.dispositivos_confiaveis      (LIKE public.dispositivos_confiaveis     INCLUDING ALL);

CREATE OR REPLACE FUNCTION negocio.fn_bloquear_escrita_replica()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION
        'Tabela % é uma réplica read-only. Escreva no dentibot_escrita.',
        TG_TABLE_NAME
        USING ERRCODE = 'P0030';
END;
$$;

DO $$
DECLARE tbl TEXT;
    tabelas TEXT[] := ARRAY[
        'clinicas','configuracoes_clinica','pessoas','papeis_permissoes',
        'usuarios_sistema','pacientes','dentistas','funcionarios','procedimentos',
        'agenda','orcamentos','itens_orcamento','prontuarios_anamnese','evolucao_clinica',
        'convenios','planos_convenio','precos_convenio','guias_autorizacao',
        'regras_comissao','comissoes_geradas','formas_pagamento','contas_receber',
        'contas_pagar','fornecedores','produtos_materiais','movimentacao_estoque',
        'configuracoes_fiscais_clinica','notas_fiscais_emitidas',
        'termos_politicas_lgpd','consentimentos_paciente','solicitacoes_titulares',
        'solicitacoes_acesso_jit','dispositivos_confiaveis'
    ];
BEGIN
    FOREACH tbl IN ARRAY tabelas LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_readonly_%I
             BEFORE INSERT OR UPDATE OR DELETE ON negocio.%I
             FOR EACH ROW EXECUTE FUNCTION negocio.fn_bloquear_escrita_replica()',
            tbl, tbl
        );
    END LOOP;
END;
$$;

GRANT SELECT ON ALL TABLES IN SCHEMA negocio TO dentibot_readonly;

CREATE SUBSCRIPTION sub_dentibot_negocio
    CONNECTION 'host=db-escrita.interno
                port=5543
                dbname=dentibot_escrita
                user=dentibot_replicator
                password=TROQUE_DEPLOY_REP
                sslmode=require'
    PUBLICATION pub_dentibot_negocio
    WITH (

        slot_name = 'slot_dentibot_leitura',
        create_slot = FALSE,

        copy_data = TRUE,
        connect   = TRUE
    );

SELECT cron.schedule('monitorar-lag-replicacao', '*/2 * * * *', $$
    DO $$
    DECLARE v_lag BIGINT;
    BEGIN
        SELECT (latest_end_lsn - received_lsn) INTO v_lag
        FROM pg_stat_subscription WHERE subname = 'sub_dentibot_negocio';

        IF v_lag > 10485760 THEN
            INSERT INTO seguranca.alertas_anomalia
                (id_clinica, severidade, tipo_anomalia, descricao, evidencia)
            VALUES (0, 4, 'LAG_REPLICACAO_CRITICO',
                format('Lag de replicação: %s bytes', v_lag),
                jsonb_build_object('lag_bytes', v_lag, 'threshold', 10485760));
        END IF;
    END; $$;
$$);
