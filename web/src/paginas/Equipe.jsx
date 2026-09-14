import { useState } from 'react';
import { api } from '../api.js';
import { STATUS_MEMBRO } from '../apresentacao.js';
import Confirmar from '../components/Confirmar.jsx';
import { Aviso, Celula, Selo, Tabela } from '../components/primitivos.jsx';
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

/* Vínculo não é status: "ainda não entrou" é diferente de "está com problema
   para entrar", e o segundo não existe aqui. Por isso tom neutro, e não alarme
   — vermelho para quem foi admitido ontem é alarme falso. */
const VINCULO = {
  sim: { rotulo: 'Acesso ativo', tom: 'cheio' },
  nao: { rotulo: 'Aguardando 1ª entrada', tom: 'contorno' },
};

export default function Equipe() {
  const [admitindo, setAdmitindo] = useState(false);
  const equipe = useRecurso('/equipe');
  const limite = useRecurso('/billing/limites/profissionais');
  const lista = equipe.dados ?? [];

  const cabe = limite.dados?.cabe ?? true;

  const recarregarTudo = () => { equipe.recarregar(); limite.recarregar(); };

  return (
    <>
      <Cabecalho
        titulo="Equipe."
        detalhe={limite.status === 'ok'
          ? `${limite.dados.usado} de ${limite.dados.limite} profissionais do plano`
          : 'Quem tem acesso a esta clínica'}
        acao={(
          <div>
            {/* O motivo gruda no botão desabilitado: num parágrafo separado ele
                não explicava por que o botão não clica. */}
            <button type="button" className="btn btn-fill"
                    disabled={!cabe}
                    aria-describedby={!cabe ? 'limite-plano' : undefined}
                    onClick={() => setAdmitindo((a) => !a)}>
              {admitindo ? 'Cancelar' : 'Admitir membro'}
            </button>
            {!cabe && limite.dados?.motivo && (
              <p className="cap cap-ash" id="limite-plano"
                 style={{ maxWidth: '26ch', marginTop: '6px' }}>
                {limite.dados.motivo}
              </p>
            )}
          </div>
        )}
      />

      {admitindo && (
        <AdmitirMembro onAdmitido={() => { setAdmitindo(false); recarregarTudo(); }} />
      )}

      <Estado status={equipe.status} erro={equipe.erro} onTentarDeNovo={equipe.recarregar}
              esqueleto={{ linhas: 5, colunas: 5 }}
              vazio={lista.length === 0 ? 'Ninguém cadastrado ainda.' : null}>
        <Tabela colunas={[
          { chave: 'pessoa', rotulo: 'Nome e e-mail' },
          { chave: 'cro', rotulo: 'CRO' },
          { chave: 'vinculo', rotulo: 'Vínculo' },
          { chave: 'papel', rotulo: 'Papel' },
          { chave: 'status', rotulo: 'Status' },
          { chave: 'acoes', rotulo: '' },
        ]}>
          {lista.map((m) => (
            <Membro key={m.idUsuario} membro={m} onMudou={recarregarTudo} />
          ))}
        </Tabela>
      </Estado>
    </>
  );
}

function Membro({ membro, onMudou }) {
  const [papel, setPapel] = useState(membro.papel);
  const [status, setStatus] = useState(membro.status);
  const [perguntando, setPerguntando] = useState(false);
  const { executar, enviando, erro, sucesso } = useAcao(onMudou);

  const mudou = papel !== membro.papel || status !== membro.status;

  const salvar = () => {
    setPerguntando(false);
    executar(api.put(`/equipe/${membro.idUsuario}`, { papel, status }),
      `${membro.nomeCompleto}: alteração salva.`);
  };

  /* Desativar tira o acesso de uma pessoa. Papel e status mudam pelo mesmo
     botão, e só um dos dois caminhos manda alguém para casa. */
  const pedirSalvar = () => (status === 'desativado' && membro.status !== 'desativado'
    ? setPerguntando(true)
    : salvar());

  const descartar = () => { setPapel(membro.papel); setStatus(membro.status); };

  return (
    <tr>
      <Celula rotulo="Nome">
        <p className="sub" style={{ fontSize: '15px' }}>{membro.nomeCompleto ?? '—'}</p>
        <p className="cap cap-ash">{membro.email}</p>
      </Celula>

      <Celula rotulo="CRO">
        {membro.croNumero ? `${membro.croNumero}/${membro.croUf}` : '—'}
      </Celula>

      <Celula rotulo="Vínculo">
        {/* O vínculo com a conta do Clerk acontece na primeira entrada da
            pessoa, por e-mail verificado. Sem este selo, um admin que cadastrou
            alguém não tem como saber se ela já entrou. */}
        <Selo mapa={VINCULO} valor={membro.vinculado ? 'sim' : 'nao'} />
      </Celula>

      <Celula rotulo="Papel">
        <select aria-label={`Papel de ${membro.nomeCompleto}`}
                value={papel} onChange={(e) => setPapel(e.target.value)}>
          {PAPEIS.map((p) => <option key={p.valor} value={p.valor}>{p.rotulo}</option>)}
        </select>
      </Celula>

      <Celula rotulo="Status">
        <select aria-label={`Status de ${membro.nomeCompleto}`}
                value={status} onChange={(e) => setStatus(e.target.value)}>
          {STATUS.map((s) => <option key={s.valor} value={s.valor}>{s.rotulo}</option>)}
        </select>
      </Celula>

      <Celula rotulo="">
        <div className="acoes">
          {mudou && (
            <>
              <button type="button" className="btn btn-sm btn-fill" disabled={enviando}
                      onClick={pedirSalvar}>
                Salvar
              </button>
              {/* Sem isto, o que você mexeu nos selects só se desfazia
                  recarregando a página inteira. */}
              <button type="button" className="btn btn-sm" disabled={enviando}
                      onClick={descartar}>
                Descartar
              </button>
            </>
          )}
        </div>
        <Aviso texto={sucesso} />
        {erro && <p className="erro-campo" role="alert">{erro.message}</p>}

        <Confirmar
          aberto={perguntando}
          titulo={`Desativar ${membro.nomeCompleto}?`}
          corpo="A pessoa perde o acesso ao sistema na próxima requisição. O histórico dela na trilha de auditoria continua."
          rotuloConfirmar="Desativar"
          perigo
          onConfirmar={salvar}
          onCancelar={() => setPerguntando(false)}
        />
      </Celula>
    </tr>
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
        }), `${form.nomeCompleto.trim()} admitido.`);
      }}
    >
      <div className="form-linha">
        <label className="campo-app">
          <span className="cap cap-ash">Nome completo</span>
          <input required maxLength={150} autoFocus
                 value={form.nomeCompleto} onChange={mudar('nomeCompleto')} />
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

      {erro && <p className="erro-campo" role="alert">{erro.message}</p>}
      <button type="submit" className="btn btn-fill" disabled={enviando}>
        {enviando ? 'Admitindo…' : 'Admitir'}
      </button>
    </form>
  );
}
