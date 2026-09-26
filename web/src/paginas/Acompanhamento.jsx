import { useState } from 'react';
import { Link } from 'react-router-dom';
import { dataCurta, reais, telHref } from '../apresentacao.js';
import { Celula, Tabela } from '../components/primitivos.jsx';
import { useRecurso } from '../dados.js';
import { prontuarioDe } from '../rotas.js';
import { Cabecalho, Estado, usePode } from './Layout.jsx';

const JANELAS = [30, 60, 90, 180];

/**
 * O que ficou para trás (Doc 03-A): tratamento parado (IA-35) e atendimento
 * sem registro (IA-05). Nada aqui é previsão — é o que o dado já mostra e
 * ninguém estava olhando.
 */
export default function Acompanhamento() {
  const pode = usePode();
  const leProntuario = pode('PRONTUARIO');
  const [dias, setDias] = useState(30);

  const parados = useRecurso(`/copiloto/tratamentos-parados?dias=${dias}`);
  // Sem permissão clínica a chamada nem sai: o 403 viraria tela de erro para
  // uma pergunta que simplesmente não é da recepção.
  const pendencias = useRecurso(leProntuario ? `/copiloto/pendencias?dias=${dias}` : null);

  const listaParados = parados.dados ?? [];
  const listaPendencias = pendencias.dados ?? [];

  return (
    <>
      <Cabecalho titulo="Acompanhamento."
                 detalhe="Tratamento parado e atendimento sem registro." />

      <div className="filtros">
        <label className="campo-app">
          <span className="cap cap-ash">Parado há pelo menos</span>
          <select value={dias} onChange={(e) => setDias(Number(e.target.value))}>
            {JANELAS.map((d) => <option key={d} value={d}>{d} dias</option>)}
          </select>
        </label>
      </div>

      <h2 className="sub secao-titulo">Tratamentos parados</h2>
      <Estado status={parados.status} erro={parados.erro} onTentarDeNovo={parados.recarregar}
              esqueleto={{ linhas: 4, colunas: 4 }}
              vazio={listaParados.length === 0
                ? `Nenhum plano aprovado parado há ${dias} dias ou mais sem consulta marcada.`
                : null}>
        <Tabela colunas={[
          { chave: 'paciente', rotulo: 'Paciente' },
          { chave: 'parado', rotulo: 'Parado há', num: true },
          { chave: 'valor', rotulo: 'Em aberto', num: true },
          { chave: 'falta', rotulo: 'Falta fazer' },
          { chave: 'contato', rotulo: 'Contato' },
        ]}>
          {listaParados.map((t) => (
            <tr key={t.idPaciente}>
              <Celula rotulo="Paciente">
                {leProntuario
                  ? <Link to={prontuarioDe(t.idPaciente)} state={{ nomePaciente: t.nomePaciente }}>
                      {t.nomePaciente ?? `Paciente ${t.idPaciente}`}
                    </Link>
                  : t.nomePaciente ?? `Paciente ${t.idPaciente}`}
              </Celula>
              <Celula rotulo="Parado há" num>{t.diasParado} dias</Celula>
              <Celula rotulo="Em aberto" num>{reais(t.valorEmAberto)}</Celula>
              <Celula rotulo="Falta fazer">{t.procedimentos.join(' · ')}</Celula>
              <Celula rotulo="Contato">
                {telHref(t.telefone) ? <a href={telHref(t.telefone)}>{t.telefone}</a> : '—'}
              </Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>

      <h2 className="sub secao-titulo">Registro pendente</h2>
      {!leProntuario ? (
        <p className="body body-ash">
          Registro clínico é do profissional: seu perfil não lê prontuário.
        </p>
      ) : (
        <Estado status={pendencias.status} erro={pendencias.erro}
                onTentarDeNovo={pendencias.recarregar} esqueleto={{ linhas: 3, colunas: 3 }}
                vazio={listaPendencias.length === 0
                  ? `Todo atendimento dos últimos ${dias} dias tem registro no prontuário.`
                  : null}>
          <Tabela colunas={[
            { chave: 'quando', rotulo: 'Quando' },
            { chave: 'paciente', rotulo: 'Paciente' },
            { chave: 'falta', rotulo: 'O que falta' },
          ]}>
            {listaPendencias.map((p) => (
              <tr key={`${p.tipo}-${p.idConsulta ?? ''}-${p.idItem ?? ''}`}>
                <Celula rotulo="Quando">{dataCurta(p.quando)}</Celula>
                <Celula rotulo="Paciente">
                  <Link to={prontuarioDe(p.idPaciente)} state={{ nomePaciente: p.nomePaciente }}>
                    {p.nomePaciente ?? `Paciente ${p.idPaciente}`}
                  </Link>
                </Celula>
                <Celula rotulo="O que falta">{p.descricao}</Celula>
              </tr>
            ))}
          </Tabela>
        </Estado>
      )}
    </>
  );
}
