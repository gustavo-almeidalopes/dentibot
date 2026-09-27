import { useMemo } from 'react';
import { STATUS_RECEBIVEL, contagem } from '../apresentacao.js';
import { Celula, Selo, Tabela } from '../components/primitivos.jsx';
import { useRecurso } from '../dados.js';
import { Cabecalho, Estado } from './Layout.jsx';

const DINHEIRO = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/* timeZone UTC porque `vencimentoEm` é um LocalDate — chega como "2026-01-31",
   sem hora. O Date interpreta isso como meia-noite UTC e, formatado no fuso de
   São Paulo, viraria 30/01: a parcela pareceria vencer um dia antes. */
const DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'UTC' });

/* BigDecimal do Jackson chega como número, mas basta alguém ligar
   WRITE_BIGDECIMAL_AS_PLAIN para virar string — e `format` de string devolve
   NaN em silêncio, que numa tela de dinheiro é o pior jeito de errar. */
const brl = (valor) => DINHEIRO.format(Number(valor ?? 0));

const LIMITE = 200;

/* O recorte de CADA número, porque eles não compartilham um.
   "Do primeiro dia do mês até hoje" estava no cabeçalho como se valesse para
   os cinco, e só `recebidoNoPeriodo` é do período — os outros são saldo
   corrente. Numa tela de dinheiro isso não é imprecisão, é erro. */
const INDICADORES = [
  ['Recebido no período', 'recebidoNoPeriodo', 'do dia 1 até hoje'],
  ['A receber', 'aReceber', 'saldo em aberto, todas as datas'],
  ['Vencido', 'vencido', 'saldo em aberto já vencido'],
  ['A pagar', 'aPagar', 'despesas em aberto'],
  ['Comissões previstas', 'comissoesPrevistas', 'ainda não liberadas'],
];

/**
 * Painel do financeiro: o que entrou, o que falta entrar, o que já venceu.
 *
 * <p>`recebidoNoPeriodo` vem do ledger e `aReceber`/`vencido` dos recebíveis em
 * aberto — perguntas diferentes, mostradas separadas de propósito. Somá-las é
 * como uma clínica acredita ter faturado o que ainda não recebeu.
 *
 * <p>Só leitura. Lançar recebimento, despesa, comissão e estorno existem no
 * back-end (`POST /financeiro/...`) e ainda não têm tela.
 */
export default function Financeiro() {
  const resumo = useRecurso('/financeiro/resumo');
  const recebiveis = useRecurso(`/financeiro/recebiveis?limite=${LIMITE}`);
  // useMemo e não `?? []`: um array novo a cada render invalidava a ordenação abaixo.
  const lista = useMemo(() => recebiveis.dados ?? [], [recebiveis.dados]);

  /* Exceção deliberada ao "nenhum endpoint novo": RecebivelResumo traz
     idPaciente e não o nome, e uma cobrança sem nome não dá para conferir. */
  const pacientes = useRecurso(`/pacientes?limite=${LIMITE}`);
  const nomeDoPaciente = useMemo(() => {
    const mapa = new Map((pacientes.dados ?? []).map((p) => [p.idPaciente, p.nomeCompleto]));
    return (id) => (id == null ? '—' : mapa.get(id) ?? `Paciente ${id}`);
  }, [pacientes.dados]);

  /* Ordem imposta aqui: a pergunta desta tabela é "o que vence primeiro".
     Parcela sem vencimento vai para o fim, e não para o começo como faria a
     comparação com undefined. */
  const ordenada = useMemo(() => [...lista].sort((a, b) =>
    String(a.vencimentoEm ?? '9999').localeCompare(String(b.vencimentoEm ?? '9999'))),
  [lista]);

  return (
    <>
      <Cabecalho titulo="Financeiro." detalhe="Visão de leitura" />

      <Estado status={resumo.status} erro={resumo.erro} onTentarDeNovo={resumo.recarregar}
              esqueleto={{ linhas: 2, colunas: 5 }}>
        <div className="indicadores">
          {INDICADORES.map(([rotulo, campo, recorte]) => (
            <div className="indicador" key={campo}
                 data-alarme={campo === 'vencido' && Number(resumo.dados?.vencido) > 0
                   ? 'sim' : 'nao'}>
              <p className="cap cap-ash">{rotulo}</p>
              <p className="indicador-valor">{brl(resumo.dados?.[campo])}</p>
              <p className="cap cap-ash">{recorte}</p>
            </div>
          ))}
        </div>
      </Estado>

      {/* Lançar recebimento, despesa, comissão e estorno existem no back-end e
          não têm tela. Sem esta frase a pessoa procura o botão, não acha, e
          conclui que o sistema está quebrado. */}
      <p className="cap cap-ash">
        Lançamentos, despesas e comissões ainda não têm tela nesta versão.
      </p>

      <Cabecalho titulo="Recebíveis." detalhe={recebiveis.status === 'ok'
        ? contagem(lista.length, LIMITE, 'parcela', 'parcelas')
        : 'Parcelas em aberto'} />

      <Estado status={recebiveis.status} erro={recebiveis.erro}
              onTentarDeNovo={recebiveis.recarregar}
              esqueleto={{ linhas: 8, colunas: 6 }}
              vazio={lista.length === 0 ? 'Nenhuma parcela em aberto.' : null}>
        <Tabela colunas={[
          { chave: 'paciente', rotulo: 'Paciente' },
          { chave: 'parcela', rotulo: 'Parcela' },
          { chave: 'vencimento', rotulo: 'Vencimento', num: true },
          { chave: 'valor', rotulo: 'Valor', num: true },
          { chave: 'saldo', rotulo: 'Saldo', num: true },
          { chave: 'status', rotulo: 'Status' },
        ]}>
          {ordenada.map((r) => (
            <tr key={r.idRecebivel}>
              <Celula rotulo="Paciente">{nomeDoPaciente(r.idPaciente)}</Celula>
              <Celula rotulo="Parcela">{r.parcelaNumero}/{r.parcelaTotal}</Celula>
              {/* `new Date(null)` é 01/01/1970, não erro — numa coluna de
                  vencimento isso passa por dado de verdade. */}
              <Celula rotulo="Vencimento" num>
                {r.vencimentoEm ? DATA.format(new Date(r.vencimentoEm)) : '—'}
              </Celula>
              <Celula rotulo="Valor" num>{brl(r.valorParcela)}</Celula>
              {/* saldoDevedor é SUM sobre o ledger (vw_saldo_recebivel), não
                  coluna guardada — não existe caminho para discordar do
                  extrato. Por isso é ele que aparece, e não uma subtração
                  feita aqui. */}
              <Celula rotulo="Saldo" num>{brl(r.saldoDevedor)}</Celula>
              <Celula rotulo="Status"><Selo mapa={STATUS_RECEBIVEL} valor={r.status} /></Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}
