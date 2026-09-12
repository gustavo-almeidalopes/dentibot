# -*- coding: utf-8 -*-
"""Documento 04 — Melhorias do DentiBot orientadas às ODS da ONU."""

import os
import sys

from reportlab.lib import colors

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dentibot_doc import LARANJA, VERDE, VERMELHO, Documento, Melhoria as M  # noqa: E402

# Azul institucional das Nações Unidas, usado como cor deste documento.
AZUL_ONU = colors.HexColor("#0A97D9")

SAIDA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                     "04-Melhorias-ODS-ONU.pdf")

doc = Documento(
    caminho=SAIDA,
    numero="04",
    titulo="Melhorias Orientadas às ODS",
    subtitulo="61 melhorias que ligam o DentiBot aos Objetivos de Desenvolvimento Sustentável "
              "da ONU — com indicador, meta e evidência auditável para cada uma.",
    resumo="",
    cor=AZUL_ONU,
    rotulo="Sustentabilidade e Impacto Social",
    col_extra="Meta ODS / indicador",
)

doc.capa()

doc.resumo_executivo([
    "Saúde bucal é um dos maiores problemas de saúde pública não resolvidos do mundo, e um dos "
    "mais desiguais: doenças bucais afetam bilhões de pessoas, são quase inteiramente evitáveis "
    "e concentram-se em quem tem menos acesso a cuidado. Um sistema de gestão odontológica de "
    "baixo custo, desenhado para o consultório de bairro, está em posição incomum para "
    "contribuir com a Agenda 2030 — não por filantropia, mas pela natureza do que faz.",

    "Este documento resiste deliberadamente ao formato mais comum de relatório de "
    "sustentabilidade — a lista de boas intenções sem verificação. Cada uma das 61 melhorias "
    "traz três elementos obrigatórios: a <b>meta específica</b> da ODS a que responde (não "
    "apenas o número do objetivo), o <b>indicador</b> que a torna mensurável e a <b>evidência</b> "
    "que o sistema precisa produzir para que a afirmação seja auditável por terceiro. "
    "Compromisso sem indicador é publicidade; com indicador e evidência, vira instrumento de "
    "gestão e credencial em edital público.",

    "A tese central é que o alinhamento às ODS é, para este projeto, uma decisão de negócio "
    "antes de ser uma decisão ética. Três razões práticas: contratos com secretarias municipais "
    "de saúde e com operadoras exigem evidência de impacto; o rastreio de resíduos e de "
    "esterilização que a ODS 12 pede é exatamente o controle que a vigilância sanitária "
    "brasileira já cobra da clínica, o que transforma obrigação em funcionalidade vendável; e o "
    "público de dentistas recém-formados — o alvo do plano Solo descrito no Documento 05 — "
    "decide fornecedor levando esses critérios em conta.",

    "As melhorias se organizam por objetivo, das que estão diretamente no caminho crítico do "
    "produto (ODS 3, 12 e 16) às que exigem parceria externa (ODS 4, 11 e 17). Onde a "
    "contribuição é indireta ou pequena, o documento diz isso explicitamente: inflar a "
    "contribuição própria é a falha mais comum — e mais facilmente desmontada — em relato de "
    "sustentabilidade.",
], destaques=[
    ("61", "melhorias mapeadas"),
    ("11", "ODS endereçadas"),
    ("100%", "com indicador definido"),
    ("2030", "horizonte da agenda"),
    ("3", "ODS no caminho crítico"),
])

doc.sumario(extras=[
    "Anexo A — Matriz de materialidade das ODS",
    "Anexo B — Painel de indicadores de impacto",
    "Anexo C — Governança do relato e limites da alegação",
])

# ─── 01 ──────────────────────────────────────────────────────────────────────
doc.secao("Por que ODS em um software odontológico",
          "A ligação entre o produto e a Agenda 2030, sem exagero")

doc.texto(
    "As ODS foram desenhadas para Estados, mas suas metas se traduzem em decisões concretas de "
    "produto. A tabela abaixo separa aquilo em que o DentiBot tem influência direta e "
    "mensurável daquilo em que a contribuição é indireta — distinção que sustenta a "
    "credibilidade de todo o restante do documento.")

doc.tabela(
    ["Objetivo", "Ligação com o DentiBot", "Natureza da contribuição"],
    [
        ["ODS 3 — Saúde e bem-estar",
         "Prevenção, adesão ao tratamento, redução de faltas e alcance de população "
         "sub-atendida.",
         "Direta. É o núcleo do produto e onde a evidência é mais forte."],
        ["ODS 12 — Consumo e produção responsáveis",
         "Rastreio de insumo e de resíduo de serviço de saúde, redução de desperdício e de "
         "papel.",
         "Direta. Coincide com obrigação sanitária que a clínica já tem."],
        ["ODS 16 — Instituições eficazes e transparentes",
         "Trilha de auditoria, transparência ao titular do dado e privilégio mínimo.",
         "Direta. A arquitetura já implementada favorece este objetivo."],
        ["ODS 8 e 9 — Trabalho decente e inovação",
         "Produtividade de microempresa de saúde e acesso de clínica pequena a tecnologia.",
         "Direta no acesso; indireta na geração de emprego."],
        ["ODS 10 — Redução das desigualdades",
         "Acessibilidade digital, custo baixo e alcance de clínica de periferia e de interior.",
         "Direta no produto; indireta no resultado social agregado."],
        ["ODS 4 — Educação de qualidade",
         "Uso acadêmico do projeto, formação de dentistas em gestão e educação do paciente.",
         "Direta no contexto universitário em que o projeto nasceu."],
        ["ODS 5 — Igualdade de gênero",
         "Odontologia é profissão majoritariamente feminina no Brasil; gestão do próprio "
         "negócio é fator de autonomia econômica.",
         "Indireta, mas mensurável na base de clientes."],
        ["ODS 11 e 13 — Cidades e clima",
         "Redução de deslocamento evitável, de papel e de energia por operação.",
         "Indireta e de magnitude modesta — deve ser relatada como tal."],
        ["ODS 17 — Parcerias",
         "Universidade, poder público, conselhos profissionais e código aberto.",
         "Direta no arranjo institucional do projeto."],
    ],
    larguras=[36 * 2.83, 68 * 2.83, 69 * 2.83],
)

doc.destaque(
    "Regra editorial deste documento",
    "Nenhuma melhoria entra sem indicador mensurável e sem a evidência que o sistema precisa "
    "gerar para comprová-la. Onde a contribuição do DentiBot for pequena diante do problema, o "
    "texto diz isso. Um relatório de impacto que atribui a um software de gestão a redução da "
    "cárie em um município não sobrevive à primeira pergunta de um avaliador sério — e leva "
    "junto a credibilidade das afirmações que eram verdadeiras.",
    cor=AZUL_ONU)

# ─── 02 ──────────────────────────────────────────────────────────────────────
doc.secao("ODS 3 — Saúde e bem-estar",
          "Metas 3.4, 3.8 e 3.c — prevenção, cobertura e força de trabalho em saúde")
doc.legenda_backlog()

doc.melhorias([
    M("ODS-01", "Programa estruturado de prevenção",
      "Transformar retorno preventivo em fluxo do sistema, com convocação por risco individual, "
      "não por data fixa. Doença bucal é majoritariamente evitável: o software de gestão que só "
      "registra tratamento reforça o modelo curativo; o que convoca para prevenção o desloca.",
      "prontuário, agenda", "Alto", "M", "3.4 · consultas preventivas / total"),
    M("ODS-02", "Redução mensurada de faltas",
      "Cadeira vazia é acesso desperdiçado: enquanto um paciente falta, outro espera. Medir e "
      "reportar a queda na taxa de ausência por clínica, comparando o período anterior e "
      "posterior à adoção.",
      "agenda, communication-service", "Alto", "M", "3.8 · taxa de ausência"),
    M("ODS-03", "Rastreio de tratamento interrompido",
      "Identificar e reconvocar quem abandonou o plano no meio. Tratamento incompleto costuma "
      "ser pior que tratamento não iniciado, e hoje ninguém percebe até o paciente reaparecer "
      "em urgência.",
      "prontuário, financeiro", "Alto", "M", "3.8 · % de planos concluídos"),
    M("ODS-04", "Registro de encaminhamento e contrarreferência",
      "Documentar encaminhamento para especialidade, serviço público ou atenção hospitalar, com "
      "retorno da informação. Sem isso o paciente some no vão entre serviços — a falha mais "
      "comum da rede de saúde brasileira.",
      "prontuário", "Médio", "M", "3.8 · encaminhamentos com retorno"),
    M("ODS-05", "Sinalização de condições sistêmicas",
      "Tornar sempre visível diabetes, hipertensão, gestação e uso de anticoagulante, com o "
      "alerta correspondente. A boca é porta de entrada de manifestação sistêmica, e o dentista "
      "é frequentemente o profissional de saúde que a pessoa vê com mais regularidade.",
      "prontuário", "Alto", "M", "3.4 · alertas registrados e vistos"),
    M("ODS-06", "Rastreio oportunístico de lesão de risco",
      "Campo estruturado para exame de tecidos moles em consulta de rotina, com lembrete "
      "periódico. Câncer de boca tem prognóstico fortemente dependente do estágio em que é "
      "detectado, e a consulta odontológica de rotina é a melhor janela de detecção que existe.",
      "prontuário", "Alto", "M", "3.4 · % de consultas com exame registrado"),
    M("ODS-07", "Educação em saúde bucal no canal do paciente",
      "Enviar orientação de escovação, dieta e uso de fio adaptada à condição da pessoa, com "
      "confirmação de leitura. Custo marginal próximo de zero sobre a integração de comunicação "
      "que o projeto já prevê.",
      "communication-service", "Médio", "M", "3.4 · alcance e taxa de leitura"),
    M("ODS-08", "Registro de saúde bucal materno-infantil",
      "Fluxo específico para gestante e primeira infância, com convocação no momento certo. "
      "É a janela de maior retorno preventivo de toda a odontologia e a mais frequentemente "
      "perdida.",
      "prontuário, agenda", "Alto", "M", "3.2 e 3.4 · gestantes acompanhadas"),
    M("ODS-09", "Painel de saúde bucal da população atendida",
      "Consolidar, em dado agregado e anonimizado, o perfil epidemiológico da clientela da "
      "clínica: prevalência, faixa etária, procedimento mais frequente, tempo entre retornos. "
      "Insumo direto para o índice comunitário proposto em IA-56.",
      "analytics", "Médio", "G", "3.8 · painel publicado por clínica"),
    M("ODS-10", "Redução da sobrecarga administrativa do profissional",
      "A meta 3.c trata de fortalecer a força de trabalho em saúde. Devolver ao dentista horas "
      "hoje gastas em tarefa administrativa é contribuição direta e mensurável — e é exatamente "
      "o benefício declarado do projeto no README.",
      "todo o sistema", "Alto", "M", "3.c · horas administrativas por semana"),
], larg_alvo=32 * 2.83, larg_extra=32 * 2.83)

# ─── 03 ──────────────────────────────────────────────────────────────────────
doc.secao("ODS 4 — Educação de qualidade",
          "Metas 4.3 e 4.4 — o projeto nasceu na universidade e deve devolver valor a ela")

doc.melhorias([
    M("ODS-11", "Modo didático com dados sintéticos",
      "Ambiente com base fictícia realista para uso em sala de aula, sem qualquer dado de "
      "paciente real. O README já declara a finalidade acadêmica do projeto; falta o modo que a "
      "viabiliza sem risco de LGPD.",
      "novo modo demo", "Alto", "M", "4.4 · alunos com acesso ao ambiente"),
    M("ODS-12", "Documentação de arquitetura como material de ensino",
      "Publicar os diagramas UML, ADRs e o mapeamento de segurança em formato reutilizável por "
      "outras disciplinas de engenharia de software. O SECURITY.md com dez padrões já é, "
      "sozinho, material de curso.",
      "docs/, SECURITY.md", "Médio", "P", "4.4 · downloads e adoções"),
    M("ODS-13", "Trilha de formação em gestão para o dentista",
      "Conteúdo curto embutido no produto sobre precificação, fluxo de caixa e indicadores. "
      "A graduação em odontologia forma clínicos, não administradores — e a maioria vai abrir o "
      "próprio consultório.",
      "novo módulo educacional", "Médio", "M", "4.4 · conclusão de trilhas"),
    M("ODS-14", "Programa de contribuição para estudantes",
      "Issues rotuladas para iniciantes, guia de contribuição e mentoria. Converte o "
      "repositório em experiência prática de engenharia para alunos da UNICID e de outras "
      "instituições.",
      "CONTRIBUTING.md, .github/", "Médio", "P", "4.4 · contribuidores externos"),
    M("ODS-15", "Dados abertos para pesquisa acadêmica",
      "Disponibilizar conjunto anonimizado e documentado para pesquisa em saúde bucal, com "
      "aprovação de comitê de ética e termo de uso. Depende de ST-60 e de escala mínima.",
      "analytics", "Médio", "G", "4.4 e 9.5 · pesquisas publicadas"),
    M("ODS-16", "Acessibilidade como requisito de formação",
      "Documentar as decisões de acessibilidade do Documento 02 como estudo de caso. Ensinar "
      "acessibilidade com exemplo real de um sistema de saúde vale mais que qualquer aula "
      "teórica sobre WCAG.",
      "docs/, design-system", "Baixo", "P", "4.4 · material publicado"),
], larg_alvo=34 * 2.83, larg_extra=30 * 2.83)

# ─── 04 ──────────────────────────────────────────────────────────────────────
doc.secao("ODS 5 e 10 — Igualdade e redução das desigualdades",
          "Metas 5.5, 5.b, 10.2 e 10.3 — quem consegue usar o sistema e quem fica de fora")

doc.texto(
    "A odontologia brasileira é uma profissão majoritariamente exercida por mulheres, e uma "
    "parcela expressiva delas atua como profissional autônoma ou dona de consultório pequeno — "
    "exatamente o perfil do plano Solo. Ferramenta de gestão acessível é, nesse contexto, "
    "instrumento de autonomia econômica. As melhorias abaixo tratam tanto disso quanto da "
    "questão mais ampla de quem consegue efetivamente usar o produto.")

doc.melhorias([
    M("ODS-17", "Acessibilidade WCAG 2.2 AA como requisito de release",
      "Interface inacessível exclui profissional e paciente com deficiência antes de qualquer "
      "outra barreira. Tornar a conformidade um gate automatizado de entrega, não uma meta de "
      "boa vontade. Detalhamento completo no Documento 02.",
      "todas as telas", "Alto", "G", "10.2 · violações críticas = zero"),
    M("ODS-18", "Funcionamento em dispositivo e rede modestos",
      "Garantir uso pleno em celular de entrada e conexão 3G ou 4G instável. Exigir hardware "
      "recente é exclusão silenciosa de clínica de periferia e de interior — justamente o "
      "público que o projeto diz querer atender.",
      "PWA, frontend/", "Alto", "M", "9.c e 10.2 · uso em rede lenta"),
    M("ODS-19", "Linguagem simples auditada",
      "Submeter todo texto voltado ao paciente a verificação de legibilidade e revisão em "
      "linguagem simples. Termo técnico incompreensível é barreira de acesso tão real quanto "
      "preço.",
      "conteúdo, cliente.html", "Médio", "M", "10.2 · índice de legibilidade"),
    M("ODS-20", "Preço acessível como decisão de produto",
      "Os planos de R$ 97 e R$ 197 mensais existem para viabilizar adoção por clínica pequena — "
      "o problema número 2 do diagnóstico do projeto. Tratar o preço como compromisso de "
      "impacto sujeito a relato, não apenas como estratégia comercial.",
      "modelo de negócio", "Alto", "M", "10.2 · clínicas com até 3 profissionais"),
    M("ODS-21", "Faixa social e gratuidade para atendimento público",
      "Condição diferenciada para clínica-escola, projeto de extensão e serviço conveniado ao "
      "SUS, com contrapartida de dado agregado para vigilância. Detalhado no Documento 05.",
      "modelo de negócio", "Alto", "M", "10.3 · atendimentos em faixa social"),
    M("ODS-22", "Painel de equidade de acesso por clínica",
      "Mostrar quem está ficando para trás — por bairro, faixa etária ou condição — e sugerir "
      "ação. Sem medir, a desigualdade de acesso permanece invisível dentro da própria "
      "clientela.",
      "analytics", "Médio", "G", "10.3 · variação de acesso entre grupos"),
    M("ODS-23", "Suporte em múltiplos canais e horários",
      "Atendimento por WhatsApp, com opção de voz, fora do horário comercial. Profissional "
      "autônomo resolve questão de sistema à noite, depois de atender — negar isso é excluir "
      "pela agenda.",
      "operação de suporte", "Médio", "M", "10.2 · resolução no primeiro contato"),
    M("ODS-24", "Métricas de negócio abertas por gênero",
      "Acompanhar e relatar a distribuição de gênero na base de clientes, na equipe e na "
      "liderança do projeto. Meta 5.5 exige dado, não declaração de intenção.",
      "governança, analytics", "Médio", "P", "5.5 · distribuição relatada"),
], larg_alvo=34 * 2.83, larg_extra=32 * 2.83)

# ─── 05 ──────────────────────────────────────────────────────────────────────
doc.secao("ODS 8 e 9 — Trabalho decente e inovação",
          "Metas 8.2, 8.3, 9.3, 9.5 e 9.c — produtividade da microempresa de saúde")

doc.melhorias([
    M("ODS-25", "Ganho de produtividade documentado",
      "Medir e publicar o tempo economizado em tarefa administrativa por clínica. É a promessa "
      "central do projeto e precisa sair do campo da afirmação para o da evidência.",
      "analytics", "Alto", "M", "8.2 · horas economizadas por mês"),
    M("ODS-26", "Formalização e conformidade fiscal assistida",
      "Emissão de nota, controle de recebimento e relatório contábil dentro do fluxo. O schema "
      "do banco já prevê configuração de emissão de nota fiscal por clínica; falta a "
      "experiência que torna a formalização mais fácil que a informalidade.",
      "financial-service", "Alto", "M", "8.3 · % de receita com nota emitida"),
    M("ODS-27", "Acesso a crédito com base em histórico",
      "Com consentimento, gerar relatório de faturamento verificável que a clínica possa usar "
      "junto a instituição financeira. Microempresa de saúde não obtém crédito por falta de "
      "comprovação, não por falta de receita.",
      "financial-service", "Médio", "G", "9.3 · clínicas com relatório emitido"),
    M("ODS-28", "Portabilidade de dados sem aprisionamento",
      "Exportação completa e documentada a qualquer momento, sem custo. Aprisionamento em "
      "fornecedor é prática que prejudica desproporcionalmente o negócio pequeno, que não tem "
      "poder de negociação.",
      "patient-service", "Alto", "M", "9.3 · exportação em < 24 h"),
    M("ODS-29", "Ergonomia e prevenção de lesão ocupacional",
      "Reduzir digitação repetitiva por voz e automação. Lesão por esforço repetitivo é causa "
      "frequente de afastamento entre dentistas e pessoal de recepção — trabalho decente na "
      "meta 8.8 inclui isso.",
      "prontuário, mobile/", "Médio", "M", "8.8 · redução de entrada manual"),
    M("ODS-30", "Publicação de resultados de pesquisa aplicada",
      "Publicar em veículo acadêmico o que o projeto aprender sobre previsão de falta, adesão "
      "ao tratamento e usabilidade em saúde. Devolve conhecimento e cria credibilidade "
      "institucional.",
      "governança", "Médio", "M", "9.5 · publicações por ano"),
    M("ODS-31", "Infraestrutura eficiente por assinante",
      "Medir e reduzir consumo computacional por clínica atendida. Eficiência é simultaneamente "
      "margem (Documento 05), preço acessível (ODS 10) e menor pegada (ODS 13).",
      "infra", "Médio", "M", "9.4 · custo e consumo por clínica"),
    M("ODS-32", "Código aberto de componentes não competitivos",
      "Liberar sob licença aberta o que não é vantagem competitiva — biblioteca de "
      "acessibilidade, cliente TISS, componentes de interface. O projeto já é MIT; a decisão é "
      "sobre o que manter aberto conforme ele se torna comercial.",
      "repositório, LICENSE", "Médio", "M", "9.5 e 17.6 · componentes publicados"),
], larg_alvo=32 * 2.83, larg_extra=32 * 2.83)

# ─── 06 ──────────────────────────────────────────────────────────────────────
doc.secao("ODS 11 e 12 — Cidades e consumo responsável",
          "Metas 11.6, 12.2, 12.4, 12.5 e 12.6 — resíduo, insumo e desperdício")

doc.texto(
    "Esta é a frente com a melhor relação entre impacto ambiental e esforço de desenvolvimento, "
    "por um motivo específico: no Brasil, o gerenciamento de resíduos de serviços de saúde já é "
    "obrigação sanitária da clínica, com plano de gerenciamento exigido e resíduo perfurocortante, "
    "químico e com amálgama sujeito a manejo específico. Isso significa que a funcionalidade "
    "ambiental é, ao mesmo tempo, uma funcionalidade de conformidade que a clínica precisa ter — "
    "e que hoje ela controla em papel.")

doc.melhorias([
    M("ODS-33", "Módulo de gerenciamento de resíduo de serviço de saúde",
      "Registrar geração, segregação, armazenamento e destinação por grupo de resíduo, com "
      "manifesto de transporte e comprovante de destinação final. Substitui a planilha e o "
      "arquivo de papel que a clínica mantém hoje para a fiscalização.",
      "novo módulo, inventory-service", "Alto", "G", "12.4 · % de resíduo com destinação "
      "comprovada"),
    M("ODS-34", "Controle específico de resíduo de amálgama",
      "Rastrear separadamente o resíduo com mercúrio, exigido por norma e ligado à Convenção de "
      "Minamata, com registro de separador e de coleta especializada.",
      "novo módulo", "Alto", "M", "12.4 · coletas registradas"),
    M("ODS-35", "Prevenção de vencimento em estoque",
      "Alertar antes do vencimento e sugerir uso prioritário ou transferência. Insumo odontológico "
      "vencido vira resíduo químico perigoso e prejuízo — evitar o descarte é melhor que "
      "gerenciá-lo.",
      "inventory-service", "Alto", "M", "12.5 · valor descartado por vencimento"),
    M("ODS-36", "Compra dimensionada pelo consumo real",
      "Sugerir quantidade a partir do consumo histórico e da agenda futura, evitando o excesso "
      "que se transforma em descarte. Conecta-se diretamente a IA-43.",
      "inventory-service", "Alto", "M", "12.2 · giro de estoque"),
    M("ODS-37", "Clínica sem papel de ponta a ponta",
      "Anamnese, consentimento, orçamento, atestado e recibo digitais com validade jurídica "
      "(ICP-Brasil). Elimina impressão, arquivo físico e o custo de guarda por prazo legal.",
      "prontuário, audit-service", "Alto", "M", "12.5 · folhas evitadas por mês"),
    M("ODS-38", "Painel ambiental da clínica",
      "Consolidar resíduo gerado por tipo, papel evitado, insumo desperdiçado e destinação, "
      "gerando relatório pronto para a vigilância sanitária. Transforma dado disperso em "
      "evidência utilizável.",
      "analytics", "Médio", "M", "12.6 · relatórios emitidos"),
    M("ODS-39", "Comparação anônima entre clínicas",
      "Mostrar a cada clínica como seu consumo e seu desperdício se comparam a clínicas "
      "semelhantes. Comparação com o par é a intervenção comportamental mais eficaz conhecida "
      "para reduzir consumo.",
      "analytics", "Médio", "M", "12.6 · redução após comparação"),
    M("ODS-40", "Redução de deslocamento evitável",
      "Teleorientação para o que não exige presença, agendamento agrupado por família e escolha "
      "de horário compatível com o trânsito reduzem viagens. O efeito climático é modesto; o "
      "efeito sobre acesso e sobre falta é grande.",
      "agenda, communication-service", "Médio", "M", "11.2 e 11.6 · deslocamentos evitados"),
], larg_alvo=34 * 2.83, larg_extra=34 * 2.83)

# ─── 07 ──────────────────────────────────────────────────────────────────────
doc.secao("ODS 13 — Ação climática",
          "Metas 13.2 e 13.3 — contribuição real, relatada com a magnitude que tem")

doc.texto(
    "Um sistema de gestão para clínicas pequenas não é um ator relevante na crise climática, e "
    "afirmar o contrário desqualificaria o restante deste documento. O que ele pode fazer com "
    "honestidade é reduzir a própria pegada, medir o que reduz e não usar a pauta como "
    "marketing.")

doc.melhorias([
    M("ODS-41", "Medição da pegada da infraestrutura",
      "Estimar e publicar a emissão associada ao processamento, ao armazenamento e ao tráfego, "
      "por clínica atendida. Não se reduz o que não se mede, e a estimativa deve declarar seu "
      "método e sua margem de erro.",
      "infra, analytics", "Médio", "M", "13.2 · emissão estimada por clínica"),
    M("ODS-42", "Preferência por região de baixa intensidade de carbono",
      "Considerar a matriz energética da região de nuvem na decisão de infraestrutura. A matriz "
      "brasileira é comparativamente limpa, o que favorece hospedagem no país — decisão que "
      "também ajuda em latência e em soberania de dado.",
      "infra", "Médio", "M", "13.2 · % em região de baixa intensidade"),
    M("ODS-43", "Eficiência computacional como requisito",
      "Tratar consumo de CPU, memória e tráfego como restrição de projeto. Cache semântico no "
      "gateway de IA, consulta indexada e imagem enxuta reduzem simultaneamente custo, latência "
      "e emissão.",
      "todo o sistema", "Médio", "M", "13.2 · recurso por requisição"),
    M("ODS-44", "Retenção de dado com finalidade declarada",
      "Guardar indefinidamente o que não tem finalidade é custo, risco de LGPD e emissão. "
      "Política de retenção e expurgo automatizada resolve os três de uma vez.",
      "database/, audit-service", "Médio", "M", "13.2 e 12.5 · volume por clínica"),
    M("ODS-45", "Otimização de imagem e mídia clínica",
      "Foto intraoral e radiografia dominam o armazenamento. Compressão sem perda diagnóstica, "
      "arquivamento em camada fria e deduplicação reduzem custo e pegada sem afetar o cuidado.",
      "patient-service, object storage", "Médio", "M", "13.2 · bytes por paciente"),
    M("ODS-46", "Relato climático honesto sobre a magnitude",
      "Publicar o que foi medido junto com o reconhecimento explícito de que a contribuição é "
      "pequena diante do problema. Exagero em relato climático é risco reputacional e, "
      "crescentemente, risco regulatório.",
      "governança", "Médio", "P", "13.3 · relatório anual publicado"),
], larg_alvo=36 * 2.83, larg_extra=32 * 2.83)

# ─── 08 ──────────────────────────────────────────────────────────────────────
doc.secao("ODS 16 — Instituições eficazes e transparentes",
          "Metas 16.5, 16.6 e 16.10 — onde a arquitetura já implementada é uma vantagem")

doc.texto(
    "Este é o objetivo em que o projeto parte da melhor posição. A arquitetura já contempla "
    "papéis de banco com privilégio mínimo, cofre de tokenização em schema separado, extensão "
    "de auditoria no PostgreSQL e um serviço de auditoria com trilha do tipo <i>append-only</i>. "
    "As melhorias abaixo convertem essa base técnica em transparência efetiva para quem é dono "
    "do dado — o paciente.")

doc.melhorias([
    M("ODS-47", "Extrato de acesso ao dado para o titular",
      "O paciente consulta quem acessou seu prontuário, quando e para quê, e pode contestar. "
      "É a materialização do direito de acesso da LGPD e, ao mesmo tempo, o diferencial "
      "descrito em IA-52.",
      "audit-service, cliente.html", "Alto", "M", "16.10 · titulares que consultaram"),
    M("ODS-48", "Trilha de auditoria íntegra e verificável",
      "Encadeamento criptográfico dos registros de auditoria, tornando alteração retroativa "
      "detectável. Sem garantia de integridade, trilha de auditoria é apenas mais uma tabela "
      "que alguém com acesso pode editar.",
      "audit-service", "Alto", "M", "16.6 · verificação de integridade"),
    M("ODS-49", "Consentimento granular e revogável",
      "Controle por finalidade, com efeito imediato e histórico completo. Aceite único de termo "
      "extenso não é consentimento informado em nenhum sentido prático.",
      "patient-service, cliente.html", "Alto", "G", "16.10 · finalidades sob controle"),
    M("ODS-50", "Privilégio mínimo com revisão periódica",
      "Revisão trimestral automatizada de quem tem acesso a quê, com remoção de permissão não "
      "usada. Acesso acumulado ao longo do tempo é a origem mais comum de vazamento interno.",
      "auth-service, governança", "Alto", "M", "16.5 · permissões revisadas"),
    M("ODS-51", "Transparência sobre o uso de IA",
      "Informar quando uma sugestão foi gerada por modelo, qual versão e quem a confirmou. "
      "Transparência algorítmica em saúde é requisito de confiança antes de ser requisito legal.",
      "ai-gateway, audit-service", "Alto", "M", "16.10 · decisões de IA rastreáveis"),
    M("ODS-52", "Canal de divulgação responsável de vulnerabilidade",
      "Publicar <code>security.txt</code> e política de divulgação, com prazo de resposta "
      "declarado. Instituição confiável recebe crítica de segurança por canal aberto, não por "
      "rede social depois do incidente.",
      "frontend/, SECURITY.md", "Médio", "P", "16.6 · tempo de resposta"),
    M("ODS-53", "Relatório público de transparência",
      "Publicar periodicamente número de incidentes, tempo de resposta, requisições de "
      "titulares atendidas e disponibilidade real do serviço. Prática consolidada em "
      "infraestrutura digital e ainda rara em software de saúde no Brasil.",
      "governança", "Médio", "M", "16.6 · relatórios publicados"),
], larg_alvo=34 * 2.83, larg_extra=32 * 2.83)

# ─── 09 ──────────────────────────────────────────────────────────────────────
doc.secao("ODS 17 e governança do impacto",
          "Metas 17.16 e 17.17 — parcerias e o sistema que sustenta o relato")

doc.melhorias([
    M("ODS-54", "Parceria com a universidade formalizada",
      "Converter a origem acadêmica em programa contínuo: extensão, iniciação científica, "
      "estágio e pesquisa aplicada, com acordo escrito. Hoje a ligação existe de fato, mas não "
      "de direito.",
      "governança, UNICID", "Alto", "M", "17.17 · convênios ativos"),
    M("ODS-55", "Contrapartida a serviço público de saúde",
      "Oferecer condição diferenciada a clínica-escola e a serviço conveniado ao SUS, com "
      "contrapartida de dado agregado anonimizado para a vigilância municipal.",
      "modelo de negócio", "Alto", "G", "17.17 · serviços públicos atendidos"),
    M("ODS-56", "Diálogo com conselhos profissionais",
      "Submeter as funcionalidades de teleodontologia e de IA à leitura dos conselhos regional "
      "e federal antes do lançamento. Reduz risco regulatório e cria interlocução institucional.",
      "governança, CFO/CRO", "Alto", "M", "17.17 · pareceres obtidos"),
    M("ODS-57", "Interoperabilidade com o ecossistema público",
      "Adotar padrão aberto de troca de dados em saúde para conversar com a rede pública. "
      "Sistema isolado não participa de política pública, por melhor que seja.",
      "interop-service", "Alto", "G", "17.16 · integrações em produção"),
    M("ODS-58", "Rede de clínicas para compra e conhecimento",
      "Compra coletiva de insumo (IA-55) e troca de boas práticas entre assinantes. Valor de "
      "rede que beneficia o participante pequeno de forma desproporcional — que é o ponto.",
      "novo módulo", "Médio", "G", "17.17 · clínicas participantes"),
    M("ODS-59", "Comitê de impacto com participação externa",
      "Instância com dentista, paciente e representante acadêmico que revisa metas e resultados "
      "de impacto. Autoavaliação sem olhar externo tende sistematicamente ao otimismo.",
      "governança", "Médio", "M", "17.17 · reuniões e pareceres"),
    M("ODS-60", "Relatório anual de impacto com verificação externa",
      "Publicar o painel do Anexo B em relatório anual e submetê-lo a verificação independente "
      "quando houver escala que justifique. Relato autodeclarado tem valor limitado em edital "
      "público e em negociação com operadora.",
      "governança", "Alto", "M", "12.6 · relatório publicado e verificado"),
    M("ODS-61", "Instrumentação dos indicadores no produto",
      "Nenhuma das 60 melhorias anteriores é verificável se o sistema não coletar o dado que a "
      "comprova. Implementar a coleta dos indicadores do Anexo B como requisito de produto, com "
      "agregação e anonimização por padrão.",
      "analytics, todo o sistema", "Alto", "G", "Pré-requisito de todo o relato"),
], larg_alvo=34 * 2.83, larg_extra=32 * 2.83)

# ─── 10 ─────────────────────────────────────────────────────────────────────
doc.secao("Anexos", "Materialidade, painel de indicadores e limites do relato")

doc.subsecao("Anexo A — Matriz de materialidade")
doc.texto(
    "Materialidade cruza a relevância do tema para as partes interessadas com a capacidade real "
    "de influência do DentiBot. Recursos limitados devem ir para o quadrante superior — e o "
    "relato deve ser proporcionalmente mais detalhado nele.")
doc.tabela(
    ["Prioridade", "ODS e temas", "Justificativa"],
    [
        ["Material e prioritário",
         "ODS 3 (prevenção, faltas, adesão), ODS 12 (resíduo e desperdício), "
         "ODS 16 (transparência e auditoria)",
         "Alta relevância para clínica, paciente e órgão regulador, e influência direta do "
         "produto. Onde a evidência é mais forte e o retorno comercial mais claro."],
        ["Material e emergente",
         "ODS 8 e 9 (produtividade e acesso a tecnologia), ODS 10 (acessibilidade e preço)",
         "Alta relevância e influência crescente conforme a base de clientes cresce. "
         "Sustentam a tese comercial do Documento 05."],
        ["Relevante e habilitador",
         "ODS 4 (educação), ODS 17 (parcerias)",
         "Influência direta no arranjo institucional do projeto e condição para acesso a "
         "edital, convênio e financiamento não diluidor."],
        ["Menor, relatar com honestidade",
         "ODS 11 e 13 (deslocamento, energia, clima), ODS 5 (gênero)",
         "Contribuição real porém modesta ou indireta. Deve ser medida e relatada sem "
         "amplificação — a credibilidade do conjunto depende disso."],
    ],
    larguras=[32 * 2.83, 62 * 2.83, 79 * 2.83],
)

doc.subsecao("Anexo B — Painel de indicadores de impacto")
doc.texto(
    "Conjunto mínimo a instrumentar no produto (ODS-61). Cada indicador precisa de linha de "
    "base medida antes de qualquer meta ser anunciada: sem linha de base, variação não "
    "significa nada.")
doc.tabela(
    ["Indicador", "ODS", "Unidade", "Origem do dado"],
    [
        ["Taxa de ausência em consulta", "3.8", "% de consultas", "appointment-service"],
        ["Consultas preventivas sobre o total", "3.4", "%", "prontuário"],
        ["Planos de tratamento concluídos", "3.8", "% dos iniciados", "prontuário"],
        ["Intervalo médio entre retornos", "3.4", "dias", "agenda"],
        ["Gestantes e crianças acompanhadas", "3.2", "número absoluto", "prontuário"],
        ["Horas administrativas economizadas", "3.c e 8.2", "horas por clínica/mês", "analytics"],
        ["Clínicas com até 3 profissionais na base", "10.2", "% da base", "auth-service"],
        ["Violações de acessibilidade críticas", "10.2", "número", "CI (axe-core)"],
        ["Resíduo com destinação comprovada", "12.4", "% do gerado", "módulo de resíduos"],
        ["Valor descartado por vencimento", "12.5", "R$ por clínica/mês", "inventory-service"],
        ["Folhas de papel evitadas", "12.5", "número por clínica/mês", "prontuário"],
        ["Emissão estimada por clínica atendida", "13.2", "kgCO₂e/mês", "infra"],
        ["Titulares que consultaram o extrato de acesso", "16.10", "% dos pacientes", "audit-service"],
        ["Requisições de titular atendidas no prazo", "16.10", "%", "governança"],
        ["Tempo de resposta a vulnerabilidade reportada", "16.6", "dias", "governança"],
        ["Convênios e parcerias ativos", "17.17", "número", "governança"],
    ],
    larguras=[62 * 2.83, 16 * 2.83, 40 * 2.83, 55 * 2.83],
    alinhar_centro=(1,),
)

doc.subsecao("Anexo C — Governança do relato e limites da alegação")
doc.lista([
    "<b>Linha de base antes da meta.</b> Nenhuma meta numérica deve ser anunciada antes de o "
    "indicador ter sido medido por pelo menos um trimestre. Meta sem linha de base é chute com "
    "aparência de compromisso.",
    "<b>Atribuição honesta.</b> Distinguir sempre o que o sistema causou do que ele apenas "
    "acompanhou. Uma queda na taxa de faltas depois da adoção é correlação; só um desenho de "
    "comparação adequado sustenta afirmação de causa.",
    "<b>Dado agregado e anonimizado por padrão.</b> Nenhum indicador de impacto pode ser "
    "produzido a partir de dado identificável sem base legal própria, distinta da base legal do "
    "atendimento (ver ST-60).",
    "<b>Verificação externa proporcional à escala.</b> Enquanto o projeto for pequeno, "
    "autodeclaração com método aberto é suficiente e honesta. A partir de contrato público ou "
    "de captação, verificação independente passa a ser necessária.",
    "<b>Publicar o que não deu certo.</b> Relatório que só contém sucesso não é lido como bom "
    "desempenho; é lido como seleção de evidência. Registrar meta não atingida e o porquê "
    "aumenta a credibilidade de todo o resto.",
    "<b>Revisão anual do mapeamento.</b> As ODS vão até 2030 e o produto muda. Revisar "
    "anualmente quais metas continuam materiais, retirando as que deixaram de ser — inclusive "
    "quando isso reduzir o número de objetivos alegados.",
])

doc.nota_metodologica(
    "<b>Método e limites.</b> O mapeamento parte dos Objetivos de Desenvolvimento Sustentável e "
    "de suas metas específicas, cruzados com as funcionalidades existentes e propostas do "
    "DentiBot e com o contexto regulatório brasileiro de resíduos de serviços de saúde, "
    "proteção de dados e exercício profissional em odontologia. <b>Nenhum número de impacto é "
    "afirmado neste documento</b> — o Anexo B define o que medir, não o que já foi medido, "
    "porque o sistema ainda não coleta esses indicadores (ODS-61). As referências normativas "
    "citadas indicam a direção de conformidade e devem ser confirmadas em sua redação vigente "
    "junto à ANVISA, ao CONAMA, à ANPD e aos conselhos de odontologia antes de qualquer "
    "declaração formal. A afirmação de alinhamento às ODS só se sustenta quando acompanhada de "
    "indicador medido e de evidência auditável; até lá, este documento descreve um compromisso "
    "de projeto, não um resultado alcançado.")

doc.build()
