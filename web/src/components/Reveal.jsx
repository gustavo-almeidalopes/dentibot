import { useEffect, useRef } from 'react';

/* A classe .visible é escrita direto no nó: a animação é 100% CSS e um
   setState aqui re-renderizaria a seção inteira sem mudar um pixel. */
export function useReveal() {
  const ref = useRef(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const still = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (still || !('IntersectionObserver' in window)) {
      el.classList.add('visible');
      return;
    }

    const obs = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (!e.isIntersecting) continue;
          e.target.classList.add('visible');
          obs.unobserve(e.target);
        }
      },
      { threshold: 0.08, rootMargin: '0px 0px -8% 0px' },
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, []);

  return ref;
}

function delayStyle(delay, style) {
  return delay ? { '--d': delay, ...style } : style;
}

/** Bloco que sobe ao entrar na viewport. */
export function Reveal({ as: Tag = 'div', delay, style, children, ...rest }) {
  const ref = useReveal();
  return (
    <Tag ref={ref} data-reveal style={delayStyle(delay, style)} {...rest}>
      {children}
    </Tag>
  );
}

/**
 * Display type: cada linha sobe de trás da própria máscara.
 * `lines` aceita nós React — a marca quebra em <span class="wm-break">.
 */
export function Lines({ as: Tag = 'h2', lines, delay, className, style, ...rest }) {
  const ref = useReveal();
  return (
    <Tag
      ref={ref}
      data-reveal
      className={className ? `${className} has-lines` : 'has-lines'}
      style={delayStyle(delay, style)}
      {...rest}
    >
      {lines.map((line, i) => (
        <span key={i} className="ln" style={{ '--l': i }}>
          <i>{line}</i>
        </span>
      ))}
    </Tag>
  );
}
