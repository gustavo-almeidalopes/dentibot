import { useState } from 'react';
import { api } from '../api.js';
import { Aviso, Celula, Tabela } from '../components/primitivos.jsx';
import { useRecurso } from '../dados.js';
import { Cabecalho, Estado } from './Layout.jsx';

const DOLAR = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'USD', maximumFractionDigits: 4 });

const RECURSOS = {
  nota_clinica: 'Nota a partir do ditado (IA-01)',
  plano_duas_linguagens: 'Plano em duas linguagens (IA-04)',
};

const POLITICA = 'https://github.com/gustavo-almeidalopes/dentibot/blob/main/docs/ia/politica.md';

/**
 * IA da clínica (IA-59, IA-63): quanto custou, o que virou registro, e o que
 * está ligado. Custo à vista é a condição para cobrar por consumo — e para a
 * clínica confiar no que paga.
 */
export default function Ia() {
  const configuracao = useRecurso('/ia/configuracao');
  const consumo = useRecurso('/ia/consumo');
  const c = configuracao.dados;

  return (
    <>
      <Cabecalho titulo="IA." detalhe="O que a clínica gasta, o que virou registro, o que está ligado." />
      <p className="body body-ash">
        A IA daqui rascunha; quem decide e assina é o dentista. Nenhum nome, CPF ou telefone de
        paciente sai para o provedor. <a href={POLITICA}>Política de IA</a>.
      </p>

      <Estado status={configuracao.status} erro={configuracao.erro}
              onTentarDeNovo={configuracao.recarregar} esqueleto={{ linhas: 2, colunas: 3 }}>
        {c && (
          <>
            <div className="indicadores">
              <div className="indicador"
                   data-alarme={Number(c.gastoNoMesUsd) >= Number(c.cotaMensalUsd) ? 'sim' : undefined}>
                <p className="cap cap-ash">Gasto no mês</p>
                <p className="indicador-valor">{DOLAR.format(c.gastoNoMesUsd)}</p>
                <p className="body body-ash">de {DOLAR.format(c.cotaMensalUsd)} de cota</p>
              </div>
              <div className="indicador" data-alarme={c.provedorConfigurado ? undefined : 'sim'}>
                <p className="cap cap-ash">Provedor</p>
                <p className="indicador-valor">{c.provedorConfigurado ? 'Configurado' : 'Sem chave'}</p>
                <p className="body body-ash">{c.modelo}</p>
              </div>
            </div>
            <Configuracao atual={c} onSalvo={configuracao.recarregar} />
          </>
        )}
      </Estado>

      <h2 className="sub secao-titulo">Consumo do mês</h2>
      <Estado status={consumo.status} erro={consumo.erro} onTentarDeNovo={consumo.recarregar}
              esqueleto={{ linhas: 2, colunas: 5 }}
              vazio={(consumo.dados ?? []).length === 0 ? 'Nenhuma chamada de IA neste mês.' : null}>
        <Tabela colunas={[
          { chave: 'recurso', rotulo: 'Recurso' },
          { chave: 'chamadas', rotulo: 'Chamadas', num: true },
          { chave: 'aceitas', rotulo: 'Aceitas', num: true },
          { chave: 'descartadas', rotulo: 'Descartadas', num: true },
          { chave: 'erros', rotulo: 'Erros', num: true },
          { chave: 'custo', rotulo: 'Custo', num: true },
        ]}>
          {(consumo.dados ?? []).map((l) => (
            <tr key={l.recurso}>
              <Celula rotulo="Recurso">{RECURSOS[l.recurso] ?? l.recurso}</Celula>
              <Celula rotulo="Chamadas" num>{l.chamadas}</Celula>
              <Celula rotulo="Aceitas" num>{l.aceitas}</Celula>
              <Celula rotulo="Descartadas" num>{l.descartadas}</Celula>
              <Celula rotulo="Erros" num>{l.erros}</Celula>
              <Celula rotulo="Custo" num>{DOLAR.format(l.custoUsd)}</Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}

function Configuracao({ atual, onSalvo }) {
  const [desligados, setDesligados] = useState(atual.recursosDesligados);
  const [cota, setCota] = useState(String(atual.cotaMensalUsd));
  const [estado, setEstado] = useState({ enviando: false, erro: null, sucesso: null });

  const salvar = async (e) => {
    e.preventDefault();
    setEstado({ enviando: true, erro: null, sucesso: null });
    try {
      await api.put('/ia/configuracao', { recursosDesligados: desligados, cotaMensalUsd: Number(cota) });
      setEstado({ enviando: false, erro: null, sucesso: 'Configuração de IA salva.' });
      onSalvo();
    } catch (err) {
      setEstado({ enviando: false, erro: err.message, sucesso: null });
    }
  };

  return (
    <form className="form-bloco" onSubmit={salvar}>
      <p className="cap cap-ash">Recursos</p>
      {atual.recursosDisponiveis.map((r) => (
        <label key={r} className="campo-check">
          <input type="checkbox" checked={!desligados.includes(r)}
                 onChange={(e) => setDesligados((d) => (e.target.checked
                   ? d.filter((x) => x !== r) : [...d, r]))} />
          {' '}{RECURSOS[r] ?? r}
        </label>
      ))}
      <label className="campo-app">
        <span className="cap cap-ash">Cota mensal (US$) — ao atingir, a IA para até o mês virar</span>
        <input type="number" min="0" step="0.01" value={cota} onChange={(e) => setCota(e.target.value)} />
      </label>
      <Aviso texto={estado.sucesso} />
      <Aviso texto={estado.erro} tom="erro" />
      <div className="acoes">
        <button type="submit" className="btn btn-fill" disabled={estado.enviando}>
          {estado.enviando ? 'Salvando…' : 'Salvar'}
        </button>
      </div>
    </form>
  );
}
