# Borda do web: cabeçalhos de segurança (ST-32)

Primeiro sub-projeto da aplicação do Documento 01 (Melhorias de Stack) à V2.
Escolhido primeiro porque não toca `api/`, que está no meio de um rename e de
uma branch de segurança ainda fora do `main`.

## O Documento 01 contra a V2

O Doc 01 foi escrito lendo a V1 — `gateway/app.py`, `services/*` em Flask,
PyMySQL, `frontend/nginx.conf`, `src/js/api.js` —, removida em `6c5d689`. Lido
contra o código de hoje, os 61 itens se dividem assim:

| Situação | Itens |
|---|---|
| Resolvido na V2 | 05, 06, 07, 09, 10, 11, 15, 19, 24, 29, 30, 31, 38, 44 (parcial), 50, 51, 52, 53, 56 |
| Não se aplica (era da V1) | 01, 02, 03, 04, 08, 16, 17, 21, 22, 33, 42 |
| Aberto, pequeno | 13, 25, 26, 32, 36, 45, 48 |
| Aberto, médio | 20, 28, 35, 37, 41, 46, 47, 54, 55 |
| Depende de hospedagem ou é YAGNI hoje | 12, 14, 18, 23, 27, 34, 39, 40, 43, 49, 57–61 |

Os abertos viram sub-projetos, nesta ordem, cada um com spec e plano próprios:

0. Base consolidada — rename `api-java/ → api/` e merge de `fix/seguranca-borda` (feito pelo dono do repositório)
1. Pipeline e cadeia de suprimentos — ST-44 (gate de cobertura), 45, 48, 37, 35, 36
2. Operação da API — ST-25, 26, 13, 28
3. **Borda do web — ST-32 (este documento)**
4. Contrato — ST-20, 55
5. Testes de sistema — ST-46, 47
6. Anexos clínicos — ST-41
7. PWA offline — ST-54

Toda melhoria segue o brutalismo do produto: obsidiana, osso, um vermelho, canto
vivo, Antonio. Inclusive as que não desenham tela.

## O que está exposto hoje

Nenhum dos dois `vercel.json` declara `headers`. Na prática:

- qualquer site pode carregar `/login` num `<iframe>` e sobrepor o formulário
  do Clerk — clickjacking na tela onde se entra num sistema de prontuário;
- um XSS não encontra CSP nenhuma: o script injetado fala com qualquer origem;
- sem `Referrer-Policy` explícita, o comportamento fica a cargo do browser —
  e a URL `/pacientes/<id>/prontuario` é o tipo de caminho que não deveria
  depender disso.

## Decisão

`headers` estáticos nos **dois** `vercel.json` — um recurso da plataforma, sem
código em runtime.

Descartados:

- **Edge Middleware com nonce** — põe uma função no caminho de toda página
  estática para proteger uma linha de script.
- **Remover o `<script>` inline do `index.html`** — ele tira `.no-js` antes da
  pintura, e é isso que segura o `[data-reveal]` da landing. Movê-lo para depois
  faz a página piscar.

O script inline entra na CSP por hash. O Vite o preserva byte a byte no build
(conferido em `web/dist/index.html`).

## Os cabeçalhos

Aplicados em `/(.*)`:

```
Content-Security-Policy:
  default-src 'self';
  script-src 'self' 'sha256-XwWdy3h8RLzX10dUs9n/fCAkQc+XFysyIb7c80Ymdkw=' https://subtle-tick-4973.clerk.accounts.dev https://challenges.cloudflare.com;
  connect-src 'self' https://subtle-tick-4973.clerk.accounts.dev;
  img-src 'self' data: https://img.clerk.com;
  style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
  font-src 'self' https://fonts.gstatic.com;
  frame-src https://challenges.cloudflare.com;
  worker-src 'self' blob:;
  object-src 'none';
  base-uri 'self';
  form-action 'self';
  frame-ancestors 'none';
  upgrade-insecure-requests
Strict-Transport-Security: max-age=63072000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=(), usb=()
```

As escolhas que não são óbvias:

- **`style-src 'unsafe-inline'`** — o Clerk injeta o CSS do widget em runtime,
  inclusive as variáveis de `aparencia` do `main.jsx` (fundo preto,
  `borderRadius: 0`). Sem isso o login perde o brutalismo. O risco fica em
  `style-src`; `script-src` continua sem `unsafe-inline` e sem `unsafe-eval`.
- **`font-src` e `style-src` com o Google Fonts** — é de onde vêm Antonio,
  Cormorant SC e Inter.
- **`frame-src` e `script-src` com `challenges.cloudflare.com`** — o Turnstile,
  a proteção contra bot do Clerk no cadastro.
- **`connect-src` sem a API** — ela ainda não tem endereço público; o
  `VITE_API_BASE` vazio cai em `/api/v1` da mesma origem, coberto por `'self'`.
- **HSTS sem `preload`** — entrar na lista de preload dos browsers é um
  compromisso que leva meses para desfazer, e vale para todo subdomínio.
- **`X-Frame-Options` junto de `frame-ancestors`** — o segundo é o padrão; o
  primeiro cobre browser antigo e não custa nada.

## O brutalismo como critério de aceite

CSP errada não dá erro na tela. Fonte bloqueada vira Helvetica, estilo do Clerk
bloqueado vira um formulário branco de canto arredondado, e a página continua
"funcionando". Por isso o aceite é visual, num deploy de preview:

1. landing: `document.fonts.check('1em Antonio')` é `true`, e o título está em
   Antonio;
2. `/login`: o widget do Clerk aparece, com fundo preto e cantos vivos;
3. console sem nenhuma violação de CSP nas duas páginas.

## O teste guardião

`web/src/borda.test.mjs`, no mesmo estilo de `api.test.mjs`, só com `node:`:

- `headers` e `rewrites` são idênticos nos dois `vercel.json` — o README já
  pede isso, e agora a máquina confere;
- todo `<script>` inline do `index.html` tem o `sha256` correspondente no
  `script-src` — editar a linha sem atualizar o hash quebra o teste, e não a
  produção;
- os seis cabeçalhos existem; `script-src` não tem `unsafe-inline` nem
  `unsafe-eval`; há `frame-ancestors 'none'` e `object-src 'none'`.

Roda em `npm test`, que já faz `node --test src/*.test.mjs`.

## O que muda depois

- **Instância de produção do Clerk** — o domínio `clerk.<domínio>` substitui
  `subtle-tick-4973.clerk.accounts.dev` em `script-src` e em `connect-src`.
- **API publicada em outro domínio** — a origem entra em `connect-src`.

Nos dois casos, nos dois arquivos. Isso vai no README, numa subseção
"Cabeçalhos de segurança", ao lado de "Rewrites".

## Fora de escopo

- COOP e CORP — não pontuam no Observatory, e COOP pode quebrar OAuth em popup.
- `report-uri` — não há coletor. Quando existir o Sentry (ST-28), ele recebe os
  relatórios de CSP.
- Telemetria do Clerk (`clerk-telemetry.com`) — bloqueada, vira ruído no
  console do preview, e não perde nada.

## Verificação

1. `npm test` e `npm run build` em `web/`.
2. Deploy de preview na Vercel, com push autorizado pelo dono do repositório, e
   os três critérios visuais acima conferidos no Chrome.
3. Mozilla Observatory com meta A. Se o preview estiver atrás da proteção de
   deploy da Vercel, o Observatory não o alcança, e a medição fica para depois
   do merge, em produção.
