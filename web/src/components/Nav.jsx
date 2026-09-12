import { useCallback, useEffect, useRef, useState } from 'react';
import { NAV_LINKS, LOGIN_HREF } from '../content.js';

export default function Nav() {
  const [open, setOpen] = useState(false);
  const toggleRef = useRef(null);
  const closeRef = useRef(null);

  const close = useCallback((returnFocus) => {
    setOpen(false);
    if (returnFocus) toggleRef.current?.focus();
  }, []);

  useEffect(() => {
    if (!open) return;

    document.body.style.overflow = 'hidden';
    closeRef.current?.focus();

    const onKey = (e) => { if (e.key === 'Escape') close(true); };
    /* Voltar ao desktop com o menu aberto deixaria o overlay preso. */
    const wide = window.matchMedia('(min-width: 901px)');
    const onWide = (e) => { if (e.matches) close(false); };

    document.addEventListener('keydown', onKey);
    wide.addEventListener('change', onWide);
    return () => {
      document.body.style.overflow = '';
      document.removeEventListener('keydown', onKey);
      wide.removeEventListener('change', onWide);
    };
  }, [open, close]);

  return (
    <>
      <div className="mbar edge">
        <a href="/" className="mbar-mark" aria-label="DentiBot — página inicial">DentiBot</a>
        <button
          type="button"
          className="btn mbar-toggle"
          ref={toggleRef}
          aria-expanded={open}
          aria-controls="menu"
          onClick={() => setOpen(true)}
        >
          Menu
        </button>
      </div>

      <div className="menu" id="menu" hidden={!open}>
        <div className="menu-top edge">
          <span className="cap cap-ash">Navegação</span>
          <button type="button" className="btn" ref={closeRef} onClick={() => close(true)}>
            Fechar
          </button>
        </div>

        {/* Navegar fecha o menu — senão o overlay tapa o destino. */}
        <nav className="menu-list edge" aria-label="Navegação principal" onClick={() => close(false)}>
          {NAV_LINKS.map((l) => <a key={l.href} href={l.href}>{l.label}</a>)}
        </nav>

        <div className="menu-foot edge">
          <a href={LOGIN_HREF} className="btn btn-fill btn-lg">Entrar</a>
          <p className="credit">Agenda · Prontuário · Cobrança · Estoque · LGPD</p>
        </div>
      </div>

      <nav className="nav edge" aria-label="Navegação principal">
        {NAV_LINKS.map((l) => <a key={l.href} href={l.href} className="btn">{l.label}</a>)}
        <a href={LOGIN_HREF} className="btn btn-fill">Entrar</a>
      </nav>
    </>
  );
}
