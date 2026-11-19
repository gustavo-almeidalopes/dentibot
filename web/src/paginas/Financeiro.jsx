import { useMemo, useState } from 'react';
import { api } from '../api.js';
import { STATUS_RECEBIVEL, contagem } from '../apresentacao.js';
import Confirmar from '../components/Confirmar.jsx';
import { Aviso, Celula, Selo, Tabela } from '../components/primitivos.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { Cabecalho, Estado, usePode } from './Layout.jsx';

const DINHEIRO = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/* timeZone UTC porque `vencimentoEm` é um LocalDate — chega como "2026-01-31",
   sem hora. O Date interpreta isso como meia-noite UTC e, formatado no fuso de
   São Paulo, viraria 30/01: a parcela pareceria vencer um dia antes. */
const DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'UTC' });

/* BigDecimal do Jackson chega como número, mas basta alguém ligar
   WRITE_BIGDECIMAL_AS_PLAIN para virar string — e `format` de string devolve
   NaN em silêncio, que numa tela de dinheiro é o pior jeito de errar. */
const brl = (valor) => DINHEIRO.format(Number(valor ?? 0));

/* O "R$" vai em sobrescrito, como o preço dos planos na landing: o número é o
   que se lê de longe. formatToParts e não um split no espaço — o pt-BR usa
   espaço não separável, e o sinal de menos vem antes do símbolo. */
function Valor({ valor }) {
  const partes = DINHEIRO.formatToParts(Number(valor ?? 0));
  const moeda = partes.filter((p) => p.type === 'currency').map((p) => p.value).join('');
  const numero = partes.filter((p) => p.type !== 'currency' && p.type !== 'literal')
    .map((p) => p.value).join('');
  return <><sup>{moeda}</sup>{numero}</>;
}

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
 * <p>Recebimento tem tela — é o que a recepção faz no balcão, e a matriz dá a
 * ela FINANCEIRO/CRIAR justamente para isso. Despesa, comissão e estorno
 * existem no back-end (`POST /financeiro/...`) e ainda não têm.
 */
export default function Financeiro() {
  const resumo = useRecurso('/financeiro/resumo');
  const recebiveis = useRecurso(`/financeiro/recebiveis?limite=${LIMITE}`);
  const lista = recebiveis.dados ?? [];
  const pode = usePode();
  const podeReceber = pode('FINANCEIRO', 'CRIAR');
  const formas = useRecurso(podeReceber ? '/financeiro/formas' : null);
  const [aviso, setAviso] = useState(null);

  /* O recebimento mexe nos dois: o saldo da parcela e o "recebido no período". */
  const recebido = (mensagem) => {
    setAviso(mensagem);
    resumo.recarregar();
    recebiveis.recarregar();
  };

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
      <Cabecalho titulo="Financeiro."
                 detalhe={podeReceber ? 'Recebimento no balcão e leitura' : 'Visão de leitura'} />

      <Estado status={resumo.status} erro={resumo.erro} onTentarDeNovo={resumo.recarregar}
              esqueleto={{ linhas: 2, colunas: 5 }}>
        <div className="indicadores">
          {INDICADORES.map(([rotulo, campo, recorte]) => (
            <div className="indicador" key={campo}
                 data-alarme={campo === 'vencido' && Number(resumo.dados?.vencido) > 0
                   ? 'sim' : 'nao'}>
              <p className="cap cap-ash">{rotulo}</p>
              <p className="indicador-valor" aria-label={brl(resumo.dados?.[campo])}>
                <Valor valor={resumo.dados?.[campo]} />
              </p>
              <p className="cap cap-ash">{recorte}</p>
            </div>
          ))}
        </div>
      </Estado>

      {/* Lançar recebimento, despesa, comissão e estorno existem no back-end e
          não têm tela. Sem esta frase a pessoa procura o botão, não acha, e
          conclui que o sistema está quebrado. */}
      <p className="aviso" data-tom="info">
        Despesas, comissões e estornos ainda não têm tela nesta versão.
      </p>

      <Cabecalho secao titulo="Recebíveis." detalhe={recebiveis.status === 'ok'
        ? contagem(lista.length, LIMITE, 'parcela', 'parcelas')
        : 'Parcelas em aberto'} />

      <Aviso texto={aviso} />

      <Estado status={recebiveis.status} erro={recebiveis.erro}
              onTentarDeNovo={recebiveis.recarregar}
              esqueleto={{ linhas: 8, colunas: 6 }}
              vazio={lista.length === 0
                ? { titulo: 'Nada em aberto.', texto: 'Nenhuma parcela a receber.' }
                : null}>
        <Tabela colunas={[
          { chave: 'paciente', rotulo: 'Paciente' },
          { chave: 'parcela', rotulo: 'Parcela' },
          { chave: 'vencimento', rotulo: 'Vencimento', num: true },
          { chave: 'valor', rotulo: 'Valor', num: true },
          { chave: 'saldo', rotulo: 'Saldo', num: true },
          { chave: 'status', rotulo: 'Status' },
          ...(podeReceber ? [{ chave: 'acoes', rotulo: '' }] : []),
        ]}>
          {ordenada.map((r) => (
            <tr key={r.idRecebivel}>
              <Celula rotulo="Paciente" className="celula-nome">{nomeDoPaciente(r.idPaciente)}</Celula>
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
              {podeReceber && (
                <Celula rotulo="">
                  {Number(r.saldoDevedor) > 0 && r.status !== 'cancelado' && (
                    <Receber recebivel={r} paciente={nomeDoPaciente(r.idPaciente)}
                             formas={formas.dados ?? []} onRecebido={recebido} />
                  )}
                </Celula>
              )}
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}

/**
 * Registrar um recebimento contra UMA parcela.
 *
 * <p>Diálogo e não linha expandida: é dinheiro, e o foco preso do `<dialog>`
 * impede que um Enter perdido na tabela registre o valor errado.
 *
 * <p>A chave de idempotência nasce ao ABRIR o diálogo: dois cliques em
 * "Registrar" são o mesmo recebimento, e cobrar duas vezes no ledger
 * append-only só se desfaz com estorno.
 */
function Receber({ recebivel, paciente, formas, onRecebido }) {
  const saldo = Number(recebivel.saldoDevedor ?? 0);
  const [aberto, setAberto] = useState(false);
  const [valor, setValor] = useState('');
  const [idForma, setIdForma] = useState('');
  const [chave, setChave] = useState(null);
  const { executar, enviando, erro } = useAcao();

  const numero = Number(String(valor).replace(',', '.'));
  /* Acima do saldo o ledger ficaria com crédito a favor do paciente — que é
     outra operação, e não a que a recepção quis fazer. */
  const valido = numero > 0 && numero <= saldo;

  const abrir = () => {
    setValor(saldo.toFixed(2));
    setIdForma('');
    setChave(crypto.randomUUID());
    setAberto(true);
  };

  const registrar = async () => {
    const r = await executar(api.post('/financeiro/lancamentos', {
      idRecebivel: recebivel.idRecebivel,
      tipo: 'recebimento',
      valor: Number(numero.toFixed(2)),
      idForma: idForma ? Number(idForma) : null,
    }, { idempotencyKey: chave }));
    if (r === null) return;
    setAberto(false);
    onRecebido(`${brl(numero)} recebido de ${paciente} — parcela ${recebivel.parcelaNumero}/${recebivel.parcelaTotal}.`);
  };

  return (
    <>
      <button type="button" className="btn btn-sm" onClick={abrir}>Receber</button>
      <Confirmar
        aberto={aberto}
        titulo={`Receber de ${paciente}`}
        corpo={(
          <>
            <p>
              Parcela {recebivel.parcelaNumero}/{recebivel.parcelaTotal} · saldo de {brl(saldo)}.
            </p>
            <div className="dialogo-campos">
              <label className="campo-app">
                <span className="cap cap-ash">Valor recebido *</span>
                <input type="number" inputMode="decimal" step="0.01" min="0.01" max={saldo}
                       required value={valor} aria-invalid={!valido || undefined}
                       onChange={(e) => setValor(e.target.value)} />
              </label>
              {formas.length > 0 && (
                <label className="campo-app">
                  <span className="cap cap-ash">Forma</span>
                  <select value={idForma} onChange={(e) => setIdForma(e.target.value)}>
                    <option value="">—</option>
                    {formas.filter((f) => f.ativo).map((f) => (
                      <option key={f.idForma} value={f.idForma}>{f.nome}</option>
                    ))}
                  </select>
                </label>
              )}
            </div>
            {numero > saldo && (
              <p className="erro-campo" role="alert">Maior que o saldo da parcela.</p>
            )}
            {erro && <p className="erro-campo" role="alert">{erro.message}</p>}
          </>
        )}
        rotuloConfirmar={enviando ? 'Registrando…' : `Registrar ${valido ? brl(numero) : ''}`.trim()}
        rotuloVoltar="Voltar"
        podeConfirmar={valido && !enviando}
        onConfirmar={registrar}
        onCancelar={() => setAberto(false)}
      />
    </>
  );
}
