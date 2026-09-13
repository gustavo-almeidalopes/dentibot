import { Lines, Reveal } from './components/Reveal.jsx';
import {
  FEATURES, NAV_LINKS, PLANS, TESTIMONIALS,
  TICKER, TICKER_TAIL, WALL_METRICS, WHATSAPP_URL, WHO,
} from './content.js';
import { CRIAR, LOGIN } from './rotas.js';

/* ══ HERO — the wordmark is the architecture ══════════════════════════ */
export function Hero() {
  return (
    <header className="hero edge">
      <Lines
        as="h1"
        className="display-xl wordmark"
        lines={[<>Denti<span className="wm-break">Bot</span></>]}
      />
      <div className="hero-foot">
        <Reveal as="p" className="cap" delay="60ms">
          Um sistema para consultórios<br />e clínicas odontológicas
        </Reveal>
        <Reveal as="p" className="credit" delay="120ms">
          Agenda · Prontuário · Cobrança · Estoque · LGPD
        </Reveal>
      </div>
    </header>
  );
}

/* ══ STATEMENT ════════════════════════════════════════════════════════ */
export function Statement() {
  return (
    <section className="sec edge stmt">
      <Lines as="h2" className="display" lines={['Agenda cheia.', 'Cadeira nunca vazia.']} />
      <Reveal as="p" className="body" delay="80ms">
        O DentiBot cuida da agenda, do prontuário, da cobrança e da LGPD do seu
        consultório. Você cuida dos pacientes.
      </Reveal>
      <Reveal className="stmt-actions" delay="160ms">
        <a href="#planos" className="btn btn-lg btn-fill">Testar 30 dias grátis</a>
        <a href="#funcionalidades" className="btn btn-lg">Ver como funciona</a>
      </Reveal>
    </section>
  );
}

export function Ticker() {
  const set = <>{TICKER}<b>Faltou</b>{TICKER_TAIL}</>;
  return (
    <div className="ticker cap" aria-hidden="true">
      <div className="ticker-track">
        <span className="ticker-set">{set}</span>
        <span className="ticker-set">{set}</span>
      </div>
    </div>
  );
}

/* ══ THE RED WALL — a no-show is the alarm state ══════════════════════ */
export function Wall() {
  return (
    <section className="wall on-red">
      <div className="edge">
        <Lines as="p" className="display-xl wall-word" lines={['Faltou.']} />

        <div className="wall-grid">
          <Reveal as="p" className="sub" delay="60ms">
            Uma cadeira vazia não volta. O horário perdido às 14h não é
            remarcado às 15h — ele simplesmente não existe mais.
          </Reveal>
          <Reveal delay="120ms">
            <p className="body">
              O DentiBot confirma no WhatsApp, cobra resposta, libera o horário
              para a fila de espera e reagenda sozinho. A falta vira encaixe
              antes de você perceber que ela aconteceu.
            </p>
            <p className="credit" style={{ marginTop: 24 }}>
              Média apurada em 500 consultórios · 2024–2025
            </p>
          </Reveal>
        </div>

        <div className="wall-metrics">
          {WALL_METRICS.map((m, i) => (
            <Reveal key={m.cap} className="wall-metric" delay={i ? `${i * 80}ms` : undefined}>
              <p className="wall-num">{m.num}</p>
              <p className="cap">{m.cap}</p>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ══ FUNCIONALIDADES ══════════════════════════════════════════════════ */
export function Features() {
  return (
    <section id="funcionalidades" className="sec edge">
      <Lines
        as="h2"
        className="display"
        lines={['Seis serviços.', 'Uma operação.']}
        style={{ marginBottom: 60 }}
      />
      {FEATURES.map((f) => (
        <Reveal key={f.svc} className="row">
          <div>
            <p className="cap cap-ash">{f.svc}</p>
            <p className="row-title">{f.title}</p>
          </div>
          <p className="body">{f.body}</p>
        </Reveal>
      ))}
    </section>
  );
}

/* ══ PARA QUEM ════════════════════════════════════════════════════════ */
export function Who() {
  return (
    <section id="para-quem" className="sec edge">
      <div className="who">
        {WHO.map((w, i) => (
          <Reveal key={w.title} className="who-half" delay={i ? '80ms' : undefined}>
            <Lines as="p" className="display" lines={[w.title]} />
            <p className="body">{w.body}</p>
            <div className="who-tags">
              {w.tags.map((t) => <span key={t}>{t}</span>)}
            </div>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

/* ══ PLANOS ═══════════════════════════════════════════════════════════ */
export function Plans() {
  return (
    <section id="planos" className="sec edge">
      <Lines as="h2" className="display" lines={['Planos.']} style={{ marginBottom: 60 }} />

      {PLANS.map((p, i) => (
        <Reveal key={p.name} className="plan" delay={i ? '80ms' : undefined}>
          <div className="plan-top">
            <Lines as="p" className="display" lines={[p.name]} />
            <p className="plan-price"><sup>R$</sup>{p.price}</p>
          </div>
          <div className="plan-grid">
            <div>
              <p className="cap cap-ash">{p.cap}</p>
              <p className="body" style={{ marginTop: 14 }}>{p.note}</p>
            </div>
            <ul className="plan-list">
              {p.items.map((it) => <li key={it}>{it}</li>)}
            </ul>
            {/* Plano leva a CRIAR CONTA, não ao widget de entrar: quem clica
                aqui ainda não tem conta — é o que o botão promete. */}
            <a href={CRIAR} className={p.fill ? 'btn btn-lg btn-fill' : 'btn btn-lg'}>
              Começar grátis
            </a>
          </div>
        </Reveal>
      ))}

      <Reveal as="p" className="credit" style={{ marginTop: 40 }}>
        Trinta dias grátis, sem cartão · Sem fidelidade · Cancelamento pelo próprio painel
      </Reveal>
    </section>
  );
}

/* ══ DEPOIMENTOS ══════════════════════════════════════════════════════ */
export function Testimonials() {
  return (
    <section id="depoimentos" className="sec edge">
      <Reveal as="figure">
        <Lines
          as="blockquote"
          className="display quote-text"
          lines={['O sistema se pagou na primeira semana.']}
        />
        <figcaption className="quote-by">
          <p className="cap">Dr. João Silva</p>
          <p className="credit">Consultório próprio · São Paulo</p>
        </figcaption>
      </Reveal>

      <div className="quotes-more">
        {TESTIMONIALS.map((t, i) => (
          <Reveal key={t.name} as="blockquote" delay={i ? '80ms' : undefined}>
            <p className="body">{t.body}</p>
            <footer>
              <p className="cap">{t.name}</p>
              <p className="credit">{t.where}</p>
            </footer>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

/* ══ CTA ══════════════════════════════════════════════════════════════ */
export function Cta() {
  return (
    <section className="sec edge">
      <Lines as="h2" className="display" lines={['Sua clínica.', 'Sem o caos.']} />
      <Reveal className="cta-actions" delay="80ms">
        <a href={CRIAR} className="btn btn-lg btn-fill">Criar minha conta grátis</a>
        <a href={LOGIN} className="btn btn-lg">Já tenho conta</a>
      </Reveal>
    </section>
  );
}

/* ══ FOOTER ═══════════════════════════════════════════════════════════ */
export function Footer() {
  const links = NAV_LINKS.filter((l) => l.href === '#funcionalidades' || l.href === '#planos');
  return (
    <footer className="edge">
      <div className="foot">
        <p className="credit">© 2025 DentiBot · CNPJ 00.000.000/0001-00</p>
        <div className="foot-links">
          {links.map((l) => (
            <a key={l.href} href={l.href} className="credit">{l.label}</a>
          ))}
          <a href="/politica-de-privacidade.pdf" className="credit">Privacidade</a>
          <a href={LOGIN} className="credit">Entrar</a>
        </div>
        <p className="credit">v2.0.0 · React + FastAPI · LGPD</p>
      </div>
    </footer>
  );
}

/* ══ WHATSAPP ═════════════════════════════════════════════════════════ */
export function WhatsApp() {
  return (
    <a
      href={WHATSAPP_URL}
      className="btn whatsapp"
      target="_blank"
      rel="noopener"
      aria-label="Falar com a equipe no WhatsApp"
    >
      <svg xmlns="http://www.w3.org/2000/svg" fill="currentColor" viewBox="0 0 16 16" width="20" height="20" aria-hidden="true">
        <path d="M13.601 2.326A7.902 7.902 0 0 0 8.002 0a7.94 7.94 0 0 0-6.74 11.588L0 16l4.524-1.235A7.94 7.94 0 0 0 8.002 16a7.9 7.9 0 0 0 5.599-13.674zM8.002 14.5a6.47 6.47 0 0 1-3.288-.894l-.235-.14-2.682.732.724-2.617-.152-.27A6.466 6.466 0 0 1 1.5 8c0-3.584 2.918-6.5 6.502-6.5a6.48 6.48 0 0 1 6.5 6.5c0 3.584-2.918 6.5-6.5 6.5zm3.62-4.833c-.197-.099-1.16-.571-1.34-.636-.181-.066-.313-.099-.445.098-.132.198-.511.637-.626.767-.115.132-.231.149-.428.05-.197-.1-.833-.307-1.587-.981-.587-.52-.981-1.16-1.097-1.356-.115-.198-.012-.304.086-.403.089-.089.198-.231.297-.347.099-.115.132-.198.198-.33.066-.132.033-.248-.017-.347-.05-.099-.445-1.073-.61-1.466-.161-.389-.325-.336-.445-.342-.115-.007-.248-.007-.38-.007-.132 0-.347.05-.529.248-.182.198-.695.68-.695 1.658 0 .977.713 1.922.813 2.052.099.132 1.402 2.144 3.402 3.007.476.205.847.327 1.137.418.477.151.911.129 1.255.078.383-.057 1.16-.474 1.323-.932.163-.457.163-.849.115-.932-.049-.084-.18-.132-.377-.231z" />
      </svg>
      <span className="wa-label">WhatsApp</span>
    </a>
  );
}
