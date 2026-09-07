# Documentos estratégicos do DentiBot

Cinco documentos de planejamento gerados a partir da auditoria do próprio
repositório. Cada um traz mais de 50 melhorias numeradas, com o arquivo ou área
alvo, estimativa de esforço e indicador de sucesso — os identificadores (`ST-`,
`UX-`, `IA-`, `ODS-`, `PN-`) são estáveis e podem ser referenciados em issues,
commits e pull requests.

| # | Documento | Conteúdo | Melhorias | Páginas |
|---|-----------|----------|-----------|---------|
| 01 | [Melhorias de Stack](01-Melhorias-de-Stack.pdf) | Arquitetura, dados, resiliência, observabilidade, segurança de plataforma, containers, CI/CD, frontend e plataforma de IA | 61 | 16 |
| 02 | [Melhorias de UX/UI Design](02-Melhorias-de-UX-UI-Design.pdf) | Design system, acessibilidade WCAG 2.2 AA, arquitetura de informação, fluxos clínicos, experiência do paciente e governança de design | 64 | 17 |
| 03 | [Novas Funcionalidades e IA](03-Novas-Funcionalidades-e-IA.pdf) | Copiloto clínico, visão computacional, agenda preditiva, comunicação autônoma, interoperabilidade e 11 apostas sem equivalente conhecido | 63 | 18 |
| 04 | [Melhorias ODS da ONU](04-Melhorias-ODS-ONU.pdf) | 11 Objetivos de Desenvolvimento Sustentável endereçados, cada melhoria com meta específica, indicador e evidência auditável | 61 | 14 |
| 05 | [Plano de Negócios](05-Plano-de-Negocios.pdf) | Mercado, concorrência, modelo de receita, economia unitária, cinco fases do MVP ao empreendimento, três cenários financeiros e riscos | 56 | 18 |

**Total: 305 melhorias em 83 páginas.**

## Como regenerar

```bash
pip install reportlab
python3 docs/generators/build_all.py
```

Ou um documento por vez:

```bash
python3 docs/generators/gen_01_stack.py
```

## Estrutura

```
docs/
├── README.md                      este arquivo
├── 01..05-*.pdf                   os documentos gerados
└── generators/
    ├── dentibot_doc.py            toolkit de apresentação (capa, sumário,
    │                              tabelas, cabeçalho/rodapé, paleta Pantone)
    ├── modelo_negocio.py          modelo financeiro do Documento 05
    ├── gen_01..05_*.py            conteúdo de cada documento
    └── build_all.py               regenera os cinco
```

### `dentibot_doc.py`

Camada de apresentação compartilhada. A paleta espelha as *custom properties*
de `:root` em `style.css`, de modo que a identidade visual dos documentos e a
do produto não divergem.

### `modelo_negocio.py`

Todas as premissas do plano de negócios em um único lugar, com o
dimensionamento de mercado, a economia unitária e a projeção de 36 meses
derivados delas. **Nenhum número financeiro é digitado no texto do Documento
05** — alterar uma premissa e reexecutar o gerador atualiza o documento inteiro
de forma consistente. Para inspecionar o modelo no terminal:

```bash
python3 docs/generators/modelo_negocio.py
```

## Limites destes documentos

Os documentos declaram os próprios limites em nota metodológica, e vale
repetir os principais aqui:

- **Documento 01 e 02** — as afirmações sobre o estado atual do código são
  verificáveis nos arquivos citados na coluna "Onde se aplica". As estimativas
  de esforço e as metas numéricas são propostas de partida, a refinar com a
  equipe e a recalibrar depois de medir a linha de base real.
- **Documento 03** — a alegação de ineditismo da Seção 10 é uma avaliação de
  repertório, não pesquisa de mercado; o Anexo C define a verificação que
  precisa ser feita antes de qualquer uso comercial dela. Os enquadramentos
  regulatórios indicam direção e não constituem parecer jurídico.
- **Documento 04** — o Anexo B define **o que medir**, não o que já foi medido:
  o sistema ainda não coleta esses indicadores (item ODS-61).
- **Documento 05** — as premissas de mercado são estimativas de ordem de
  grandeza para planejamento, com a fonte de validação indicada no Anexo A.
  Os preços de R$ 97 e R$ 197 são os efetivamente publicados em `index.html`;
  o restante é projeção.
