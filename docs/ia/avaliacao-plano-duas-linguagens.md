# Avaliação — `plano_duas_linguagens` (IA-04)

**Finalidade.** A partir de um orçamento já montado pelo dentista, gerar duas
versões do plano: a técnica (para prontuário e convênio) e a do paciente, em
linguagem simples — o porquê de cada etapa, alternativas e o que acontece se
nada for feito. É a peça que mais pesa na aceitação do tratamento.

**Base legal.** Execução do tratamento a pedido do titular e dever de
informação ao paciente (CDC, Código de Ética Odontológica). A entrada é o
orçamento — procedimentos, dentes e valores —, sem nome nem documento.

**Risco e contenção.**

| Risco | Contenção |
| --- | --- |
| Prometer resultado ("vai ficar perfeito") | Instrução proíbe promessa de resultado; o dentista revisa antes de entregar. |
| Incluir procedimento que não está no orçamento | Instrução restringe à lista; a lista vai junto na tela para conferência. |
| Explicação clínica errada | O texto é rascunho; o dentista é quem explica e assina. O descarte fica registrado. |

**População e viés.** A linguagem é ajustada para leigo, sem atributo do
paciente na entrada — não há subgrupo tratado de forma diferente.

**Monitoramento.** Taxa de descarte em `/api/v1/ia/consumo`; revisão acima de
40% num mês.

**Conjunto de referência.** Orçamentos com restauração, canal, extração e
prótese: a versão do paciente precisa citar cada procedimento, não pode conter
promessa de resultado nem valor diferente do orçamento.
