import { useEffect, useState } from 'react';
import { api } from '../api.js';

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

/* O que foi tocado, na língua de quem não trabalha na clínica. */
const RECURSOS = {
  'prontuario.evolucao': 'as anotações do seu atendimento',
  'prontuario.odontograma': 'o mapa dos seus dentes',
  'prontuario.anexo': 'suas radiografias e fotos',
  'prontuario.alertas': 'seus alertas de saúde',
  'lgpd.consentimento': 'seus consentimentos',
  'lgpd.preferencia': 'suas autorizações',
  'lgpd.acesso_titular': 'o link desta página',
  paciente: 'uma cópia dos seus dados',
};
const ACOES = { leitura: 'abriu', exportacao: 'exportou', criacao: 'registrou' };

/**
 * O paciente, sem conta, pelo link que a clínica entregou (IA-52, IA-53).
 *
 * <p>O token vive no fragmento (#): o navegador não o manda a servidor nenhum
 * nem o põe no Referer. Ele só sai daqui no corpo de um POST à API.
 */
export default function Titular() {
  const [token] = useState(() => window.location.hash.slice(1));
  const [painel, setPainel] = useState(null);
  const [falha, setFalha] = useState(token ? null : 'sem-token');

  useEffect(() => {
    if (!token) return undefined;
    let vivo = true;
    api.post('/titular/painel', { token })
      .then((p) => vivo && setPainel(p))
      .catch((e) => vivo && setFalha(e.status === 404 ? 'invalido' : e.message));
    return () => { vivo = false; };
  }, [token]);

  if (falha) {
    return (
      <main id="main" className="sec edge">
        <h1 className="display">Link<br />inválido.</h1>
        <p className="body body-ash titular-texto">
          {falha === 'sem-token' || falha === 'invalido'
            ? 'Este link não abre mais: ele expira em 30 dias, e cada link novo cancela o anterior. Peça outro à clínica.'
            : falha}
        </p>
      </main>
    );
  }
  if (!painel) {
    return <main id="main" className="sec edge"><p className="cap cap-ash">Carregando…</p></main>;
  }

  return (
    <main id="main" className="sec edge">
      <h1 className="display">Seus<br />dados.</h1>
      <p className="body body-ash titular-texto">
        Na {painel.clinica}. Aqui você confere quem acessou o seu prontuário e decide, uma a uma,
        o que autoriza. O que você mudar vale na hora.
      </p>
      <Autorizacoes token={token} iniciais={painel.preferencias} />
      <Extrato token={token} acessos={painel.acessos} />
    </main>
  );
}

function Autorizacoes({ token, iniciais }) {
  const [preferencias, setPreferencias] = useState(iniciais);
  const [erro, setErro] = useState(null);

  const alternar = async (p) => {
    setErro(null);
    try {
      setPreferencias(await api.post('/titular/preferencias',
        { token, finalidade: p.finalidade, permitido: !p.permitido }));
    } catch (e) {
      setErro(e.message);
    }
  };

  return (
    <section className="titular-secao" aria-label="O que você autoriza">
      <h2 className="sub">O que você autoriza</h2>
      {erro && <p className="body titular-erro" role="alert">{erro}</p>}
      <ul className="titular-lista">
        {preferencias.map((p) => (
          <li key={p.finalidade}>
            <p className="body">{p.descricao}</p>
            <p className="cap cap-ash">
              {p.permitido ? 'Autorizado' : 'Não autorizado'}
              {p.alteradaEm && ` desde ${DATA_HORA.format(new Date(p.alteradaEm))}`}
            </p>
            <button type="button" className="btn" onClick={() => alternar(p)}
                    aria-pressed={p.permitido}>
              {p.permitido ? 'Não autorizo' : 'Autorizo'}
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}

function Extrato({ token, acessos }) {
  return (
    <section className="titular-secao" aria-label="Quem acessou seus dados">
      <h2 className="sub">Quem acessou seus dados</h2>
      <p className="body body-ash titular-texto">
        Os últimos 12 meses, como um extrato. Não reconhece um acesso? Conteste: a clínica tem
        15 dias para responder.
      </p>
      {acessos.length === 0
        ? <p className="body body-ash">Nenhum acesso registrado.</p>
        : (
          <ul className="titular-lista">
            {acessos.map((a) => <Acesso key={a.idEvento} token={token} a={a} />)}
          </ul>
        )}
    </section>
  );
}

function Acesso({ token, a }) {
  const [aberto, setAberto] = useState(false);
  const [texto, setTexto] = useState('');
  const [estado, setEstado] = useState(null);

  const contestar = async (e) => {
    e.preventDefault();
    try {
      await api.post('/titular/oposicoes', { token, idEvento: a.idEvento, texto });
      setEstado('Contestação registrada. A clínica tem 15 dias para responder.');
      setAberto(false);
    } catch (err) {
      setEstado(err.message);
    }
  };

  return (
    <li>
      <p className="body">
        {a.quem} {ACOES[a.acao] ?? a.acao} {RECURSOS[a.recurso] ?? a.recurso}
      </p>
      <p className="cap cap-ash">{DATA_HORA.format(new Date(a.em))}</p>
      {estado && <p className="cap" role="status">{estado}</p>}
      {!aberto
        ? <button type="button" className="btn btn-sm" onClick={() => setAberto(true)}>Não reconheço</button>
        : (
          <form onSubmit={contestar} className="titular-form">
            <label className="cap" htmlFor={`contestar-${a.idEvento}`}>O que houve?</label>
            <textarea id={`contestar-${a.idEvento}`} rows={3} maxLength={300} required
                      value={texto} onChange={(e) => setTexto(e.target.value)} />
            <button type="submit" className="btn btn-sm" disabled={!texto.trim()}>Enviar contestação</button>
          </form>
        )}
    </li>
  );
}
