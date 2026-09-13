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
  const recebiveis = useRecurso('/financeiro/recebiveis?limite=100');
  const lista = recebiveis.dados ?? [];

  return (
    <>
      <Cabecalho titulo="Financeiro." detalhe="Do primeiro dia do mês até hoje" />

      <Estado status={resumo.status} erro={resumo.erro} onTentarDeNovo={resumo.recarregar}>
        <ul className="lista">
          {[
            ['Recebido no período', 'recebidoNoPeriodo'],
            ['A receber', 'aReceber'],
            ['Vencido', 'vencido'],
            ['A pagar', 'aPagar'],
            ['Comissões previstas', 'comissoesPrevistas'],
          ].map(([rotulo, campo]) => (
            <li className="row" key={campo}>
              <div><p className="sub">{rotulo}</p></div>
              <div>
                <p className="body" style={campo === 'vencido' && Number(resumo.dados?.vencido) > 0
                  ? { color: 'var(--alarm)' } : undefined}>
                  {brl(resumo.dados?.[campo])}
                </p>
              </div>
            </li>
          ))}
        </ul>
      </Estado>

      <Cabecalho titulo="Recebíveis." detalhe={recebiveis.status === 'ok'
        ? `${lista.length} parcelas`
        : 'GET /api/v1/financeiro/recebiveis'} />

      <Estado status={recebiveis.status} erro={recebiveis.erro}
              onTentarDeNovo={recebiveis.recarregar}
              vazio={lista.length === 0 ? 'Nenhuma parcela em aberto.' : null}>
        <ul className="lista">
          {lista.map((r) => (
            <li className="row" key={r.idRecebivel}>
              <div>
                <p className="sub">Parcela {r.parcelaNumero}/{r.parcelaTotal}</p>
                <p className="cap cap-ash">{r.status}</p>
              </div>
              <div>
                <p className="body">{brl(r.valorParcela)}</p>
                {/* saldoDevedor é SUM sobre o ledger (vw_saldo_recebivel), não
                    coluna guardada — não existe caminho para discordar do
                    extrato. Por isso é ele que aparece, e não uma subtração
                    feita aqui. */}
                <p className="cap cap-ash">Saldo {brl(r.saldoDevedor)}</p>
              </div>
              <div className="acoes">
                {/* `new Date(null)` é 01/01/1970, não erro — numa coluna de
                    vencimento isso passa por dado de verdade. */}
                <p className="body">
                  {r.vencimentoEm ? DATA.format(new Date(r.vencimentoEm)) : '—'}
                </p>
              </div>
            </li>
          ))}
        </ul>
      </Estado>
    </>
  );
}
