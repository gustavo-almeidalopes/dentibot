import { useState } from 'react';
import { api } from '../api.js';
import { rotuloDaCondicao } from '../odontograma.js';
import { Aviso } from './primitivos.jsx';

/**
 * Ditado → rascunho (IA-01). O texto volta para o campo de evolução, editável;
 * as sugestões de odontograma são lançadas uma a uma pelo dentista; o que a IA
 * não conseguiu ancorar fica à vista. Nada é gravado aqui.
 *
 * <p>Não há microfone de propósito: o reconhecimento de voz do navegador manda
 * o áudio para servidor de terceiro, e áudio de consulta não sai da clínica
 * (IA-23). Quem quiser ditar usa o ditado do sistema operacional no campo.
 */
export default function Ditado({ idPaciente, onRascunho, rascunho, onLancado }) {
  const [ditado, setDitado] = useState('');
  const [estado, setEstado] = useState({ enviando: false, erro: null });

  const estruturar = async () => {
    setEstado({ enviando: true, erro: null });
    try {
      const r = await api.post('/copiloto/nota-clinica', { idPaciente: Number(idPaciente), ditado });
      onRascunho(r);
      setEstado({ enviando: false, erro: null });
    } catch (e) {
      setEstado({ enviando: false, erro: e.message });
    }
  };

  const lancar = async (s) => {
    setEstado({ enviando: true, erro: null });
    try {
      await api.post(`/pacientes/${idPaciente}/prontuario/odontograma`,
        { dente: s.dente, face: s.face, condicao: s.condicao, observacao: s.observacao });
      onLancado(s);
      setEstado({ enviando: false, erro: null });
    } catch (e) {
      setEstado({ enviando: false, erro: e.message });
    }
  };

  return (
    <div className="form-bloco ditado">
      <label className="campo-app">
        <span className="cap cap-ash">Ditado (organizado por IA, você revisa)</span>
        <textarea rows={3} maxLength={8000} value={ditado}
                  onChange={(e) => setDitado(e.target.value)}
                  placeholder="Ex.: restauração em resina no 36, face oclusal, sem intercorrências." />
      </label>
      <div className="acoes">
        <button type="button" className="btn btn-sm" disabled={!ditado.trim() || estado.enviando}
                onClick={estruturar}>
          {estado.enviando ? 'Organizando…' : 'Organizar em rascunho'}
        </button>
      </div>
      <Aviso texto={estado.erro} tom="erro" />

      {rascunho && (
        <div className="ditado-rascunho">
          <p className="cap">Rascunho gerado por IA — nada foi gravado</p>
          {rascunho.lancamentos.length > 0 && (
            <ul className="lista">
              {rascunho.lancamentos.map((s) => (
                <li key={`${s.dente}-${s.face}-${s.condicao}`} className="ditado-sugestao">
                  <span>
                    Dente {s.dente}{s.face ? ` · face ${s.face}` : ''} · {rotuloDaCondicao(s.condicao)}
                    {s.observacao ? ` · ${s.observacao}` : ''}
                  </span>
                  <button type="button" className="btn btn-sm" disabled={estado.enviando}
                          onClick={() => lancar(s)}>Lançar no odontograma</button>
                </li>
              ))}
            </ul>
          )}
          {rascunho.naoConfirmado.length > 0 && (
            <>
              <p className="cap cap-ash">Não confirmado — confira antes de registrar</p>
              <ul className="lista">
                {rascunho.naoConfirmado.map((n) => <li key={n} className="body body-ash">{n}</li>)}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  );
}
