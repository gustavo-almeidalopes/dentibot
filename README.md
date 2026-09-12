# DentiBot v2.0.0

Software de gestão para consultório odontológico. Multi-tenant: cada clínica é
um tenant, e o isolamento é imposto pelo Postgres, não pelo código da aplicação.

```text
web/            landing page + telas web — React 19 (Vite)
app/            app Android e iOS — um projeto Expo / React Native
api-java/       API — Spring Boot 3, Java 21, PostgreSQL, Redis
infrastructure/ compose local (Postgres, Redis, MinIO) e provisionamento
docs/           decisões de arquitetura
```

Um backend, três clientes. Web e mobile são clientes diferentes do mesmo
domínio: mesma API, mesma autorização, mesmo banco. Não há API por plataforma.

## Rodar

Precisa de Docker Desktop, JDK 21 e Node. Um comando para sair do zero:

```powershell
./infrastructure/init.ps1
```

Ele sobe Postgres, Redis e MinIO, espera o healthcheck (não um `sleep`), roda
as migrations com o role migrador e **confere o invariante de RLS antes de
devolver o prompt** — um banco com migration aplicada e RLS incompleto parece
saudável por fora. `-Recriar` apaga os volumes e começa do zero.

Depois, cada peça:

```bash
cd api-java && ./mvnw spring-boot:run     # http://localhost:8080
cd web      && npm install && npm run dev # http://localhost:5173
cd app      && npm install && npm start   # depois 'a' (Android) ou 'i' (iOS)
```

O Postgres local escuta na **5433**, não na 5432: é comum já haver um
PostgreSQL nativo na máquina, e o certo é o projeto se desviar.

## Verificar

```bash
cd api-java && ./mvnw test    # inclui os testes de isolamento e de schema
cd app && npm run typecheck && npm test
cd web && npm run build
```

Os testes de `api-java` que mais importam não testam regra de negócio: eles
consultam o catálogo do Postgres para provar que toda tabela com `id_clinica`
tem RLS com `FORCE`, que partição tem política própria, que staff da plataforma
não alcança dado clínico e que a aplicação não é dona das tabelas. A V1 deste
projeto pretendia 31 políticas de RLS, criou zero, e vazava dados entre
clínicas sem um único teste vermelho.

## Publicar

`web/` vai para a Vercel, e o `vercel.json` da raiz é quem descreve o build.
Não existe `package.json` na raiz: sem esse arquivo a Vercel clona o
repositório, não encontra nada para construir e publica um deploy vazio — que
fica `READY` no painel e responde **404** no navegador.

```json
"installCommand":  "cd web && npm ci",
"buildCommand":    "cd web && npm run build",
"outputDirectory": "web/dist"
```

O `rewrites` manda todo caminho sem arquivo correspondente para o
`index.html`. `/login` e `/clientes` são lidos de `window.location.pathname`
em `web/src/main.jsx`, e não existem como arquivo no `dist` — sem o rewrite,
abrir uma dessas URLs direto dá 404. Arquivo estático continua tendo
precedência sobre o rewrite, então `/assets/*` não é afetado.

Se o **Root Directory** do projeto na Vercel for apontado para `web`, o
`vercel.json` da raiz deixa de valer: a Vercel só lê o do diretório raiz do
projeto. Por isso o mesmo rewrite está também em `web/vercel.json` — assim o
deploy sai correto nas duas configurações.

Duas variáveis de ambiente no projeto da Vercel, ambas embutidas no bundle
pelo Vite e portanto públicas:

| Variável | O que quebra sem ela |
| --- | --- |
| `VITE_CLERK_PUBLISHABLE_KEY` | a landing carrega, mas o widget do Clerk em `/login` não monta |
| `VITE_API_BASE` | o front chama `/api` no domínio da Vercel, onde não há back-end |

Falta ainda o **Deployment Protection**, que é do painel e não do
repositório: com "Vercel Authentication" em _Standard Protection_, todo
domínio `*.vercel.app` — o de produção inclusive — pede login da Vercel antes
de servir a página. Só domínio próprio fica de fora.

## Arquitetura

- **[docs/architecture/tenancy.md](docs/architecture/tenancy.md)** — como uma
  clínica é impedida de ler os dados de outra. Leia antes de mexer em acesso a
  dados: é possível desligar o isolamento sem ver erro nenhum.
- **[app/README.md](app/README.md)** — escopo do mobile, sessão e o que as
  lojas exigem antes de publicar.

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

`api-java/` é a parte viva: agenda, pacientes, prontuário, identidade,
onboarding, auditoria, outbox, idempotência e rate limit, sobre 17 migrations.

`app/` cobre login, agenda do dia com as transições de consulta, lista de
pacientes e perfil. O que ficou fora e por quê está no README dele.

`web/` é a landing page. As telas `/clientes` e `/login` existem em
`web/src/`, mas `web/src/api.js` foi escrito para uma API que não é esta —
rotas sem `/api/v1`, `password` em vez de `senha`, `access_token` em snake_case
e token em `localStorage`, que é justamente o que o `AutenticacaoController`
recusa fazer. Essas três telas não funcionam contra o backend atual até esse
cliente ser reescrito.
