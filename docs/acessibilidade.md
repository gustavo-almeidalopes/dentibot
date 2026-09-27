# Acessibilidade

A DentiBot mira a WCAG 2.2 nível AA — ODS-17, 18, 19 e 16 do Documento 04. Este
documento diz o que já é provado por máquina, o que não é, e como conferir o
resto à mão.

## O que prova o quê

| Camada | Onde | Prova | Não prova |
|---|---|---|---|
| Invariantes | `web/src/acessibilidade.test.mjs`, `web/src/legibilidade.test.mjs`, `app/src/acessibilidade.test.mjs` — em todo PR, no job `contratos` | contraste de todo token de texto e de foco nos dois temas; borda de campo a 3:1; nenhum literal da marca pintando o papel; vermelho nunca como texto no tema claro; idioma, zoom, foco, `h1`; marca de condição do odontograma a 3:1; token do JSX existe no CSS; anel do campo do Clerk declarado; landing sem Clerk; fontes do próprio domínio; triagem com Flesch ≥ 75 | nada que dependa do DOM renderizado |
| Navegador | `web/a11y/auditar.mjs` — job `acessibilidade` | axe-core (A e AA das WCAG 2.0, 2.1 e 2.2) em `/`, `/login` e no 404; reflow a 320px; carga que falha vira mensagem | as telas autenticadas; o que o axe não mede — contraste de não-texto, foco, estado —, cerca de dois terços dos problemas reais |
| Peso | `web/a11y/peso.mjs` — job `acessibilidade` | JS e CSS que a landing carrega de cara abaixo de 104 KB em gzip | tempo de carga em rede real |
| Roteiro manual | este documento, a cada release | as telas autenticadas, com teclado, NVDA, zoom e reflow | — |

Rodar localmente, com o Chrome instalado:

    cd web && npm test && npm run a11y
    cd app && npm test

No CI, o `/login` só é auditado se o repositório tiver a variável
`VITE_CLERK_PUBLISHABLE_KEY` (Settings → Secrets and variables → Actions →
**Variables**). É a chave publicável — pública por definição. Sem ela, o resumo
do job diz **AUDITORIA PARCIAL**, e a landing e o 404 são auditados do mesmo
jeito: eles não dependem mais da chave.

## As decisões, como estudo de caso

**O defeito era sempre o mesmo.** Um literal da marca pensado para a obsidiana,
usado sobre o papel do tema claro. O cinza `#838383` dá 5,54:1 no preto e 3,6:1
no papel; o osso some no papel; o vermelho `#ed1c24` dá 4,79:1 no preto e 4,16:1
no papel. A correção nunca criou cor: trocou o literal pelo token semântico
(`--tinta`, `--tinta-fraca`, `--fundo`), que no escuro vale o mesmo literal — a
landing não mudou um pixel, e o teste trava esses valores — e no claro vale o
que passa.

- `.cap-ash` e `.body-ash`: 67 usos em 9 arquivos, corrigidos numa linha cada.
- `.btn`, `.btn-fill` e `.skip-link`: borda e fundo em osso sumiam no papel; o
  botão do sistema parecia texto solto. Agora é a caixa preta de canto vivo
  (`.btn-sm`, a ação de linha, com 1px).

**Vermelho é marca, não tinta.** No tema claro o vermelho é filete, borda ou
fundo; o texto de erro, de alarme e de perigo é tinta (17,95:1). No hover do
botão de perigo o texto é obsidiana, e não tinta: `#111` sobre o vermelho dá
4,31:1; `#000`, 4,79:1.

**Hairline decora; controle se enxerga.** O critério 1.4.11 pede 3:1 para o que
identifica um controle ou um estado. A borda de campo (1,69:1 no papel, 2,10:1
no preto), a do app móvel e as marcas de condição do odontograma passaram para
`--tinta-fraca`/`#838383`. As hairlines de tabela ficaram finas: são separação.

**O Clerk desenha o campo do jeito dele.** A aparência do widget no `/login`
pedia `--brand`, `--ink`, `--surface` e `--line`, de outro design system, que
aqui não existem: o fallback deixava o link em teal a 3,84:1 e o campo com canto
de 10px. Agora ela cuida só de encaixe, e o teste de tokens do JSX impede a
volta. Medido no navegador, o Clerk também não usa o `colorBorder` puro: ele
pinta o campo com um box-shadow dessa cor a 11% — cerca de 1,1:1, qualquer que
seja a cor. O anel é declarado em `elements.formFieldInput` (1px `#838383`, 2px
osso no foco), e com `!important`, porque a regra de variante do Clerk
(`.cl-internal-…[data-variant]`) tem mais especificidade que o `elements`.

**Rede modesta.** A landing carregava 154,9 KB de JS e CSS em gzip (148,9 de
JS, com o app inteiro), mais o clerk-js e o @clerk/ui do CDN do Clerk, mais o
Google Fonts. Agora carrega 93,9 KB, e nenhuma requisição externa: o Clerk só
entra nas rotas de sessão, cada tela é um chunk, e as fontes vêm do próprio
domínio — o que também tira as origens do Google da CSP da borda do web. As
fontes são estáticas, uma por peso, e só baixa o peso que a página usa; por
isso a conferência é por peso: `document.fonts.check('700 1em Antonio')`.
Dividir em chunks criou uma falha nova, a do `import()` que rejeita quando o 3G
cai; o `RecuperaCarga` troca a tela branca por uma frase e "Tentar de novo".

**Linguagem simples.** A triagem de saúde da ficha, que o paciente lê sozinho no
pré-cadastro, dava 58,1 no Flesch adaptado ao português — "cardíacos",
"hipertensão", "uso contínuo". Reescrita ("pressão alta", "remédio todo dia"),
dá 89,4. A contagem de sílabas é heurística e serve para comparar versões.

### Achados do axe

A primeira auditoria (27/09/2026, axe-core 4.13) deu zero violação na landing e
no 404, e uma no `/login`: o "Secured by" do rodapé do widget, cinza sobre o
laranja `#f36b16` que o Clerk pinta ali **só na instância de desenvolvimento**
(1,25:1). O fundo laranja é parte do selo "Development mode", que é a única
exclusão do axe; a exclusão cobre o rodapé do widget quando o selo está
presente. Em produção não há selo, o rodapé é preto (5,54:1) e volta a ser
auditado.

## Roteiro manual — a cada release

Com a API e o web rodando (`iniciar.bat`), entrar com uma conta de teste de cada
papel que alcance a tela. Em cada tela — Agenda, Pacientes, Prontuário,
Financeiro, Equipe, Auditoria, Cadastro — e no `/login`:

1. **Só teclado.** Chegar a todas as ações com Tab e Shift+Tab, na ordem da
   leitura; o anel de foco sempre visível — inclusive nos campos do Clerk, cujo
   anel de 2px osso só dá para ver com a janela em foco; `Esc` fecha o diálogo de
   confirmação e devolve o foco a quem o abriu; setas trocam as abas do
   Prontuário.
2. **NVDA** (Windows, gratuito). O título da tela é anunciado; o aviso de sucesso
   é lido sem interromper; o erro interrompe; o selo de status é lido com o texto
   dele; cada campo diz o próprio rótulo.
3. **Zoom a 200%** (Ctrl +). Nada cortado nem sobreposto.
4. **Reflow a 320px** (DevTools, largura 320). Sem rolagem horizontal no corpo; a
   tabela vira blocos empilhados.
5. **Contraste de estado.** Hover e foco de botão, campo com erro, selo de
   alarme, dente do odontograma e os campos do perfil do usuário (menu do
   `UserButton`) legíveis — o axe não mede estado.

### Registro

| Data | Versão (commit) | Quem | Telas | Achados |
|---|---|---|---|---|
| | | | | |

## Declaração pública

Uma página pública de declaração de acessibilidade entra depois da primeira
linha deste registro: declarar conformidade antes de medir é o que o Documento
04 proíbe.
