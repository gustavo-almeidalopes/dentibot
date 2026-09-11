# -*- coding: utf-8 -*-
"""Documento 02 — Melhorias de UX/UI Design do DentiBot."""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dentibot_doc import AMARELO, AZUL, LARANJA, VERDE, VERMELHO, Documento, Melhoria as M  # noqa: E402

def _luminancia(hexadecimal):
    """Luminancia relativa de uma cor sRGB, conforme a WCAG 2.x."""
    h = hexadecimal.lstrip("#")
    canais = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    linear = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in canais]
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]


def contraste(cor_a, cor_b):
    """Razao de contraste entre duas cores, formatada em pt-BR (ex.: '4,5:1')."""
    la, lb = _luminancia(cor_a), _luminancia(cor_b)
    razao = (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    return f"{razao:.1f}".replace(".", ",") + ":1"


SAIDA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                     "02-Melhorias-de-UX-UI-Design.pdf")

doc = Documento(
    caminho=SAIDA,
    numero="02",
    titulo="Melhorias de UX/UI Design",
    subtitulo="Design system, acessibilidade e fluxos clínicos — 64 melhorias para uma "
              "interface que o dentista aprende em um dia e usa por dez anos.",
    resumo="",
    cor=LARANJA,
    rotulo="Design de Produto e Experiência",
    col_extra="Como medir o ganho",
)

doc.capa()

doc.resumo_executivo([
    "O diferencial declarado do DentiBot não é ter mais funcionalidades que o concorrente — é "
    "ser <b>simples e barato o bastante</b> para uma clínica de pequeno porte adotar sem "
    "treinamento formal. O próprio diagnóstico do projeto aponta que \"clínicas pequenas não "
    "têm recursos para treinamento em sistemas complexos\" e que \"a falta de padronização no "
    "uso de softwares clínicos gera dificuldades na adaptação dos profissionais\". Isso torna "
    "o design de interação a principal frente competitiva do produto, não um acabamento.",

    "A base visual existente é sólida e incomum em projeto acadêmico: <code>style.css</code> "
    "define uma paleta Pantone documentada em <i>custom properties</i>, a landing page traz "
    "<i>skip link</i>, <code>aria-label</code>, <code>aria-expanded</code> e "
    "<code>aria-controls</code> corretos, e existem páginas dedicadas para mais de quarenta "
    "códigos de erro HTTP. O problema não é ausência de cuidado — é ausência de <b>sistema</b>: "
    "os tokens moram apenas no CSS da landing page, há somente dois arquivos de estilo "
    "(<code>login.css</code> e <code>recepcionista.css</code>) para todas as telas internas, e "
    "as 40+ páginas HTML repetem cabeçalho, navegação e rodapé, o que garante que a interface "
    "vai divergir com o tempo.",

    "Há também um risco de acessibilidade objetivo e mensurável: o verde da marca "
    "(<code>#00DF76</code>) sobre o branco do fundo (<code>#FAFAF5</code>) rende razão de "
    "contraste de aproximadamente <b>1,7:1</b>, muito abaixo do mínimo de 4,5:1 exigido pela "
    "WCAG 2.2 nível AA para texto. O amarelo <code>#FFC72C</code> é ainda mais crítico, "
    "em torno de 1,5:1. As duas "
    "cores funcionam bem como <i>fundo</i> com texto escuro (o verde sobre o preto da marca "
    "atinge cerca de 7,9:1), e a correção é de regra de uso, não de troca de identidade.",

    "As 64 melhorias a seguir cobrem doze frentes, do sistema de design à governança de "
    "pesquisa, sempre ancoradas nas telas que já existem em <code>src/pages/</code>. A régua "
    "adotada é a WCAG 2.2 nível AA, referência do eMAG e do Decreto 9.094/2017 para serviços "
    "digitais no Brasil, e um requisito prático de mercado: parte relevante dos pacientes de "
    "uma clínica é idosa e usa a área do paciente no próprio celular.",
], destaques=[
    ("64", "melhorias de design"),
    ("1,7:1", "contraste verde/branco hoje"),
    ("4,5:1", "mínimo WCAG 2.2 AA"),
    ("40+", "páginas HTML duplicadas"),
    ("2", "arquivos CSS internos"),
])

doc.sumario(extras=[
    "Anexo A — Régua de contraste da paleta Pantone",
    "Anexo B — Inventário de telas e prioridade de redesenho",
    "Anexo C — Roadmap de design em três ondas",
])

# ─── 01 ──────────────────────────────────────────────────────────────────────
doc.secao("Diagnóstico da experiência atual",
          "O que o inventário das telas revela")

doc.texto(
    "O levantamento cobriu a landing page (<code>index.html</code>, <code>style.css</code>, "
    "<code>script.js</code>), as onze telas de aplicação em <code>src/pages/</code>, os dois "
    "arquivos de estilo em <code>src/css/</code>, o cliente de API em <code>src/js/api.js</code>, "
    "as quarenta e uma páginas de erro HTTP e as telas móveis em <code>mobile/</code>. "
    "As observações abaixo são verificáveis no repositório.")

doc.tabela(
    ["Área", "Observação", "Efeito sobre o usuário"],
    [
        ["Sistema visual",
         "Os tokens de cor, tipografia e espaçamento existem só em :root de style.css, usado "
         "pela landing page. As telas internas têm apenas login.css e recepcionista.css.",
         "Cada tela nova reinventa botão, campo e espaçamento; a interface envelhece "
         "inconsistente e o usuário reaprende a cada seção."],
        ["Contraste",
         "Verde #00DF76 e amarelo #FFC72C sobre fundo claro ficam por volta de 1,7:1 e 1,8:1.",
         "Texto, ícone e estado de foco nessas cores são ilegíveis para parte dos usuários, "
         "sobretudo com baixa visão ou tela sob luz de consultório."],
        ["Duplicação de marcação",
         "Mais de quarenta arquivos HTML repetem cabeçalho, navegação e rodapé; "
         "src/partials/*.php nunca é processado pelo Nginx estático.",
         "Correção de usabilidade precisa ser replicada dezenas de vezes e alguma tela sempre "
         "fica para trás."],
        ["Nomenclatura",
         "A tela da recepção está publicada como src/pages/recpecionista.html, com erro de "
         "digitação no nome do arquivo.",
         "URL exposta ao usuário com erro de português, link quebrado quando alguém corrige o "
         "nome e prejuízo de credibilidade."],
        ["Páginas de erro",
         "Existem 41 páginas para códigos de 400 a 511, incluindo casos raríssimos em produto "
         "clínico (418, 426, 451, 510).",
         "Esforço concentrado em cenários improváveis enquanto os erros que realmente ocorrem "
         "— sessão expirada, offline, conflito de horário — não têm tratamento na interface."],
        ["Estados da interface",
         "Não há esqueleto de carregamento, estado vazio, confirmação otimista ou tratamento "
         "de falha de rede em src/js/api.js.",
         "Diante de lentidão a tela fica parada e o usuário clica de novo, gerando duplicidade "
         "de agendamento ou cobrança."],
        ["Fluxo de acesso",
         "Login, verificação de 2FA e seleção de perfil (selectperfil.html) são três telas "
         "sequenciais em toda entrada no sistema.",
         "Atrito diário alto para quem entra várias vezes ao dia entre um paciente e outro."],
        ["Tipografia",
         "Space Grotesk e Inter carregadas do Google Fonts em tempo de execução.",
         "Dependência externa que atrasa a primeira renderização em conexão ruim e implica "
         "transferência de IP do usuário a terceiro — ponto a documentar na LGPD."],
        ["Tema",
         "Só existe tema claro. Consultório costuma operar com luz reduzida durante "
         "procedimento.",
         "Ofuscamento e fadiga visual no uso prolongado ao lado da cadeira."],
        ["Móvel",
         "mobile/android/index.kt e mobile/ios/index.swift reproduzem a landing page em "
         "Compose e SwiftUI, sem telas de produto.",
         "Não existe experiência móvel real, embora o público-alvo trabalhe em pé e circulando "
         "pela clínica."],
    ],
    larguras=[26 * 2.83, 74 * 2.83, 73 * 2.83],
)

doc.destaque(
    "Princípio orientador destas 64 melhorias",
    "<b>Uma tarefa clínica não pode competir com a interface pela atenção do profissional.</b> "
    "O dentista está de luva, com o paciente na cadeira e o tempo contado. Toda decisão de "
    "design deste documento é avaliada por três perguntas: (1) reduz o número de cliques ou de "
    "decisões em uma tarefa recorrente? (2) permite recuperação sem perda de dado quando o "
    "usuário erra ou a rede cai? (3) funciona igual para quem tem baixa visão, usa apenas "
    "teclado ou está em um celular de entrada em rede 4G? Uma melhoria que não responde "
    "\"sim\" a pelo menos uma delas não entrou na lista.",
    cor=LARANJA)

# ─── 02 ──────────────────────────────────────────────────────────────────────
doc.secao("Design system e fundamentos visuais",
          "Transformar a paleta Pantone em um sistema que escala")
doc.legenda_backlog()

doc.melhorias([
    M("UX-01", "Tokens de design como fonte única de verdade",
      "Promover as <i>custom properties</i> de <code>:root</code> a um pacote de tokens "
      "(cor, tipografia, espaçamento, raio, sombra, duração) publicado em JSON e compilado para "
      "CSS, Kotlin e Swift. Hoje a paleta existe só na landing page e as telas internas a "
      "reescrevem à mão.",
      "style.css, src/css/, mobile/", "Alto", "M", "100% das telas consumindo o mesmo token"),
    M("UX-02", "Escala tipográfica e de espaçamento definidas",
      "Adotar uma escala modular explícita (por exemplo 12/14/16/20/24/32/48) e espaçamento em "
      "múltiplos de 4 px. Sem escala, cada tela escolhe um tamanho e o resultado é ruído "
      "visual que o usuário percebe como \"desorganizado\" sem saber apontar por quê.",
      "style.css, src/css/", "Alto", "P", "Redução de ~70% nos valores tipográficos distintos"),
    M("UX-03", "Biblioteca de componentes documentada",
      "Construir botão, campo, seletor, tabela, modal, aviso, abas, paginação e navegação como "
      "componentes com estados (padrão, foco, ativo, carregando, desabilitado, erro), "
      "documentados em Storybook ou página viva de referência.",
      "novo design-system/", "Alto", "G", "Nova tela montada sem CSS próprio"),
    M("UX-04", "Regras de uso das cores da marca",
      "Verde e amarelo têm contraste insuficiente como texto sobre fundo claro. Definir na "
      "documentação que ambos são cores de <i>preenchimento</i> com texto escuro por cima, "
      "criar variantes escuras para uso como texto e link, e travar a regra em teste "
      "automatizado de contraste.",
      "style.css, design-system", "Alto", "P", "Zero par cor/fundo abaixo de 4,5:1"),
    M("UX-05", "Tema escuro nativo",
      "Definir o par de temas em tokens semânticos (superfície, conteúdo, borda, ênfase), "
      "respeitar <code>prefers-color-scheme</code> e permitir troca manual persistida. "
      "Atendimento com luz reduzida é a norma, não a exceção, em consultório.",
      "style.css, todas as telas", "Médio", "M", "Adoção do tema escuro entre profissionais"),
    M("UX-06", "Auto-hospedar as fontes",
      "Servir Inter e Space Grotesk do próprio domínio, com subconjunto latino e "
      "<code>font-display: swap</code>. Elimina requisição a terceiro, remove a transferência "
      "de IP do usuário para fora e corta um bloqueio de renderização no 4G.",
      "index.html:8-9, frontend/", "Médio", "P", "Redução de 200 a 400 ms no LCP"),
    M("UX-07", "Sistema de ícones consistente",
      "Adotar um conjunto único de ícones em SVG, entregue como <i>sprite</i>, com significado "
      "documentado e rótulo textual obrigatório. Ícone sozinho, sem texto, é a principal fonte "
      "de erro de interpretação em software clínico.",
      "src/, design-system", "Médio", "M", "Zero ação crítica identificada só por ícone"),
    M("UX-08", "Densidade ajustável",
      "Oferecer modo compacto e confortável. A recepcionista quer ver trinta linhas de agenda "
      "de uma vez; o dentista de luva, olhando de longe, precisa de alvos maiores. A mesma "
      "densidade não serve aos dois.",
      "design-system, agenda", "Médio", "M", "Preferência adotada por >30% dos usuários"),
], larg_alvo=34 * 2.83, larg_extra=32 * 2.83)

# ─── 03 ──────────────────────────────────────────────────────────────────────
doc.secao("Acessibilidade digital",
          "WCAG 2.2 nível AA como piso, não como meta")

doc.texto(
    "Software de saúde é usado por pessoas com todo tipo de condição: pacientes idosos "
    "consultando a área do cliente, profissionais com daltonismo lendo um alerta de alergia, "
    "recepcionistas com lesão por esforço repetitivo que preferem teclado ao mouse. "
    "Acessibilidade aqui não é conformidade formal — é a diferença entre um alerta clínico ser "
    "visto ou não.")

doc.melhorias([
    M("UX-09", "Auditoria WCAG 2.2 AA de todas as telas",
      "Executar avaliação automatizada (axe-core no CI) somada a revisão manual, gerando um "
      "backlog com critério de sucesso e severidade por tela. Automação sozinha encontra por "
      "volta de um terço dos problemas reais.",
      "src/pages/, index.html", "Alto", "M", "Zero violação crítica no axe-core"),
    M("UX-10", "Contraste conforme em texto, ícone e estado",
      "Corrigir os pares reprovados (verde e amarelo sobre fundo claro), garantir 4,5:1 em "
      "texto normal, 3:1 em texto grande e 3:1 em componentes de interface e estados de foco.",
      "style.css, src/css/", "Alto", "M", "100% de conformidade no verificador"),
    M("UX-11", "Foco visível e navegação por teclado completa",
      "Assegurar indicador de foco de alto contraste em todo elemento interativo (critério "
      "2.4.11 da WCAG 2.2), ordem de tabulação previsível e nenhuma armadilha de foco em modal. "
      "Recepção trabalha muito mais rápido no teclado que no mouse.",
      "todas as telas", "Alto", "M", "Agendamento concluído só com teclado"),
    M("UX-12", "Semântica e regiões de marco",
      "Aplicar <code>header</code>, <code>nav</code>, <code>main</code>, <code>aside</code> e "
      "<code>footer</code> em todas as telas internas, com um <code>h1</code> único e hierarquia "
      "sem salto. A landing page já faz isso corretamente; as telas de aplicação, não.",
      "src/pages/*.html", "Alto", "P", "Estrutura navegável por leitor de tela"),
    M("UX-13", "Estender o skip link a todas as telas",
      "<code>index.html</code> tem \"Ir para o conteúdo\"; as telas internas não. Replicar e "
      "acrescentar atalhos para agenda do dia e busca de paciente.",
      "src/pages/*.html", "Médio", "P", "Skip link em 100% das telas"),
    M("UX-14", "Formulários acessíveis por padrão",
      "Todo campo com <code>label</code> associado, instrução antes do campo, erro descrito em "
      "texto e vinculado por <code>aria-describedby</code>, campo inválido marcado com "
      "<code>aria-invalid</code>, e nunca cor como único indicador de erro.",
      "login.html, cadastros", "Alto", "M", "Erro compreensível por leitor de tela"),
    M("UX-15", "Regiões dinâmicas anunciadas",
      "Confirmações, contadores e mensagens que aparecem sem recarga precisam de "
      "<code>aria-live</code> adequado. Sem isso, quem usa leitor de tela não sabe que o "
      "agendamento foi salvo.",
      "src/js/", "Médio", "P", "100% das notificações anunciadas"),
    M("UX-16", "Alvos de toque e tolerância a erro",
      "Garantir área mínima de 44 × 44 px em alvo tocável (critério 2.5.8), espaçamento entre "
      "ações destrutivas e adjacentes, e nunca colocar \"cancelar consulta\" ao lado de "
      "\"confirmar\".",
      "src/pages/, mobile/", "Alto", "P", "Redução de toques acidentais em ações críticas"),
    M("UX-17", "Respeitar preferência de movimento reduzido",
      "<code>script.js</code> aplica revelação por rolagem via <code>data-reveal</code>. "
      "Envolver todas as transições em <code>prefers-reduced-motion</code> — animação pode "
      "provocar náusea e desorientação em usuários sensíveis.",
      "script.js, style.css", "Baixo", "P", "Animação desligada quando solicitado"),
    M("UX-18", "Independência de cor no odontograma e nos status",
      "Estados clínicos e financeiros não podem depender só de cor: acrescentar padrão de "
      "preenchimento, ícone e rótulo. Daltonismo afeta cerca de 8% dos homens — em um "
      "odontograma isso é risco assistencial.",
      "dentista.html, financeiro.html", "Alto", "M", "Estado compreensível em escala de cinza"),
], larg_alvo=32 * 2.83, larg_extra=32 * 2.83)

# ─── 04 ──────────────────────────────────────────────────────────────────────
doc.secao("Arquitetura de informação e navegação",
          "Onde as coisas ficam e como se chega até elas")

doc.melhorias([
    M("UX-19", "Casca de aplicação única",
      "Substituir as 40+ páginas independentes por uma casca comum (navegação, cabeçalho, "
      "notificações, busca, perfil) na qual o conteúdo é carregado. Corrige o problema de "
      "manutenção e dá continuidade visual entre seções.",
      "src/pages/, src/partials/", "Alto", "G", "Alteração de navegação em 1 arquivo"),
    M("UX-20", "Corrigir a URL da tela de recepção",
      "Renomear <code>src/pages/recpecionista.html</code> para "
      "<code>recepcionista.html</code>, mantendo redirecionamento 301 da rota antiga. URL com "
      "erro de digitação é visível ao usuário e mina a confiança em software de saúde.",
      "src/pages/recpecionista.html", "Médio", "P", "URL correta com redirecionamento ativo"),
    M("UX-21", "Navegação por perfil, não por módulo",
      "Organizar o menu pelo que cada papel faz no dia (Minha agenda, Meus pacientes, "
      "Financeiro) em vez de espelhar a arquitetura de microsserviços. A estrutura interna do "
      "sistema não deve vazar para a interface.",
      "casca de aplicação", "Alto", "M", "Tarefa alcançada em ≤ 2 cliques"),
    M("UX-22", "Busca global com atalho de teclado",
      "Campo único que encontra paciente, consulta, procedimento e item de estoque, acionado "
      "por atalho, com resultados agrupados e navegáveis pelo teclado. É o recurso mais usado "
      "em sistema de clínica e hoje não existe.",
      "casca de aplicação", "Alto", "M", "Tempo de localizar paciente < 5 s"),
    M("UX-23", "Trilha de navegação e estado de contexto",
      "Deixar sempre visível em que clínica, paciente e data o usuário está operando. Erro de "
      "contexto (registrar no prontuário errado) é o incidente mais grave possível neste tipo "
      "de sistema.",
      "casca de aplicação", "Alto", "P", "Zero registro em prontuário incorreto"),
    M("UX-24", "Painel inicial acionável por papel",
      "Substituir a tela inicial genérica por um painel com o que exige ação agora: próximos "
      "atendimentos, confirmações pendentes, estoque em nível crítico, contas em atraso. "
      "O sistema deve abrir mostrando trabalho, não menu.",
      "dentista.html, recepcionista, coordenador", "Alto", "M", "Ação iniciada direto do painel"),
], larg_alvo=38 * 2.83, larg_extra=30 * 2.83)

# ─── 05 ──────────────────────────────────────────────────────────────────────
doc.secao("Acesso, autenticação e perfis",
          "O fluxo executado várias vezes por dia por cada usuário")

doc.melhorias([
    M("UX-25", "Reduzir o atrito diário de entrada",
      "Hoje toda entrada percorre login, verificação de 2FA e seleção de perfil "
      "(<code>selectperfil.html</code>). Manter sessão confiável por dispositivo, lembrar o "
      "último perfil e exigir 2FA só em dispositivo novo ou ação sensível, sem reduzir a "
      "segurança efetiva.",
      "login.html, selectperfil.html", "Alto", "M", "Tempo de entrada < 10 s no uso diário"),
    M("UX-26", "Tela de 2FA que explica o que fazer",
      "Campo com dígitos separados, colagem automática do código, contagem regressiva de "
      "validade, botão de reenvio com bloqueio e caminho claro de recuperação. É o ponto de "
      "maior abandono em qualquer sistema com segundo fator.",
      "login.html, auth-service", "Alto", "M", "Taxa de sucesso no 2FA > 95%"),
    M("UX-27", "Bloqueio de tela em vez de logout",
      "Sessão inativa deve bloquear a tela preservando o trabalho em andamento, exigindo apenas "
      "reautenticação rápida. Logout que descarta um prontuário meio preenchido é a forma mais "
      "rápida de perder a confiança do usuário.",
      "casca de aplicação", "Alto", "M", "Zero perda de rascunho por expiração"),
    M("UX-28", "Troca de perfil sem sair do sistema",
      "Em clínica pequena a mesma pessoa acumula papéis. Permitir alternar entre perfis pelo "
      "menu, com indicação permanente do papel ativo e das permissões correspondentes.",
      "selectperfil.html, casca", "Médio", "M", "Troca de papel em 1 clique"),
    M("UX-29", "Primeiro acesso guiado",
      "Assistente que configura clínica, horários, profissionais e procedimentos mais comuns em "
      "poucos passos, com dados de exemplo removíveis. O plano comercial promete 30 dias "
      "gratuitos: o valor precisa aparecer na primeira sessão, não na terceira semana.",
      "novo fluxo de onboarding", "Alto", "M", "Primeira consulta agendada em < 15 min"),
    M("UX-30", "Recuperação de conta sem suporte humano",
      "Fluxo completo de esqueci a senha, perdi o dispositivo de 2FA e códigos de recuperação, "
      "com comunicação clara. Cada chamado de recuperação atendido manualmente corrói a margem "
      "de um plano de R$ 97 por mês.",
      "login.html, auth-service", "Alto", "M", "80% das recuperações sem suporte"),
], larg_alvo=36 * 2.83, larg_extra=30 * 2.83)

# ─── 06 ──────────────────────────────────────────────────────────────────────
doc.secao("Agenda e fluxo de atendimento",
          "A tela mais aberta do sistema, todos os dias")

doc.melhorias([
    M("UX-31", "Agenda com múltiplas visões",
      "Dia, semana, mês e linha por profissional, com preferência lembrada por usuário. "
      "Recepção pensa em semana; o dentista pensa no próprio dia.",
      "agenda", "Alto", "M", "Visão preferida usada sem reconfigurar"),
    M("UX-32", "Arrastar para remarcar com verificação de conflito",
      "Remarcação por arrasto, com validação de conflito e de horário de trabalho antes de "
      "soltar, além de desfazer imediato. Remarcação é uma das operações mais frequentes e hoje "
      "exige refazer o agendamento inteiro.",
      "agenda", "Alto", "M", "Remarcação em < 15 s"),
    M("UX-33", "Encaixe inteligente de horário",
      "Ao buscar horário, mostrar as melhores opções considerando duração do procedimento, "
      "profissional habilitado, cadeira disponível e preferência do paciente, em vez de deixar "
      "a recepção caçar espaço vazio na grade.",
      "agenda, appointment-service", "Alto", "M", "Tempo de agendar reduzido em 50%"),
    M("UX-34", "Status visual do fluxo do paciente",
      "Indicar claramente agendado, confirmado, chegou, em atendimento, finalizado e faltou, "
      "com transição em um clique. A recepção precisa saber quem está na sala de espera sem "
      "abrir cada consulta.",
      "agenda, recepção", "Alto", "M", "Estado da sala de espera visível de imediato"),
    M("UX-35", "Bloqueios, intervalos e disponibilidade",
      "Interface para almoço, férias, congresso, manutenção de equipamento e horários "
      "recorrentes. Sem isso a agenda oferece horários impossíveis e a confiança na "
      "ferramenta cai.",
      "agenda", "Alto", "M", "Zero agendamento em horário indisponível"),
    M("UX-36", "Lista de espera com preenchimento automático",
      "Ao liberar um horário por cancelamento, oferecer a vaga automaticamente aos pacientes da "
      "lista de espera pelos canais já integrados. Converte falta em receita — e é uma dor "
      "direta do público-alvo.",
      "agenda, communication-service", "Alto", "M", "% de horários vagos reocupados"),
    M("UX-37", "Impressão e exportação da agenda do dia",
      "Folha do dia legível, com paciente, procedimento, observação e alerta clínico. Muitas "
      "clínicas ainda operam com papel ao lado da cadeira, e negar isso não muda o "
      "comportamento — só empurra o usuário para fora do sistema.",
      "agenda", "Médio", "P", "Uso da folha do dia por clínica"),
], larg_alvo=38 * 2.83, larg_extra=29 * 2.83)

# ─── 07 ──────────────────────────────────────────────────────────────────────
doc.secao("Prontuário e registro clínico",
          "Onde o dentista passa o tempo de atendimento")

doc.melhorias([
    M("UX-38", "Odontograma interativo e acessível",
      "Representação vetorial por dente e face, com seleção por teclado, rótulo textual em cada "
      "estado e legenda sempre visível. É o componente mais característico de um software "
      "odontológico e o mais sensível a erro de leitura.",
      "dentista.html", "Alto", "G", "Registro por dente em < 3 s, navegável por teclado"),
    M("UX-39", "Linha do tempo do paciente",
      "Histórico único e cronológico com consultas, procedimentos, imagens, prescrições, "
      "pagamentos e comunicações, filtrável. Substitui a busca em abas separadas por uma "
      "narrativa contínua do caso.",
      "prontuário", "Alto", "M", "Histórico compreendido em < 30 s"),
    M("UX-40", "Registro estruturado com texto livre",
      "Combinar campos estruturados (necessários para relatório e futura IA) com espaço livre "
      "de anotação. Formulário rígido demais faz o profissional escrever tudo em observação e "
      "destrói o valor do dado.",
      "prontuário", "Alto", "M", "70% dos registros com campo estruturado preenchido"),
    M("UX-41", "Alertas clínicos persistentes",
      "Alergia, condição sistêmica, uso de anticoagulante e gestação precisam estar visíveis em "
      "todas as telas do paciente, com destaque que não dependa apenas de cor. É segurança do "
      "paciente, não decoração.",
      "prontuário, agenda", "Alto", "M", "Alerta visível em 100% das telas do paciente"),
    M("UX-42", "Salvamento automático e recuperação de rascunho",
      "Registro clínico não pode ser perdido por queda de rede, bateria ou clique errado. "
      "Salvar localmente de forma contínua, indicar o estado do salvamento e recuperar ao "
      "reabrir.",
      "prontuário, src/js/", "Alto", "M", "Zero perda de registro relatada"),
    M("UX-43", "Modelos de evolução e procedimento",
      "Permitir modelos por procedimento e por profissional, aplicáveis em um clique e "
      "editáveis. Consulta de rotina repete a mesma estrutura de texto dezenas de vezes por "
      "semana.",
      "prontuário", "Médio", "M", "Tempo de registro reduzido em 40%"),
    M("UX-44", "Comparação de imagens lado a lado",
      "Visualizador com zoom, comparação antes e depois e anotação sobre a imagem. Serve tanto "
      "à decisão clínica quanto à conversa com o paciente sobre o tratamento proposto.",
      "prontuário", "Médio", "G", "Uso do comparador em consulta de retorno"),
], larg_alvo=34 * 2.83, larg_extra=32 * 2.83)

# ─── 08 ──────────────────────────────────────────────────────────────────────
doc.secao("Recepção, financeiro e almoxarifado",
          "As telas que sustentam a operação e a margem da clínica")

doc.melhorias([
    M("UX-45", "Cadastro de paciente em etapas",
      "Coletar o mínimo para agendar (nome, telefone, data de nascimento) e completar o resto "
      "depois, com preenchimento de endereço por CEP e validação clara de CPF. Cadastro longo "
      "no balcão trava a fila da recepção.",
      "recepção, patient-service", "Alto", "M", "Cadastro inicial em < 60 s"),
    M("UX-46", "Orçamento visual e compreensível",
      "Apresentar o plano de tratamento em linguagem de paciente, com foto ou diagrama, opções "
      "de parcelamento e aceite digital. A decisão de aceitar tratamento acontece nesta tela — "
      "ela é comercial, não administrativa.",
      "financeiro.html", "Alto", "G", "Taxa de aceite de orçamento"),
    M("UX-47", "Painel financeiro com resposta direta",
      "Responder às três perguntas do dono da clínica logo na abertura: quanto entrou hoje, "
      "quanto está em atraso, quanto está previsto para o mês — antes de qualquer gráfico.",
      "financeiro.html, relatorios.html", "Alto", "M", "Resposta obtida sem aplicar filtro"),
    M("UX-48", "Conciliação de recebimentos assistida",
      "Sugerir correspondência entre pagamento recebido e cobrança em aberto, com confirmação "
      "em lote. Conciliação manual é a tarefa administrativa mais penosa de uma clínica "
      "pequena.",
      "financeiro.html", "Alto", "G", "Tempo de conciliação reduzido em 60%"),
    M("UX-49", "Estoque orientado a consumo",
      "Mostrar nível crítico, previsão de ruptura pelo consumo histórico e sugestão de compra, "
      "com registro por leitura de código de barras. Falta de anestésico cancela o dia inteiro "
      "de atendimento.",
      "almoxarifado.html", "Alto", "M", "Zero ruptura de item crítico"),
    M("UX-50", "Rastreio de esterilização com o mínimo de toques",
      "Registrar ciclo, kit e responsável por leitura de QR, com bloqueio automático de kit "
      "vencido. O README já prevê o papel de responsável por esterilização; a interface precisa "
      "tornar o registro mais rápido que a planilha que a clínica usa hoje.",
      "almoxarifado.html", "Alto", "M", "100% dos kits rastreados"),
], larg_alvo=36 * 2.83, larg_extra=30 * 2.83)

# ─── 09 ──────────────────────────────────────────────────────────────────────
doc.secao("Experiência do paciente",
          "A parte do produto que a clínica usa para se diferenciar")

doc.melhorias([
    M("UX-51", "Área do paciente sem senha",
      "Acesso por link assinado enviado no WhatsApp, com validade curta. Paciente não cria nem "
      "lembra senha de software de clínica — exigir isso garante que a área não será usada.",
      "cliente.html, auth-service", "Alto", "M", "Taxa de acesso à área do paciente"),
    M("UX-52", "Confirmação de consulta em um toque",
      "Confirmar, remarcar ou cancelar direto da mensagem, sem instalar aplicativo nem fazer "
      "login. Cada falta evitada equivale a uma cadeira produtiva recuperada.",
      "communication-service, cliente.html", "Alto", "M", "Redução da taxa de faltas"),
    M("UX-53", "Anamnese preenchida antes da consulta",
      "Formulário digital respondido pelo paciente em casa, no celular, com salvamento parcial. "
      "Libera a recepção e melhora a qualidade do dado clínico, que hoje é preenchido às "
      "pressas no balcão.",
      "cliente.html, patient-service", "Alto", "M", "% de anamneses concluídas antes da consulta"),
    M("UX-54", "Instruções pós-procedimento no canal certo",
      "Enviar orientação específica do procedimento realizado, em linguagem simples, com "
      "cronograma e canal de dúvida. Reduz retorno desnecessário e melhora a adesão ao "
      "tratamento.",
      "communication-service", "Médio", "M", "Redução de contatos por dúvida pós-consulta"),
    M("UX-55", "Consentimento e privacidade compreensíveis",
      "Apresentar finalidade, base legal e direitos do titular em linguagem de paciente, com "
      "controle granular e histórico de consentimento. A Política de Privacidade já existe em "
      "<code>src/politicy/</code>; falta a experiência de consentir e revogar.",
      "cliente.html, src/politicy/", "Alto", "M", "Consentimento registrado e revogável"),
    M("UX-56", "Pedido de avaliação no momento certo",
      "Solicitar avaliação após atendimento concluído e pago, com um único toque e caminho "
      "privado para insatisfação. Reputação online é o principal canal de aquisição de uma "
      "clínica de bairro.",
      "communication-service", "Médio", "P", "Avaliações públicas por clínica ao mês"),
], larg_alvo=38 * 2.83, larg_extra=30 * 2.83)

# ─── 10 ──────────────────────────────────────────────────────────────────────
doc.secao("Mobile, conteúdo e governança de design",
          "Multiplataforma, microtexto e como manter a qualidade ao longo do tempo")

doc.melhorias([
    M("UX-57", "Escopo real do aplicativo móvel",
      "Definir o app pelo que só faz sentido no celular: agenda do dia, ficha do próximo "
      "paciente, captura de foto intraoral e confirmação do paciente. Hoje "
      "<code>mobile/</code> replica a landing page, o que não entrega valor de produto.",
      "mobile/android/, mobile/ios/", "Alto", "G", "App em beta com 3 fluxos completos"),
    M("UX-58", "Paridade visual entre web, Android e iOS",
      "Gerar os tokens de UX-01 para Compose e SwiftUI, respeitando as convenções de cada "
      "plataforma sem quebrar a identidade. Interface móvel divergente ensina o usuário duas "
      "vezes.",
      "mobile/, design-system", "Médio", "M", "Zero divergência de token entre plataformas"),
    M("UX-59", "Web instalável e offline",
      "PWA com agenda do dia em cache e fila de sincronização atende a maior parte da "
      "necessidade móvel sem manter dois aplicativos nativos — decisão relevante para uma "
      "equipe pequena.",
      "src/, frontend/", "Alto", "M", "Instalações do PWA por clínica"),
    M("UX-60", "Guia de microtexto em português claro",
      "Padronizar voz, uso de termo técnico, formato de data, moeda e mensagem de erro. Erro "
      "que diz o que aconteceu, por que aconteceu e o que fazer agora reduz chamado de suporte "
      "mais que qualquer artigo de ajuda.",
      "todas as telas, design-system", "Médio", "M", "Redução de chamados por confusão"),
    M("UX-61", "Racionalizar as páginas de erro",
      "Reduzir as 41 páginas HTTP a um modelo único parametrizado e investir o esforço nos "
      "erros que de fato ocorrem: sessão expirada, sem conexão, sem permissão, conflito de "
      "agenda, pagamento recusado — cada um com ação de recuperação.",
      "src/pages/4xx e 5xx, frontend/nginx.conf", "Médio", "M", "41 arquivos reduzidos a 1 modelo"),
    M("UX-62", "Estados vazios, de carregamento e de falha",
      "Definir para cada tela o que aparece sem dado (com ação sugerida), durante o "
      "carregamento (esqueleto, não travamento) e em falha (com opção de repetir). É onde a "
      "interface atual simplesmente não responde.",
      "todas as telas, src/js/api.js", "Alto", "M", "3 estados definidos em 100% das telas"),
    M("UX-63", "Pesquisa contínua com usuários reais",
      "Estabelecer ciclo de teste de usabilidade moderado com dentistas e recepcionistas antes "
      "de cada entrega relevante. Cinco participantes por rodada revelam a maioria dos "
      "problemas graves a um custo compatível com o projeto.",
      "processo de produto", "Alto", "M", "1 rodada de teste por ciclo de entrega"),
    M("UX-64", "Métricas de experiência em produção",
      "Instrumentar tempo até a primeira consulta agendada, taxa de conclusão por fluxo, erro "
      "por formulário e Core Web Vitals, sempre com dado agregado e anonimizado. Sem medição, "
      "melhoria de design vira questão de opinião.",
      "src/js/, analytics", "Alto", "M", "Painel de UX revisado mensalmente"),
], larg_alvo=40 * 2.83, larg_extra=29 * 2.83)

# ─── 11 ──────────────────────────────────────────────────────────────────────
doc.secao("Anexos", "Contraste, inventário de telas e roadmap")

doc.subsecao("Anexo A — Régua de contraste da paleta Pantone")
doc.texto(
    "Razões calculadas pela fórmula de contraste da WCAG 2.x. O piso é 4,5:1 para texto normal, "
    "3:1 para texto grande (a partir de 18,66 px em negrito ou 24 px) e 3:1 para componentes de "
    "interface e indicadores de foco.")
_BRANCO_UI, _FUNDO_UI, _TINTA = "#FAFAF5", "#F4F5F0", "#2C2C2C"
_pares = [
    ("#00DF76", _BRANCO_UI, "verde sobre branco",
     "Somente como preenchimento de área; nunca como texto ou ícone informativo."),
    (_TINTA, "#00DF76", "preto sobre verde",
     "Padrão do botão primário: fundo verde com texto escuro."),
    ("#FFC72C", _BRANCO_UI, "amarelo sobre branco",
     "Somente como preenchimento; o texto sobre ele deve ser escuro."),
    (_TINTA, "#FFC72C", "preto sobre amarelo",
     "Destaque e chamada de atenção com texto escuro."),
    ("#FE5000", _BRANCO_UI, "laranja sobre branco",
     "Título e ícone grande; exige variante escurecida para texto corrido."),
    ("#DA291C", _BRANCO_UI, "vermelho sobre branco",
     "Erro e alerta em texto, sempre com ícone e rótulo junto."),
    (_TINTA, _FUNDO_UI, "preto sobre fundo",
     "Texto padrão da interface."),
    ("#97999B", _BRANCO_UI, "cinza sobre branco",
     "Não usar em texto; apenas em borda decorativa e separador."),
]


def _veredito(razao_txt):
    valor = float(razao_txt.replace(":1", "").replace(",", "."))
    if valor >= 4.5:
        return "Aprova"
    if valor >= 3.0:
        return "Reprova (aprova em texto grande)"
    return "Reprova"


doc.tabela(
    ["Combinação", "Razão", "Texto normal", "Uso recomendado"],
    [[f"{a} {b} — {nome}", contraste(a, b), _veredito(contraste(a, b)), uso]
     for a, b, nome, uso in _pares],
    larguras=[52 * 2.83, 16 * 2.83, 30 * 2.83, 75 * 2.83],
    alinhar_centro=(1,),
)

doc.texto(
    "<b>Consequência prática:</b> a identidade da marca não muda. Muda a <i>regra de uso</i> — "
    "verde e amarelo passam a ser cores de superfície com conteúdo escuro por cima, e ganham "
    "variantes escurecidas para quando precisarem virar texto ou link. Um teste automatizado de "
    "contraste no CI impede a regressão.")

doc.subsecao("Anexo B — Inventário de telas e prioridade de redesenho")
doc.tabela(
    ["Tela", "Arquivo", "Papel principal", "Prioridade"],
    [
        ["Agenda e atendimento", "src/pages/dentista.html", "Dentista", "1 — Crítica"],
        ["Recepção", "src/pages/recpecionista.html", "Recepcionista", "1 — Crítica"],
        ["Login e 2FA", "src/pages/login.html", "Todos", "1 — Crítica"],
        ["Seleção de perfil", "src/pages/selectperfil.html", "Todos", "2 — Alta"],
        ["Financeiro", "src/pages/financeiro.html", "Coordenador e recepção", "2 — Alta"],
        ["Área do paciente", "src/pages/cliente.html", "Paciente", "2 — Alta"],
        ["Almoxarifado", "src/pages/almoxarifado.html", "Almoxarife", "3 — Média"],
        ["Coordenação", "src/pages/coordenador.html", "Coordenador", "3 — Média"],
        ["Relatórios", "src/pages/relatorios.html", "Coordenador", "3 — Média"],
        ["Notificações", "src/pages/notificacoes.html", "Todos", "3 — Média"],
        ["Configurações", "src/pages/configuracoes.html", "Coordenador", "4 — Baixa"],
        ["Páginas de erro (41)", "src/pages/4xx.html e 5xx.html", "Todos",
         "4 — Consolidar em modelo único"],
        ["Landing page", "index.html", "Visitante e prospecto", "2 — Alta (aquisição)"],
        ["Aplicativos móveis", "mobile/android/, mobile/ios/", "Dentista",
         "3 — Definir escopo antes de desenhar"],
    ],
    larguras=[38 * 2.83, 50 * 2.83, 47 * 2.83, 38 * 2.83],
)

doc.subsecao("Anexo C — Roadmap de design em três ondas")
doc.tabela(
    ["Onda", "Foco", "Itens", "Resultado esperado"],
    [
        ["Onda 1 — Fundação",
         "Sistema de design e conformidade mínima de acessibilidade",
         "UX-01, UX-02, UX-04, UX-06, UX-09, UX-10, UX-11, UX-12, UX-13, UX-14, UX-16, UX-20, "
         "UX-62",
         "Interface consistente e sem violação crítica de WCAG; base para desenhar rápido."],
        ["Onda 2 — Fluxos críticos",
         "Agenda, prontuário, acesso e experiência do paciente",
         "UX-03, UX-05, UX-15, UX-17 a UX-19, UX-21 a UX-37, UX-51 a UX-53, UX-60, UX-61",
         "As tarefas de maior frequência ficam mais rápidas e recuperáveis de erro."],
        ["Onda 3 — Diferenciação",
         "Clínico avançado, financeiro assistido, mobile e governança",
         "UX-38 a UX-50, UX-54 a UX-59, UX-63, UX-64",
         "Experiência que sustenta o preço praticado e o discurso comercial do Documento 05."],
    ],
    larguras=[30 * 2.83, 40 * 2.83, 60 * 2.83, 43 * 2.83],
)

doc.nota_metodologica(
    "<b>Método.</b> As melhorias derivam da inspeção do código de interface do repositório "
    "(<code>index.html</code>, <code>style.css</code>, <code>script.js</code>, "
    "<code>src/pages/</code>, <code>src/css/</code>, <code>src/js/api.js</code> e "
    "<code>mobile/</code>) confrontada com as heurísticas de Nielsen, a WCAG 2.2 nível AA e as "
    "necessidades declaradas dos atores descritos no README do projeto. As razões de contraste "
    "do Anexo A foram calculadas a partir dos valores hexadecimais definidos em "
    "<code>style.css</code> e devem ser reconfirmadas em ferramenta de verificação sobre a "
    "interface renderizada, já que sobreposição, opacidade e antialiasing podem alterar o "
    "resultado percebido. As metas quantitativas são propostas de partida: nenhuma delas "
    "substitui teste com usuários reais (UX-63), que é a única forma de validar se a melhoria "
    "funcionou.")

doc.build()
