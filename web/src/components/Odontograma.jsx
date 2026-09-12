import { useMemo, useState } from 'react';
import { Lines, Reveal, useReveal } from './Reveal.jsx';
import { LOWER_ARCH, QUADRANTS, TOOTH_NAMES, UPPER_ARCH } from '../content.js';

const ALL = [...UPPER_ARCH, ...LOWER_ARCH];

function Arch({ teeth, label, selected, onSelect }) {
  return (
    <div className="arch" role="group" aria-label={label}>
      {teeth.map((t, i) => (
        <button
          key={t.fdi}
          type="button"
          className="fdi"
          data-fdi={t.fdi}
          data-s={t.s}
          /* Preenche da direita do paciente para a esquerda, arcada por arcada. */
          style={{ '--i': i }}
          aria-label={`Elemento ${t.fdi}`}
          aria-pressed={selected === t.fdi}
          onClick={() => onSelect(t.fdi)}
        >
          {t.fdi}
        </button>
      ))}
    </div>
  );
}

export default function Odontograma() {
  const [selected, setSelected] = useState('16');
  const chartRef = useReveal();

  const tooth = useMemo(() => ALL.find((t) => t.fdi === selected) ?? ALL[0], [selected]);
  const name = `${TOOTH_NAMES[tooth.fdi[1]]} ${QUADRANTS[tooth.fdi[0]]}`;

  return (
    <section className="sec edge">
      <div className="odonto-head">
        <Lines as="h2" className="display" lines={['Cada dente', 'tem histórico.']} />
        <Reveal as="p" className="credit" delay="60ms">
          Notação FDI · 32 elementos · registro por face
        </Reveal>
      </div>

      <div id="odontograma" ref={chartRef} data-reveal style={{ '--d': '100ms' }}>
        <Arch teeth={UPPER_ARCH} label="Arcada superior" selected={selected} onSelect={setSelected} />
        <Arch teeth={LOWER_ARCH} label="Arcada inferior" selected={selected} onSelect={setSelected} />

        <div className="record" aria-live="polite">
          <p className="record-num">{tooth.fdi}</p>
          <div className="record-line">
            <p className="cap">{name}</p>
            <p className="body">{tooth.detail ?? 'Hígido · sem ocorrências registradas'}</p>
          </div>
        </div>
      </div>
    </section>
  );
}
