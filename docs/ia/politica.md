# Política de IA do DentiBot

Documento público (IA-63). Vale para todo recurso de inteligência artificial do
produto, hoje e depois. Se um recurso não cumpre o que está aqui, ele não é
lançado.

## O limite que não se negocia

**A IA assiste; quem decide é o cirurgião-dentista.** Nenhum recurso emite
diagnóstico, prescreve tratamento ou grava registro clínico sozinho. Software
com finalidade diagnóstica é dispositivo médico (ANVISA) e o ato clínico é
privativo do profissional (CFO). A IA do DentiBot prioriza, rascunha e explica;
o dentista revisa, assina e responde.

## Os seis controles, e onde cada um mora no código

| Controle | Como é garantido | Onde |
| --- | --- | --- |
| Rastreabilidade | Toda chamada grava modelo, entrada já redigida, saída, tokens e custo, em tabela sem UPDATE nem DELETE para a aplicação. | `ia.chamadas` (V23), `GatewayDeIa` |
| Redação de PII antes do envio | CPF, CNPJ, e-mail e telefone saem pela forma; o nome do paciente sai porque quem chama o informa. O provedor vê `[PACIENTE]`. | `GatewayDeIa.redigir`, `ScrubberDePii` |
| Confirmação humana registrada | A sugestão volta para a tela; o aceite ou o descarte é uma decisão identificada, uma por sugestão, também sem UPDATE. | `ia.confirmacoes` (V23), `POST /api/v1/ia/chamadas/{id}/confirmacao` |
| Desligamento por funcionalidade | A clínica desliga qualquer recurso de IA sem perder o resto do sistema. | `ia.configuracao`, `PUT /api/v1/ia/configuracao` |
| Custo visível e cota | O custo por clínica é público para a própria clínica (IA-59), e a cota mensal bloqueia (429) antes de estourar. | `GET /api/v1/ia/consumo` |
| Avaliação contínua e viés | Cada recurso tem uma avaliação escrita antes do lançamento, com conjunto de referência e a pergunta de viés respondida. | `docs/ia/avaliacao-*.md` |

## Provedor

Os modelos são da Anthropic (Claude), pela API — o provedor é
**sub-processador** e está listado em
[`docs/security/subprocessadores.md`](../security/subprocessadores.md). Nenhum
identificador direto do paciente é enviado. O texto clínico redigido é enviado
para processamento e não é usado pelo provedor para treino, conforme os termos
comerciais da API.

## Antes de lançar um recurso novo

Uma avaliação em `docs/ia/avaliacao-<recurso>.md`, respondendo:

1. **Finalidade** — que dor resolve, para quem.
2. **Base legal** (LGPD art. 7 e 11) — por que o dado pode ser tratado assim.
3. **Risco** — o que acontece se a saída estiver errada, e como o erro é
   detectado antes de causar dano.
4. **População afetada** e **viés** — há subgrupo que o recurso trataria pior?
5. **Monitoramento** — que número, olhado com que frequência, desliga o recurso.
6. **Conjunto de referência** — os casos que toda versão nova precisa acertar.

Recurso sem avaliação não entra em `IaApi.RECURSOS`, e o gateway recusa
recurso fora dessa lista.

## O que NÃO é IA, e não é chamado assim

Resumo de retorno, radar de abandono, alertas falados, risco de falta,
sugestão de compra e pendências de registro (Doc 03-A) são **regra e contagem
sobre o dado da clínica** — sem modelo, auditáveis por qualquer pessoa. A
contagem de faltas usa só o histórico da própria pessoa: nenhum dado de bairro,
idade ou renda entra, e por isso não há como ela discriminar subgrupo.
