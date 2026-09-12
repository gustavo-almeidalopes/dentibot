package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.dentibot.TesteIntegracao;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invariantes de schema verificados contra o catálogo do Postgres.
 *
 * <p>Este é o teste que a V1 do projeto não tinha. Lá, um bloco {@code DO} criava
 * as políticas de RLS em laço; ele abortava na décima primeira tabela (uma sem
 * coluna {@code id_clinica}), e como {@code DO} é uma transação só, NENHUMA
 * política era criada. O script "rodava", o sistema subia, e o vazamento entre
 * clínicas era demonstrável em três linhas de SQL. Nada nem ninguém avisava.
 *
 * <p>Uma consulta ao catálogo teria pego tudo de uma vez. É o que está aqui.
 */
@DisplayName("Conformidade do schema")
class ConformidadeDoSchemaTest extends TesteIntegracao {

    /** Schemas cujas tabelas pertencem a uma clínica. */
    private static final String SCHEMAS_DE_DOMINIO = """
            'clinicas','identidade','pacientes','agenda','prontuario','orcamento',
            'financeiro','billing','estoque','lgpd','auditoria','plataforma'
            """;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @Transactional(readOnly = true)
    @DisplayName("toda tabela com id_clinica tem RLS, FORCE e política tenant_isolation")
    void todaTabelaComTenantEstaProtegida() {
        List<String> violacoes = jdbc.sql("""
                        WITH t AS (
                          SELECT c.oid, n.nspname AS s, c.relname AS r,
                                 c.relrowsecurity, c.relforcerowsecurity
                          FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                          WHERE c.relkind = 'r' AND n.nspname IN (%s)
                        )
                        SELECT t.s || '.' || t.r || ' -> ' ||
                               CASE WHEN NOT t.relrowsecurity      THEN 'RLS desligado'
                                    WHEN NOT t.relforcerowsecurity THEN 'sem FORCE ROW LEVEL SECURITY'
                                    ELSE 'sem política tenant_isolation' END
                        FROM t
                        WHERE EXISTS (
                                SELECT 1 FROM pg_attribute a
                                WHERE a.attrelid = t.oid
                                  AND a.attname IN ('id_clinica','clinica_id')
                                  AND a.attnum > 0 AND NOT a.attisdropped)
                          AND (NOT t.relrowsecurity
                               OR NOT t.relforcerowsecurity
                               OR NOT EXISTS (SELECT 1 FROM pg_policies p
                                              WHERE p.schemaname = t.s AND p.tablename = t.r
                                                AND p.policyname = 'tenant_isolation'))
                        ORDER BY 1
                        """.formatted(SCHEMAS_DE_DOMINIO))
                .query(String.class)
                .list();

        assertThat(violacoes)
                .as("""
                    Tabela com id_clinica sem proteção completa. As três coisas são \
                    necessárias: ENABLE (vale para quem não é dono), FORCE (vale também \
                    para o dono) e a política. Faltando qualquer uma, o isolamento é \
                    decorativo.""")
                .isEmpty();
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("partição de tabela particionada tem política própria — RLS do pai não desce")
    void particoesTambemTemPolitica() {
        List<String> semPolitica = jdbc.sql("""
                        SELECT n.nspname || '.' || c.relname
                        FROM pg_class c
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        JOIN pg_inherits i ON i.inhrelid = c.oid
                        WHERE c.relkind = 'r'
                          AND n.nspname = 'auditoria'
                          AND NOT EXISTS (SELECT 1 FROM pg_policies p
                                          WHERE p.schemaname = n.nspname
                                            AND p.tablename = c.relname)
                        ORDER BY 1
                        """)
                .query(String.class)
                .list();

        assertThat(semPolitica)
                .as("""
                    Partição sem política própria. No Postgres a política da tabela \
                    particionada NÃO é herdada: consultar a partição direto \
                    (auditoria.eventos_2026) devolve as linhas de TODAS as clínicas. \
                    Foi um vazamento real encontrado durante este porte.""")
                .isEmpty();
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("nenhuma tabela de dado clínico tem política de staff da plataforma")
    void staffNaoAlcancaDadoClinico() {
        List<String> vazamentos = jdbc.sql("""
                        SELECT schemaname || '.' || tablename || ' / ' || policyname
                        FROM pg_policies
                        WHERE schemaname IN ('pacientes','prontuario','agenda','lgpd','orcamento')
                          AND (COALESCE(qual, '') LIKE '%staff_atual%'
                               OR COALESCE(with_check, '') LIKE '%staff_atual%')
                        ORDER BY 1
                        """)
                .query(String.class)
                .list();

        assertThat(vazamentos)
                .as("""
                    "Suporte N1 com zero dado clínico" precisa ser regra de banco, não \
                    promessa de código. Se uma política de dado clínico consultar \
                    plataforma.staff_atual(), o eixo B ganhou acesso a prontuário.""")
                .isEmpty();
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("a aplicação não tem UPDATE nem DELETE no que é append-only")
    void appendOnlyRealmenteEhAppendOnly() {
        List<String> comEscrita = jdbc.sql("""
                        SELECT table_schema || '.' || table_name || ' tem ' || privilege_type
                        FROM information_schema.table_privileges
                        WHERE grantee = 'dentibot_app'
                          AND privilege_type IN ('UPDATE','DELETE')
                          AND (table_schema, table_name) IN (
                                ('auditoria','eventos'),
                                ('auditoria','eventos_plataforma'),
                                ('prontuario','evolucoes'),
                                ('prontuario','odontograma_lancamentos'),
                                ('financeiro','lancamentos'),
                                ('estoque','movimentacoes'))
                        ORDER BY 1
                        """)
                .query(String.class)
                .list();

        assertThat(comEscrita)
                .as("""
                    Auditoria, evolução clínica, ledger e movimentação de estoque são \
                    append-only por norma (CFO-226, LGPD art. 37) ou por integridade \
                    contábil. O REVOKE é a primeira camada; o trigger é a segunda.""")
                .isEmpty();
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("a aplicação não consegue inserir clínica direto: só pela função de provisionamento")
    void criarTenantTemUmaPortaSo() {
        List<String> privilegio = jdbc.sql("""
                        SELECT grantee || ' tem INSERT em clinicas.clinicas'
                        FROM information_schema.table_privileges
                        WHERE table_schema = 'clinicas' AND table_name = 'clinicas'
                          AND privilege_type = 'INSERT' AND grantee = 'dentibot_app'
                        """)
                .query(String.class)
                .list();

        assertThat(privilegio)
                .as("""
                    Criar tenant é a única escrita que não acontece de dentro de um \
                    tenant. A porta é clinicas.provisionar(), que roda como \
                    dentibot_provisionador. Se a app tiver INSERT direto, a porta virou \
                    sugestão.""")
                .isEmpty();
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("a aplicação não é superusuária nem dona das tabelas")
    void appNaoIgnoraRls() {
        Boolean superusuaria = jdbc.sql(
                        "SELECT rolsuper FROM pg_roles WHERE rolname = 'dentibot_app'")
                .query(Boolean.class).single();

        assertThat(superusuaria)
                .as("Superusuário ignora RLS por completo — toda política viraria enfeite.")
                .isFalse();

        List<String> tabelasQueAppPossui = jdbc.sql("""
                        SELECT n.nspname || '.' || c.relname
                        FROM pg_class c
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        JOIN pg_roles r ON r.oid = c.relowner
                        WHERE c.relkind = 'r' AND r.rolname = 'dentibot_app'
                        """)
                .query(String.class).list();

        assertThat(tabelasQueAppPossui)
                .as("""
                    Dono de tabela ignora RLS a menos que FORCE esteja ligado. Depender \
                    disso é frágil: a app não deve possuir nada. Foi exatamente o que \
                    invalidou o RLS da V1.""")
                .isEmpty();
    }
}
