/**
 * Confirmação de ato irreversível.
 *
 * <p>Sobre o `<dialog>` nativo, e não sobre uma div com `role="dialog"`: foco
 * preso, `Esc`, fundo inerte e devolução do foco ao elemento anterior vêm da
 * plataforma. A versão em div precisaria de todas as quatro coisas escritas à
 * mão, e é sempre a devolução do foco que fica faltando.
 *
 * <p>`showModal()` e não o atributo `open`: só a chamada dá o modo modal — com
 * `open` o resto da página continua alcançável por Tab.
 */
import { useEffect, useRef } from 'react';

export default function Confirmar({
  aberto, titulo, corpo, rotuloConfirmar = 'Confirmar', perigo = false,
  onConfirmar, onCancelar,
}) {
  const ref = useRef(null);

  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (aberto && !dialogo.open) dialogo.showModal();
    if (!aberto && dialogo.open) dialogo.close();
  }, [aberto]);

  return (
    <dialog
      className="dialogo"
      ref={ref}
      /* Esc e clique no fundo disparam `cancel`/`close` sem passar pelos
         botões. Sem isto o estado do React continuaria "aberto" com o diálogo
         fechado, e o segundo clique não abriria nada. */
      onCancel={onCancelar}
      onClose={onCancelar}
    >
      <h2 className="sub">{titulo}</h2>
      {corpo && <div className="body">{corpo}</div>}
      <div className="dialogo-acoes">
        {/* O foco começa no cancelar, que é o botão seguro: `autofocus` no
            destrutivo transforma Enter distraído em ato irreversível. */}
        <button type="button" className="btn btn-sm" onClick={onCancelar}>Cancelar</button>
        <button
          type="button"
          className={`btn btn-sm ${perigo ? 'btn-perigo' : 'btn-fill'}`}
          onClick={onConfirmar}
        >
          {rotuloConfirmar}
        </button>
      </div>
    </dialog>
  );
}
