import { useState } from 'react';
import { api } from '../api.js';
import Confirmar from './Confirmar.jsx';

/* As quatro transições que o AgendaController expõe. `pergunta` marca o que
   não tem volta — o back-end responde 409 ao caminho inverso — e esses ficavam
   a um clique, encostados no botão que não faz mal nenhum. */
const ACOES = {
  confirmar: { rotulo: 'Confirmar', feito: 'Consulta confirmada.' },
  cancelar: {
    rotulo: 'Cancelar', feito: 'Consulta cancelada — o horário ficou livre.',
    pergunta: 'Cancelar a consulta', confirmar: 'Cancelar consulta', voltar: 'Manter',
    corpo: 'O horário volta a ficar livre na agenda do dentista.',
  },
  falta: {
    rotulo: 'Faltou', feito: 'Falta registrada.',
    pergunta: 'Registrar falta', confirmar: 'Registrar falta',
    corpo: 'Esta mudança não tem volta pela tela: o back-end recusa o caminho inverso.',
  },
  concluir: {
    rotulo: 'Concluir', feito: 'Consulta concluída.',
    pergunta: 'Concluir a consulta', confirmar: 'Concluir',
    corpo: 'Esta mudança não tem volta pela tela: o back-end recusa o caminho inverso.',
  },
};

/**
 * Um POST de transição de consulta — Agenda e Recepção usam o mesmo.
 *
 * <p>A `Idempotency-Key` é gerada UMA VEZ por montagem do botão, não por
 * clique: dois toques em "Confirmar" são a mesma requisição lógica, e uma chave
 * nova no segundo clique faria o filtro do back-end tratá-lo como operação
 * distinta — que é exatamente o que ele existe para impedir.
 *
 * <p>Cancelar pede o motivo porque o `PedidoCancelamento` o exige (`@NotBlank`):
 * sem o campo aqui, o botão terminava num 400.
 */
export default function Transicao({ id, acao, paciente, executar, enviando }) {
  const { rotulo, feito, pergunta, confirmar, voltar, corpo } = ACOES[acao];
  const [chave] = useState(() => crypto.randomUUID());
  const [perguntando, setPerguntando] = useState(false);
  const [motivo, setMotivo] = useState('');
  const pedeMotivo = acao === 'cancelar';

  const disparar = () => {
    setPerguntando(false);
    executar(
      api.post(`/consultas/${id}/${acao}`, pedeMotivo ? { motivo: motivo.trim() } : {},
        { idempotencyKey: chave }),
      paciente ? `${paciente}: ${feito.charAt(0).toLowerCase()}${feito.slice(1)}` : feito,
    );
  };

  return (
    <>
      <button type="button" className={`btn btn-sm${pergunta ? ' btn-perigo' : ''}`}
              disabled={enviando}
              onClick={() => (pergunta ? setPerguntando(true) : disparar())}>
        {rotulo}
      </button>
      {pergunta && (
        <Confirmar
          aberto={perguntando}
          titulo={`${pergunta}${paciente ? ` de ${paciente}` : ''}?`}
          corpo={(
            <>
              <p>{corpo}</p>
              {pedeMotivo && (
                <label className="campo-app dialogo-campo">
                  <span className="cap cap-ash">Motivo *</span>
                  <input required maxLength={255} value={motivo}
                         placeholder="ex.: paciente pediu para remarcar"
                         onChange={(e) => setMotivo(e.target.value)} />
                </label>
              )}
            </>
          )}
          rotuloConfirmar={confirmar}
          rotuloVoltar={voltar}
          podeConfirmar={!pedeMotivo || motivo.trim() !== ''}
          perigo
          onConfirmar={disparar}
          onCancelar={() => setPerguntando(false)}
        />
      )}
    </>
  );
}
