# UX e UI das telas do sistema

Como as sete telas autenticadas do `web/` deixam de ser a landing page reusada
e passam a ser uma ferramenta de trabalho.

O `web/` nasceu como landing page brutalista — fundo obsidiana, tipografia de
manifesto, uma parede vermelha. As telas do sistema vieram depois e herdaram
esses primitivos porque estavam à mão, não porque servem. O resultado é
mensurável, não uma questão de gosto: `.row` é um grid de duas colunas com
`padding-block: 42px`, e Agenda, Pacientes, Financeiro e Equipe passam três
filhos para ele — a coluna de ações cai numa linha implícita, hoje, em
produção.

Este documento é o desenho da correção. Ele não inventa fluxo nem consome
endpoint que a tela ainda não chama, com duas exceções nomeadas ao final.

**O que "as sete telas" quer dizer, para não haver dúvida depois:** Agenda,
Pacientes, Prontuário, Financeiro, Equipe e Auditoria, mais a casca
(`Layout.jsx`), que as seis compartilham. `Cadastro.jsx` não é redesenhada —
herda tema e primitivos porque monta `.app-main`, e é só isso.
`NaoEncontrada.jsx` continua escura, porque pertence à landing.

## O que está quebrado

Levantado lendo `web/src` inteiro contra os controllers de `api-java`.

**Defeitos**

1. `--ink` e `--hair-dim` nunca são definidos em lugar nenhum. Há 9 usos de
   `var(--ink…)`. Onde há fallback (`var(--ink, #fff)`) passa despercebido;
   onde não há, quebra — `.cartao-cvv { background: var(--ink) }` resolve para
   transparente, e o CVV branco-sobre-preto do cartão some.
2. `.row` recebe três filhos onde o grid define duas colunas (Agenda,
   Pacientes, Financeiro, Equipe).
3. O `detalhe` do `Cabecalho` imprime o endpoint enquanto carrega:
   `GET /api/v1/consultas`, `GET /api/v1/pacientes`, `GET /api/v1/equipe`,
   `GET /api/v1/auditoria`, `GET /api/v1/financeiro/recebiveis`. Visível para
   o cliente pagante.
4. Pacientes pede `limite=200`, que é exatamente o `LIMITE_MAXIMO` do
   `PacienteController`, e escreve "200 no cadastro". Numa clínica com 900
   pacientes a frase é falsa.
5. Financeiro rotula os cinco indicadores com "Do primeiro dia do mês até
   hoje", mas só `recebidoNoPeriodo` é do período: `aReceber`, `vencido` e
   `aPagar` são saldo corrente. Erro de informação numa tela de dinheiro.
6. O Prontuário declara `role="tablist"` e `role="tab"` sem `tabpanel`,
   `aria-controls` ou navegação por seta. ARIA pela metade mente para o leitor
   de tela sobre o que aquilo é.

**Ausências**

7. Nenhuma ação bem-sucedida avisa que deu certo — `useAcao` expõe só `erro`.
8. "Faltou" e "Concluir" são transições irreversíveis (o back-end responde 409
   na volta) disparadas por um clique, ao lado dos botões não destrutivos.
9. Status — o campo mais escaneado de toda lista — é texto cinza.
10. "Carregando…" é um parágrafo solto; a página salta quando os dados chegam.
11. O odontograma codifica sete condições em borda e não tem legenda.
12. O Prontuário chama o paciente de "Paciente 7" e o autor de "Dentista 3".

## Decisões

Tomadas com o dono do produto antes deste documento.

| Decisão | Escolha |
|---|---|
| Escopo | UI/UX das sete telas. Sem fluxo novo, sem endpoint novo — salvo as duas exceções da última seção. |
| Cor | App claro por padrão; landing e 404 continuam pretos. |
| Dispositivo | Mesa é o caso principal; celular precisa funcionar, não precisa ser desenhado duas vezes. |
| Linguagem visual | "Papel clínico": a marca virada do avesso — mesmas hairlines, mesmo canto vivo, vermelho só para alarme. Não SaaS genérico. |

A quarta decisão tem uma razão de custo, não só de identidade: canto vivo e
hairline são mais baratos que raio e sombra — menos CSS, menos estado de
hover, menos superfície para errar em dois temas — e já estão escritos nesta
base. O que cansa na tela preta não é a falta de raio; é o `padding-block` de
42px de um primitivo de manifesto usado como linha de tabela.

## Fundação

### Duas camadas de token

O `:root` de hoje confunde literal de marca com papel semântico: `--bone` é ao
mesmo tempo "o branco da DentiBot" e "a cor do texto". Enquanto for a mesma
variável, claro e escuro não coexistem.

Os literais ficam como estão — `--obsidian`, `--bone`, `--ash`, `--alarm`. A
marca não muda. Acima deles entram os semânticos, e todo componente passa a
usar só estes:

| Token | Papel |
|---|---|
| `--fundo` | fundo da página |
| `--superficie` | bloco sobre o fundo (formulário, cartão de indicador) |
| `--tinta` | texto principal |
| `--tinta-fraca` | texto secundário |
| `--hair` | hairline de estrutura |
| `--hair-fraca` | hairline interna de tabela |
| `--foco` | anel de foco |

`--ink` vira `--tinta` e `--hair-dim` vira `--hair-fraca` nos 9 pontos em que
aparecem. Os dois defeitos somem por construção, não por remendo.

### A troca de tema não usa JavaScript

As telas do sistema são as únicas que montam `.app-main`:

```css
:root:has(.app-main) {
  color-scheme: light;
  --fundo:       #faf9f7;   /* papel, não branco de tela */
  --superficie:  #ffffff;
  --tinta:       #111111;
  --tinta-fraca: #5c5c5c;
  --hair:        rgba(0, 0, 0, .22);
  --hair-fraca:  rgba(0, 0, 0, .10);
  --foco:        #111111;
}
```

`--tinta-fraca` é `#5c5c5c` e **não** o `--ash` da marca. Medido: `#838383`
sobre `#faf9f7` dá 3,66:1, abaixo do mínimo AA de 4,5:1 para texto normal —
o cinza que funciona sobre obsidiana não funciona sobre papel. `#5c5c5c` dá
6,22:1. Reusar `--ash` no tema claro é o erro fácil aqui, e ele passa
despercebido porque *parece* certo.

Sem `useEffect`, sem limpeza no desmonte, sem piscada na troca de rota — o
seletor é vivo. Landing e 404 continuam pretos porque não montam `.app-main`.
`/cadastro` clareia junto, e isso é correto: já é tela de sistema.

A alternativa era um `useEffect` escrevendo `dataset` no `documentElement`, e
ela traz de brinde o bug de limpeza que o `:has` não tem.

### Dois arquivos

`style.css` tem 999 linhas e mistura o manifesto com a ferramenta. Fica
`style.css` com tokens e landing; nasce `app.css` com o sistema — as ~270
linhas atuais mais o que este desenho acrescenta, importado pelo `main.jsx`.
A parte do app é a que vai crescer, e é nela que um valor chumbado passa
despercebido no meio do CSS da parede vermelha.

## Primitivos

`.row` sai das telas do sistema. No lugar:

**`.tabela`** — o primitivo que a Auditoria já usa, promovido: cabeçalho fixo
na rolagem, hairline por linha, `tabular-nums` nas colunas numéricas, célula
de ação à direita. Abaixo de 620px cada linha vira bloco empilhado com rótulo
vindo de `data-rotulo`, em vez de rolagem horizontal. Serve Pacientes,
Financeiro, Equipe e Auditoria.

**`.dia`** — a Agenda não é tabela, é eixo do tempo: calha de hora à esquerda,
alinhada, e o bloco da consulta à direita. A lista é ordenada por `inicioEm`
na tela, não confiada à ordem da resposta. Consultas que se sobrepõem no
horário ficam lado a lado no mesmo degrau da calha, e não empilhadas — duas
cadeiras atendendo às 14h é o caso normal de uma clínica, não uma anomalia.

**`.selo`** — status com forma, nunca só cor: contorno para *agendada*,
preenchido para *confirmada*, cinza para *realizada*, riscado para
*cancelada*, alarme para *faltou* e *vencido*. Um mapa, quatro telas.

**Esqueleto** — `Estado` ganha a prop `esqueleto` (quantas linhas, quantas
colunas) e desenha essa forma enquanto carrega, no lugar de "Carregando…".
Quem não passar a prop continua vendo o texto, então nenhuma tela quebra na
troca.

**`Aviso` com `role="status"`** — `useAcao` passa a guardar `sucesso` além de
`erro`; o aviso aparece no topo da tela e some sozinho. Sem biblioteca de
toast: são cerca de 20 linhas.

**`Confirmar` sobre o `<dialog>` nativo** — foco preso, `Esc` e fundo de
graça, zero dependência. Usado em "Faltou", "Concluir" e ao mudar membro para
*desativado*.

**Botões** — o wipe do `.btn` (o `::before` com `scaleX`) fica na landing. No
app vira transição instantânea, mantendo o canto vivo: numa barra de navegação
e numa coluna de ações, oito varreduras simultâneas é ruído. Entram `.btn-sm`
para ação de linha e `.btn-perigo` para transição irreversível.

**Navegação** — deixa de ser fileira de blocos preenchidos e vira barra de
abas com linha de base: o ativo é sublinhado de 2px em alarme mais peso. O
`aria-current` do `NavLink` continua fazendo o trabalho de acessibilidade.

**Escala** — corpo a 15px com entrelinha 1.45. O título de tela cai de ~51px
(`display-sm`, `clamp(2rem, 6vw, 3.2rem)`) para ~30px, mantendo o Antonio. O
ponto final dos títulos fica: é o único lugar onde a voz da marca sobrevive
dentro da ferramenta, e não custa nada.

## Tela por tela

**Agenda** — vira eixo do tempo. A consulta mostra `14:00–14:45`: o
`terminoEm` já vem no `ConsultaResumo` e hoje é descartado. O
`telefonePaciente` também vem e é descartado — entra como link `tel:`, que é o
dado que a recepção mais usa no dia. O `detalhe` passa a dizer a data por
extenso. Atalhos *Ontem / Hoje / Amanhã* ao lado do seletor, porque hoje
trocar de dia exige abrir o date picker. Status vira selo. "Faltou" e
"Concluir" ganham confirmação, e o destrutivo sai de perto dos outros. Quando
o filtro de dentista está ativo, o estado vazio diz isso — hoje diz "nenhuma
consulta" e a pessoa conclui que o dia está livre.

**Pacientes** — tabela: Nome | Telefone | Status | ação, telefone clicável.
Quando voltarem exatamente 200 registros, a tela escreve "200 primeiros", não
"200 no cadastro". Busca que filtra os já carregados, rotulada como tal — o
rótulo honesto evita prometer o que a API ainda não faz. Ao abrir "Novo
paciente", foco no primeiro campo e rolagem até ele. Estado vazio com o botão
de criar dentro.

**Prontuário** — o nome do paciente já está carregado na tela de Pacientes:
vai por `state` do `<Link>`, com queda para o id quando alguém entra pela URL
direta. Zero requisição nova. As abas ganham o padrão ARIA completo
(`tabpanel`, `aria-controls`, setas). O odontograma ganha legenda fixa, e em
tela de mesa o formulário do dente abre ao lado, não abaixo. A evolução
retificada troca `opacity: .62` por selo e marca de margem: opacidade em
registro clínico é perda de informação, e o texto precisa continuar legível. O
aviso de que abrir a tela grava auditoria sai da letra miúda e vira faixa
persistente — é obrigação de transparência, não decoração.

**Financeiro** — os cinco indicadores saem de `<li class="row">` e viram
painel, cada um declarando o próprio recorte: `recebidoNoPeriodo` é do
período; `aReceber`, `vencido` e `aPagar` são saldo corrente. Recebíveis vira
tabela ordenada por vencimento, com o vencimento na coluna dele — hoje está
dentro de `.acoes`, à direita, onde o olho procura botão. A tela passa a
declarar que é só leitura: hoje a pessoa procura "lançar recebimento", não
acha, e conclui que o sistema está quebrado.

**Equipe** — tabela: Nome/E-mail | CRO | Vínculo | Papel | Status | ação.
"Aguardando primeira entrada" sai do vermelho, que é alarme: é o estado normal
de quem foi admitido ontem. Ao lado de "Salvar" entra "Descartar", porque hoje
só recarregando. Mudar alguém para *desativado* pede confirmação. O aviso de
plano lotado gruda no botão desabilitado com `aria-describedby`, em vez de
ficar num parágrafo que não explica por que o botão não clica.

**Auditoria** — herda os primitivos. Atalhos de 7/30/90 dias. O campo
"Recurso" é texto livre com placeholder `prontuario.evolucao`, e quem não
conhece a nomenclatura não consegue filtrar: vira `<datalist>` alimentado
pelos valores da resposta atual. A tela passa a dizer que mostra no máximo 200
eventos — hoje trunca calada. O selo vermelho de staff da plataforma fica: é
exatamente o que deve saltar aos olhos.

**Casca (`Layout.jsx`)** — `SemAcesso` vira estado vazio de verdade. O
`correlacaoId` ganha botão de copiar, que é o que o suporte pede por telefone.
Ao lado do `UserButton`, o papel do usuário, que vem do `/eu` de graça. Nome
da clínica não: o `/eu` devolve apenas `papel`, `staffPapel` e `permissoes`, e
mostrá-lo exigiria `GET /clinica`.

## Acessibilidade

Não é seção separada do desenho, é condição dele:

- Status por forma além de cor (selo, borda, risco) — o odontograma já faz
  isso para as condições e é o padrão a seguir.
- Contraste mínimo AA no tema claro, incluindo `--tinta-fraca` sobre
  `--superficie`, que é onde este tipo de paleta costuma falhar.
- `<dialog>` nativo em vez de div com `role="dialog"`: foco preso e `Esc` sem
  código próprio.
- Padrão ARIA de abas completo ou nenhum.
- `role="status"` no aviso de sucesso e `aria-live="assertive"` no erro — o
  `Estado` já acerta isso e serve de modelo.
- Anel de foco visível em tudo que recebe foco, com `--foco` no tema certo.

## Verificação

O que prova que isto funcionou, na ordem em que se descobre:

1. `npm run build` no `web/` passa.
2. `node --test` continua verde (`api.test.mjs`, `cartao.test.mjs`,
   `documento.test.mjs`, `rotas.test.mjs`).
3. Nenhum `var(--ink` e nenhum `var(--hair-dim` restam no CSS — `grep`.
4. Nenhuma string `GET /api/v1/` restante em `web/src/paginas` — `grep`.
5. As sete telas abertas a 1280px e a 400px sem rolagem horizontal no corpo.
6. Landing e 404 continuam pretos; app e `/cadastro`, claros.

Os itens 3 e 4 são `grep` porque são o tipo de defeito que volta sem ninguém
notar.

## Fora de escopo

Registrado para não parecer esquecimento. Nada aqui foi julgado desnecessário
— foi adiado.

- **Agendar consulta pelo web.** `POST /api/v1/consultas` existe e não tem
  botão. Não dá para marcar uma consulta pelo navegador.
- **Cancelar consulta.** `POST /consultas/{id}/cancelar` existe, com motivo.
- **Escritas do financeiro.** `/lancamentos`, `/despesas`,
  `/despesas/{id}/pagar`, `/comissoes/{id}/liberar`, `/formas`,
  `/regras-comissao` — nenhuma tela.
- **Busca e paginação de pacientes no servidor.** O `PacienteController` tem
  keyset (`apos`), a tela não usa.
- **Módulos inteiros sem tela:** Orçamentos, Estoque, LGPD (termos,
  consentimentos, solicitações do titular), Billing (assinatura, faturas,
  uso), Procedimentos da clínica.
- **Modo escuro no app**, com chave e preferência por usuário.

## As duas exceções

Ambas consomem endpoint que outra tela já chama, custam uma linha, e sem elas
a tela fica visivelmente amadora:

1. O Prontuário chama `GET /equipe` para trocar "Dentista 3" pelo nome de quem
   assinou a evolução. A Auditoria já faz exatamente isso, pelo mesmo motivo:
   o evento traz id, não nome, porque `auditoria` não pode depender de
   `identidade`.
2. Recebíveis chama `GET /pacientes` para trocar `idPaciente` pelo nome.

Nenhuma terceira.
