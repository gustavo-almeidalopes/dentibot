# -*- coding: utf-8 -*-
"""Documento 05 — Plano de Negócios do DentiBot, do MVP ao empreendimento."""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dentibot_doc import AMARELO, AZUL, LARANJA, VERDE, VERMELHO, Documento, Melhoria as M  # noqa: E402
from modelo_negocio import (  # noqa: E402
    ACELERADO, BASE, BOOTSTRAP, CENARIOS, brl, mes_ponto_equilibrio, num,
    painel_cenarios, pct, pior_caixa, resumo_trimestral,
)

SAIDA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                     "05-Plano-de-Negocios.pdf")

P = BASE
_boot_caixa, _boot_mes = pior_caixa(BOOTSTRAP)
_boot_equilibrio = mes_ponto_equilibrio(BOOTSTRAP)

doc = Documento(
    caminho=SAIDA,
    numero="05",
    titulo="Plano de Negócios",
    subtitulo="Do MVP ao empreendimento real — mercado, modelo de receita, economia unitária, "
              "três cenários financeiros e 56 iniciativas para executar o plano.",
    resumo="",
    cor=AMARELO,
    rotulo="Estratégia e Negócios",
    col_extra="Fase / prazo",
)

doc.capa()

doc.resumo_executivo([
    "O DentiBot é um sistema de gestão para clínicas odontológicas de pequeno e médio porte, "
    "hoje um projeto acadêmico da UNICID com arquitetura de microsserviços implementada, "
    "landing page comercial no ar e planos publicados a R$ 97 e R$ 197 por mês. Este documento "
    "trata da pergunta seguinte: <b>o que precisa acontecer para que isso vire um negócio que "
    "se sustenta</b> — e quanto custa cada caminho possível.",

    f"A tese é de nicho e de eficiência de capital, não de conquista de mercado. O software "
    f"odontológico brasileiro já tem participantes estabelecidos e bem capitalizados; competir "
    f"com eles pelo mesmo cliente e com o mesmo produto é uma disputa que uma equipe pequena "
    f"perde. O DentiBot ataca a faixa que os sistemas completos atendem mal por serem caros e "
    f"complexos demais: o dentista autônomo e a clínica de até oito profissionais. Com preço "
    f"médio de {brl(P.arpu, 2)} por mês no mix esperado e custo variável de "
    f"{brl(P.custo_variavel_mensal, 2)} por cliente, a margem bruta modelada é de "
    f"{pct(P.margem_bruta)} — patamar que sustenta um negócio de assinatura desde que a "
    f"aquisição seja barata.",

    f"A modelagem financeira apresenta três cenários, e a recomendação é explícita. O cenário "
    f"<b>Bootstrap</b> chega a {num(BOOTSTRAP.marcos_clientes[36])} clínicas em 36 meses, atinge "
    f"resultado mensal positivo no mês {_boot_equilibrio} e exige capital acumulado de "
    f"{brl(abs(_boot_caixa))} — valor compatível com fomento acadêmico, incubadora, edital de "
    f"inovação ou receita reinvestida, sem diluição societária. O cenário <b>Base</b> chega a "
    f"{num(P.marcos_clientes[36])} clínicas e {brl(P.arpu * P.marcos_clientes[36] * 12)} de "
    f"receita anualizada, mas exige {brl(abs(pior_caixa(BASE)[0]))} e não atinge equilíbrio "
    f"dentro da janela. O cenário <b>Acelerado</b> chega a "
    f"{num(ACELERADO.marcos_clientes[36])} clínicas ao custo de "
    f"{brl(abs(pior_caixa(ACELERADO)[0]))} em capital externo.",

    "<b>A recomendação é começar pelo Bootstrap</b> e migrar para o Base apenas quando três "
    "condições estiverem provadas com dado real: retenção acima de 90% ao ano em uma coorte de "
    "pelo menos cinquenta clínicas, custo de aquisição estável abaixo do modelado e um canal de "
    "vendas que funcione sem depender do fundador. Captar antes disso é comprar crescimento com "
    "hipóteses não verificadas — a forma mais cara de descobrir que a hipótese estava errada.",

    "Existe um bloqueio anterior a tudo isso, tratado na Seção 08 e na iniciativa PN-31: "
    "<b>a titularidade da propriedade intelectual</b>. O projeto foi desenvolvido como trabalho "
    "de disciplina, por uma equipe de seis pessoas, em uma instituição de ensino. Nada neste "
    "plano pode ser executado com segurança jurídica antes que a propriedade do código, a "
    "participação de cada integrante e a posição da universidade estejam formalizadas por "
    "escrito. É a primeira coisa a resolver, custa pouco e, deixada para depois, inviabiliza "
    "qualquer captação ou venda futura.",
], destaques=[
    ("56", "iniciativas do plano"),
    (brl(P.arpu, 0), "receita média/cliente"),
    (f"{P.ltv_cac:.1f}x", "LTV sobre CAC"),
    (f"{P.payback_meses:.1f}", "meses de payback"),
    (f"mês {_boot_equilibrio}", "equilíbrio no Bootstrap"),
])

doc.sumario(extras=[
    "Anexo A — Premissas do modelo e onde validar cada uma",
    "Anexo B — Projeção trimestral do cenário Base",
    "Anexo C — Indicadores de acompanhamento",
    "Anexo D — Riscos e planos de mitigação",
])

# ─── 01 ──────────────────────────────────────────────────────────────────────
doc.secao("Problema, solução e proposta de valor",
          "O que o projeto já diagnosticou, traduzido em tese comercial")

doc.texto(
    "O README do projeto lista oito problemas do setor. Três deles constituem a oportunidade "
    "comercial e definem para quem o produto é feito; os demais são consequência. A tabela "
    "abaixo faz essa tradução, e é dela que decorre todo o posicionamento adotado.")

doc.tabela(
    ["Problema diagnosticado no projeto", "Tradução comercial", "O que o DentiBot oferece"],
    [
        ["Soluções digitais disponíveis são de alto custo, inviabilizando a adoção por clínicas "
         "menores.",
         "Existe uma faixa de mercado precificada para fora do produto adequado — e ela é "
         "grande, porque a maior parte dos consultórios brasileiros é pequena.",
         "Preço de entrada de R$ 97 por mês com funcionalidade essencial completa, sem "
         "fidelidade e sem custo de implantação."],
        ["Clínicas pequenas não têm recursos para treinamento em sistemas complexos.",
         "O custo real de um sistema não é a mensalidade: é a semana perdida aprendendo. Quem "
         "reduz esse custo ganha o cliente que já desistiu de outro sistema.",
         "Interface que se aprende usando, primeiro acesso guiado e migração de dados "
         "assistida — as prioridades do Documento 02."],
        ["Dentistas e secretárias perdem tempo em tarefas repetitivas; a ausência de integração "
         "entre agenda, prontuário, pagamento e estoque dificulta o fluxo.",
         "A dor é tempo, e tempo de dentista tem preço por hora conhecido. Isso permite "
         "argumentar retorno em vez de discutir mensalidade.",
         "Integração nativa entre os sete domínios e automação por IA das tarefas de maior "
         "frequência — as apostas H1 do Documento 03."],
    ],
    larguras=[52 * 2.83, 62 * 2.83, 59 * 2.83],
)

doc.destaque(
    "Proposta de valor em uma frase",
    "<b>O sistema de gestão que o dentista autônomo consegue pagar, aprender sozinho e usar no "
    "mesmo dia — e que trabalha para reduzir a cadeira vazia em vez de apenas registrar que ela "
    "ficou vazia.</b> A primeira metade da frase é o que abre a porta e determina o preço; a "
    "segunda é o que sustenta a permanência e justifica o aumento de plano ao longo do tempo.",
    cor=AMARELO)

# ─── 02 ──────────────────────────────────────────────────────────────────────
doc.secao("Dimensionamento de mercado",
          "TAM, SAM e SOM construídos a partir de premissas explícitas")

doc.texto(
    "Os números abaixo são <b>estimativas de ordem de grandeza para planejamento</b>, derivadas "
    "das premissas do Anexo A. Não são dados de pesquisa de mercado, e o Anexo A indica a fonte "
    "oficial que precisa confirmar cada premissa antes de qualquer uso externo do número. "
    "A metodologia é intencionalmente simples e auditável: quantidade de estabelecimentos "
    "multiplicada pela receita média anual por cliente no mix de planos esperado.")

doc.tabela(
    ["Camada", "Definição adotada", "Estabelecimentos", "Receita potencial/ano"],
    [
        ["TAM", "Todos os estabelecimentos odontológicos privados do Brasil.",
         num(P.tam_estabelecimentos), brl(P.tam_anual)],
        ["SAM", f"Recorte de pequeno e médio porte ({pct(P.fatia_pequeno_medio_porte, 0)} do "
                f"TAM), com perfil e capacidade de adotar um sistema em nuvem.",
         num(P.sam_estabelecimentos), brl(P.sam_anual)],
        ["SOM", f"Meta de penetração de {pct(P.penetracao_alvo_5_anos, 0)} do SAM em cinco anos.",
         num(P.som_estabelecimentos), brl(P.som_anual)],
    ],
    larguras=[16 * 2.83, 92 * 2.83, 30 * 2.83, 35 * 2.83],
    alinhar_centro=(2, 3),
)

doc.texto(
    f"A leitura correta desses números não é \"o mercado vale {brl(P.tam_anual)}\". É esta: "
    f"<b>capturar {pct(P.penetracao_alvo_5_anos, 0)} do segmento acessível constrói um negócio "
    f"de aproximadamente {brl(P.som_anual)} de receita recorrente anual</b> — pequeno para um "
    f"fundo de risco, mas suficiente para sustentar uma equipe de dez a quinze pessoas com "
    f"margem saudável e independência. Essa é a ambição declarada deste plano, e ela é "
    f"deliberada: um negócio que precisa de {pct(0.20, 0)} do mercado para funcionar tem uma "
    f"tese frágil; um que funciona com {pct(P.penetracao_alvo_5_anos, 0)} tem uma tese robusta.")

doc.subsecao("Concorrência e posicionamento")
doc.texto(
    "O mercado brasileiro de software odontológico tem participantes estabelecidos, alguns com "
    "mais de uma década de operação e base instalada relevante. O levantamento formal desses "
    "concorrentes — funcionalidades, preço praticado, contrato e posicionamento — é a iniciativa "
    "PN-09 e precisa ser feito com evidência datada, não por impressão. A tabela abaixo trabalha "
    "com <b>arquétipos de posicionamento</b>, que é o que se pode afirmar com honestidade antes "
    "desse levantamento.")
doc.tabela(
    ["Arquétipo", "Como compete", "Vulnerabilidade que o DentiBot explora"],
    [
        ["Suíte completa consolidada",
         "Amplitude funcional, base instalada, integração com convênios e força de vendas.",
         "Preço e complexidade. Cobra por módulo e exige implantação e treinamento — barreira "
         "exata que exclui o consultório de um ou dois profissionais."],
        ["Plataforma multiespecialidade",
         "Atende várias áreas da saúde com o mesmo produto, ganhando escala.",
         "Generalidade. Odontograma, controle de esterilização e faturamento TISS odontológico "
         "raramente são profundos em produto que atende todas as especialidades."],
        ["Sistema instalado localmente",
         "Preço baixo, licença única, familiaridade de longa data.",
         "Ausência de nuvem, de móvel, de LGPD por desenho e de comunicação integrada com o "
         "paciente. Base envelhecendo e sem caminho de atualização."],
        ["Agenda ou marketplace de pacientes",
         "Aquisição de pacientes e agendamento online.",
         "Não é sistema de gestão: não cobre prontuário, estoque, financeiro nem auditoria. "
         "É complemento, e por isso candidato natural a integração em vez de disputa."],
        ["Planilha, agenda de papel e WhatsApp",
         "Custo zero e nenhuma curva de aprendizado.",
         "É o concorrente real da maior parte do público-alvo. Perde em segurança, em LGPD e em "
         "memória do negócio — e é contra ele que a mensagem comercial deve ser escrita."],
    ],
    larguras=[36 * 2.83, 55 * 2.83, 82 * 2.83],
)

# ─── 03 ──────────────────────────────────────────────────────────────────────
doc.secao("Modelo de receita e economia unitária",
          "Como o dinheiro entra e a que custo")

doc.subsecao("Estrutura de planos")
doc.tabela(
    ["Plano", "Preço mensal", "Anual", "Público", "Papel no modelo"],
    [
        ["Solo", brl(P.preco_solo), brl(P.preco_solo * 10),
         "Dentista autônomo, 1 profissional",
         "Porta de entrada e maior volume da base; sustenta o discurso de acesso."],
        ["Clínica", brl(P.preco_clinica), brl(P.preco_clinica * 10),
         "Clínica de até 8 profissionais",
         "Principal fonte de margem e destino natural da expansão de conta."],
        ["Enterprise", "Sob consulta", "Sob consulta",
         "Rede com mais de 8 dentistas",
         "Receita alta por conta e validação institucional; exige atendimento dedicado."],
        ["Faixa social", "Diferenciada", "Diferenciada",
         "Clínica-escola e serviço conveniado ao SUS",
         "Impacto (ODS-21 e ODS-55), presença acadêmica e credencial para edital público."],
    ],
    larguras=[20 * 2.83, 24 * 2.83, 20 * 2.83, 46 * 2.83, 63 * 2.83],
    alinhar_centro=(1, 2),
)
doc.texto(
    f"O plano anual embute dois meses de desconto — cerca de {pct(P.desconto_anual, 0)} — em "
    f"troca de caixa antecipado e de menor cancelamento. Para um negócio com necessidade de "
    f"capital como a modelada aqui, aumentar a fração de contratos anuais é a alavanca de caixa "
    f"mais barata que existe: não custa nada além de um desconto que já está anunciado.")

doc.subsecao("Fontes de receita além da assinatura")
doc.lista([
    "<b>Mensagens transacionais acima da franquia.</b> O plano Solo inclui 500 lembretes de "
    "WhatsApp por mês; o excedente é repassado com margem, o que alinha receita a uso real.",
    "<b>Consumo de IA acima da franquia.</b> Com o custo por clínica visível (IA-59), o "
    "excedente vira receita previsível em vez de prejuízo silencioso.",
    "<b>Migração de dados assistida.</b> Serviço pago e opcional que remove a maior objeção de "
    "quem já usa outro sistema — e que, por ser trabalhoso, deve ser cobrado.",
    "<b>Marketplace de insumos.</b> Comissão sobre a compra coletiva descrita em IA-55, "
    "disponível apenas com escala relevante de clínicas.",
    "<b>Licenciamento acadêmico.</b> Ambiente didático para instituições de ensino, com receita "
    "modesta mas alto valor de posicionamento e de formação de futuros clientes.",
])

doc.subsecao("Economia unitária")
doc.tabela(
    ["Indicador", "Valor modelado", "Leitura"],
    [
        ["Receita média por cliente (ARPU)", brl(P.arpu, 2) + " / mês",
         f"Mix de {pct(P.mix_solo, 0)} Solo, {pct(P.mix_clinica, 0)} Clínica e "
         f"{pct(P.mix_enterprise, 0)} Enterprise."],
        ["Custo variável por cliente", brl(P.custo_variavel_mensal, 2) + " / mês",
         "Infraestrutura, mensagens, processamento de IA e suporte proporcional."],
        ["Margem bruta", pct(P.margem_bruta),
         "Dentro da faixa saudável para software como serviço; a meta é elevá-la com "
         "eficiência de infraestrutura (ST-43, ODS-31)."],
        ["Custo de aquisição (CAC)", brl(P.cac),
         "Mistura de conteúdo, indicação e vendas diretas. É a premissa mais frágil do modelo e "
         "a primeira a ser medida com dado real."],
        ["Cancelamento mensal", pct(P.churn_mensal),
         f"Implica vida média de {P.vida_media_meses:.0f} meses. Cada ponto percentual a menos "
         f"tem efeito maior sobre o resultado que qualquer ganho de preço."],
        ["Valor do cliente (LTV)", brl(P.ltv),
         "Margem bruta acumulada ao longo da vida média do cliente."],
        ["LTV sobre CAC", f"{P.ltv_cac:.1f}x",
         "Acima de 3x, patamar considerado saudável. Confortável o bastante para absorver erro "
         "de estimativa nas premissas."],
        ["Retorno do CAC (payback)", f"{P.payback_meses:.1f} meses",
         "Abaixo de 12 meses, o que viabiliza crescer reinvestindo a própria receita — condição "
         "central do cenário Bootstrap."],
    ],
    larguras=[46 * 2.83, 34 * 2.83, 93 * 2.83],
    alinhar_centro=(1,),
)

doc.destaque(
    "A premissa que mais importa não é o preço — é o cancelamento",
    f"Com {pct(P.churn_mensal)} de cancelamento mensal, o cliente permanece em média "
    f"{P.vida_media_meses:.0f} meses. Se o cancelamento subir para 5%, a vida média cai para 20 "
    f"meses e o LTV encolhe cerca de {pct(1 - (1/0.05)/P.vida_media_meses, 0)} — o que sozinho "
    f"derruba a relação LTV/CAC de {P.ltv_cac:.1f}x para perto de {P.ltv * (0.7) / P.cac:.1f}x e "
    f"transforma um negócio saudável em um negócio que não paga a própria aquisição. "
    f"Por isso as iniciativas de retenção (PN-18 a PN-23) têm prioridade sobre as de aquisição "
    f"em todo o plano, e por isso a Fase 1 é medida por permanência, não por número de clientes.",
    cor=VERMELHO)

# ─── 04 ──────────────────────────────────────────────────────────────────────
doc.secao("Do MVP ao empreendimento",
          "Cinco fases, cada uma com uma pergunta a responder e um critério de saída")

doc.texto(
    "Cada fase existe para responder a uma pergunta específica. Avançar sem ter respondido é a "
    "forma mais comum de gastar capital construindo algo que ninguém quer — e o critério de "
    "saída existe justamente para tornar esse erro visível antes que ele seja caro.")

doc.tabela(
    ["Fase", "Período", "Pergunta a responder", "Critério objetivo de saída"],
    [
        ["F0 — Fundação", "Meses 1 a 4",
         "O produto pode ser usado com dados de paciente real sem risco inaceitável?",
         "Onda 1 do Documento 01 concluída (testes, TLS, rate limiting, CORS, segredos); "
         "propriedade intelectual formalizada; empresa constituída."],
        ["F1 — Piloto", "Meses 5 a 9",
         "Uma clínica real usa o sistema todos os dias e paga por ele?",
         f"{BOOTSTRAP.marcos_clientes[9]} clínicas pagantes, uso semanal acima de 80%, nenhum "
         f"cancelamento por falha do produto, retorno documentado por escrito."],
        ["F2 — Tração", "Meses 10 a 18",
         "A aquisição funciona sem depender do fundador e o cliente permanece?",
         f"CAC medido e estável, retenção anual acima de 90%, pelo menos um canal de aquisição "
         f"repetível, {BOOTSTRAP.marcos_clientes[18]} clínicas ou mais."],
        ["F3 — Escala", "Meses 19 a 30",
         "O modelo suporta crescer sem que o custo cresça na mesma proporção?",
         "Margem bruta estável ou crescente com base três vezes maior; suporte por cliente "
         "decrescente; equipe além dos fundadores operando com autonomia."],
        ["F4 — Empreendimento", "Meses 31 a 36 e adiante",
         "O negócio se sustenta sozinho e resiste à saída de qualquer indivíduo?",
         "Resultado mensal positivo, governança societária formal, dependência de pessoa-chave "
         "reduzida e decisão consciente entre permanecer independente ou captar."],
    ],
    larguras=[26 * 2.83, 20 * 2.83, 60 * 2.83, 67 * 2.83],
)

doc.subsecao("Escopo do MVP: o que entra e o que fica de fora")
doc.texto(
    "MVP não é a versão pequena de tudo — é a versão completa do essencial. O critério aplicado: "
    "entra o que impede a clínica de operar o dia inteiro dentro do sistema; fica de fora tudo "
    "que ela consegue contornar por mais alguns meses. Cada item excluído tem uma fase de "
    "destino, para que a exclusão seja adiamento e não abandono.")
doc.tabela(
    ["Entra no MVP (F0 e F1)", "Fica para depois", "Fase de destino"],
    [
        ["Cadastro de paciente, agenda, prontuário com odontograma, lembrete por WhatsApp, "
         "cobrança simples, controle básico de estoque, perfis de acesso, 2FA, trilha de "
         "auditoria e exportação de dados.",
         "Faturamento TISS para convênios, relatórios gerenciais avançados, aplicativo móvel "
         "nativo, integração com laboratório de prótese e marketplace de insumos.",
         "TISS e relatórios em F2; móvel e integrações em F3; marketplace em F4."],
        ["As apostas de IA de horizonte H1 do Documento 03: previsão de falta, confirmação "
         "proporcional ao risco, preenchimento de vaga cancelada, radar de abandono e "
         "verificação de consistência.",
         "Transcrição ambiente, visão computacional, recepcionista virtual autônoma e simulação "
         "estética.",
         "Recepcionista virtual em F2; transcrição em F3; visão computacional em F4, condicionada "
         "a parecer regulatório."],
    ],
    larguras=[68 * 2.83, 58 * 2.83, 47 * 2.83],
)

# ─── 05 ──────────────────────────────────────────────────────────────────────
doc.secao("Projeção financeira e escolha de cenário",
          "Quanto custa cada velocidade de crescimento")

doc.texto(
    "Os três cenários usam a mesma economia unitária e diferem apenas em velocidade de "
    "crescimento, estrutura de custo fixo e custo de aquisição. Todos os valores são calculados "
    "a partir das premissas do Anexo A pelo modelo em <code>modelo_negocio.py</code>, versionado "
    "junto a este documento: alterar uma premissa e reexecutar o gerador atualiza o documento "
    "inteiro, o que evita a divergência típica entre o texto e a planilha.")

_painel = painel_cenarios()
doc.tabela(
    ["Cenário", "Clientes em 36m", "Receita anualizada", "CAC", "LTV/CAC", "Capital necessário",
     "Equilíbrio mensal"],
    [[c["cenario"], num(c["clientes_36m"]), brl(c["arr_36m"]), brl(c["cac"]),
      f"{c['ltv_cac']:.1f}x", brl(c["capital_necessario"]),
      f"mês {c['equilibrio']}" if c["equilibrio"] else "após o mês 36"]
     for c in _painel],
    larguras=[22 * 2.83, 22 * 2.83, 30 * 2.83, 18 * 2.83, 18 * 2.83, 32 * 2.83, 31 * 2.83],
    alinhar_centro=(1, 2, 3, 4, 5, 6),
)

doc.destaque(
    "Recomendação: começar pelo Bootstrap",
    f"O cenário Bootstrap é o único dos três que atinge resultado mensal positivo dentro da "
    f"janela de 36 meses (mês {_boot_equilibrio}) e o único cuja necessidade de capital — "
    f"{brl(abs(_boot_caixa))} acumulados — é compatível com fomento acadêmico, incubadora "
    f"universitária, edital de inovação ou receita reinvestida, <b>sem diluição societária</b>. "
    f"Ele entrega uma base menor, de {num(BOOTSTRAP.marcos_clientes[36])} clínicas, mas entrega "
    f"um negócio que existe sem depender de terceiros. Migrar para o cenário Base depois, com "
    f"retenção e CAC já provados por dado real, custa muito menos participação societária do que "
    f"captar agora com hipóteses. A pressa é o item mais caro de qualquer plano de negócios.",
    cor=VERDE)

doc.subsecao("Como ler a necessidade de capital")
doc.lista([
    f"<b>Não é prejuízo, é investimento em aquisição.</b> No cenário Base, a maior parte do "
    f"consumo de caixa é custo de aquisição de clientes que se pagam em "
    f"{P.payback_meses:.1f} meses. Interromper a aquisição interrompe o consumo de caixa quase "
    f"imediatamente — o negócio não é estruturalmente deficitário.",
    "<b>O caixa piora antes de melhorar.</b> Assinatura mensal recebe o valor do cliente ao "
    "longo de dois anos e paga a aquisição à vista. Crescer mais rápido piora o caixa no curto "
    "prazo mesmo quando cada cliente é lucrativo — é a matemática normal do modelo, não um sinal "
    "de erro.",
    "<b>O contrato anual é a alavanca mais barata.</b> Cada cliente que migra para o plano anual "
    "antecipa dez meses de receita e reduz o cancelamento. Elevar a fração de anuais é a forma "
    "de melhorar o caixa que não exige capital nem novo cliente.",
    "<b>A faixa social não é caridade contabilizada como prejuízo.</b> Ela abre porta para "
    "contrato público, dá credencial em edital e forma dentistas que se tornam clientes ao abrir "
    "o próprio consultório. Deve ter orçamento definido e ser medida como investimento.",
])

# ─── 06 ──────────────────────────────────────────────────────────────────────
doc.secao("Iniciativas do plano de negócios",
          "56 ações concretas, da constituição da empresa à expansão")
doc.legenda_backlog()

doc.subsecao("Produto e MVP")
doc.melhorias([
    M("PN-01", "Congelar o escopo do MVP por escrito",
      "Documentar e assinar o que entra e o que não entra no MVP, com a fase de destino de cada "
      "exclusão. Escopo não escrito cresce sozinho, e escopo que cresce sozinho atrasa a "
      "primeira receita indefinidamente.",
      "produto", "Alto", "P", "F0 · mês 1"),
    M("PN-02", "Executar a Onda 1 do Documento 01",
      "Testes automatizados, TLS efetivo, rate limiting, CORS restrito e segredos sem valor "
      "padrão. Esta é a condição de segurança para atender o primeiro paciente real — não é "
      "negociável nem adiável.",
      "engenharia", "Alto", "G", "F0 · meses 1 a 4"),
    M("PN-03", "Instrumentar o produto desde o primeiro dia",
      "Medir ativação, uso semanal, abandono por tela e erro. Começar a medir depois de ter "
      "clientes significa perder exatamente a coorte mais informativa: a primeira.",
      "produto, analytics", "Alto", "M", "F0 · mês 3"),
    M("PN-04", "Migração de dados a partir dos sistemas concorrentes",
      "Importadores para os formatos de exportação mais comuns e para planilha. É a maior "
      "objeção de quem já usa outro sistema, e resolvê-la converte o concorrente em fonte de "
      "clientes.",
      "produto", "Alto", "G", "F1 · meses 6 a 9"),
    M("PN-05", "Primeiro acesso guiado e dados de demonstração",
      "O período de teste de 30 dias anunciado na landing page só funciona se o valor aparecer "
      "na primeira sessão. Assistente de configuração e base de exemplo removível.",
      "produto", "Alto", "M", "F1 · mês 5"),
    M("PN-06", "Entregar as apostas H1 de IA",
      "Previsão de falta, confirmação por risco e radar de abandono são o que diferencia o "
      "produto no material comercial e o que produz retorno mensurável já no primeiro trimestre "
      "de uso.",
      "produto", "Alto", "G", "F2 · meses 10 a 15"),
    M("PN-07", "Ciclo de retorno estruturado com clínicas piloto",
      "Conversa quinzenal com cada clínica do piloto, com registro e priorização pública. Dez "
      "clientes que falam valem mais que mil que apenas usam — e depois da Fase 1 esse acesso "
      "direto desaparece.",
      "produto", "Alto", "M", "F1 · contínuo"),
    M("PN-08", "Publicar página de estado do serviço",
      "Disponibilidade e incidentes visíveis publicamente. Barato de fazer, difícil de "
      "imitar em credibilidade, e frequentemente exigido por clínica maior antes de assinar.",
      "engenharia", "Médio", "P", "F2 · mês 12"),
], larg_alvo=24 * 2.83, larg_extra=24 * 2.83)

doc.subsecao("Comercial e entrada no mercado")
doc.melhorias([
    M("PN-09", "Levantamento formal de concorrentes",
      "Assinar o teste gratuito dos principais participantes, registrar funcionalidade, preço, "
      "contrato e experiência com evidência datada. Sem isso, todo argumento comercial é "
      "suposição — e o Anexo C do Documento 03 depende deste levantamento.",
      "comercial", "Alto", "M", "F0 · mês 2"),
    M("PN-10", "Definir e escrever o cliente ideal",
      "Perfil explícito: consultório de 1 a 3 dentistas, com secretária, na Grande São Paulo, "
      "usando planilha ou sistema instalado localmente. Público mal definido produz mensagem "
      "genérica, que não converte ninguém.",
      "comercial", "Alto", "P", "F0 · mês 2"),
    M("PN-11", "Programa de indicação",
      "Dentista confia em dentista. Estruturar indicação com benefício para as duas partes é o "
      "canal de menor custo de aquisição disponível para este mercado e o mais compatível com "
      "o cenário Bootstrap.",
      "comercial", "Alto", "M", "F1 · mês 7"),
    M("PN-12", "Conteúdo de gestão para dentistas",
      "Material sobre precificação, redução de faltas e obrigações de LGPD na clínica. Atrai "
      "quem tem a dor antes de ele procurar sistema, e constrói autoridade que a propaganda paga "
      "não compra.",
      "marketing", "Alto", "M", "F1 · contínuo"),
    M("PN-13", "Presença em eventos e associações",
      "Congressos regionais, associações de especialidade e grupos de dentistas. Canal de custo "
      "baixo e conversão alta em mercado que decide por confiança e recomendação.",
      "comercial", "Médio", "M", "F2 · contínuo"),
    M("PN-14", "Parceria com clínicas-escola",
      "O estudante que aprende no DentiBot leva o sistema ao abrir o consultório. É aquisição de "
      "prazo longo e custo quase nulo, e casa com a origem acadêmica do projeto.",
      "comercial, ODS-55", "Alto", "M", "F2 · meses 12 a 18"),
    M("PN-15", "Processo de vendas documentado",
      "Roteiro, qualificação, demonstração padrão e tratamento de objeções escritos. É o que "
      "permite que a primeira contratação comercial produza resultado sem o fundador na sala.",
      "comercial", "Alto", "M", "F2 · mês 14"),
    M("PN-16", "Prova de retorno com número do próprio cliente",
      "Calculadora que mostra, com os dados da clínica, quanto ela perde em faltas e quanto o "
      "sistema custa. Transforma a conversa de despesa em conversa de retorno.",
      "comercial, produto", "Alto", "M", "F2 · mês 13"),
    M("PN-17", "Casos de sucesso documentados",
      "Três clínicas com número verificável de antes e depois, com autorização de uso. Prova "
      "social específica converte muito mais que depoimento genérico na landing page.",
      "marketing", "Alto", "M", "F2 · mês 16"),
], larg_alvo=26 * 2.83, larg_extra=24 * 2.83)

doc.subsecao("Retenção e sucesso do cliente")
doc.melhorias([
    M("PN-18", "Meta de ativação nos primeiros 14 dias",
      "Definir o marco que prevê permanência — provavelmente a primeira semana com agenda "
      "cheia registrada — e organizar todo o acompanhamento inicial em torno dele.",
      "sucesso do cliente", "Alto", "M", "F1 · mês 6"),
    M("PN-19", "Alerta antecipado de cancelamento",
      "Detectar queda de uso, chamado repetido e falha de pagamento antes do pedido formal de "
      "cancelamento. Reter é ordens de grandeza mais barato que adquirir.",
      "sucesso do cliente", "Alto", "M", "F2 · mês 15"),
    M("PN-20", "Entrevista obrigatória em todo cancelamento",
      "Conversar com quem sai, registrar o motivo de forma padronizada e revisar mensalmente. É "
      "a fonte de informação mais valiosa e mais desperdiçada de um negócio de assinatura.",
      "sucesso do cliente", "Alto", "P", "F1 · contínuo"),
    M("PN-21", "Acompanhamento periódico proativo",
      "Contato trimestral que revisa o uso e sugere o que a clínica não está aproveitando. "
      "Cliente que usa mais funcionalidades cancela menos, e a conversa gera oportunidade de "
      "expansão de plano.",
      "sucesso do cliente", "Alto", "M", "F2 · mês 12"),
    M("PN-22", "Caminho de expansão Solo para Clínica",
      "Tornar natural e visível a migração quando o consultório contrata o segundo profissional. "
      "Expandir conta existente é a receita mais barata que existe.",
      "produto, comercial", "Alto", "M", "F2 · mês 14"),
    M("PN-23", "Recuperação de pagamento recusado",
      "Nova tentativa automática, aviso com antecedência e atualização fácil do cartão. Parte "
      "relevante do cancelamento em assinatura é falha técnica de cobrança, não decisão do "
      "cliente.",
      "financeiro, produto", "Alto", "M", "F1 · mês 8"),
], larg_alvo=30 * 2.83, larg_extra=24 * 2.83)

doc.subsecao("Financeiro e precificação")
doc.melhorias([
    M("PN-24", "Meio de pagamento recorrente e PIX",
      "Cartão recorrente, boleto e PIX automático com conciliação. Sem cobrança recorrente "
      "confiável não existe negócio de assinatura, apenas venda avulsa repetida manualmente.",
      "financeiro", "Alto", "M", "F0 · mês 4"),
    M("PN-25", "Contabilidade e regime tributário definidos",
      "Escolher o regime adequado ao porte e à natureza da receita de software, com contador "
      "especializado. Decisão tributária errada no início custa caro e é trabalhosa de desfazer.",
      "financeiro, jurídico", "Alto", "P", "F0 · mês 2"),
    M("PN-26", "Painel de indicadores de assinatura",
      "MRR, cancelamento, CAC, LTV, retenção por coorte e caixa acompanhados mensalmente. "
      "Gerir assinatura por saldo bancário é gerir olhando pelo retrovisor.",
      "financeiro", "Alto", "M", "F1 · mês 9"),
    M("PN-27", "Revisão de preço com base em valor entregue",
      "Reavaliar preço anualmente com dado de uso e de retorno gerado. O preço atual foi definido "
      "antes de existir cliente pagante — é uma hipótese, e precisa ser tratada como tal.",
      "comercial", "Médio", "M", "F2 · mês 18"),
    M("PN-28", "Controle de custo unitário de infraestrutura",
      "Acompanhar custo de nuvem, mensagem e IA por clínica, com alerta de desvio. É o que "
      "protege a margem bruta modelada em um plano de R$ 97 por mês.",
      "engenharia, financeiro", "Alto", "M", "F1 · mês 8"),
    M("PN-29", "Incentivo estruturado ao plano anual",
      "Campanha ativa de migração para o anual, com o desconto já anunciado. Melhora caixa e "
      "reduz cancelamento sem custo adicional de aquisição.",
      "comercial, financeiro", "Alto", "P", "F2 · mês 12"),
    M("PN-30", "Reserva de caixa mínima definida",
      "Manter reserva equivalente a seis meses de custo fixo antes de acelerar aquisição. "
      "Regra simples que separa o negócio que atravessa um trimestre ruim do que não atravessa.",
      "financeiro", "Alto", "P", "F2 · contínuo"),
], larg_alvo=30 * 2.83, larg_extra=24 * 2.83)

doc.subsecao("Jurídico, regulatório e conformidade")
doc.melhorias([
    M("PN-31", "Resolver a titularidade da propriedade intelectual",
      "O projeto nasceu como trabalho de disciplina, com seis integrantes e vínculo "
      "institucional. Formalizar por escrito quem detém o código, qual a participação de cada "
      "pessoa e qual a posição da universidade. <b>É o bloqueio número um do plano</b>: sem "
      "isso não há captação, venda nem sócio novo possível.",
      "jurídico", "Alto", "M", "F0 · mês 1"),
    M("PN-32", "Constituir a empresa e o acordo de sócios",
      "Sociedade com objeto adequado, e acordo de sócios com prazo de aquisição de "
      "participação, regra de saída e resolução de impasse. Sociedade entre colegas de faculdade "
      "sem acordo escrito é a causa mais comum de morte de startup jovem.",
      "jurídico", "Alto", "M", "F0 · mês 3"),
    M("PN-33", "Contrato de tratamento de dados com as clínicas",
      "A clínica é a <b>controladora</b> dos dados do paciente; o DentiBot é <b>operador</b>. "
      "Essa distinção define responsabilidades e precisa estar no contrato, junto com "
      "subcontratação, prazo de retenção e notificação de incidente.",
      "jurídico", "Alto", "M", "F0 · mês 4"),
    M("PN-34", "Encarregado de dados e programa de LGPD",
      "Nomear encarregado, manter registro das operações de tratamento, definir base legal por "
      "finalidade e ter plano de resposta a incidente. Obrigação legal e, na prática, item de "
      "checklist em qualquer venda para cliente maior.",
      "jurídico", "Alto", "M", "F1 · mês 6"),
    M("PN-35", "Termos de uso e política de privacidade revisados",
      "A política já existe em <code>src/politicy/</code>; falta revisão jurídica para uso "
      "comercial, com limitação de responsabilidade, disponibilidade prometida e regra de "
      "encerramento com devolução de dados.",
      "jurídico", "Alto", "P", "F0 · mês 4"),
    M("PN-36", "Consulta prévia sobre o enquadramento das funções de IA",
      "Antes de lançar triagem por imagem ou atendimento remoto, obter parecer sobre "
      "enquadramento sanitário e sobre as normas do conselho profissional. Detalhado no Anexo B "
      "do Documento 03.",
      "jurídico, produto", "Alto", "M", "F2 · antes do lançamento"),
    M("PN-37", "Seguro de responsabilidade civil e cibernética",
      "Cobertura para incidente com dado de saúde e para falha do serviço. Custo previsível "
      "diante de um evento que, sem seguro, encerra a empresa.",
      "jurídico, financeiro", "Médio", "P", "F2 · mês 15"),
], larg_alvo=30 * 2.83, larg_extra=28 * 2.83)

doc.subsecao("Operação, pessoas e organização")
doc.melhorias([
    M("PN-38", "Suporte com prazo declarado e medido",
      "Canal único, prazo de resposta publicado e medição de satisfação. Suporte é a "
      "funcionalidade mais visível de um sistema de gestão — é onde o cliente decide se "
      "recomenda ou cancela.",
      "operação", "Alto", "M", "F1 · mês 5"),
    M("PN-39", "Base de conhecimento e autoatendimento",
      "Artigos e vídeos curtos para as dúvidas mais frequentes. Cada chamado evitado protege a "
      "margem de um plano de R$ 97 por mês, em que o custo de suporte é o maior risco unitário.",
      "operação", "Alto", "M", "F1 · mês 8"),
    M("PN-40", "Plantão para incidente crítico",
      "Escala e procedimento para o sistema fora do ar em horário de atendimento. Clínica parada "
      "perde receita naquele dia e não perdoa duas vezes.",
      "operação, engenharia", "Alto", "M", "F2 · mês 12"),
    M("PN-41", "Procedimento de encerramento de contrato",
      "Exportação completa, prazo de retenção e exclusão definitiva documentados. Sair fácil é "
      "argumento de venda, não risco — e é obrigação sob a LGPD.",
      "operação, jurídico", "Médio", "P", "F1 · mês 9"),
    M("PN-42", "Rotina de revisão semanal de indicadores",
      "Reunião curta e fixa sobre os números do Anexo C. Ritmo de gestão é o que diferencia "
      "projeto de empresa, e custa apenas disciplina.",
      "gestão", "Alto", "P", "F1 · contínuo"),
    M("PN-43", "Documentação operacional e continuidade",
      "Registrar como cada processo funciona, para que a saída de uma pessoa não pare a "
      "operação. Em equipe de origem acadêmica, com rotatividade natural entre semestres, isso "
      "é risco concreto e previsível.",
      "gestão", "Alto", "M", "F1 · contínuo"),
    M("PN-44", "Definir papéis e responsabilidades",
      "O README já distribui funções entre líder, analistas, desenvolvedores, testadores, "
      "projetistas de banco e documentador. Traduzir isso em papéis de empresa, com decisão e "
      "responsabilidade claras.",
      "gestão", "Alto", "P", "F0 · mês 2"),
    M("PN-45", "Plano de dedicação e remuneração dos fundadores",
      "Definir quem se dedica em tempo integral, quando e com qual remuneração. Ambiguidade "
      "sobre dedicação é a origem mais comum de conflito societário no primeiro ano.",
      "gestão, jurídico", "Alto", "P", "F0 · mês 3"),
    M("PN-46", "Primeiras contratações planejadas",
      "Sequência recomendada: pessoa de suporte e sucesso do cliente antes de vendedor, e "
      "vendedor antes de mais um desenvolvedor. Contratar vendas antes de o produto reter é "
      "encher um balde furado.",
      "gestão", "Alto", "M", "F2 · meses 12 a 18"),
    M("PN-47", "Programa de estágio com a universidade",
      "Formalizar a entrada de estudantes com mentoria e projeto definido. Custo baixo, "
      "contribuição real e alinhamento com ODS-54 e ODS-11.",
      "gestão, UNICID", "Médio", "P", "F2 · mês 12"),
    M("PN-48", "Cultura e forma de trabalho escritas",
      "Como se decide, como se revisa código, como se trata incidente e como se dá retorno. "
      "Escrever cedo é barato; corrigir cultura depois de vinte pessoas, não.",
      "gestão", "Médio", "P", "F2 · mês 15"),
    M("PN-49", "Conselho consultivo",
      "Três a cinco pessoas com experiência em saúde, software como serviço e jurídico, em "
      "reunião trimestral. Acesso a experiência que a equipe não tem, a custo baixo e sem "
      "diluição.",
      "gestão", "Médio", "P", "F2 · mês 18"),
], larg_alvo=28 * 2.83, larg_extra=26 * 2.83)

doc.subsecao("Capital, parcerias e expansão")
doc.melhorias([
    M("PN-50", "Mapear fomento não diluidor",
      "FAPESP PIPE, FINEP, editais de inovação, incubadora universitária e programas de fomento "
      "regional. É a fonte de capital mais adequada ao cenário Bootstrap e a mais compatível com "
      "a origem acadêmica do projeto.",
      "captação", "Alto", "M", "F0 · mês 3"),
    M("PN-51", "Material de captação pronto e atualizado",
      "Apresentação, modelo financeiro e sala de dados organizados desde cedo. Oportunidade de "
      "captação aparece com prazo curto; quem monta o material depois da oportunidade a perde.",
      "captação", "Médio", "M", "F2 · mês 18"),
    M("PN-52", "Critérios objetivos para decidir captar",
      "Definir por escrito, antes da conversa com investidor, o que precisa estar provado: "
      "retenção, CAC estável e canal repetível. Captar sem esses critérios é vender participação "
      "no momento de menor valorização possível.",
      "captação, gestão", "Alto", "P", "F2 · mês 18"),
    M("PN-53", "Parceria com contabilidade e emissão de nota",
      "Integração com serviço de nota fiscal e com escritório contábil especializado em saúde. "
      "Resolve dor real e cria canal de indicação bidirecional.",
      "parcerias", "Médio", "M", "F2 · mês 15"),
    M("PN-54", "Convênio com secretaria municipal de saúde",
      "Contrato com serviço público usando as credenciais de impacto do Documento 04. Receita "
      "previsível, validação institucional e alinhamento com ODS-55.",
      "parcerias", "Alto", "G", "F3 · meses 19 a 30"),
    M("PN-55", "Expansão geográfica planejada",
      "Consolidar a Grande São Paulo antes de abrir novas praças, e expandir por região "
      "metropolitana com apoio de indicação local. Expansão prematura dilui suporte e destrói a "
      "qualidade que sustenta a retenção.",
      "comercial", "Alto", "M", "F3 · meses 19 a 30"),
    M("PN-56", "Avaliar adjacências antes de novos mercados",
      "Fisioterapia, psicologia e outras clínicas pequenas compartilham grande parte do fluxo de "
      "trabalho. Avaliar essa expansão só depois de liderar o nicho odontológico — nicho "
      "abandonado cedo demais é a forma mais comum de perder os dois mercados.",
      "estratégia", "Médio", "G", "F4 · mês 36 e adiante"),
], larg_alvo=26 * 2.83, larg_extra=28 * 2.83)

# ─── 07 ──────────────────────────────────────────────────────────────────────
doc.secao("Anexos", "Premissas, projeção, indicadores e riscos")

doc.subsecao("Anexo A — Premissas do modelo e onde validar cada uma")
doc.texto(
    "Toda premissa abaixo é uma estimativa de planejamento. A coluna de validação indica a fonte "
    "que precisa confirmá-la antes que o número derivado saia de um documento interno. "
    "As premissas vivem em <code>docs/generators/modelo_negocio.py</code>.")
doc.tabela(
    ["Premissa", "Valor adotado", "Como validar"],
    [
        ["Estabelecimentos odontológicos privados no Brasil",
         num(P.estabelecimentos_odonto_brasil),
         "Estatísticas do Conselho Federal de Odontologia, CNES/DataSUS e cadastro de empresas "
         "por CNAE de atividade odontológica."],
        ["Fração de pequeno e médio porte", pct(P.fatia_pequeno_medio_porte, 0),
         "Distribuição de porte no CNES e pesquisa própria por amostra na região-alvo."],
        ["Penetração alvo em 5 anos", pct(P.penetracao_alvo_5_anos, 0),
         "Comparação com a curva histórica de adoção de participantes estabelecidos do setor."],
        ["Mix de planos",
         f"{pct(P.mix_solo, 0)} / {pct(P.mix_clinica, 0)} / {pct(P.mix_enterprise, 0)}",
         "Composição real da base após as primeiras 50 clínicas pagantes."],
        ["Custo variável por cliente", brl(P.custo_variavel_mensal, 2) + " / mês",
         "Fatura real de nuvem, mensagens e IA dividida pela base, medida por três meses (PN-28)."],
        ["Custo de aquisição", brl(P.cac),
         "Gasto total de marketing e vendas dividido por clientes adquiridos, por coorte. "
         "É a premissa mais frágil do modelo."],
        ["Cancelamento mensal", pct(P.churn_mensal),
         "Retenção por coorte a partir do sexto mês de operação. Antes disso não há dado, "
         "apenas referência de mercado."],
        ["Trajetória de clientes", "Ver cenários",
         "Confrontar com o realizado a cada trimestre e recalibrar; o modelo é hipótese até "
         "encontrar o primeiro dado real."],
    ],
    larguras=[62 * 2.83, 26 * 2.83, 85 * 2.83],
    alinhar_centro=(1,),
)

doc.subsecao("Anexo B — Projeção trimestral do cenário Base")
doc.tabela(
    ["Trimestre", "Clientes", "MRR ao final", "Receita no trimestre", "Custo no trimestre",
     "Resultado", "Caixa acumulado"],
    [[t["trimestre"], num(t["clientes"]), brl(t["mrr"]), brl(t["receita_trimestre"]),
      brl(t["custo_trimestre"]), brl(t["resultado_trimestre"]), brl(t["caixa"])]
     for t in resumo_trimestral(BASE)],
    larguras=[20 * 2.83, 18 * 2.83, 24 * 2.83, 30 * 2.83, 28 * 2.83, 26 * 2.83, 30 * 2.83],
    alinhar_centro=(0, 1, 2, 3, 4, 5, 6),
)
doc.texto(
    f"O caixa acumulado é negativo em toda a janela porque o cenário Base financia aquisição "
    f"antes de a base madura pagar por ela — comportamento normal do modelo de assinatura, "
    f"detalhado na Seção 05. O vale de caixa atinge {brl(abs(pior_caixa(BASE)[0]))} no mês "
    f"{pior_caixa(BASE)[1]}, e é esse número, não o prejuízo contábil, que define a necessidade "
    f"de capital. No cenário Bootstrap o mesmo vale é de {brl(abs(_boot_caixa))}, com resultado "
    f"mensal positivo a partir do mês {_boot_equilibrio}.")

doc.subsecao("Anexo C — Indicadores de acompanhamento")
doc.tabela(
    ["Indicador", "Frequência", "Meta de referência", "Por que importa"],
    [
        ["Receita recorrente mensal (MRR)", "Semanal", "Crescer todo mês",
         "Medida única de saúde de negócio de assinatura."],
        ["Cancelamento mensal", "Mensal", f"Abaixo de {pct(P.churn_mensal)}",
         "A premissa de maior efeito sobre o LTV e sobre a viabilidade do plano."],
        ["Custo de aquisição por canal", "Mensal", f"Abaixo de {brl(P.cac)}",
         "Determina qual canal escalar e qual encerrar."],
        ["Retorno do CAC", "Mensal", f"Abaixo de {P.payback_meses:.0f} meses",
         "Define se é possível crescer com receita própria."],
        ["Ativação em 14 dias", "Semanal", "Acima de 60%",
         "Melhor previsor conhecido de permanência no primeiro ano."],
        ["Uso semanal ativo", "Semanal", "Acima de 80% da base",
         "Clínica que não usa toda semana já cancelou — só não avisou."],
        ["Chamados de suporte por cliente", "Mensal", "Decrescente",
         "Protege a margem e sinaliza problema de usabilidade."],
        ["Disponibilidade do serviço", "Contínua", "Acima de 99,5%",
         "Clínica parada perde receita no dia e não perdoa a repetição."],
        ["Reserva de caixa", "Mensal", "Acima de 6 meses de custo fixo",
         "Separa o negócio que atravessa um trimestre ruim do que não atravessa."],
    ],
    larguras=[48 * 2.83, 22 * 2.83, 40 * 2.83, 63 * 2.83],
    alinhar_centro=(1,),
)

doc.subsecao("Anexo D — Riscos e planos de mitigação")
doc.tabela(
    ["Risco", "Probabilidade × impacto", "Mitigação"],
    [
        ["Propriedade intelectual não resolvida entre integrantes e universidade",
         "Média × Muito alto",
         "PN-31 antes de qualquer outra iniciativa. Sem isso, captação, venda ou entrada de sócio "
         "ficam juridicamente inviáveis."],
        ["Dissolução da equipe ao fim do ciclo acadêmico",
         "Alta × Alto",
         "PN-32, PN-43 e PN-45: acordo de sócios com prazo de aquisição de participação, "
         "documentação operacional e definição escrita de dedicação."],
        ["Cancelamento acima do modelado",
         "Média × Muito alto",
         "PN-18 a PN-23 têm prioridade sobre aquisição; a Fase 1 é avaliada por permanência, não "
         "por volume de clientes."],
        ["CAC muito acima da premissa",
         "Alta × Alto",
         "Priorizar indicação e conteúdo (PN-11, PN-12); medir por canal desde o primeiro real "
         "gasto e encerrar canal que não converte."],
        ["Incidente de segurança com dado de saúde",
         "Baixa × Muito alto",
         "Onda 1 do Documento 01, seguro cibernético (PN-37), plano de resposta a incidente e "
         "programa de LGPD (PN-34)."],
        ["Concorrente estabelecido reduz preço para a faixa de entrada",
         "Média × Alto",
         "Diferenciar por IA aplicada à redução de faltas (Documento 03) e por experiência "
         "(Documento 02), não por preço — disputa de preço com quem tem mais capital é perdida."],
        ["Custo de IA acima do previsto corrói a margem",
         "Média × Alto",
         "Gateway de LLM com cota e cache semântico (ST-61), custo visível por clínica (IA-59) e "
         "franquia com excedente cobrado."],
        ["Mudança regulatória em IA aplicada à saúde",
         "Média × Médio",
         "Posicionamento assistivo desde o desenho (Documento 03), consulta prévia (PN-36) e "
         "desligamento por funcionalidade sem perda do restante do sistema."],
        ["Dependência de canal de terceiro para comunicação",
         "Média × Médio",
         "Abstrair o canal na camada de comunicação e manter alternativa por SMS e e-mail, que a "
         "arquitetura já contempla."],
        ["Capital não obtido no momento necessário",
         "Média × Alto",
         "Adotar o cenário Bootstrap como plano principal: ele foi construído para não depender "
         "de captação."],
    ],
    larguras=[52 * 2.83, 30 * 2.83, 91 * 2.83],
    alinhar_centro=(1,),
)

doc.nota_metodologica(
    "<b>Método e limites.</b> Todos os valores financeiros deste documento são calculados pelo "
    "modelo em <code>docs/generators/modelo_negocio.py</code> a partir das premissas do Anexo A, "
    "e nenhum número foi digitado manualmente no texto — alterar uma premissa e reexecutar o "
    "gerador atualiza o documento inteiro de forma consistente. <b>As premissas são estimativas "
    "de ordem de grandeza para planejamento, não dados de pesquisa de mercado</b>, e o Anexo A "
    "indica a fonte oficial que precisa confirmar cada uma. Os preços de R$ 97 e R$ 197 são os "
    "efetivamente publicados em <code>index.html</code>; os demais valores são projeções. "
    "As orientações societárias, tributárias e regulatórias apontam direção e "
    "<b>não constituem parecer jurídico ou contábil</b>: PN-25, PN-31, PN-32, PN-33 e PN-36 "
    "exigem profissional habilitado. Nenhum resultado descrito aqui foi alcançado — este é um "
    "plano, e seu valor está em ser confrontado com o realizado a cada trimestre e recalibrado.")

doc.build()
