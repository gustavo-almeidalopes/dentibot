# Acessibilidade digital como gate de entrega (ODS-17, 18, 19, 16)

Primeiro sub-projeto da aplicação do Documento 04 (Melhorias ODS da ONU) à V2.
O pedido foi "a DentiBot acessível a todos, com o máximo de ODS"; entre os
sentidos de "acessível", o dono do produto escolheu começar pelo digital.

## O Documento 04 contra a V2

O Doc 04 foi escrito lendo a V1 — `cliente.html`, `communication-service`,
`inventory-service`, `audit-service`. Lido contra o código de hoje, os 61 itens
se dividem assim:

| Situação | Itens |
|---|---|
| Resolvido na V2 | 20 (preço publicado e imposto em `billing.planos`), 43 (keyset, índice, SUM sobre ledger) |
| Código, cabe agora | 01–06, 08, 11, 12, 14, 16–19, 21, 28, 32–36, 38, 44, 47–50, 52, 61 |
| Mesa do CEO (não é código) | 10, 13, 23, 24, 25, 30, 46, 53–56, 59, 60 |
| Bloqueado por fundação ausente | 07, 09, 15, 22, 26, 27, 29, 31, 37, 39–42, 45, 51, 57, 58 |

O que destrava os bloqueados: canal ou identidade de paciente (07, 40 e a parte
ativa de 01, 08, 47, 49); API hospedada (31, 41, 42); anexos, ST-41 (45); IA
(29, 51); escala de dados (09, 15, 22, 39, 58); integração externa (26 NFS-e,
27, 37 ICP-Brasil, 57 RNDS).

"Máximo de ODS" dentro da regra do próprio documento: ODS só entra no relato com
indicador medido e evidência. Alegação inflada é a primeira coisa que um
avaliador derruba, e leva junto as verdadeiras.

Os itens de código viram sub-projetos, nesta ordem, cada um com spec e plano
próprios — o programa ODS vem antes do Documento 05:

1. **Acessibilidade digital como gate de entrega — ODS-17, 18, 19, 16 (este documento)**
2. Segurança do paciente no prontuário — ODS-05, 06, 08
3. Painel de impacto sobre dado existente — ODS-61
4. Continuidade do cuidado, sem canal — ODS-01, 02, 03, 04
5. Transparência ao titular — ODS-47, 49, 50, 52, 48
6. Estoque e resíduos (PGRSS, amálgama) — ODS-35, 36, 33, 34, 38
7. Portabilidade e retenção — ODS-28, 44
8. Faixa social — ODS-21
9. Ensino e código aberto — ODS-11, 12, 14, 32 (não há `LICENSE` no repositório; decisão do dono)

Achados que decidiram a ordem: o vermelho da marca reprova AA como texto no tema
claro (abaixo); `pacientes.anamnese` — condição sistêmica, gravidez, alergia — é
gravada no cadastro e nenhuma rota nem tela a lê; metade do Anexo B já é consulta
sobre dado que a V2 grava (faltas, planos concluídos, vencimento, prazo LGPD); e
tudo que fala com o paciente esbarra na ausência de identidade de paciente e de
canal de mensagem.

Toda melhoria segue o brutalismo do produto: obsidiana, osso, um vermelho, canto
vivo, Antonio. Inclusive as que não desenham tela.

## O que já está certo

O desenho de UX de 13/09 deixou boa base: skip link nas quatro portas, `header`,
`nav` e `main`, um `h1` por tela via `Cabecalho`, `aria-current`, `role="status"`
e `role="alert"`, `<dialog>` nativo, abas com o padrão ARIA completo,
`prefers-reduced-motion` e selo de status por forma, não só por cor. O app móvel
tem alvos de 44pt e papel e rótulo em todo controle, e só tema escuro, onde o
vermelho passa (4,79:1).

## Decisões

| Decisão | Escolha |
|---|---|
| Meta | WCAG 2.2 nível AA. O Doc 02 cita 44×44 px como critério 2.5.8; está trocado — o 2.5.8 (AA) pede 24×24, e 44×44 é o 2.5.5 (AAA). Os 44pt do app ficam, como guia de plataforma. |
| Gate | Híbrido: invariantes em `node:test` em todo PR, axe-core em navegador real nas rotas públicas, roteiro manual por release nas telas autenticadas. |
| Mobile | Entra, pelas mesmas invariantes de contraste aplicadas ao `theme.ts`. |
| Base | `feat/ux-ui-telas-do-sistema`: o tema claro ainda não está no `main`. |
| Aceite | Visual, no navegador: CSS errado não dá erro, dá Helvetica e canto redondo. |

Descartado: axe também nas telas autenticadas. Exigiria o segredo do Clerk no
GitHub, usuário de teste e um mock por endpoint para manter — e automação acha
por volta de um terço dos problemas reais (o próprio Doc 02 diz), então o roteiro
manual seria necessário de qualquer jeito.

## 1. Contraste

Os defeitos são todos do mesmo tipo: literal da marca, pensado para obsidiana,
usado sobre papel. A correção não cria cor nenhuma — nem segundo vermelho, nem
cinza novo.

**O cinza da marca nas telas claras.** `.cap-ash` e `.body-ash` (`style.css`)
pintam com `--ash` (#838383) direto: 3,6:1 sobre o papel, abaixo dos 4,5:1. São
67 usos em 9 arquivos — Agenda, Auditoria, Cadastro, Equipe, Financeiro, casca,
Pacientes, Prontuário e a ficha do paciente. Conserto na raiz, uma linha: as duas
classes passam a usar `--tinta-fraca`. No tema escuro `--tinta-fraca` já é
`--ash`, então a landing não muda um pixel; no claro vira #5c5c5c (6,36:1).

**Vermelho é marca, não tinta.** #ed1c24 dá 4,16:1 sobre o papel e 4,38:1 sobre
o branco. No tema claro ele deixa de ser cor de texto em `.erro-campo`,
`.aviso[data-tom="erro"]`, `.selo[data-tom="alarme"]`, `.selo-staff`,
`.btn-perigo` e `.indicador[data-alarme="sim"]`. O texto vai para `--tinta`, e o
vermelho fica no filete de 3px à esquerda — o recurso que o aviso e o selo já
usam. Detalhes:

- `.btn-perigo`: borda vermelha e texto em tinta. No hover, fundo vermelho com
  texto **obsidiana**, não `--tinta`: #111 sobre o vermelho dá 4,31:1 e reprova;
  #000 dá 4,79:1.
- `.indicador` em alarme: o número em tinta, o filete no indicador. Assim a regra
  fica sem exceção, e o teste dela é uma linha.

**Hairline decora; controle se enxerga.** O que recebe digitação precisa se
distinguir do fundo a 3:1 (critério 1.4.11), pela borda ou pelo preenchimento.
Hoje não se distingue:

- tema claro: campos com fundo transparente e borda `--hair` (preto a 22%) —
  1,69:1. Passam a usar `--tinta-fraca` na borda (6,36:1);
- tema escuro: `.pagamento-campo input` tem o mesmo defeito com `--hair` branco a
  26% — 2,10:1 — e recebe a mesma troca. O formulário de cartão
  (`FormularioCartao.jsx`) não é montado por nenhuma tela hoje; entra porque a
  invariante varre todo campo, e remover o código fica fora daqui;
- widget do Clerk: o `colorBorder` do `aparencia` do `main.jsx` passa de branco a
  26% (2,10:1) para #838383 (5,54:1);
- app móvel: o `Campo` usa `cor.fio` (branco a 26%) sobre obsidiana — 2,10:1.
  Passa a `cor.cinza` (5,54:1).

As hairlines de tabela, de bloco e de divisória continuam finas: são separação,
não controle. O axe não mede contraste de não-texto — por isso isto vai para o
teste estático.

**Dente ausente.** No odontograma, `opacity: .38` apaga o número junto com o
dente: 2,46:1. O tracejado já diz "ausente"; a opacidade sai do número.

**O `/login`, medido no navegador.** O `Login.jsx` passa ao widget uma segunda
aparência (`APARENCIA`, de `95dcecc`) com tokens de outro design system —
`--brand` #0f766e, `--ink`, `--surface`, `--line`, `--radius-sm` —, que aqui não
existem e caem no fallback. Resultado computado: o link "REGISTRE-SE" em teal,
3,84:1 sobre obsidiana, reprovando AA e fora da paleta; o campo de e-mail branco
com canto de 10px. A `APARENCIA` do Login passa a cuidar só de encaixe (card sem
moldura, cabeçalho escondido, botões com `.btn`) e deixa cor e raio com o
`aparencia` brutalista do `main.jsx` — que é o que a borda do web já supunha
valer ("fundo preto e cantos vivos"). O campo do widget segue a regra acima:
obsidiana, borda #838383, canto vivo.

**Os botões e o skip link, medidos num `.app-main`.** O `.btn` da landing é
`border: 2px solid var(--bone)` com o `::before` em `--bone`, e o `.btn-fill` é
fundo `--bone`. No tema claro isso vira borda branca de 2px sobre o papel e bloco
branco sobre o papel: os botões do sistema parecem texto solto, e o hover some. O
`.skip-link` tem o mesmo defeito. É a raiz do `.cap-ash` de novo, e a correção é
a mesma: o `.btn`, o `.btn-fill`, os `::before`, os hovers e o `.skip-link` trocam
`--bone` e `--obsidian` por `--tinta` e `--fundo`. No escuro esses tokens valem
exatamente osso e obsidiana — a landing não muda —, e no claro o botão vira a
caixa preta de canto vivo que o "papel clínico" do desenho de UX já pedia.

**Mais três vermelhos e contornos, na mesma regra:** o `Estado` do `Layout.jsx`
pinta a mensagem de erro com `style={{ color: 'var(--alarm)' }}` inline — passa
a usar `.erro-campo`; o `UserButton` usa `colorBorder: 'rgba(0, 0, 0, .22)'`
(1,69:1 nos campos do perfil) — passa a #5c5c5c; e o hover do dente é branco a 7%
sobre o papel, invisível — passa a `--hair-fraca`.

**O que fica como está, porque passa:** o foco vermelho do dente (4,16:1 contra
3:1 de contorno), o sublinhado vermelho da aba ativa (vem com negrito), a marca de
margem da evolução retificada e toda a landing escura — vermelho sobre preto
4,79:1, cinza 5,54:1, preto sobre a parede vermelha 4,79:1.

**Na mesma branch:** `Cadastro.jsx` troca `.pagamento-erro` por `.erro-campo`. É o
que o teste `nenhuma tela usa a classe de erro do formulário de pagamento` já
exige — hoje ele é o único vermelho da suíte do web —, e é a mesma troca que o
dono do repositório tem sem commit no checkout principal; as duas convergem sem
conflito.

## 2. O gate estático

Em `node:test`, sem dependência nova, no job `contratos` que já existe. O
projeto não tem jsdom nem testing-library e não vai ganhar; o que dá para provar
por máquina é o que apodrece calado.

`web/src/acessibilidade.test.mjs`:

1. **Contraste por token**, calculado com a fórmula da WCAG a partir dos dois
   `:root` do próprio CSS (`style.css` e `:root:has(.app-main)` do `app.css`):
   texto (`--tinta`, `--tinta-fraca`) sobre `--fundo` e `--superficie` ≥ 4,5:1;
   `--foco` ≥ 3:1; e a borda declarada em toda regra de `input`, `select` e
   `textarea` resolve para ≥ 3:1 contra o `--fundo` do tema daquele arquivo.
   Trocar um token por valor que reprova quebra o teste, não a produção.
2. **Literal da marca nas telas claras.** Toda regra cujas classes aparecem todas
   em `paginas/` (menos o 404, que é escuro) e nos componentes que elas montam —
   `FichaPaciente`, `Confirmar` e `primitivos` — reprova se pintar texto com
   `--ash`, `--bone` ou `--alarm`, ou fundo e borda com `--bone`; e nenhum
   `style` inline dessas telas pinta texto com esses três. É o teste que teria
   pegado o `.cap-ash` e o `.btn`. `--obsidian` como texto é permitido: dá 20:1 no
   papel e 4,79:1 no vermelho — é ele o texto do `.btn-perigo` no hover. E, para a
   troca por token semântico ser segura, o teste trava os valores do escuro:
   `--fundo` e `--superficie` obsidiana, `--tinta` osso, `--tinta-fraca` cinza.
3. **Estrutura:** `lang="pt-BR"` no `index.html`; viewport sem `user-scalable=no`
   nem `maximum-scale` (zoom a 200%, 1.4.4); nenhum `outline: none` ou
   `outline: 0` sem substituto; nenhum `tabIndex` positivo; todo arquivo de
   `paginas/`, menos a casca `Layout.jsx`, com `h1` ou `Cabecalho`.
4. **Token do JSX existe no CSS.** Todo `var(--x)` escrito em `.jsx` ou `.js`
   precisa estar definido no CSS — é o teste que teria pegado a `APARENCIA` do
   Login com `--brand`, `--ink`, `--surface` e `--line`.
5. **Rede modesta:** o grafo de imports estáticos a partir do `main.jsx` não
   alcança `@clerk/*`; o `index.html` não chama o Google Fonts; o `Outlet` do
   `Layout` está dentro de um `Suspense`.

`app/src/acessibilidade.test.mjs`: os mesmos pares de contraste, lidos do
`theme.ts` como texto — o mesmo idioma do `contrato.test.mjs`, que roda sem
`npm ci`.

Fora dele: conferir rótulo de campo por regex no JSX. Frágil demais; o axe cobre
isso nas rotas públicas e o roteiro manual nas internas.

## 3. O gate no navegador

- **Dependências:** `playwright-core` e `@axe-core/playwright`, como
  devDependencies do `web/`. O `-core` não baixa navegador: usa o Chrome já
  instalado — o do runner do GitHub (`channel: 'chrome'`) e o da máquina de quem
  roda.
- **Script:** `web/a11y/auditar.mjs`. Sobe o `dist/` com o `preview()` do próprio
  Vite, visita `/`, `/login` e uma URL inexistente (o 404), roda o axe com as
  tags `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa` e `wcag22aa`, e **reprova com
  qualquer violação**, não só as críticas. A superfície é pequena e permite ser
  estrito; falso positivo vira regra desligada com comentário, nunca silêncio.
- **Evidência:** grava `a11y-relatorio.json` — versão do axe, data, URLs, regras
  aprovadas e violadas — como artefato do CI. É a evidência auditável do ODS-17,
  cujo indicador é "violações críticas = zero, origem: CI (axe-core)".
- **CI:** job novo, `acessibilidade`, no `ci.yml`: `npm ci`, build, orçamento de
  peso (seção 4) e o script. `npm run a11y` roda o mesmo localmente.
- **A chave do Clerk.** O `/login` precisa de `VITE_CLERK_PUBLISHABLE_KEY` no
  build. É pública por definição e entra como variável do repositório
  (`vars.VITE_CLERK_PUBLISHABLE_KEY`) — configuração do dono no GitHub. Sem ela,
  o `/login` é pulado e o resumo do job diz **auditoria parcial** em letras
  claras. Atenção ao medir: sem a chave o build é só a tela de "Configuração
  ausente" e o minificador descarta o app — 101 KB gzip contra os 149 KB reais.
  Depois da seção 4, a landing e o 404 deixam de depender da chave.
- **Exclusão única:** o selo "Development mode" do Clerk, que é da instância de
  teste e não existe em produção.
- **Dois testes a mais, que o axe não faz,** no mesmo script e no mesmo
  navegador: reflow — cada rota a 320px de largura sem rolagem horizontal
  (critério 1.4.10) —; e carga que falha — com o chunk do `ComClerk` bloqueado, o
  `/login` mostra a mensagem de recuperação, não uma tela branca.

## 4. Peso na rede modesta

Medido hoje, com a chave: um chunk só, 517,2 KB — 148,9 KB em gzip, 127,4 KB em
brotli — com o app inteiro. Quem abre a landing num 3G baixa Agenda, Prontuário e
Odontograma junto. Além dele, o `ClerkProvider` envolve a landing, e o Clerk
baixa `clerk-js` e `@clerk/ui` do CDN dele em toda página; e as três fontes vêm do
Google, que recebe o IP de cada visitante.

- **O Clerk sai da landing.** O `ClerkProvider` passa a envolver só `/login`,
  `/cadastro` e as telas do sistema, numa rota de layout carregada sob demanda,
  que leva junto o `aparencia` e a tela de "Configuração ausente". A landing
  para de baixar `clerk-js` e `@clerk/ui`, e para de cair quando a chave falta —
  hoje o aviso de configuração derruba a página de vendas junto. O custo: a
  navegação da landing perde o botão de usuário (`<Show when="signed-in">` do
  `Nav.jsx`). "Entrar" continua — já era link estático de propósito —, e o
  `/login` já trata sessão ativa com "Abrir o sistema".
- **Divisão por rota** com `React.lazy` e `Suspense`: cada tela do sistema vira
  chunk próprio. O `fallback` é o texto "Carregando…" com `role="status"`, na
  tipografia da casa. Há um `Suspense` dentro do `ComClerk` e outro em volta do
  `Outlet` do `Layout`: trocar de tela não desmonta o `ClerkProvider` nem faz a
  casca clara piscar para o escuro.
- **Carga que falha.** Dividir em chunks cria uma falha que o bundle único não
  tinha: o 3G cai no meio da navegação, o `import()` rejeita e o React desmonta
  tudo — tela branca. Um limite de erro (`RecuperaCarga`) em volta das rotas troca
  a tela branca por uma frase e "Tentar de novo". É o público do ODS-18 que mais
  encontra isso.
- **Fontes do próprio domínio,** pelos pacotes `@fontsource` de Antonio,
  Cormorant SC e Inter (licença OFL), importados no `main.jsx`. Saem do
  `index.html` o `<link>` do Google Fonts e os dois `preconnect`: duas
  negociações de DNS e TLS a menos no 3G, e nenhum IP de visitante indo para um
  terceiro. O nome de família fica o mesmo — `document.fonts.check('1em Antonio')`
  continua sendo o aceite. A CSP da borda do web (`2026-09-26-borda-web-cabecalhos-design.md`)
  perde `fonts.googleapis.com` e `fonts.gstatic.com`: se ela entrar antes, este
  sub-projeto os remove; se depois, ela já nasce sem.
- **Orçamento de peso,** no job de acessibilidade, depois do build: soma o gzip
  do JS e do CSS que o `index.html` carrega (entrada, `modulepreload` e folha de
  estilo — os chunks sob demanda não entram) e reprova acima do limite. O limite
  é o medido depois destas mudanças, mais 10%, escrito no script com o porquê.
  Sobe só por decisão explícita, no mesmo commit que explica o motivo.

## 5. Linguagem simples, roteiro manual e estudo de caso

**ODS-19.** O texto que o paciente lê na V2 é a ficha do pré-cadastro — o widget
do Clerk é tradução do fornecedor. Os rótulos e perguntas da ficha, hoje
espalhados no JSX de `FichaPaciente.jsx`, passam a morar numa constante
exportada, revisados em linguagem simples. O teste calcula o índice de Flesch
adaptado ao português (Martins et al., 1996:
`248,835 − 1,015 × palavras/frase − 84,6 × sílabas/palavra`) sobre a triagem de
saúde — o aviso e as oito perguntas — e reprova abaixo de 75, onde começa a faixa
"muito fácil". Medido: o texto de hoje dá 58,1 e passaria num piso de 50 com
"cardíacos", "hipertensão" e "uso contínuo"; a revisão ("pressão alta", "remédio
todo dia") dá 89,4. O método é heurístico — sílaba contada por grupo vocálico —,
e o documento de acessibilidade diz isso.

**Roteiro manual** para as telas autenticadas, a cada release: só teclado (ordem
de tabulação, `Esc` no diálogo, setas nas abas), NVDA (aviso, erro e selo
anunciados), zoom a 200% e reflow a 320px de largura, e conferência de contraste
nos estados de foco e hover. Com tabela de registro: data, versão, quem, achados.
É a evidência nas telas que o axe não alcança.

**ODS-16.** Um arquivo só, `docs/acessibilidade.md`: as decisões como estudo de
caso — vermelho como marca, hairline contra borda de controle, a raiz do
`.cap-ash`, o Clerk fora da landing, o `/login` com tokens de outro sistema —,
com os números de antes e depois; o que o gate prova e o que não prova; e o
roteiro com o registro das auditorias.

## Verificação

1. `npm test` no `web/` e no `app/` — incluindo o teste que hoje está vermelho.
2. `npm run build` e `npm run a11y` no `web/`, com a chave e sem ela.
3. No navegador, a 1280px e a 400px:
   - landing idêntica à de antes, títulos em Antonio (`document.fonts.check`);
   - `/login` com o widget preto, canto vivo, link em osso;
   - telas claras com o vermelho só em filete, e os campos com borda visível;
   - nenhuma requisição para `fonts.googleapis.com` nem, na landing, para o
     domínio do Clerk (aba de rede).
4. O orçamento de peso anotado com o número medido.

## Fora de escopo

- axe nas telas autenticadas (o caminho descartado acima);
- página pública de declaração de acessibilidade — entra depois da primeira
  auditoria manual registrada; declarar antes de medir é o que o Doc 04 proíbe;
- offline e instalável — é o ST-54 do Doc 01;
- linguagem simples de texto que ainda não existe (portal do paciente).

## O que muda depois

- **Instância de produção do Clerk:** o selo de desenvolvimento some, e a exclusão
  do axe sai junto.
- **Telas novas:** entram sozinhas na invariante 2 se ficarem em `paginas/`;
  componente novo montado por tela clara entra na lista do teste.
- **Primeira auditoria manual registrada:** destrava a declaração pública de
  acessibilidade, com o nível alcançado e as limitações conhecidas.
