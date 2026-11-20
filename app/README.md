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
- **Clerk de produção.** O `preview` usa a instância de dev (`pk_test_`). Loja
  exige `pk_live_` no environment `production` do EAS — hoje ele está vazio, e
  um build `production` sem a chave fecha na abertura.
- **API no ar.** O build embute `https://api.dentibot.com.br`, que ainda não
  resolve. Até subir, o app entra (o Clerk é nuvem) e a agenda diz "Sem
  conexão com a API.".

## Build e release

Projeto EAS: `@gust4_lop3s/dentibot`. Ninguém compila nesta máquina: o EAS roda
o `prebuild` e o Gradle/Xcode nos workers dele, na imagem que o SDK 57 fixa.
`android/` e `ios/` são gerados e ignorados.

| Perfil | Android | iOS | Para quê |
|---|---|---|---|
| `preview` | APK assinado | IPA ad hoc | instalar direto em aparelho de teste |
| `production` | AAB | IPA App Store | lojas |

```bash
cd app
npm run eas -- login                                   # uma vez por máquina
npm run eas -- build -p android --profile preview      # APK
npm run eas -- build -p ios --profile preview          # exige conta Apple
```

No CI: GitHub → Actions → **mobile** → *Run workflow* (perfil e plataforma).

**Pré-requisitos:** Node 24.17.0 e uma conta Expo. JDK, Android SDK, Xcode e
CocoaPods não são necessários localmente. Android: `targetSdk 36`,
`minSdk 24` (Android 7.0), vindos do RN 0.86. R8 e shrink de recursos ligados
via `expo-build-properties`, com uma regra só: `-dontwarn
kotlin.MustUseReturnValues`. O `kotlinx-io` traz essa anotação de um Kotlin
mais novo que o R8 do AGP 8.12, e sem a regra o `minifyReleaseWithR8` falha.
Anotação ausente não quebra nada em runtime. Remova a regra quando o AGP subir.

### Subir a versão

- **Versão de exibição:** `npm version patch|minor|major` no `app/`. O
  `app.config.js` repassa `package.json#version` ao Expo; não há outra cópia.
- **Número de build** (`versionCode` / `buildNumber`): é do EAS
  (`appVersionSource: remote`) e sobe sozinho a cada build. Para forçar:
  `npm run eas -- build:version:set`.

### Onde está cada segredo

Nada disto está no repositório; o `.gitignore` barra `*.jks`, `*.p8`, `*.p12`,
`*.key`, `*.mobileprovision` e `.env*`.

| Segredo | Onde vive | Como chegar |
|---|---|---|
| Keystore Android (+ alias e senhas) | EAS credentials do projeto | `npm run eas -- credentials -p android` |
| Cópia de segurança do keystore | fora do repo, ex. `%USERPROFILE%\.dentibot\` | baixar pelo comando acima |
| `EXPO_TOKEN` (CI) | GitHub → Settings → Secrets → Actions | gerar em expo.dev → Access tokens |
| `EXPO_PUBLIC_API_URL`, `EXPO_PUBLIC_CLERK_PUBLISHABLE_KEY` | EAS → environments `preview` / `production` | `npm run eas -- env:list --environment preview` |
| Certificado de distribuição + perfil iOS | EAS credentials (quando houver conta Apple) | gerados no 1º `build -p ios` |
| Chave da App Store Connect API (`.p8`) | EAS submit credentials | `npm run eas -- credentials -p ios` |

### Trocar material de assinatura

- **Keystore Android:** para APK fora da loja, um keystore novo **não instala
  por cima** do antigo — o aparelho exige desinstalar. Na Play, perder o
  keystore de upload não é fatal (o Google reseta a chave de upload). Faça
  backup antes de qualquer troca; troque por `eas credentials`.
- **iOS:** certificado e perfil vencem em 1 ano; `eas credentials -p ios`
  regenera, e o próximo build usa o novo.
- **`EXPO_TOKEN`:** revogar em expo.dev, gerar outro e atualizar o secret.

### iOS sem conta Apple (estado atual)

Não há IPA: assinar exige o Apple Developer Program. O projeto já está pronto
para ele — bundle `br.com.dentibot.app`, sem permissões no `Info.plist`,
`ITSAppUsesNonExemptEncryption: false`. Quando a conta existir:

1. Team ID em developer.apple.com → Membership.
2. `npm run eas -- build -p ios --profile preview` — o EAS pede o login Apple,
   registra o bundle id, cria certificado de distribuição e perfil **Ad Hoc**
   (registre antes os UDIDs com `npm run eas -- device:create`).
3. Loja: `--profile production` gera perfil **App Store**; envio com
   `npm run eas -- submit -p ios` (TestFlight).

Nenhum passo exige Mac. Projeto Xcode local, se um dia for útil:
`npx expo prebuild -p ios` num Mac gera `ios/DentiBot.xcworkspace`, esquema
`DentiBot`.

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
