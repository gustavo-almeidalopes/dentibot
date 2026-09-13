import { Link } from 'react-router-dom';
import { AGENDA } from '../rotas.js';

/**
 * 404 de verdade.
 *
 * <p>O mapa de rotas anterior devolvia a landing para qualquer caminho
 * desconhecido, com status 200 — um link errado parecia funcionar, e o
 * catch-all do vercel.json fazia o mesmo no servidor. Um endereço que não
 * existe precisa dizer isso.
 */
export default function NaoEncontrada() {
  return (
    <main id="main" className="sec edge">
      <h1 className="display">Não<br />encontrado.</h1>
      <p className="body body-ash" style={{ maxWidth: '52ch' }}>
        Este endereço não existe. Se você chegou por um link de dentro do
        sistema, ele está quebrado — vale avisar.
      </p>
      <p style={{ marginTop: 'var(--spacing-30, 30px)', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
        <Link className="btn btn-fill btn-lg" to={AGENDA}>Ir para a agenda</Link>
        <a className="btn btn-lg" href="/">Voltar ao site</a>
      </p>
    </main>
  );
}
