import { useCallback, useRef } from 'react';

/* Mesma decisão do Reveal.jsx, e aqui ela pesa mais: o pointermove dispara a
   ~60 Hz e um setState por evento re-renderizaria a árvore inteira sessenta
   vezes por segundo para mudar duas variáveis de CSS. Os valores vão direto no
   nó com setProperty, e quem anima é o compositor. */

const INICIAL = { rx: 0, ry: 0, mx: 50, my: 50, brilho: 0 };

function aplicar(el, { rx, ry, mx, my, brilho }) {
  el.style.setProperty('--rx', `${rx}deg`);
  el.style.setProperty('--ry', `${ry}deg`);
  el.style.setProperty('--mx', `${mx}%`);
  el.style.setProperty('--my', `${my}%`);
  el.style.setProperty('--brilho', brilho);
}

/**
 * Card com inclinação 3D seguindo o ponteiro.
 *
 * @param tiltLimit   graus máximos de rotação em cada eixo
 * @param scale       escala no hover
 * @param perspective distância do observador, em px. Menor = distorção mais
 *                    violenta; a percepção de "3D" vem daqui, não do tiltLimit.
 * @param effect      'evade' inclina para longe do cursor (o canto apontado
 *                    afunda), 'gravitate' inclina na direção dele. É uma troca
 *                    de sinal e muda completamente a sensação: evade parece um
 *                    objeto sendo empurrado, gravitate parece um ímã.
 * @param spotlight   brilho radial acompanhando o ponteiro
 */
export default function TiltCard({
  tiltLimit = 15,
  scale = 1.05,
  perspective = 1200,
  effect = 'evade',
  spotlight = true,
  className = '',
  style,
  children,
  ...rest
}) {
  const ref = useRef(null);

  const mover = useCallback((e) => {
    const el = ref.current;
    if (!el) return;

    /* getBoundingClientRect a cada evento é de propósito: guardar o retângulo
       no pointerenter economizaria a medição, mas o valor fica errado assim que
       a página rola ou o layout muda com o card ainda sob o cursor. */
    const r = el.getBoundingClientRect();
    const px = (e.clientX - r.left) / r.width;
    const py = (e.clientY - r.top) / r.height;

    // -0.5..0.5 a partir do centro. rotateX é o eixo horizontal, então quem o
    // controla é o movimento VERTICAL do ponteiro — trocar os dois é o erro
    // clássico aqui, e o resultado parece só "estranho", nunca obviamente errado.
    const sinal = effect === 'gravitate' ? -1 : 1;
    const rx = sinal * (0.5 - py) * tiltLimit * -1;
    const ry = sinal * (px - 0.5) * tiltLimit;

    aplicar(el, { rx, ry, mx: px * 100, my: py * 100, brilho: spotlight ? 1 : 0 });
  }, [tiltLimit, effect, spotlight]);

  const sair = useCallback(() => {
    const el = ref.current;
    if (el) aplicar(el, INICIAL);
  }, []);

  return (
    <div
      ref={ref}
      className={`tilt ${className}`.trim()}
      /* Sem onPointerEnter: o primeiro pointermove já posiciona tudo, e um
         handler a mais só adiantaria o mesmo cálculo em um frame. */
      onPointerMove={mover}
      onPointerLeave={sair}
      /* Cancel cobre o caso que o leave não cobre: o dedo que sai da tela pela
         borda, ou o navegador tomando o ponteiro de volta. Sem isto o card fica
         torto para sempre no toque. */
      onPointerCancel={sair}
      style={{
        '--perspectiva': `${perspective}px`,
        '--escala': scale,
        ...style,
      }}
      {...rest}
    >
      <div className="tilt-plano">
        {children}
        {spotlight && <span className="tilt-brilho" aria-hidden="true" />}
      </div>
    </div>
  );
}
