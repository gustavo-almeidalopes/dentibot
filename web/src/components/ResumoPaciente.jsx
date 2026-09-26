import { useEffect, useRef } from 'react';
import { dataCurta, reais } from '../apresentacao.js';
import { useRecurso } from '../dados.js';
import { Estado } from '../paginas/Layout.jsx';
import { falar, vozDisponivel } from '../voz.js';

/**
 * Meia tela antes de chamar o paciente (IA-06), com os alertas falados (IA-27).
 *
 * <p>Cada parte chega do back-end com `permitido`: a recepção recebe o mesmo
 * resumo sem a ficha clínica, e aqui isso vira "sem acesso" — nunca um "nenhum
 * alerta" que diria ao dentista que o paciente não tem alergia.
 *
 * <p>`falarAoAbrir`: quem abre o resumo na agenda está prestes a chamar o
 * paciente, e o clique que abriu é o gesto que o navegador exige para falar.
 */
export default function ResumoPaciente({ idPaciente, falarAoAbrir = false }) {
  const resumo = useRecurso(`/pacientes/${idPaciente}/resumo`);
  const r = resumo.dados;
  const avisos = r?.alertas?.dados?.avisos;
  const falou = useRef(false);

  useEffect(() => {
    if (falarAoAbrir && !falou.current && avisos?.length) {
      falou.current = true;
      falar(avisos);
    }
  }, [falarAoAbrir, avisos]);

  return (
    <section className="resumo" aria-label="Resumo do paciente">
      <Estado status={resumo.status} erro={resumo.erro} onTentarDeNovo={resumo.recarregar}
              esqueleto={{ linhas: 2, colunas: 4 }}>
        {r && (
          <>
            <Alertas secao={r.alertas} />
            <div className="indicadores">
              <Indicador rotulo="Última conduta" secao={r.ultimaEvolucao}
                         valor={(e) => (e ? dataCurta(e.registradoEm) : 'Nenhuma')}
                         detalhe={(e) => e && resumir(e.descricao)} />
              <Indicador rotulo="Plano em aberto" secao={r.planoEmAberto}
                         valor={(itens) => `${itens.length} ${itens.length === 1 ? 'item' : 'itens'}`}
                         detalhe={(itens) => itens.map(rotuloDoItem).join(' · ') || null} />
              <Indicador rotulo="A receber" secao={r.financeiro}
                         valor={(f) => reais(f.emAberto)}
                         detalhe={(f) => (Number(f.vencido) > 0 ? `${reais(f.vencido)} vencido` : null)}
                         alarme={(f) => Number(f.vencido) > 0} />
              <Indicador rotulo="Agenda" secao={r.agenda}
                         valor={(a) => (a?.proximaMarcada ? `Próxima ${dataCurta(a.proximaMarcada)}` : 'Nada marcado')}
                         detalhe={(a) => a?.ultimaRealizada && `Última ${dataCurta(a.ultimaRealizada)}`} />
            </div>
          </>
        )}
      </Estado>
    </section>
  );
}

function Alertas({ secao }) {
  if (!secao.permitido) {
    return <p className="cap cap-ash">Alertas clínicos: não disponíveis para o seu perfil.</p>;
  }
  const avisos = secao.dados?.avisos ?? [];
  if (avisos.length === 0) {
    return <p className="cap cap-ash">Nenhum alerta na triagem de saúde.</p>;
  }
  return (
    <div className="resumo-alertas" role="alert">
      <p className="cap">Alertas</p>
      <ul>
        {avisos.map((a) => <li key={a}>{a}</li>)}
      </ul>
      {vozDisponivel() && (
        <button type="button" className="btn btn-sm" onClick={() => falar(avisos)}>
          Ouvir de novo
        </button>
      )}
    </div>
  );
}

function Indicador({ rotulo, secao, valor, detalhe, alarme }) {
  const permitido = secao.permitido;
  const d = secao.dados;
  return (
    <div className="indicador" data-alarme={permitido && alarme?.(d) ? 'sim' : undefined}>
      <p className="cap cap-ash">{rotulo}</p>
      <p className="indicador-valor">{permitido ? valor(d) : 'Sem acesso'}</p>
      {permitido && detalhe?.(d) && <p className="body body-ash">{detalhe(d)}</p>}
    </div>
  );
}

const rotuloDoItem = (i) =>
  `${i.nomeProcedimento ?? 'Procedimento'}${i.dente ? ` (${i.dente})` : ''}`;

const resumir = (texto) => (texto && texto.length > 140 ? `${texto.slice(0, 139)}…` : texto);
