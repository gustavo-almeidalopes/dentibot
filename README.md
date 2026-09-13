# DentiBot v2.0.0

Software de gestão para consultório odontológico. Multi-tenant: cada clínica é
um tenant, e o isolamento é imposto pelo Postgres, não pelo código da aplicação.

```text
web/            landing page + telas web — React 19 (Vite)
app/            app Android e iOS — um projeto Expo / React Native
api-java/       API — Spring Boot 3, Java 25, PostgreSQL, Redis
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

As duas telas fora da landing (`/login` e `/clientes`) são a mesma
`index.html`: o `main.jsx` escolhe o componente por `window.location.pathname`,
sem router. Em servidor estático isso só funciona com rewrite — abrir `/login`
direto procuraria um arquivo `/login`, que não existe, e daria o mesmo 404.

Rota nova em `web/src/main.jsx` pede rewrite novo nos dois arquivos. O
catch-all `/(.*)` ficou de fora de propósito: com ele, URL inexistente
devolveria a landing com 200 em vez de um 404 de verdade.

### Variáveis de ambiente

Em **Settings > Environment Variables** do projeto:

| Variável | Quando | Para quê |
| --- | --- | --- |
| `VITE_CLERK_PUBLISHABLE_KEY` | sempre | Chave pública do Clerk. O `ClerkProvider` não recebe `publishableKey` por prop — o `@clerk/react` cai em `import.meta.env`. O Vite resolve isso **no build**, então quem precisa da variável é a Vercel, não o browser. Sem ela a landing sobe normal e `/login` aparece sem o widget: some o formulário, não a página. |
| `VITE_API_BASE` | se a API estiver em outro domínio | Precisa incluir o `/api`. Vazio = `/api` do mesmo host, o que só serve se algo estiver fazendo proxy. |

`VITE_*` entra no bundle, que é público. Nenhum segredo aqui — a chave do
Clerk é publicável por definição e a secret key é do back-end.

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
