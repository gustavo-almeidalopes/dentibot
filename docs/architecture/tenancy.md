# Tenancy

Como uma clínica é impedida de ler os dados de outra.

Este documento é citado por `application.yml` e por `V1__plataforma_base.sql`
sempre pelo mesmo motivo: o isolamento aqui não é um `WHERE id_clinica = ?`
escrito com disciplina em cada consulta — é uma propriedade do banco. Quem
mexer no acesso a dados sem entender isso consegue desligá-lo sem ver erro
nenhum.

## O que aconteceu na V1

A V1 deste projeto pretendia 31 políticas de RLS. Zero foram criadas, e o
vazamento entre clínicas era reproduzível em três linhas de SQL. A causa não
foi política mal escrita: a aplicação conectava com o role que criara as
tabelas, e o Postgres deixa o dono ignorar RLS. Não havia erro, não havia
aviso, não havia teste vermelho.

Daí as duas regras que governam tudo abaixo:

1. A aplicação nunca é dona de tabela nem superusuária.
2. Toda garantia tem teste que falha quando ela some.

## A cadeia

Cinco elos. Qualquer um que quebre derruba o isolamento — e todos foram
construídos para quebrar **fechando** (zero linhas ou exceção), nunca abrindo.

```text
FiltroAutenticacao            valida o JWT → ContextoAtual (ThreadLocal)
        │
        ▼
GerenciadorTransacaoComTenant doBegin() → set_config(..., is_local=true)
        │
        ▼
GUCs da transação             app.clinica · app.usuario · app.staff
        │                     app.correlacao · app.worker
        ▼
plataforma.clinica_atual()    NULLIF(current_setting('app.clinica', true), '')::BIGINT
        │
        ▼
POLICY tenant_isolation       USING (id_clinica = plataforma.clinica_atual())
```

**Elo 1 — contexto da requisição.** `FiltroAutenticacao` traduz o token em
`ContextoRequisicao` e o guarda em `ContextoAtual`, limpando no `finally`. O
record recusa no construtor um contexto com `clinicaId` **e** `staffPapel`: os
dois eixos de autorização são mutuamente exclusivos por construção, não por
convenção.

**Elo 2 — injeção no banco.** `GerenciadorTransacaoComTenant` estende
`JdbcTransactionManager` e sobrescreve `doBegin`, aplicando o contexto logo
após o `super`. `BancoConfig` o registra como `@Primary` — esse bean é a
diferença entre "temos RLS configurado" e "o RLS está valendo".

Três detalhes, cada um capaz de derrubar a cadeia sozinho:

- **`SET LOCAL` não aceita bind.** É comando utilitário. A saída tentadora —
  concatenar o id na string — troca um problema de tenancy por um de injeção
  de SQL. `set_config()` é forma de função e aceita parâmetro.
- **O terceiro argumento `true` é `is_local`.** Sem ele o valor sobrevive ao
  fim da transação e, com pool de conexões, a próxima requisição herda o
  tenant da anterior em silêncio. É o pior bug possível neste sistema e é uma
  letra de diferença.
- **Tem de ser depois do `begin`.** Fora de transação, um `set_config` local
  vale só para o statement e some.

**Elo 3 — as GUCs.** Cinco, todas com `is_local=true`, todas escritas como
string vazia quando ausentes (nunca `NULL`).

**Elo 4 — as funções de contexto.** `current_setting(..., true)` — o `true` é
`missing_ok`; sem ele uma requisição sem tenant levanta exceção em vez de
filtrar. `NULLIF(..., '')` porque GUC indefinida devolve string vazia e
`''::BIGINT` estoura. Todas são `STABLE` com `SET search_path = pg_catalog`,
para que ninguém plante uma função homônima num schema anterior do
`search_path` e sequestre a decisão.

**Elo 5 — as políticas.** 50 tabelas têm `ENABLE` + `FORCE`; 45 delas seguem o
padrão abaixo (as outras são `staff_somente` ou `worker`, e as partições de
auditoria recebem a sua por `format()`, uma por partição):

```sql
ALTER TABLE <t> ENABLE ROW LEVEL SECURITY;
ALTER TABLE <t> FORCE  ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON <t>
    FOR ALL TO dentibot_app
    USING      (id_clinica = plataforma.clinica_atual())
    WITH CHECK (id_clinica = plataforma.clinica_atual());
```

`ENABLE` liga a política para quem não é dono; `FORCE` liga também para o
dono. As duas são necessárias, e é a segunda que quase todo mundo esquece.
`USING` filtra leitura, `WITH CHECK` barra escrita cruzada — sem a segunda, o
tenant A insere linha com `id_clinica` de B e ela some do alcance de A.

## Por que falha fechando

`plataforma.clinica_atual()` devolve `NULL` quando não há tenant. Em SQL,
`NULL` não casa com nada — nem com `NULL`. Logo `id_clinica = NULL` devolve
zero linhas.

Uma requisição sem contexto não vê o banco inteiro: não vê nada. O sistema
para em vez de vazar.

O problema disso é que zero linhas também é um resultado plausível: a tela
abre vazia, exceção nenhuma aparece, e o próximo passo de alguém com pressa é
suspeitar do RLS e desligá-lo. Por isso existe `GuardaDeTransacao`, um aspecto
`@Before` em tudo anotado com `@Repository` que recusa acesso fora de
transação com a mensagem já contendo o conserto (`@Transactional`, inclusive
`readOnly = true` para leitura).

> Nota que ficou no histórico: sobrescrever
> `JdbcTemplate.execute(PreparedStatementCreator, PreparedStatementCallback)`
> **não** funciona como guarda — o `query()` do Spring chama uma sobrecarga
> interna de três argumentos e passa ao largo da pública. O teste que deveria
> falhar passou. É por isso que o guard tem teste próprio.

## Os quatro sujeitos

Três operações legítimas não cabem dentro de um tenant. Nenhuma delas ganhou
exceção na política — cada uma ganhou um **sujeito próprio**, com privilégio
mínimo. A diferença importa: uma flag de sessão a aplicação liga sozinha; a
associação a um role, não.

| Role | Login | Posse | Existe para |
|---|---|---|---|
| `dentibot_migrador` | sim | dono de tudo | Flyway. Credencial separada da app |
| `dentibot_app` | sim | **nenhuma** | a aplicação. Sem superusuário, sem criar nada |
| `dentibot_provisionador` | não | `clinicas.provisionar()` | criar clínica |
| `dentibot_autenticador` | não | as funções de login | resolver tenant antes do login |

**Provisionamento (V11).** Com `FORCE`, inserir a clínica 7 exigiria já estar
no tenant 7 — que ainda não existe. `dentibot_app` perde `INSERT` em
`clinicas.clinicas` e ganha só `EXECUTE` em `clinicas.provisionar()`, que é
`SECURITY DEFINER` e pertence ao provisionador. A função faz uma coisa: insere
a linha. Configurações, pessoa e usuário admin vêm depois, pelo Java, já com
`app.clinica` definido — a superfície `SECURITY DEFINER` tem o tamanho de um
`INSERT`.

Note que o alcance real do provisionador é definido pelo `GRANT INSERT, SELECT`
(sem `UPDATE`/`DELETE`), e não pela política, que é `USING (true)`: privilégio
e política são avaliados em conjunto.

**Login (V3).** Aqui mora a armadilha que custou uma sessão de depuração:

> **`SECURITY DEFINER` não contorna RLS.** Ele só troca *quem* a função é. Com
> `FORCE` ligado, nem o dono escapa — e um role que nenhuma política nomeia (o
> migrador, já que as políticas são `TO dentibot_app`) enxerga zero linhas. A
> função "funcionava", devolvia `NULL`, e o login respondia "credenciais
> inválidas" para a senha certa.

`dentibot_autenticador` tem política explícita e privilégio **por coluna**:
`GRANT SELECT (id_clinica, email) ON identidade.usuarios`. Se
`resolver_clinica_por_email` um dia tentar ler `senha_hash`, o Postgres
recusa. (Para staff o hash é necessário — é o fluxo de autenticação inteiro —
e a função devolve apenas as colunas dele.)

**Worker.** `app.worker = 'true'` e `plataforma.modo_worker()`, com políticas
próprias em `plataforma.outbox`, `eventos_processados`, `webhooks_recebidos`,
`idempotencia`, `billing.*` e `financeiro.cobrancas`. O construtor de
`ContextoRequisicao` recusa worker com tenant ou staff: um worker não age como
usuário.

`ContextoBanco` (`promoverClinica`/`promoverUsuario`) é a única porta para
promover tenant no meio de uma transação, e existe para exatamente dois
fluxos: login e onboarding. É classe com nome próprio, e não um `set_config`
solto num serviço, para que qualquer uso novo apareça numa busca por
referências e precise ser justificado.

## Os dois eixos

Usuário de clínica (eixo A) e staff da plataforma (eixo B) nunca se cruzam.
Staff não tem `id_clinica`, então nenhuma GUC de tenant o destrava; ele é
reconhecido por `plataforma.staff_atual() IS NOT NULL`.

Staff enxerga o cadastro das clínicas — `staff_leitura` em `clinicas.clinicas`,
`FOR SELECT` apenas — porque billing e suporte precisam. **Nenhuma tabela de
dado clínico** (`pacientes`, `prontuario`, `agenda`, `lgpd`) ganha política
equivalente. "Suporte N1 com zero dado clínico" é regra de banco verificada
por teste, não promessa de código.

## O que está travado por teste

`ConformidadeDoSchemaTest` consulta o catálogo do Postgres, não o código:

- toda tabela com `id_clinica` tem RLS, `FORCE` e `tenant_isolation`;
- toda partição tem política própria — no Postgres a política da tabela
  particionada **não** é herdada, e consultar a partição direto a contorna
  (foi um vazamento real encontrado durante o porte, em `auditoria.eventos`);
- nenhuma tabela de dado clínico tem política que consulte `staff_atual()`;
- a aplicação não tem `UPDATE`/`DELETE` no que é append-only;
- a aplicação não insere clínica direto, só pela função de provisionamento;
- **a aplicação não é superusuária nem dona das tabelas** — o teste que a V1
  não tinha.

`IsolamentoDeTenantTest` prova o comportamento de ponta a ponta. Cada asserção
de negação vem acompanhada de uma de permissão: sem isso, "A não vê B" fica
verde com o banco vazio, que é a forma mais comum de um teste de isolamento
mentir.

## Como quebrar isso sem perceber

Ordem decrescente de probabilidade:

1. **Apontar a app para o role dono** (ou dar superusuário a ela). Todas as
   políticas viram decoração, sem um erro sequer. É literalmente o bug da V1.
2. **Esquecer `FORCE`** numa tabela nova. `ENABLE` sozinho não vale para o
   dono.
3. **Esquecer `is_local=true`** num `set_config`. O tenant vaza para a próxima
   requisição pela conexão do pool.
4. **Criar partição sem política própria.** A do pai não desce.
5. **Consultar fora de transação.** Hoje isso levanta
   `AcessoForaDeTransacaoException` — mas só em classe anotada com
   `@Repository`. Acesso a dados fora dessa anotação não é coberto.
6. **Escrever `WHERE id_clinica = ?` no Java** achando que é defesa em
   profundidade. Não é: cria a impressão de que o filtro é da aplicação e
   torna aceitável, numa consulta futura, esquecê-lo. O `WHERE` é do banco.

Ao adicionar tabela com `id_clinica`: `ENABLE` + `FORCE` + `tenant_isolation`
com `USING` **e** `WITH CHECK`. `ConformidadeDoSchemaTest` cobra os três.
