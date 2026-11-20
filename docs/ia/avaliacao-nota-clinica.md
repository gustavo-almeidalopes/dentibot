# Avaliação — `nota_clinica` (IA-01)

**Finalidade.** O dentista dita (ou digita) em linguagem natural o que fez; o
sistema devolve um rascunho de evolução e sugestões de lançamento no
odontograma, para revisar. O ganho é registrar durante o atendimento, e não no
fim do dia, quando o detalhe já se perdeu.

**Base legal.** Tutela da saúde (LGPD art. 11, II, f), pelo profissional que já
trata o paciente; o registro clínico é obrigação (CFO-226/2020). O texto sai
redigido — sem nome, CPF, telefone ou e-mail.

**Risco e contenção.**

| Risco | Contenção |
| --- | --- |
| O modelo inventa procedimento que não foi feito | Instrução explícita para não inventar; o que não estiver no ditado vai para "não ancorado", mostrado à parte. |
| Dente ou condição fora do vocabulário | Validação por regra: dente fora da notação FDI ou condição fora da lista é descartado da sugestão e listado como não confirmado. |
| O rascunho entra no prontuário sem revisão | Não entra: nada é gravado pela IA. O texto vai para o campo editável; quem grava é o dentista, e o aceite fica em `ia.confirmacoes`. |
| Nome do paciente vazando no ditado | O nome é passado ao gateway e substituído por `[PACIENTE]` antes do envio. |

**População e viés.** Afeta o registro, não a conduta; não usa atributo do
paciente além do ditado. Risco de viés baixo; o monitoramento é por taxa de
descarte.

**Monitoramento.** Taxa de descarte (`descartadas / chamadas`, em
`/api/v1/ia/consumo`). Acima de 40% num mês, o recurso é revisto; a clínica
pode desligá-lo a qualquer momento.

**Conjunto de referência.** Os ditados de `api/src/test/resources/ia/` (quando
houver chave de teste) — cada versão de instrução precisa manter os dentes e
condições esperados.
