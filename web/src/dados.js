import { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';

/**
 * Carrega um recurso da API com os três estados que toda tela precisa.
 *
 * <p>Existe porque as cinco telas do app fazem exatamente a mesma dança:
 * carregando → ok → erro, com cancelamento no desmonte. Repetir isso cinco
 * vezes garante que uma delas vai esquecer o cancelamento e escrever num
 * componente já desmontado — em dev o StrictMode monta duas vezes e o bug
 * aparece; em produção ele aparece quando o usuário navega rápido.
 *
 * @param caminho o caminho da API, ou null para não carregar nada ainda
 */
export function useRecurso(caminho, { inicial = null } = {}) {
  const [versao, setVersao] = useState(0);
  const [estado, setEstado] = useState({ chave: null, status: 'carregando', dados: inicial });
  const chave = caminho ? `${caminho}#${versao}` : null;

  /* O efeito só busca. "Carregando" não é gravado aqui: setState síncrono no
     corpo do efeito renderizava a tela duas vezes a cada navegação. Ele é
     derivado abaixo — resposta de outra chave é resposta velha. */
  useEffect(() => {
    if (!chave) return undefined;
    let vivo = true;
    api.get(caminho)
      .then((dados) => { if (vivo) setEstado({ chave, status: 'ok', dados }); })
      .catch((erro) => { if (vivo) setEstado({ chave, status: 'erro', erro, dados: inicial }); });
    return () => { vivo = false; };
  }, [chave, caminho, inicial]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);
  return { ...estadoVisivel(chave, estado, inicial), recarregar };
}

/** O que a tela mostra, dado o que chegou por último. Separado para ser testável. */
export function estadoVisivel(chave, estado, inicial) {
  if (!chave) return { status: 'ok', dados: inicial };
  // Resposta de outra chave é velha: continua na tela enquanto a nova chega.
  if (estado.chave !== chave) return { status: 'carregando', dados: estado.dados };
  return estado;
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
      /* Sucesso mudo era o buraco: a lista recarregava e quem clicou não sabia
         se clicou. Quem não passar mensagem continua sem aviso — nem toda
         escrita precisa de uma, e "Salvo." em cima de um formulário que já
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
