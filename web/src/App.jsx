import Nav from './components/Nav.jsx';
import Odontograma from './components/Odontograma.jsx';
import {
  Cta, Features, Footer, Hero, Plans, Statement,
  Testimonials, Ticker, Wall, WhatsApp, Who,
} from './sections.jsx';

export default function App() {
  return (
    <>
      <a href="#main" className="skip-link">Ir para o conteúdo</a>

      <Nav />

      <main id="main">
        <Hero />
        <Statement />
        <Ticker />
        <Wall />
        <Odontograma />
        <Features />
        <Who />
        <Plans />
        <Testimonials />
      </main>

      <Cta />
      <Footer />
      <WhatsApp />
    </>
  );
}
