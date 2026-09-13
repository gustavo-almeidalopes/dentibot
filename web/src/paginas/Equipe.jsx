import { useState } from 'react';
import { api } from '../api.js';
import { useAcao, useRecurso } from '../dados.js';
import { Cabecalho, Estado } from './Layout.jsx';

const PAPEIS = [
  { valor: 'ADMIN', rotulo: 'Administrador' },
  { valor: 'DENTISTA', rotulo: 'Dentista' },
  { valor: 'RECEPCIONISTA', rotulo: 'Recepcionista' },
  { valor: 'FINANCEIRO', rotulo: 'Financeiro' },
  { valor: 'AUXILIAR', rotulo: 'Auxiliar' },
];

const STATUS = [
  { valor: 'ativo', rotulo: 'Ativo' },
  { valor: 'bloqueado', rotulo: 'Bloqueado' },
  { valor: 'desativado', rotulo: 'Desativado' },
];

export default function Equipe() {
  const [admitindo, setAdmitindo] = useState(false);
  const equipe = useRecurso('/equipe');
  const limite = useRecurso('/billing/limites/profissionais');
  const lista = equipe.dados ?? [];

  const cabe = limite.dados?.cabe ?? true;

  return (
    <>
      <Cabecalho
        titulo="Equipe."
        detalhe={limite.status === 'ok'
          ? `${limite.dados.usado} de ${limite.dados.limite} profissionais do plano`
          : 'GET /api/v1/equipe'}
        acao={(
          <button type="button" className="btn btn-fill"
                  disabled={!cabe}
                  onClick={() => setAdmitindo((a) => !a)}>
            {admitindo ? 'Cancelar' : 'Admitir membro'}
          </button>
        )}
      />

      {/* O limite do plano é explicado ANTES de a pessoa preencher o formulário.
          Deixar admitir e falhar no envio é fazer alguém digitar à toa. */}
      {!cabe && limite.dados?.motivo && (
        <p className="aviso-plano" role="status">{limite.dados.motivo}</p>
      )}

      {admitindo && (
        <AdmitirMembro onAdmitido={() => {
          setAdmitindo(false);
          equipe.recarregar();
          limite.recarregar();
        }} />
      )}

      <Estado status={equipe.status} erro={equipe.erro} onTentarDeNovo={equipe.recarregar}
              vazio={lista.length === 0 ? 'Ninguém cadastrado ainda.' : null}>
        <ul className="lista">
          {lista.map((m) => (
            <Membro key={m.idUsuario} membro={m} onMudou={() => {
              equipe.recarregar();
              limite.recarregar();
            }} />
          ))}
        </ul>
      </Estado>
    </>
  );
}

function Membro({ membro, onMudou }) {
  const [papel, setPapel] = useState(membro.papel);
  const [status, setStatus] = useState(membro.status);
  const { executar, enviando, erro } = useAcao(onMudou);

  const mudou = papel !== membro.papel || status !== membro.status;

  return (
    <li className="row row-membro">
      <div>
        <p className="sub">{membro.nomeCompleto ?? '—'}</p>
        <p className="cap cap-ash">{membro.email}</p>
        {membro.croNumero && (
          <p className="cap cap-ash">CRO {membro.croNumero}/{membro.croUf}</p>
        )}
      </div>

      <div>
        {/* "Ainda não entrou" é diferente de "está com problema para entrar", e
            sem este selo um admin que cadastrou alguém não tem como saber qual
            dos dois é. O vínculo com a conta do Clerk acontece na primeira
            entrada da pessoa, por e-mail verificado. */}
        <p className="cap" data-vinculado={membro.vinculado ? 'sim' : 'nao'}>
          {membro.vinculado ? 'Acesso ativo' : 'Aguardando primeira entrada'}
        </p>
      </div>

      <div className="acoes">
        <select aria-label={`Papel de ${membro.nomeCompleto}`}
                value={papel} onChange={(e) => setPapel(e.target.value)}>
          {PAPEIS.map((p) => <option key={p.valor} value={p.valor}>{p.rotulo}</option>)}
        </select>
        <select aria-label={`Status de ${membro.nomeCompleto}`}
                value={status} onChange={(e) => setStatus(e.target.value)}>
          {STATUS.map((s) => <option key={s.valor} value={s.valor}>{s.rotulo}</option>)}
        </select>
        {mudou && (
          <button type="button" className="btn btn-fill" disabled={enviando}
                  onClick={() => executar(api.put(`/equipe/${membro.idUsuario}`, { papel, status }))}>
            Salvar
          </button>
        )}
      </div>

      {erro && <p className="pagamento-erro" role="alert">{erro.message}</p>}
    </li>
  );
}

function AdmitirMembro({ onAdmitido }) {
  const [form, setForm] = useState({
    nomeCompleto: '', email: '', papel: 'DENTISTA',
    croNumero: '', croUf: '', especialidade: '',
  });
  const { executar, enviando, erro } = useAcao(onAdmitido);
  const ehDentista = form.papel === 'DENTISTA';

  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  return (
    <form
      className="form-bloco"
      onSubmit={(e) => {
        e.preventDefault();
        executar(api.post('/equipe', {
          nomeCompleto: form.nomeCompleto.trim(),
          email: form.email.trim(),
          papel: form.papel,
          croNumero: ehDentista ? form.croNumero.trim() : null,
          croUf: ehDentista ? form.croUf.trim().toUpperCase() : null,
          especialidade: ehDentista ? (form.especialidade.trim() || null) : null,
        }));
      }}
    >
      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Nome completo</span>
          <input required maxLength={150} value={form.nomeCompleto} onChange={mudar('nomeCompleto')} />
        </label>
        <label className="campo-app">
          <span className="cap cap-ash">E-mail</span>
          <input required type="email" maxLength={254} value={form.email} onChange={mudar('email')} />
        </label>
      </div>

      <label className="campo-app">
        <span className="cap cap-ash">Papel</span>
        <select value={form.papel} onChange={mudar('papel')}>
          {PAPEIS.map((p) => <option key={p.valor} value={p.valor}>{p.rotulo}</option>)}
        </select>
      </label>

      {ehDentista && (
        <div className="form-linha">
          <label className="campo-app">
            <span className="cap cap-ash">CRO</span>
            <input required maxLength={20} value={form.croNumero} onChange={mudar('croNumero')} />
          </label>
          <label className="campo-app">
            <span className="cap cap-ash">UF</span>
            <input required maxLength={2} pattern="[A-Za-z]{2}" value={form.croUf}
                   onChange={mudar('croUf')} />
          </label>
          <label className="campo-app">
            <span className="cap cap-ash">Especialidade</span>
            <input maxLength={80} value={form.especialidade} onChange={mudar('especialidade')} />
          </label>
        </div>
      )}

      {/* Não existe campo de senha, e isso é desenho: ninguém escolhe credencial
          de outra pessoa. Ela cria a própria conta e o vínculo acontece na
          primeira entrada — um admin que definisse a senha conseguiria entrar
          como o subordinado, e a auditoria registraria os atos no nome errado. */}
      <p className="cap cap-ash">
        A pessoa cria a própria conta ao entrar pela primeira vez com este e-mail.
        Nenhuma senha é definida aqui.
      </p>

      {erro && <p className="pagamento-erro" role="alert">{erro.message}</p>}
      <button type="submit" className="btn btn-fill" disabled={enviando}>
        {enviando ? 'Admitindo…' : 'Admitir'}
      </button>
    </form>
  );
}
