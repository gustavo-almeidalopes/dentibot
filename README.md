# DentiBot v2.0.0

Software de gestão para consultório odontológico. Multi-tenant: cada clínica é
um tenant, e o isolamento é imposto pelo Postgres, não pelo código da aplicação.

```text
web/            landing page + telas web — React 19 (Vite)
app/            app Android e iOS — um projeto Expo / React Native
api/            API — Spring Boot 4, Java 25, PostgreSQL, Redis
infrastructure/ compose local (Postgres, Redis, MinIO) e provisionamento
docs/           decisões de arquitetura
```

Um backend, três clientes. Web e mobile são clientes diferentes do mesmo
domínio: mesma API, mesma autorização, mesmo banco. Não há API por plataforma.

## Rodar

Precisa de Docker Desktop, JDK 25 e Node. Um comando para sair do zero:

```powershell
./infrastructure/init.ps1
```

Ele sobe Postgres, Redis e MinIO, espera o healthcheck (não um `sleep`), roda
as migrations com o role migrador e **confere o invariante de RLS antes de
devolver o prompt** — um banco com migration aplicada e RLS incompleto parece
saudável por fora. `-Recriar` apaga os volumes e começa do zero.

Depois, cada peça:

```bash
cd api      && ./mvnw spring-boot:run     # http://localhost:8080
cd web      && npm install && npm run dev # http://localhost:5173
cd app      && npm install && npm start   # depois 'a' (Android) ou 'i' (iOS)
```

O Postgres local escuta na **5433**, não na 5432: é comum já haver um
PostgreSQL nativo na máquina, e o certo é o projeto se desviar.

No Windows, o mesmo está em `.bat` na raiz, para abrir com dois cliques:

| Arquivo | Faz |
| --- | --- |
| `iniciar.bat` | `init.ps1` e, se ele passar, API e web cada um na sua janela. `-Recriar` é repassado. |
| `api.bat` | A API com as variáveis locais já definidas. O issuer do Clerk sai da chave em `web/.env.local` — nada para colar. |
| `web.bat`, `app.bat` | `npm ci` na primeira vez, depois o servidor de dev. |
| `testar.bat` | O "Verificar" abaixo, parando na primeira falha. |
| `parar.bat` | Derruba a infraestrutura; os volumes ficam. |

Variável já definida no ambiente ganha da local.

## Verificar

```bash
cd api && ./mvnw test         # inclui os testes de isolamento e de schema
cd app && npm run typecheck && npm test
cd web && npm run build
cd web && npm run a11y        # axe-core, reflow e peso da landing — precisa do Chrome
```

Os testes de `api` que mais importam não testam regra de negócio: eles
consultam o catálogo do Postgres para provar que toda tabela com `id_clinica`
tem RLS com `FORCE`, que partição tem política própria, que staff da plataforma
não alcança dado clínico e que a aplicação não é dona das tabelas. A V1 deste
projeto pretendia 31 políticas de RLS, criou zero, e vazava dados entre
clínicas sem um único teste vermelho.

O contrato da API mora em `api/openapi.json`, gerado do código (ST-20). Mudou um
endpoint? O `ContratoOpenApiTest` falha até o arquivo ser regenerado —
`./mvnw test -Dtest=ContratoOpenApiTest -Dcontrato.atualizar=true` — e a mudança
aparece no diff do PR. Do outro lado, `contrato.test.mjs` no `web/` e no `app/`
confere cada chamada dos clientes contra esse arquivo (ST-55).

`cd web && npm run e2e` roda as jornadas no Chrome (ST-46). As públicas rodam
sempre, inclusive no CI; as logadas — agenda, pacientes, financeiro e o
cadastro de paciente até a evolução no prontuário — pedem a API de pé
(`iniciar.bat`) e `E2E_EMAIL`, um usuário do Clerk de dev sem 2FA que já passou
pelo `/cadastro`. `E2E_URL` aponta para um deploy e vira smoke: lá a jornada
que grava dados não roda.

Carga (ST-47) é `perf/agenda.js`, em k6 pelo Docker; o cabeçalho do script diz
como rodar e o que a API precisa. Reprova com p95 da agenda acima de 400 ms ou
qualquer endpoint 20% pior que `perf/linha-de-base.json` (`GRAVAR=1` a
regrava).

## Operar

| Onde | O quê | Variável |
| --- | --- | --- |
| `GET /actuator/health/liveness` | O processo está vivo? Não toca em nada externo. | — |
| `GET /actuator/health/readiness` | Pode receber tráfego? Inclui o banco; Redis não, porque o rate limit falha aberto. | — |
| `GET /actuator/prometheus` | Métricas, com Basic `prometheus:<senha>`. Sem a variável, fechado para todos. | `DENTIBOT_METRICAS_SENHA` |
| Sentry (API) | Só erro 500, depois do `ScrubberDePii`, com ids de clínica e usuário — nunca nome. | `SENTRY_DSN`, `DENTIBOT_AMBIENTE` |
| Sentry (web) | O SDK só baixa se houver DSN. O host exato do projeto entra em `connect-src` nos dois `vercel.json`. | `VITE_SENTRY_DSN` |
| `pg_stat_statements` | Consultas mais caras no total; `auto_explain` grava o plano das que passam de 500 ms. O `init.ps1` cria a extensão. | — |
| Anexos (ST-41) | Bucket S3-compatível; o arquivo vai do navegador direto a ele, com SHA-256 amarrado na URL assinada. Sem as variáveis, anexo responde 503 e o resto sobe. O bucket precisa de CORS para PUT da origem do web. | `DENTIBOT_S3_ENDPOINT`, `DENTIBOT_S3_REGIAO`, `DENTIBOT_S3_BUCKET`, `DENTIBOT_S3_CHAVE`, `DENTIBOT_S3_SEGREDO` |
| IA (Doc 03) | Claude pela API da Anthropic. Nome de paciente sai como `[PACIENTE]`; cada chamada fica em `ia.chamadas` com custo, e a clínica liga/desliga e põe cota em `/ia`. Sem a chave, os recursos de IA respondem 503 e o resto sobe. Os preços precisam acompanhar o modelo (Opus 5: 5/25; Sonnet 5: 2/10 US$ por MTok). Política: `docs/ia/politica.md`. | `DENTIBOT_IA_CHAVE`, `DENTIBOT_IA_MODELO`, `DENTIBOT_IA_PRECO_ENTRADA`, `DENTIBOT_IA_PRECO_SAIDA` |
| WhatsApp (Doc 03-D) | WhatsApp Cloud API da Meta. Cada clínica cadastra o próprio *phone number ID* em Conversas → Canal; o webhook é `POST /api/v1/webhooks/whatsapp`, conferido por HMAC com o segredo do app. Fora da janela de 24 h só sai modelo aprovado: cadastre no Business Manager um modelo de utilidade com um único parâmetro de corpo (`{{1}}`) e o nome em `DENTIBOT_WHATSAPP_MODELO`. Sem token, as mensagens ficam programadas e nada sai; sem segredo, nenhum webhook entra. | `DENTIBOT_WHATSAPP_TOKEN`, `DENTIBOT_WHATSAPP_SEGREDO_DO_APP`, `DENTIBOT_WHATSAPP_TOKEN_DE_VERIFICACAO`, `DENTIBOT_WHATSAPP_MODELO` |
| Titular (Doc 03-E) | O admin gera no prontuário (aba Mensagens → Privacidade) um link de 30 dias para o paciente: `/titular#<token>`. O token fica no fragmento e o banco guarda só o SHA-256. | — |

## Publicar o `web/` na Vercel

O front-end não fica na raiz do repositório — está em `web/`. Na configuração
padrão a Vercel constrói a raiz, onde não existe `package.json` nem
`index.html`: ela publica um diretório vazio e responde `404: NOT_FOUND` em
todo caminho, inclusive em `/`. É o 404 clássico deste repositório, e não tem
nada a ver com o código do front.

Por isso há **dois** `vercel.json`, um para cada valor possível de Root
Directory. A Vercel lê só o que estiver no Root Directory configurado no
projeto e ignora o outro — eles nunca valem ao mesmo tempo:

| Root Directory | Arquivo que vale | O que ele faz |
| --- | --- | --- |
| `web` (recomendado) | `web/vercel.json` | Deixa a Vercel detectar o Vite sozinha; só fixa o preset e os rewrites. |
| `./` (padrão) | `vercel.json` | Compila `web/` a partir da raiz e publica `web/dist`. |

Mexeu nos rewrites de um, mexa no outro: são a mesma lista repetida, porque a
Vercel não tem como herdar entre os dois.

### Rewrites

Toda tela é a mesma `index.html`: quem escolhe o componente é o React Router,
em `web/src/main.jsx`. Em servidor estático isso só funciona com rewrite —
abrir `/agenda` direto procuraria um arquivo `/agenda`, que não existe. Por
isso os dois arquivos mandam para a `index.html` todo caminho que não começa
com `/api/`, e rota nova no router não pede rewrite novo. Endereço inexistente
cai na rota `*`, que responde "Não encontrado." — o 404 é da aplicação.

`/api/` fica de fora de propósito: com o rewrite, a chamada à API receberia
HTML em vez de um 404. O efeito é que, na Vercel, `/api/v1/...` só responde se
houver API em algum lugar — ver "A API não vai junto", abaixo.

### Cabeçalhos de segurança

Os dois arquivos declaram os mesmos `headers` (ST-32): CSP, HSTS,
`nosniff`, `X-Frame-Options`, `Referrer-Policy` e `Permissions-Policy`. O
`web/src/borda.test.mjs` reprova se os dois divergirem ou se o script inline do
`index.html` mudar sem o `sha256` correspondente na CSP.

CSP errada não dá erro na tela: fonte bloqueada vira Helvetica e o Clerk perde
o estilo, e a página continua "funcionando". Toda origem nova entra nos **dois**
arquivos:

- **Clerk de produção** — `clerk.<domínio>` troca `subtle-tick-4973.clerk.accounts.dev`
  em `script-src` e `connect-src`;
- **API em outro domínio** — a origem entra em `connect-src`.
- **Bucket de anexos (R2)** — o navegador faz PUT direto nele: a origem do
  bucket (`https://<conta>.r2.cloudflarestorage.com`) entra em `connect-src`.
  "Abrir" usa `window.open`, que é navegação, e não depende de `img-src`.

### Variáveis de ambiente

Em **Settings > Environment Variables** do projeto:

| Variável | Quando | Para quê |
| --- | --- | --- |
| `VITE_CLERK_PUBLISHABLE_KEY` | sempre | Chave pública do Clerk. O `ClerkProvider` não recebe `publishableKey` por prop — o `@clerk/react` cai em `import.meta.env`. O Vite resolve isso **no build**, então quem precisa da variável é a Vercel, não o browser. Sem ela a landing sobe normal e `/login` aparece sem o widget: some o formulário, não a página. |
| `VITE_API_BASE` | na Vercel, sempre: a API não está no mesmo domínio | Precisa incluir o `/api/v1`. Vazio = `/api/v1` do mesmo host, o que só serve se algo estiver fazendo proxy. |

`VITE_*` entra no bundle, que é público. Nenhum segredo aqui — a chave do
Clerk é publicável por definição e a secret key é do back-end.

### A API não vai junto

A Vercel publica só o `web/`. O back-end (Spring Boot, Postgres e Redis) roda
em outro lugar, e o build precisa saber onde: sem `VITE_API_BASE`, o front
chama `/api/v1` no próprio domínio da Vercel, que responde 404. O login
funciona, porque é do Clerk, mas nenhuma tela depois dele tem dado — o app
mostra "API ausente." com o menu no lugar, para ninguém ficar preso numa tela.

Para ligar os dois:

1. Publique a API com a origem do site em `DENTIBOT_CORS_ORIGINS` e em
   `DENTIBOT_CLERK_ORIGINS` — sem a segunda, o token que o Clerk emitiu para o
   site é recusado.
2. Defina `VITE_API_BASE=https://<api>/api/v1` na Vercel.
3. Ponha `https://<api>` em `connect-src` nos dois `vercel.json`.
4. Publique o site de novo: `VITE_*` entra no bundle na hora do build.

## Arquitetura

- **[docs/architecture/tenancy.md](docs/architecture/tenancy.md)** — como uma
  clínica é impedida de ler os dados de outra. Leia antes de mexer em acesso a
  dados: é possível desligar o isolamento sem ver erro nenhum.
- **[app/README.md](app/README.md)** — escopo do mobile, sessão e o que as
  lojas exigem antes de publicar.
- **[docs/acessibilidade.md](docs/acessibilidade.md)** — o que o gate de
  acessibilidade prova, o que não prova, e o roteiro manual de cada release.

Pontos fixos do desenho:

- **O tenant vem do token, nunca do corpo da requisição.** Nenhum DTO tem
  `idClinica`.
- **O `WHERE` é do banco.** A aplicação não filtra por clínica; o RLS filtra, e
  sem contexto devolve zero linhas em vez de vazar.
- **A aplicação não é dona de nenhuma tabela.** Se fosse, o Postgres a
  deixaria ignorar as políticas.
- **Migration aplicada nunca muda.** Checksum divergente falha o start; não se
  "repara".
- **Nenhum segredo no repositório.** Tudo por variável de ambiente; o gate de
  gitleaks é bloqueante. As senhas do compose local são em claro de propósito,
  para que fique óbvio que não são de produção.

## Estado

`api/` é a parte viva: agenda, pacientes, prontuário, identidade,
onboarding, auditoria, outbox, idempotência e rate limit, sobre 17 migrations.

`app/` cobre login, agenda do dia com as transições de consulta, lista de
pacientes e perfil. O que ficou fora e por quê está no README dele.

`web/` é a landing e o sistema da clínica sobre a API v1: agenda, pacientes e
prontuário, conversas, acompanhamento, financeiro, estoque, equipe, auditoria
e IA, ligados pelo menu do `Layout`, que mostra a cada papel só o que o `/eu`
diz que ele alcança. Publicado sozinho, não tem dado — ver "A API não vai
junto".
