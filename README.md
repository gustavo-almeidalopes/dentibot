# DentiBot: Inovação em Gestão Odontológica com Sistema de Software

![DentiBot Logo](./logo.png) <!-- Faça o upload da imagem do logo para o repositório e substitua por './logo.png' ou URL hospedada -->

## Descrição

O DentiBot é um sistema de gestão odontológica inovador e acessível, desenvolvido para consultórios e clínicas de pequeno e médio porte. Ele visa superar as barreiras de complexidade e altos custos associadas a outras soluções do mercado, automatizando e simplificando tarefas administrativas e operacionais, como cadastro de pacientes, controle de prontuários, agendamentos e muito mais. Isso permite que os profissionais concentrem seus esforços no atendimento clínico e em procedimentos de maior complexidade.

O sistema integra funcionalidades essenciais de gestão clínica, incluindo:
- Prontuário digital seguro e acessível a qualquer momento.
- Agendamento inteligente com confirmação automática.
- Controle de estoque e financeiro.
- Emissão de relatórios gerenciais.
- Assinatura digital de documentos.
- Comunicação otimizada com pacientes via WhatsApp, SMS e e-mail.

Fundamentado em princípios de modelagem orientada a objetos (UML), o DentiBot combina tecnologia e gestão clínica em um modelo confiável e de rápida aprendizagem, servindo tanto como referência acadêmica quanto como ferramenta prática para clínicas odontológicas que buscam modernização, eficiência operacional e aprimoramento da experiência do paciente.

Este projeto foi desenvolvido para a disciplina de Engenharia de Software da Universidade da Cidade de São Paulo (UNICID), sob orientação do Prof. Jadir Custodio Mendonça Junior.

## Equipe de Desenvolvimento

| Número | Função | Descrição das Responsabilidades | Nome |
|--------|--------|---------------------------------|------|
| 1 | Líder | Acompanhamento das tarefas, atualização da documentação no Teams, integração do software com demais equipes, Redação técnica | Gustavo Roberto de Almeida Lopes |
| 2 | Analistas | Análise e especificação de requisitos | Baptista Luís Teixeira Chuma<br>Moisés Balsi Filho |
| 3 | Desenvolvedores | Desenho e Implementação | Baptista Luís Teixeira Chuma<br>Gustavo Roberto de Almeida Lopes |
| 4 | Testadores | Testes e gestão da qualidade | Samuel Soares Ferraz<br>Moisés Balsi Filho |
| 5 | Projetista de BD | Documentação e projeto de Banco de dados | Samuel Soares Ferraz<br>Moisés Balsi Filho |
| 6 | Documentador | Desenvolvimento e integração de toda documentação constante neste arquivo | Gustavo Roberto de Almeida Lopes<br>Samuel Soares Ferraz |

## Finalidade do Projeto

Desenvolver um sistema de gestão para clínicas odontológicas que otimize tarefas administrativas e operacionais, promovendo maior organização e eficiência. Além disso, atua como modelo conceitual e acadêmico de aplicação de software na área da saúde, demonstrando como a tecnologia pode apoiar a tomada de decisão e aprimorar a qualidade do atendimento clínico.

### Diagnóstico da Situação Atual

| Seq | Descrição do Problema |
|-----|-----------------------|
| 1 | Dentistas e secretárias perdem tempo executando tarefas repetitivas, como organização de agendas e prontuários manuais. |
| 2 | As soluções digitais disponíveis no mercado são de alto custo, inviabilizando a adoção por clínicas menores. |
| 3 | Falta de modelos acessíveis e didáticos para demonstração de sistemas de gestão aplicados à odontologia em ambientes acadêmicos. |
| 4 | O excesso de tarefas manuais aumenta o risco de erros e compromete a eficiência do atendimento. |
| 5 | A falta de padronização no uso de softwares clínicos gera dificuldades na adaptação dos profissionais. |
| 6 | A ausência de integração entre agendamento, prontuário, pagamentos e estoque dificulta o fluxo de trabalho. |
| 7 | Clínicas pequenas não têm recursos para treinamento em sistemas complexos, dificultando a inovação. |
| 8 | Dificuldade em implantar novas tecnologias de forma prática e segura no fluxo clínico existente. |

### Benefícios Esperados

| Seq | Descrição do Benefício |
|-----|------------------------|
| 1 | Redução do tempo gasto em tarefas repetitivas, aumentando a produtividade da clínica. |
| 2 | Criação de um sistema de baixo custo, acessível para clínicas pequenas e para fins acadêmicos. |
| 3 | Possibilidade de uso didático em universidades, ampliando o aprendizado em sistemas de gestão aplicados à odontologia. |
| 4 | Padronização e organização de informações clínicas, garantindo maior segurança e eficiência. |
| 5 | Liberação do profissional para focar em procedimentos de maior complexidade e valor clínico. |
| 6 | Facilitação da introdução de novas tecnologias no fluxo clínico, de forma prática e segura. |
| 7 | Estímulo à inovação tecnológica no setor odontológico, mesmo em ambientes com recursos limitados. |
| 8 | Melhoria na experiência do paciente, com atendimentos mais ágeis, organizados e seguros. |

## Atores e Sistemas Envolvidos

### Áreas de Negócio

- **Dentista**: Executar rotinas de apoio (organizar bandejas, acionar movimentos padronizados), selecionar e executar rotinas do DentiBot, disparo de comandos hands-free, uso do E-Stop, checklist de biossegurança, registro de observações.
- **Recepcionista/Atendente**: Agendamento, confirmação e comunicação com pacientes, enviar lembretes, registrar observações, reagendar, interface com dentistas.
- **Almoxarife/Responsável por Esterilização**: Rastrear kits, liberar/bloquear kits, inventário, comunicação com DentiBot.
- **Coordenador/Administrador**: Gestão de perfis e permissões, homologação de macros, auditoria de logs, backup/restore, planejamento de manutenção, conformidade com LGPD.

### Atores Externos

- **WhatsApp Business API**: Enviar mensagens de lembrete, receber respostas, webhooks de status, integração com agenda e logs.
- **Google Maps Platform**: Geocodificar endereços, calcular tempos e distâncias, validar endereços, integrar com agendamento, suportar replanejamento.
- **Provedor de E-mail (SMTP/API)**: Entregar e-mails, webhooks de bounce, gerar relatórios, integrar com logs.
- **Google Login API**: Simplificar cadastro e login, autenticar identidade, obter informações de perfil, gerenciar OAuth, reduzir riscos de segurança, experiência consistente em plataformas.

### Sistemas Envolvidos

- **Sistemas de Convênios/Operadoras (TISS/TUSS)**: Retornar autorização/glosa, enviar códigos TUSS, fornecer regras para auditoria.

## Requisitos do Projeto

Os requisitos estão apresentados como templates no documento de visão. Eles incluem requisitos funcionais (RF), regras de negócio (RN) e não funcionais, abrangendo qualidade, funcionalidade, confiabilidade, usabilidade, implementação e tecnológicos. Como são placeholders, o desenvolvimento deve preencher esses com detalhes específicos.

Exemplos de categorias não funcionais:
- **Conformidade**: Normas como portaria 95893/42, NBR13596, ISO/IEC12119.
- **Segurança de Acesso**: Evitar acesso não autorizado, ex.: uso de Token.
- **Disponibilidade**: 24 horas, inclusive fins de semana.
- **Recuperabilidade**: Backups diários.
- **Segurança**: Riscos pessoais, materiais, etc.
- **Inteligibilidade**: Glossário e help on-line.
- **Apreensibilidade**: Help on-line e formação de multiplicadores.
- **Operacionalidade**: Consultas consolidadas.
- **Plataforma**: Distribuída e Mainframe.
- **Tecnologia**: Webservices, Wireless, Token.
- **Implantação**: Em iterações, em unidades piloto.

Requisitos inversos: Especificar o que NÃO será atendido para clareza.

## Instalação e Uso

O projeto está em desenvolvimento. Instruções serão adicionadas conforme o código for implementado.

1. Clone o repositório: `git clone https://github.com/gustavo-almeidalopes/dentibot.git`
2. Instale dependências: (a definir, ex.: Python com bibliotecas como Flask, SQLAlchemy, etc.).
3. Rode o sistema: (a definir).

## Referências

- QUEIROGA, Isis Samara de Melo et al. Robótica na cirurgia odontológica: revisão integrativa. Research, Society and Development, v. 10, n. 4, p. e137301, 2021. Disponível em: [link](https://rsdjournal.org/rsd/article/view/13730/12361). Acesso em: 24 ago. 2025.
- LIU, L. Robotics in Dentistry: A Narrative Review. Journal of Dental Research, v. 102, n. 4, p. 409-416, 2023. Disponível em: [link](https://pmc.ncbi.nlm.nih.gov/articles/PMC10047128/). Acesso em: 24 ago. 2025.
- BAHRAMI, R. et al. Robot-assisted dental implant surgery procedure. Journal of Prosthodontic Research, v. 68, n. 4, p. 391-396, 2024. Disponível em: [link](https://www.sciencedirect.com/science/article/pii/S199179022400076X). Acesso em: 24 ago. 2025.
- VESLI, E. The future of dentistry through robotics. British Dental Journal, v. 239, p. 1-4, 2025. Disponível em: [link](https://www.nature.com/articles/s41415-025-8345-8). Acesso em: 24 ago. 2025.
- ALVES, V. P. Efetividade da inteligência artificial em detectar cárie dentária. Revista Cromg, v. 2023, p. 1-10, 2023. Disponível em: [link](https://revista.cromg.org.br/index.php/rcromg/article/download/490/275/2219). Acesso em: 24 ago. 2025.

## Licença

Versão: 1.1.0 (04/09/2025)  
