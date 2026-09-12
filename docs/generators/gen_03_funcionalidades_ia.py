# -*- coding: utf-8 -*-
"""Documento 03 — Novas Funcionalidades e Inteligência Artificial no DentiBot."""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dentibot_doc import AZUL, LARANJA, VERDE, VERMELHO, Documento, Melhoria as M  # noqa: E402

SAIDA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                     "03-Novas-Funcionalidades-e-IA.pdf")

doc = Documento(
    caminho=SAIDA,
    numero="03",
    titulo="Novas Funcionalidades e IA",
    subtitulo="63 funcionalidades para o DentiBot, das apostas raras no mercado brasileiro às "
              "que não encontramos equivalente em nenhum concorrente conhecido.",
    resumo="",
    cor=AZUL,
    rotulo="Produto, Inovação e Inteligência Artificial",
    col_extra="Depende de / regulação",
)

doc.capa()

doc.resumo_executivo([
    "Este documento propõe 63 funcionalidades organizadas por grau de diferenciação. A maioria "
    "existe de alguma forma no mercado e o valor está na execução; um conjunto menor — reunido "
    "na Seção 10 — descreve capacidades para as quais <b>não encontramos equivalente</b> nos "
    "sistemas de gestão odontológica conhecidos no Brasil. Fazemos essa distinção de forma "
    "explícita porque a afirmação \"nenhum aplicativo tem isso\" não é verificável por inspeção "
    "de código: ela exige pesquisa de mercado formal, e a Seção 11 define como conduzi-la antes "
    "de qualquer uso da alegação em material comercial.",

    "A orientação de produto vem do próprio diagnóstico do projeto. O DentiBot não vai ganhar de "
    "um concorrente estabelecido oferecendo o mesmo cadastro de paciente por um preço menor — "
    "essa é uma corrida que uma equipe pequena perde. Ele ganha resolvendo, com IA, três dores "
    "que o software tradicional apenas registra em vez de atacar: <b>a cadeira vazia</b> "
    "(falta e cancelamento), <b>o tempo do dentista gasto digitando</b> em vez de tratando, e "
    "<b>o tratamento abandonado no meio</b> — que é perda simultânea de receita para a clínica "
    "e de saúde para o paciente.",

    "Há um limite regulatório que estrutura todo o documento e precisa ser dito na primeira "
    "página: <b>software que faz diagnóstico é dispositivo médico</b>. No Brasil isso implica "
    "regularização junto à ANVISA, e no exercício profissional implica as resoluções do Conselho "
    "Federal de Odontologia. Por isso nenhuma funcionalidade aqui é apresentada como diagnóstica "
    "autônoma: a IA prioriza, sinaliza, rascunha e explica — quem decide, assina e responde é "
    "sempre o cirurgião-dentista. Essa não é uma limitação a contornar; é o que torna o produto "
    "vendável sem risco jurídico.",

    "As funcionalidades estão classificadas em três horizontes. <b>H1</b> usa a base de dados "
    "que a clínica já gera e entrega valor em semanas. <b>H2</b> exige a plataforma de dados "
    "descrita no Documento 01 (itens ST-57 a ST-61) e um volume mínimo de clínicas ativas. "
    "<b>H3</b> depende de parceria clínica, aprovação regulatória ou massa de dados que só "
    "existe com escala — e por isso entra no plano como aposta, não como promessa comercial.",
], destaques=[
    ("63", "funcionalidades propostas"),
    ("11", "sem equivalente conhecido"),
    ("3", "horizontes de execução"),
    ("H1", "entrega em semanas"),
    ("0", "diagnóstico autônomo"),
])

doc.sumario(extras=[
    "Anexo A — Matriz de esforço e diferenciação",
    "Anexo B — Requisitos regulatórios por funcionalidade",
    "Anexo C — Como validar a alegação de ineditismo",
])

# ─── 01 ──────────────────────────────────────────────────────────────────────
doc.secao("Premissas de produto",
          "O que torna uma funcionalidade defensável e o que a torna apenas cara")

doc.texto(
    "Antes da lista, quatro critérios que qualquer item precisou atender para entrar. Eles "
    "existem porque a armadilha mais comum em produto com IA é construir a demonstração "
    "impressionante que ninguém usa na segunda semana.")

doc.tabela(
    ["Critério", "Pergunta que ele responde", "Por que importa neste projeto"],
    [
        ["Dor real e frequente",
         "O usuário enfrenta isso toda semana ou apenas uma vez por trimestre?",
         "Funcionalidade de uso raro não muda a decisão de assinar nem a de renovar; ela só "
         "aumenta o custo de manutenção e a superfície de suporte."],
        ["Dado disponível",
         "A clínica já gera esse dado no uso normal do sistema, ou seria preciso pedir que ela "
         "alimente algo novo?",
         "Modelo que exige entrada manual extra não é usado. As apostas de H1 são justamente as "
         "que se alimentam do que a agenda e o prontuário já produzem."],
        ["Defensabilidade",
         "Um concorrente com mais capital copia isso em quanto tempo?",
         "Integração ao fluxo de trabalho e dado longitudinal acumulado são difíceis de copiar; "
         "chamar um modelo de linguagem genérico não é."],
        ["Responsabilidade clara",
         "Se a saída da IA estiver errada, quem responde e como o erro é detectado?",
         "Em saúde, funcionalidade sem resposta a essa pergunta é passivo jurídico. Toda "
         "sugestão precisa de origem rastreável e de confirmação humana registrada."],
    ],
    larguras=[27 * 2.83, 52 * 2.83, 94 * 2.83],
)

doc.destaque(
    "Limite inegociável: assistência, nunca diagnóstico autônomo",
    "Nenhuma funcionalidade deste documento emite diagnóstico, prescreve tratamento ou age "
    "clinicamente sem confirmação humana. Software com finalidade diagnóstica é enquadrado como "
    "dispositivo médico e exige regularização sanitária no Brasil — um caminho viável, mas que "
    "é um projeto próprio, com custo, prazo e evidência clínica, e não uma funcionalidade de "
    "roadmap. O DentiBot posiciona a IA como <b>copiloto</b>: ela prioriza a fila de laudos, "
    "sinaliza região de interesse, rascunha o texto que o profissional revisa e explica ao "
    "paciente o que o dentista decidiu. A decisão clínica, a assinatura e a responsabilidade "
    "permanecem integralmente com o cirurgião-dentista, e o sistema registra em trilha "
    "auditável quem confirmou o quê e quando.",
    cor=VERMELHO)

# ─── 02 ──────────────────────────────────────────────────────────────────────
doc.secao("Copiloto clínico",
          "Devolver ao dentista o tempo que hoje ele gasta digitando")
doc.legenda_backlog()

doc.melhorias([
    M("IA-01", "Nota clínica a partir de ditado",
      "O profissional dita a evolução em linguagem natural e o sistema estrutura em campos do "
      "prontuário — dente, face, procedimento, material, observação — apresentando para revisão. "
      "O ganho não é escrever mais rápido: é registrar durante o atendimento em vez de acumular "
      "para o fim do dia, quando o detalhe já se perdeu.",
      "prontuário, ai-gateway", "Alto", "M", "H1 · ST-61"),
    M("IA-02", "Transcrição ambiente com trecho de origem",
      "Com consentimento explícito do paciente, transcrever a consulta e propor a nota clínica. "
      "O diferencial de segurança é a <b>rastreabilidade obrigatória</b>: cada afirmação da nota "
      "aponta o trecho do áudio que a originou, e o que o modelo não conseguir ancorar é marcado "
      "como não confirmado em vez de ser afirmado.",
      "prontuário, ai-gateway", "Alto", "G", "H2 · consentimento LGPD art. 11"),
    M("IA-03", "Busca com resposta citada sobre o histórico",
      "Perguntar em português \"qual foi a conduta no 36 e por que trocamos a restauração?\" e "
      "receber resposta construída apenas sobre o prontuário daquele paciente, sempre com link "
      "para o registro de origem. Sem citação verificável, a resposta não é exibida.",
      "prontuário, pgvector", "Alto", "M", "H2 · ST-59, ST-61"),
    M("IA-04", "Plano de tratamento em duas linguagens",
      "Gerar simultaneamente a versão técnica (para o prontuário e o convênio) e a versão em "
      "linguagem de paciente, com o porquê de cada etapa, alternativas e o que acontece se nada "
      "for feito. É a peça que mais influencia a aceitação do orçamento.",
      "prontuário, financeiro", "Alto", "M", "H1 · ST-61"),
    M("IA-05", "Verificação de consistência antes de fechar",
      "Ao encerrar o atendimento, apontar incoerências verificáveis por regra: procedimento "
      "lançado em dente ausente, material sem baixa no estoque, alergia registrada versus "
      "medicação prescrita, código de convênio incompatível. Regra determinística primeiro; "
      "modelo apenas onde a regra não alcança.",
      "prontuário, inventory-service", "Alto", "M", "H1"),
    M("IA-06", "Resumo de retorno em dez segundos",
      "Antes de chamar o paciente, um resumo de meia tela com o essencial: última conduta, "
      "pendências do plano, alertas, situação financeira e o que ficou combinado. Substitui a "
      "leitura do histórico inteiro entre um paciente e outro.",
      "prontuário, agenda", "Alto", "M", "H1"),
    M("IA-07", "Codificação TUSS assistida",
      "Sugerir o código TUSS correspondente ao procedimento descrito, com a justificativa e o "
      "histórico de glosa daquele código naquele convênio. O README já lista os sistemas de "
      "convênio como ator externo; esta é a ponte prática entre o registro clínico e o "
      "faturamento.",
      "financial-service", "Alto", "M", "H2 · integração TISS/TUSS"),
], larg_alvo=32 * 2.83, larg_extra=30 * 2.83)

# ─── 03 ──────────────────────────────────────────────────────────────────────
doc.secao("Visão computacional odontológica",
          "Imagem como dado estruturado, não como anexo")

doc.texto(
    "As referências acadêmicas do próprio projeto já apontam esta direção — há trabalho "
    "publicado sobre efetividade de inteligência artificial na detecção de cárie dentária. "
    "O ponto de atenção é regulatório: quanto mais a funcionalidade se aproxima de afirmar um "
    "achado, mais ela se aproxima de ser dispositivo médico. As funcionalidades abaixo foram "
    "desenhadas para ficar do lado assistivo dessa linha.")

doc.melhorias([
    M("IA-08", "Captura guiada de foto intraoral",
      "O aplicativo orienta enquadramento, foco e iluminação em tempo real e rejeita imagem "
      "inadequada na hora. Padronizar a captura é pré-requisito de tudo o que vem depois: sem "
      "isso, qualquer modelo recebe entrada ruim e produz saída inútil.",
      "mobile/", "Alto", "M", "H1"),
    M("IA-09", "Triagem por foto no canal do paciente",
      "O paciente envia foto pelo WhatsApp e o sistema classifica a urgência para priorizar o "
      "encaixe — sem afirmar achado clínico, apenas ordenando a fila humana. Reduz a espera de "
      "quem tem urgência real e evita deslocamento de quem não tem.",
      "communication-service", "Alto", "G", "H2 · triagem, não diagnóstico"),
    M("IA-10", "Marcação de região de interesse em radiografia",
      "Destacar áreas para o profissional examinar, apresentando sempre a imagem original ao "
      "lado e exigindo confirmação explícita antes de qualquer registro. A sinalização nunca "
      "entra sozinha no prontuário.",
      "prontuário", "Alto", "G", "H3 · avaliar enquadramento ANVISA"),
    M("IA-11", "Comparação temporal automática de imagens",
      "Alinhar imagens da mesma região feitas em datas diferentes e destacar o que mudou. Para "
      "acompanhamento de lesão e de tratamento periodontal, ver a diferença vale mais do que ler "
      "duas descrições separadas por seis meses.",
      "prontuário", "Alto", "G", "H2"),
    M("IA-12", "Odontograma preenchido a partir de imagem",
      "Pré-preencher restaurações, ausências e próteses já existentes a partir das imagens, "
      "deixando ao profissional apenas revisar. Elimina a tarefa mais tediosa do primeiro "
      "atendimento e é o momento de maior atrito na adoção do sistema.",
      "prontuário", "Alto", "G", "H3 · exige revisão obrigatória"),
    M("IA-13", "Conferência de bandeja e kit por imagem",
      "Uma foto da bandeja montada confere o instrumental contra o protocolo do procedimento e o "
      "kit de esterilização contra o rastreio do almoxarifado. Conecta o papel de responsável "
      "por esterilização, já previsto no README, à segurança do procedimento.",
      "almoxarifado", "Médio", "G", "H3"),
    M("IA-14", "Documentação fotográfica automática do caso",
      "Organizar sozinho as fotos por consulta, região e etapa do tratamento, montando a "
      "sequência do caso sem trabalho manual. Serve à defesa profissional, ao ensino e à "
      "conversa com o paciente.",
      "prontuário", "Médio", "M", "H1"),
], larg_alvo=30 * 2.83, larg_extra=33 * 2.83)

# ─── 04 ──────────────────────────────────────────────────────────────────────
doc.secao("Agenda e receita preditivas",
          "Atacar a cadeira vazia, que é a maior perda silenciosa da clínica")

doc.melhorias([
    M("IA-15", "Previsão de falta por consulta",
      "Estimar a probabilidade de ausência a partir de histórico, antecedência, horário, clima, "
      "distância e canal de confirmação, e agir sobre as de maior risco. É a funcionalidade de "
      "maior retorno financeiro imediato do documento, e usa apenas dado que a agenda já produz.",
      "appointment-service, analytics", "Alto", "M", "H1 · ST-57, ST-58"),
    M("IA-16", "Confirmação proporcional ao risco",
      "Em vez do lembrete padrão para todos, escalonar canal, momento e insistência conforme o "
      "risco previsto. Evita saturar o paciente pontual e resgatar o que provavelmente faltaria.",
      "communication-service", "Alto", "M", "H1 · IA-15"),
    M("IA-17", "Sobreagendamento calibrado",
      "Sugerir, com limite conservador e sempre sob aprovação humana, encaixe adicional em "
      "faixas de alta probabilidade de falta. Aumenta a ocupação sem produzir sala de espera "
      "lotada — e o limite existe para proteger a experiência do paciente.",
      "agenda", "Alto", "M", "H2 · IA-15"),
    M("IA-18", "Preenchimento automático de vaga liberada",
      "Ao surgir cancelamento, ofertar o horário à lista de espera pelos canais integrados, por "
      "ordem de aderência (distância, disponibilidade declarada, urgência), e confirmar o "
      "primeiro que aceitar.",
      "agenda, communication-service", "Alto", "M", "H1"),
    M("IA-19", "Duração de procedimento aprendida",
      "Cada profissional leva um tempo diferente em cada procedimento. Aprender a duração real "
      "por profissional e por perfil de paciente corrige a agenda melhor do que qualquer padrão "
      "cadastrado à mão, e reduz atraso acumulado no dia.",
      "appointment-service", "Alto", "M", "H2"),
    M("IA-20", "Previsão de receita e fluxo de caixa",
      "Projetar entrada dos próximos noventa dias a partir de tratamentos em andamento, "
      "recorrência, sazonalidade e inadimplência histórica. Dono de clínica pequena decide "
      "contratação e compra sem essa visibilidade hoje.",
      "financial-service, analytics", "Alto", "M", "H2 · ST-57"),
    M("IA-21", "Gêmeo digital da clínica",
      "Simulador de capacidade que responde a perguntas de decisão: o que acontece com a fila e "
      "com a receita se eu contratar mais um dentista, abrir aos sábados ou comprar a segunda "
      "cadeira? Transforma o sistema de registro em ferramenta de decisão.",
      "analytics", "Alto", "G", "H3 · ST-57"),
], larg_alvo=34 * 2.83, larg_extra=29 * 2.83)

# ─── 05 ──────────────────────────────────────────────────────────────────────
doc.secao("Voz, ambiente e operação sem as mãos",
          "O profissional está de luva — a interface precisa considerar isso")

doc.melhorias([
    M("IA-22", "Modo cadeira por comando de voz",
      "Operação por voz das ações que ocorrem com o paciente na cadeira: próximo paciente, abrir "
      "radiografia, registrar procedimento, chamar a recepção. O README já descreve disparo de "
      "comandos sem as mãos como responsabilidade do dentista — esta é a implementação disso.",
      "prontuário, mobile/", "Alto", "G", "H2"),
    M("IA-23", "Processamento de voz no dispositivo",
      "Executar o reconhecimento localmente sempre que possível, sem enviar áudio de consulta "
      "para a nuvem. É a diferença entre uma funcionalidade que o comitê de privacidade da "
      "clínica aprova e uma que ele veta.",
      "mobile/, ai-gateway", "Alto", "G", "H3 · LGPD art. 11"),
    M("IA-24", "Parada de emergência sempre acessível",
      "Botão físico e comando de voz que interrompem qualquer automação em execução, com "
      "registro em auditoria. O README já prevê o uso de E-Stop; qualquer automação assistida "
      "precisa dele como requisito, não como opcional.",
      "prontuário, audit-service", "Alto", "M", "H2 · segurança"),
    M("IA-25", "Rotinas de apoio parametrizadas",
      "Sequências configuráveis que preparam o contexto do procedimento — abrir prontuário, "
      "carregar protocolo, separar kit, iniciar cronômetro — homologadas pelo coordenador antes "
      "de ficarem disponíveis, como o README já prevê para homologação de macros.",
      "prontuário, coordenador", "Médio", "M", "H2"),
    M("IA-26", "Checklist de biossegurança assistido",
      "Conduzir por voz o checklist antes e depois do procedimento, registrando conformidade sem "
      "exigir que alguém toque no computador entre um paciente e outro.",
      "almoxarifado, audit-service", "Médio", "M", "H2"),
    M("IA-27", "Leitura em voz alta de alertas críticos",
      "Anunciar alergia, condição sistêmica e uso de anticoagulante ao abrir o atendimento. "
      "Alerta que precisa ser lido na tela compete com o paciente pela atenção do profissional; "
      "alerta falado, não.",
      "prontuário", "Alto", "P", "H1 · acessibilidade"),
], larg_alvo=34 * 2.83, larg_extra=27 * 2.83)

# ─── 06 ──────────────────────────────────────────────────────────────────────
doc.secao("Comunicação autônoma com o paciente",
          "O canal que o paciente já usa, operado sem sobrecarregar a recepção")

doc.melhorias([
    M("IA-28", "Recepcionista virtual no WhatsApp",
      "Agendar, remarcar, cancelar, informar preparo e responder dúvida administrativa em "
      "conversa natural, com transferência imediata para humano em qualquer sinal clínico ou "
      "reclamação. A integração com a API do WhatsApp Business já é premissa do projeto.",
      "communication-service", "Alto", "G", "H2 · escalonamento obrigatório"),
    M("IA-29", "Atendimento fora do horário comercial",
      "Boa parte da demanda de agendamento chega à noite e no fim de semana, quando a clínica "
      "não responde e o paciente procura outra. Resolver o agendamento simples em qualquer "
      "horário captura demanda hoje perdida.",
      "communication-service", "Alto", "M", "H2 · IA-28"),
    M("IA-30", "Mensagem adaptada ao perfil do paciente",
      "Ajustar canal, horário, tom e nível de detalhe conforme o histórico de resposta de cada "
      "pessoa. Idoso que responde por ligação e jovem que só lê áudio no WhatsApp não devem "
      "receber a mesma abordagem.",
      "communication-service", "Médio", "M", "H2"),
    M("IA-31", "Detecção de urgência na conversa",
      "Reconhecer relato compatível com urgência (trauma, dor intensa, edema, sangramento) e "
      "escalar imediatamente para humano com prioridade, sem tentar resolver sozinho. É a regra "
      "de segurança mais importante do canal automatizado.",
      "communication-service", "Alto", "M", "H2 · segurança do paciente"),
    M("IA-32", "Retorno de contato sem resposta",
      "Reengajar de forma educada e com limite de frequência quem não respondeu, encerrando a "
      "sequência ao primeiro sinal de desinteresse. Persistência excessiva custa mais reputação "
      "do que a consulta vale.",
      "communication-service", "Médio", "P", "H1"),
    M("IA-33", "Análise de sentimento nas interações",
      "Identificar insatisfação em fase inicial e alertar a coordenação antes que ela vire "
      "avaliação pública negativa. Para clínica de bairro, reputação online é o principal canal "
      "de aquisição.",
      "communication-service", "Médio", "M", "H2"),
], larg_alvo=34 * 2.83, larg_extra=31 * 2.83)

# ─── 07 ──────────────────────────────────────────────────────────────────────
doc.secao("Prevenção, adesão e jornada longitudinal",
          "A frente que alinha o interesse da clínica ao do paciente")

doc.melhorias([
    M("IA-34", "Risco individual de cárie e doença periodontal",
      "Estimar risco a partir de histórico, frequência de retorno, hábitos declarados e evolução "
      "clínica, e usar isso para definir o intervalo de retorno de cada pessoa em vez do "
      "\"volte em seis meses\" aplicado a todos.",
      "prontuário, analytics", "Alto", "G", "H3 · validação clínica"),
    M("IA-35", "Radar de abandono de tratamento",
      "Identificar quem parou no meio do plano e por qual etapa, priorizando o resgate por "
      "impacto clínico e financeiro. Tratamento interrompido é a maior perda conjunta de saúde "
      "e de receita, e hoje ninguém percebe até o paciente sumir.",
      "prontuário, financeiro", "Alto", "M", "H1"),
    M("IA-36", "Retorno com intervalo personalizado",
      "Convocar cada paciente no intervalo adequado ao seu risco, com mensagem que explica o "
      "motivo. Convocação genérica é ignorada; convocação com razão específica converte.",
      "communication-service", "Alto", "M", "H2 · IA-34"),
    M("IA-37", "Acompanhamento pós-procedimento ativo",
      "Perguntar ativamente sobre dor, sangramento e sensibilidade nos dias seguintes, com "
      "escalonamento automático em resposta preocupante. Detecta complicação antes que ela vire "
      "urgência.",
      "communication-service", "Alto", "M", "H1"),
    M("IA-38", "Educação personalizada e verificável",
      "Enviar orientação específica da condição e do procedimento, em linguagem simples, com "
      "confirmação de leitura registrada. Serve à adesão e também à documentação de que a "
      "orientação foi prestada.",
      "communication-service", "Médio", "M", "H1"),
    M("IA-39", "Jornada familiar",
      "Reconhecer o núcleo familiar e coordenar agendamento, prevenção e cobrança de forma "
      "conjunta. Mãe que agenda a própria consulta junto com a dos dois filhos economiza três "
      "deslocamentos — e a clínica preenche três cadeiras.",
      "patient-service, agenda", "Médio", "M", "H2"),
], larg_alvo=34 * 2.83, larg_extra=27 * 2.83)

# ─── 08 ──────────────────────────────────────────────────────────────────────
doc.secao("Gestão, financeiro e convênios",
          "Onde a IA protege a margem da clínica")

doc.melhorias([
    M("IA-40", "Prevenção de glosa antes do envio",
      "Verificar a guia contra o histórico de glosa do convênio e apontar o que provavelmente "
      "será recusado, com a correção sugerida. Glosa descoberta depois custa retrabalho e "
      "atraso de caixa; evitada antes, custa um clique.",
      "financial-service", "Alto", "G", "H2 · TISS/TUSS"),
    M("IA-41", "Cobrança com abordagem calibrada",
      "Definir o melhor momento, canal e tom para cada devedor, priorizando recuperação sem "
      "desgastar o relacionamento. Clínica pequena não pode escolher entre receber e manter o "
      "paciente.",
      "financial-service", "Alto", "M", "H2"),
    M("IA-42", "Parcelamento sugerido por perfil",
      "Propor condição de pagamento que maximize a probabilidade de aceite do tratamento "
      "respeitando o limite de risco definido pela clínica, com transparência total sobre "
      "juros e prazo.",
      "financeiro", "Médio", "M", "H2"),
    M("IA-43", "Compra de insumos no momento certo",
      "Prever consumo por procedimento agendado, sugerir quantidade e disparar cotação. O "
      "controle de estoque previsto no projeto passa de registro passivo a instrumento de "
      "economia.",
      "inventory-service", "Alto", "M", "H1"),
    M("IA-44", "Rentabilidade por procedimento e por profissional",
      "Calcular margem real considerando tempo de cadeira, material, comissão (o schema já "
      "prevê regra de comissão por clínica) e taxa de convênio. Muita clínica descobre tarde "
      "que o procedimento mais frequente é o menos rentável.",
      "financial-service, relatórios", "Alto", "M", "H2 · ST-57"),
    M("IA-45", "Diagnóstico do negócio em linguagem natural",
      "Um resumo semanal que explica o que mudou e por quê, com a ação sugerida, em vez de mais "
      "um gráfico para o dono interpretar sozinho depois do expediente.",
      "relatórios, ai-gateway", "Médio", "M", "H2 · ST-61"),
], larg_alvo=34 * 2.83, larg_extra=27 * 2.83)

# ─── 09 ──────────────────────────────────────────────────────────────────────
doc.secao("Interoperabilidade e ecossistema",
          "Deixar de ser um sistema isolado")

doc.melhorias([
    M("IA-46", "Interoperabilidade em padrão aberto",
      "Expor e consumir dados em FHIR, viabilizando integração com o ecossistema de saúde "
      "digital brasileiro e com a Rede Nacional de Dados em Saúde. É pré-requisito de qualquer "
      "conversa com operadora, rede de clínicas ou poder público.",
      "novo interop-service", "Alto", "G", "H3 · RNDS"),
    M("IA-47", "Portabilidade completa do prontuário",
      "Exportação íntegra, legível e assinada de todo o dado do paciente, por iniciativa dele. "
      "É direito do titular na LGPD e, na prática comercial, o antídoto à objeção de "
      "aprisionamento em fornecedor.",
      "patient-service", "Alto", "M", "H1 · LGPD art. 18"),
    M("IA-48", "Assinatura digital com validade jurídica",
      "Assinar prontuário, consentimento e atestado com certificado ICP-Brasil, com carimbo de "
      "tempo. O README já cita assinatura digital de documentos entre as funcionalidades "
      "essenciais.",
      "audit-service", "Alto", "M", "H2 · ICP-Brasil"),
    M("IA-49", "Integração com laboratório de prótese",
      "Acompanhar pedido, prazo e etapa da prótese dentro do sistema, com o laboratório "
      "atualizando o status. Hoje esse acompanhamento vive em grupo de WhatsApp e some.",
      "novo módulo", "Médio", "G", "H3 · parceria"),
    M("IA-50", "API pública e integrações",
      "Publicar API documentada e webhooks para contabilidade, meio de pagamento, emissor de "
      "nota e ferramenta de marketing. Cada integração reduz o motivo de trocar de sistema.",
      "gateway", "Médio", "M", "H2 · ST-20"),
    M("IA-51", "Segunda opinião assíncrona entre profissionais",
      "Solicitar parecer de outro cirurgião-dentista da rede sobre um caso, com dado "
      "pseudonimizado, consentimento do paciente e prazo. Cria valor de rede: a plataforma fica "
      "mais útil a cada clínica que entra.",
      "novo módulo", "Alto", "G", "H3 · CFO, teleodontologia"),
], larg_alvo=32 * 2.83, larg_extra=31 * 2.83)

# ─── 10 ──────────────────────────────────────────────────────────────────────
doc.secao("Sem equivalente conhecido no mercado",
          "As apostas de maior diferenciação — e as que exigem mais validação")

doc.texto(
    "As onze funcionalidades desta seção são aquelas para as quais <b>não identificamos "
    "equivalente</b> nos sistemas de gestão odontológica que conhecemos. Trata-se de avaliação "
    "de repertório, não de pesquisa de mercado sistemática — o Anexo C descreve o procedimento "
    "de verificação que precisa ser executado antes de qualquer uso comercial da alegação de "
    "ineditismo. Independentemente do resultado dessa verificação, todas resolvem problema "
    "real e valem por si.")

doc.melhorias([
    M("IA-52", "Extrato de acesso ao próprio prontuário",
      "O paciente vê, como quem confere um extrato bancário, quem acessou seus dados, quando e "
      "com qual finalidade — e pode contestar um acesso. Transparência radical vira argumento "
      "comercial para a clínica: nenhum concorrente conhecido oferece isso ao titular, e o "
      "audit-service com trilha append-only já existe no projeto.",
      "audit-service, cliente.html", "Alto", "M", "H2 · LGPD art. 18"),
    M("IA-53", "Consentimento granular e revogável em tempo real",
      "Em vez do aceite único de termo, o paciente controla finalidade por finalidade — lembrete "
      "por WhatsApp, uso de imagem em ensino, contribuição anonimizada para modelo — e revoga "
      "quando quiser, com efeito imediato e verificável no sistema.",
      "patient-service, cliente.html", "Alto", "G", "H2 · LGPD art. 8 §5"),
    M("IA-54", "Prontuário 4D: a evolução da boca em linha do tempo",
      "Odontograma versionado que reproduz a evolução da boca do paciente ao longo dos anos como "
      "uma animação, com o diff entre dois momentos. Além de instrumento clínico, é a peça de "
      "conversa mais persuasiva possível sobre a importância da prevenção.",
      "prontuário", "Alto", "G", "H3"),
    M("IA-55", "Compra coletiva entre clínicas da plataforma",
      "Agregar a demanda prevista de insumos de várias clínicas e negociar em bloco com "
      "distribuidores. Uma clínica de dois dentistas não tem poder de compra; quinhentas "
      "clínicas juntas têm — e o ganho é dividido com elas. Vira efeito de rede difícil de "
      "copiar e forte motivo de permanência.",
      "inventory-service, novo módulo", "Alto", "G", "H3 · escala mínima"),
    M("IA-56", "Índice de saúde bucal da comunidade",
      "Devolver ao município e à universidade, em dado agregado e anonimizado, o panorama "
      "epidemiológico que a rede de clínicas produz — prevalência por região, faixa etária e "
      "acesso. Nenhum sistema de gestão devolve valor público a partir do dado que coleta.",
      "analytics, novo módulo", "Alto", "G", "H3 · ST-60, comitê de ética"),
    M("IA-57", "Agenda que considera o deslocamento do paciente",
      "Usar a plataforma de mapas já prevista como ator externo do projeto para propor horários "
      "compatíveis com o trânsito e a origem do paciente, e antecipar atraso previsível. Falta "
      "por trânsito é tratada hoje como imprevisto; é previsível.",
      "agenda, Google Maps", "Alto", "M", "H2"),
    M("IA-58", "Painel de equidade de acesso",
      "Mostrar à clínica quem está ficando para trás — por bairro, faixa etária ou condição — e "
      "sugerir ação corretiva. Conecta a operação diária às metas de equidade tratadas no "
      "Documento 04 e é insumo de contrato com o poder público.",
      "analytics", "Médio", "G", "H3 · ODS 3 e 10"),
    M("IA-59", "Custo de IA visível por clínica",
      "Mostrar ao assinante quanto de processamento de IA ele consumiu e o que aquilo produziu "
      "de resultado. Software com IA costuma esconder esse custo; expor cria confiança e "
      "sustenta a conversa de plano por consumo do Documento 05.",
      "ai-gateway, financeiro", "Médio", "M", "H2 · ST-61"),
    M("IA-60", "Modo contingência total",
      "Quando falta internet ou energia, o dispositivo local mantém a agenda do dia, o cadastro "
      "e o registro de atendimento, sincronizando depois com resolução de conflito explícita. "
      "Concorrente em nuvem simplesmente para — e a clínica atende no papel.",
      "PWA, mobile/", "Alto", "G", "H2 · ST-54"),
    M("IA-61", "Simulação visual do resultado, com marca obrigatória",
      "Gerar previsão visual do resultado estético a partir de foto, sempre com marca d'água "
      "indicando simulação, aviso de que não é promessa de resultado e registro de que o "
      "paciente foi informado. A tecnologia existe; o diferencial é fazê-la com o cuidado ético "
      "que o Código de Ética Odontológica exige em publicidade.",
      "prontuário", "Médio", "G", "H3 · CFO, publicidade"),
    M("IA-62", "Contrato vivo de plano de tratamento",
      "O plano aceito vira um acordo acompanhável pelas duas partes: etapas concluídas, valores "
      "pagos, prazos, o que falta e o efeito clínico de atrasar — assinado digitalmente e "
      "atualizado sozinho. Substitui o orçamento em PDF que ninguém revisita.",
      "financeiro, prontuário", "Alto", "G", "H3 · IA-48"),
], larg_alvo=36 * 2.83, larg_extra=29 * 2.83)

# ─── 11 ──────────────────────────────────────────────────────────────────────
doc.secao("Governança de IA e conformidade",
          "O que precisa existir antes do primeiro modelo entrar em produção")

doc.melhorias([
    M("IA-63", "Comitê de IA responsável e política pública",
      "Instituir avaliação obrigatória de cada funcionalidade de IA antes do lançamento — "
      "finalidade, base legal, risco, população afetada, plano de monitoramento — e publicar a "
      "política. Transforma conformidade em argumento de venda para clínica que teme IA.",
      "governança", "Alto", "M", "Pré-requisito de tudo nesta lista"),
], larg_alvo=36 * 2.83, larg_extra=29 * 2.83)

doc.texto(
    "Além do item acima, seis controles de engenharia são condição de entrada para qualquer "
    "funcionalidade deste documento. Eles não são funcionalidades vendáveis, e por isso não "
    "recebem numeração no backlog de produto — são requisitos de plataforma, detalhados no "
    "Documento 01.")
doc.lista([
    "<b>Rastreabilidade completa.</b> Toda saída de IA registra modelo, versão, entrada "
    "pseudonimizada, saída, custo e quem confirmou — na trilha append-only do audit-service.",
    "<b>Redação de dados pessoais antes do envio.</b> Nenhum identificador direto sai da "
    "plataforma em direção a um provedor de modelo (ST-61).",
    "<b>Avaliação contínua com conjunto de referência.</b> Métricas de qualidade por versão, "
    "com bloqueio automático de promoção quando houver regressão.",
    "<b>Detecção de viés por subgrupo.</b> Verificar se o modelo de previsão de falta penaliza "
    "sistematicamente pacientes de determinada região ou faixa de renda — o que transformaria "
    "uma otimização de agenda em discriminação algorítmica.",
    "<b>Confirmação humana registrada.</b> Nenhuma sugestão clínica ou financeira entra em "
    "registro oficial sem aceite explícito e identificado.",
    "<b>Desligamento por funcionalidade.</b> A clínica pode desativar qualquer recurso de IA "
    "individualmente, sem perder o restante do sistema.",
])

# ─── 12 ──────────────────────────────────────────────────────────────────────
doc.secao("Anexos", "Priorização, regulação e validação de ineditismo")

doc.subsecao("Anexo A — Matriz de esforço e diferenciação")
doc.texto(
    "Leitura sugerida: começar pelo quadrante de alto valor e baixo esforço (as vitórias que "
    "financiam o resto), manter no máximo duas apostas de H3 em paralelo e nunca iniciar H2 ou "
    "H3 sem os pré-requisitos de plataforma do Documento 01 concluídos.")
doc.tabela(
    ["Quadrante", "Funcionalidades", "Recomendação"],
    [
        ["Alto valor · esforço baixo ou médio\n(fazer primeiro)",
         "IA-01, IA-04, IA-05, IA-06, IA-15, IA-16, IA-18, IA-27, IA-32, IA-35, IA-37, IA-38, "
         "IA-43, IA-47, IA-57",
         "Formam o pacote de IA do MVP comercial: usam dado que a clínica já gera, não dependem "
         "de escala e produzem efeito mensurável em receita já no primeiro trimestre."],
        ["Alto valor · esforço alto\n(planejar com cuidado)",
         "IA-02, IA-03, IA-09, IA-11, IA-17, IA-19, IA-20, IA-22, IA-28, IA-29, IA-31, IA-40, "
         "IA-44, IA-48, IA-52, IA-53, IA-60",
         "São o que sustenta o preço praticado no plano Clínica. Exigem a plataforma de dados "
         "e devem entrar de uma em uma, com métrica de adoção antes da próxima."],
        ["Aposta de longo prazo\n(H3)",
         "IA-10, IA-12, IA-13, IA-21, IA-23, IA-34, IA-46, IA-49, IA-51, IA-54, IA-55, IA-56, "
         "IA-58, IA-61, IA-62",
         "Dependem de escala, parceria, validação clínica ou aprovação regulatória. Entram no "
         "plano como visão de produto, nunca como promessa em contrato ou material de venda."],
        ["Melhoria incremental\n(oportunista)",
         "IA-07, IA-08, IA-14, IA-24, IA-25, IA-26, IA-30, IA-33, IA-36, IA-39, IA-41, IA-42, "
         "IA-45, IA-50, IA-59",
         "Boa relação custo-benefício quando encaixadas em trabalho já planejado na mesma área "
         "do produto."],
    ],
    larguras=[38 * 2.83, 60 * 2.83, 75 * 2.83],
)

doc.subsecao("Anexo B — Requisitos regulatórios por tipo de funcionalidade")
doc.tabela(
    ["Tipo de funcionalidade", "Enquadramento a avaliar", "Consequência prática"],
    [
        ["Assistente de texto e organização\n(IA-01, IA-04, IA-06, IA-45)",
         "Não é dispositivo médico; incide LGPD por tratar dado sensível de saúde.",
         "Base legal definida, registro de tratamento e revisão humana. Caminho mais rápido "
         "para produção."],
        ["Triagem e priorização\n(IA-09, IA-15, IA-31)",
         "Zona cinzenta: prioriza fila humana sem afirmar achado clínico.",
         "Documentar explicitamente que a saída não é diagnóstico, manter decisão humana e "
         "consultar assessoria regulatória antes do lançamento."],
        ["Sinalização em imagem clínica\n(IA-10, IA-11, IA-12)",
         "Alta probabilidade de enquadramento como dispositivo médico (software com finalidade "
         "diagnóstica).",
         "Exige projeto próprio de regularização sanitária, evidência clínica e sistema de "
         "gestão da qualidade. Não lançar sem parecer formal."],
        ["Atendimento remoto ao paciente\n(IA-28, IA-29, IA-51)",
         "Sujeito às normas do Conselho Federal de Odontologia sobre teleodontologia.",
         "Delimitar claramente o que é administrativo (permitido) e o que é ato clínico "
         "(privativo do profissional habilitado)."],
        ["Simulação estética\n(IA-61)",
         "Código de Ética Odontológica, capítulo de publicidade e comunicação.",
         "Marca d'água obrigatória, aviso de que não constitui promessa de resultado e registro "
         "do consentimento informado."],
        ["Uso secundário de dados\n(IA-56, IA-58, treino de modelos)",
         "LGPD artigos 7, 11 e 12; pesquisa com dado de saúde pode exigir comitê de ética.",
         "Anonimização efetiva comprovada (ST-60), base legal própria e transparência ativa ao "
         "titular."],
    ],
    larguras=[45 * 2.83, 58 * 2.83, 70 * 2.83],
)

doc.subsecao("Anexo C — Como validar a alegação de ineditismo")
doc.texto(
    "A Seção 10 afirma que não encontramos equivalente às onze funcionalidades listadas. Essa "
    "afirmação é honesta sobre o que é — uma avaliação de repertório — e não substitui "
    "verificação formal. Antes de qualquer uso comercial, publicitário ou acadêmico da alegação "
    "de ineditismo, executar os cinco passos abaixo e registrar as evidências com data.")
doc.lista([
    "<b>Levantamento de concorrentes.</b> Mapear os sistemas de gestão odontológica ativos no "
    "Brasil e registrar, com captura de tela datada, a lista de funcionalidades publicada por "
    "cada um.",
    "<b>Teste em avaliação gratuita.</b> Material de marketing não é evidência: assinar o "
    "período de teste dos principais concorrentes e verificar o comportamento real do produto.",
    "<b>Busca de anterioridade.</b> Consultar bases de patente (INPI e internacionais) e "
    "literatura acadêmica para os conceitos de maior originalidade — em especial IA-54, IA-55 e "
    "IA-56.",
    "<b>Entrevista com quem usa.</b> Perguntar a dentistas que já trocaram de sistema o que "
    "existia no anterior. Usuário conhece funcionalidade que o site do fornecedor não destaca.",
    "<b>Registro datado e revisão periódica.</b> Guardar a evidência com data e refazer a "
    "verificação a cada seis meses: mercado de software muda, e uma alegação verdadeira hoje "
    "pode ficar falsa — e legalmente arriscada — em um ano.",
])

doc.nota_metodologica(
    "<b>Método e limites.</b> As funcionalidades foram derivadas do domínio de negócio descrito "
    "no README do projeto (atores, problemas diagnosticados e benefícios esperados), da "
    "arquitetura implementada em <code>services/</code> e das referências acadêmicas já citadas "
    "pelo próprio projeto sobre robótica e inteligência artificial em odontologia. "
    "<b>A classificação em H1, H2 e H3 e as estimativas de esforço são projeções de "
    "planejamento</b>, não compromissos. Os enquadramentos regulatórios do Anexo B são "
    "orientações de direção, elaboradas a partir dos princípios gerais aplicáveis, e "
    "<b>não constituem parecer jurídico</b>: antes do lançamento de qualquer funcionalidade das "
    "faixas de triagem, imagem clínica ou atendimento remoto, é obrigatória consulta a "
    "assessoria especializada em regulação sanitária e ao Conselho Regional de Odontologia. "
    "A afirmação de ineditismo da Seção 10 está expressamente condicionada à verificação "
    "descrita no Anexo C.")

doc.build()
