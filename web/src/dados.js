import { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';

/**
 * Carrega um recurso da API com os três estados que toda tela precisa.
 *
 * <p>Cancela no desmonte: em dev o StrictMode monta duas vezes, e em produção o
 * usuário navega rápido.
 *
 * @param caminho o caminho da API, ou null para não carregar nada ainda
 */
export function useRecurso(caminho, { inicial = null } = {}) {
  const [estado, setEstado] = useState({ status: 'carregando', dados: inicial });

  const recarregar = useCallback(() => {
    if (!caminho) {
      setEstado({ status: 'ok', dados: inicial });
      return () => {};
    }
    let vivo = true;
    setEstado((e) => ({ ...e, status: 'carregando' }));

    api.get(caminho)
      .then((dados) => { if (vivo) setEstado({ status: 'ok', dados }); })
      .catch((erro) => { if (vivo) setEstado({ status: 'erro', erro, dados: inicial }); });

    return () => { vivo = false; };
  }, [caminho, inicial]);

  useEffect(recarregar, [recarregar]);

  return { ...estado, recarregar };
}

/** Dispara uma escrita e devolve o estado dela, sem segurar a tela inteira. */
export function useAcao(aoConcluir) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  const executar = useCallback(async (promessa, mensagem) => {
    setEnviando(true);
    setErro(null);
    setSucesso(null);
    try {
      const resultado = await promessa;
      aoConcluir?.(resultado);
      /* Sem mensagem, sem aviso: "Salvo." em cima de um formulário que já
         fechou é ruído. */
      if (mensagem) setSucesso(mensagem);
      return resultado;
    } catch (e) {
      setErro(e);
      return null;
    } finally {
      setEnviando(false);
    }
  }, [aoConcluir]);

  return { executar, enviando, erro, sucesso, limparSucesso: () => setSucesso(null) };
}
