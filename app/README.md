# DentiBot — app

App móvel da clínica. **Um** projeto Expo/React Native que compila para Android
e iOS; não há código separado por plataforma, e é esse o ponto.

## Para quem é

Equipe da clínica — dentista, recepção, admin. **Não é o app do paciente**, e
isso não é escolha de produto: o backend não tem autenticação de paciente.
`identidade.usuarios` são usuários de clínica com `Papel`
(`admin`/`dentista`/`recepcionista`/`financeiro`/`auxiliar`), e
`pacientes.pacientes` é tabela de dado, sem credencial. O `ContextoRequisicao`
tem exatamente dois eixos — usuário de clínica e staff da plataforma — e nenhum
deles é "paciente". Um app de paciente exige primeiro um terceiro eixo de
identidade no backend.

## O que tem

| Tela | Endpoint |
|---|---|
| Login | `POST /api/v1/auth/login`, `GET /api/v1/auth/me` |
| Agenda do dia | `GET /api/v1/consultas?de=&ate=` + confirmar / concluir / faltou / cancelar |
| Pacientes | `GET /api/v1/pacientes?limite=&apos=` (keyset) |
| Perfil | `POST /api/v1/auth/logout` |

A agenda é o app. O caso de uso é o dentista entre dois pacientes, telefone na
mão, tocando "Confirmar" ou "Concluir".

## O que não tem, de propósito

- **Odontograma.** Por elemento e face, em 6 polegadas, é UI cara e pior que a
  web. Fica no desktop.
- **Agendar consulta.** Exige seletor de paciente, de dentista, de procedimento
  e de horário com checagem de conflito — trabalho de balcão, em teclado.
- **Cadastrar paciente.** Sete campos com CPF e convênio. O endpoint existe; a
  tela não.
- **Financeiro e estoque.** Nada no telefone justifica ainda.
- **Push, analytics, Sentry.** Entram quando houver o que notificar e o que
  observar. Cada SDK entra na declaração de Data Safety do Google Play e no
  formulário de privacidade da Apple — inclusive o que você não usa de fato.
- **Permissões de aparelho: zero.** `android.permissions` é `[]` e o
  `Info.plist` não pede nada. Sem câmera, sem localização, sem contatos. Só
  entra permissão quando uma tela precisar, e no momento em que precisar.

## Rodar

```bash
cd app
npm install
cp .env.example .env      # ajuste EXPO_PUBLIC_API_URL
npm start                 # depois: 'a' para Android, 'i' para iOS
```

`EXPO_PUBLIC_API_URL` por ambiente:

| Onde | Valor |
|---|---|
| Emulador Android | `http://10.0.2.2:8080` |
| Simulador iOS | `http://localhost:8080` |
| Aparelho físico | `http://<ip-da-máquina>:8080` |

### A pegadinha do desenvolvimento

O refresh viaja em cookie com `Secure`, e **cookie `Secure` não é armazenado
sobre HTTP puro**. Contra a API local em `http://`, o login funciona e a sessão
morre 15 minutos depois, quando o access token expira e não há refresh para
renovar. No backend, em dev:

```
DENTIBOT_COOKIE_SEGURO=false
```

Em produção isso fica `true` — é o padrão e não se mexe.

Lembre também de `DENTIBOT_CORS_ORIGINS`: é irrelevante aqui. App nativo não
manda `Origin` e não passa por CORS. Se a requisição do app falha, o problema
não é CORS.

## Verificar

```bash
npm run typecheck     # tsc --noEmit
npm test              # node --test src/*.test.mjs
npx expo-doctor
```

`src/contrato.test.mjs` **lê o código Java** e compara com o app:

- a lista de rotas que exigem `Idempotency-Key` contra
  `FiltroIdempotencia.PREFIXOS_OBRIGATORIOS`;
- `ACOES_POR_STATUS` contra as transições de `AgendaServico`.

É o tipo de duplicata que apodrece calada: o backend ganha uma transição, o app
segue oferecendo os botões antigos e o usuário toma 409 sem entender. O teste
falha em vez disso. Segue o estilo do repositório, onde `FronteiraDeSchemaTest`
varre SQL e `ConformidadeDoSchemaTest` consulta o catálogo do Postgres.

## Sessão e credenciais

- **Access token só em memória.** Dura 15 minutos; persistir criaria uma cópia
  de credencial para alguém achar. Fechar o app zera a variável.
- **Refresh em cookie `HttpOnly`**, no cookie store nativo (NSHTTPCookieStorage
  no iOS, CookieManager no Android), que persiste entre execuções. Nenhum
  código do app toca nele — é ilegível para o JavaScript, que é a propriedade
  desejada.
- Na abertura o app **não sabe** se tem sessão, porque não consegue ler o
  cookie. Ele pergunta: um `POST /auth/refresh`. 200 significa que havia
  sessão; 401, que não havia.

Isso troca uma requisição de partida por não ter credencial legível no
aparelho. A alternativa que o OWASP MASVS prefere — refresh em
`expo-secure-store` (Keychain/Keystore) — exige que o backend aceite o refresh
no corpo, e hoje ele só o lê de `@CookieValue("dentibot_refresh")`
(`AutenticacaoController.java:77`). São ~6 linhas no controller; enquanto não
existirem, o cookie é o caminho correto.

> **Verificar em aparelho físico antes de confiar:** que o cookie sobrevive ao
> fechamento do app nas duas plataformas. O comportamento é o documentado do
> RN, mas é a única premissa deste desenho que não dá para checar por
> typecheck.

## Antes de publicar

Nada abaixo é código — é o que as lojas exigem e ainda não existe:

- **URLs de privacidade.** A Apple exige uma URL de política de privacidade no
  App Store Connect; o Google Play exige a seção Data Safety consistente com
  ela. Precisam existir e responder: `/privacy` e `/privacy/choices`.
- **Exclusão de conta** provavelmente **não** se aplica: o app não cria conta.
  Usuários são provisionados pelo admin da clínica. Confirmar antes de
  submeter.
- **Store billing (IAP)** provavelmente **não** se aplica: é assinatura B2B de
  software, vendida fora do app, e o app não vende nada. Não construa o
  adaptador de Store Billing sem confirmar a regra vigente.
- **Ícones e splash.** `app.json` define só a cor de fundo.
- **EAS Build** para gerar os binários e assinar. Nada de credencial de
  assinatura no repositório — o `.gitignore` já barra `*.jks`, `*.p8`, `*.p12`,
  `*.mobileprovision`.

## Estrutura

```text
app/
├─ app.json                 Expo: bundle id, package, scheme, permissões (vazias)
└─ src/
   ├─ api.ts                cliente HTTP, sessão, idempotência, contratos
   ├─ auth.tsx              ProvedorDeAuth + useAuth
   ├─ theme.ts              tokens transcritos de web/src/style.css
   ├─ ui.tsx                Botao, Campo, Erro, Titulo…
   ├─ contrato.test.mjs     compara app × backend
   └─ app/                  rotas (expo-router)
      ├─ _layout.tsx        provider + fundo preto
      ├─ index.tsx          porteiro: /agenda ou /login
      ├─ login.tsx
      └─ (clinica)/         tudo aqui exige sessão (guarda no layout)
         ├─ _layout.tsx     tabs
         ├─ agenda.tsx
         ├─ pacientes.tsx
         └─ perfil.tsx
```

O guarda de sessão está no `_layout.tsx` do grupo, não em cada tela, para que
tela nova nasça protegida — mesmo princípio do `anyRequest().authenticated()`
na cadeia de segurança do backend.
