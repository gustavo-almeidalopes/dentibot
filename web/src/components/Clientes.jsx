import { useEffect, useState } from 'react';
import { Lines, Reveal } from './Reveal.jsx';
import { api } from '../api.js';

/* ISO 'AAAA-MM-DD' → 'DD/MM/AAAA'. Sem new Date(): a data chega sem hora e o
   parser a trata como UTC, o que joga o dia para trás em UTC-3. */
const dataBr = (iso) => iso?.split('-').reverse().join('/');

const endereco = (c) => [
  c.telefone,
  [c.endereco, c.numero].filter(Boolean).join(', '),
  c.complemento,
  c.cep,
].filter(Boolean).join(' · ');

/** GET /api/clients — servido por api/backend/Api.cs sobre o Postgres. */
export default function Clientes() {
  const [estado, setEstado] = useState({ status: 'carregando' });

  useEffect(() => {
    let vivo = true;
    api.get('/clients')
      .then((clientes) => { if (vivo) setEstado({ status: 'ok', clientes }); })
      .catch((erro) => { if (vivo) setEstado({ status: 'erro', erro: erro.message }); });
    // O StrictMode monta duas vezes em dev: sem isto a resposta da primeira
    // montagem escreve num componente já desmontado.
    return () => { vivo = false; };
  }, []);

  const { status, clientes = [], erro } = estado;

  return (
    <main id="main" className="sec edge">
      <Lines as="h1" className="display" lines={['Clientes.']} />
      <Reveal as="p" className="credit" delay="60ms">
        {status === 'ok' ? `${clientes.length} no cadastro` : 'GET /api/clients'}
      </Reveal>

      <div
        style={{ marginTop: 'var(--spacing-50)' }}
        aria-live="polite"
        aria-busy={status === 'carregando'}
      >
        {status === 'carregando' && <p className="body body-ash">Carregando…</p>}

        {status === 'erro' && (
          <p className="body" style={{ color: 'var(--alarm)' }}>
            Não foi possível carregar os clientes: {erro}
          </p>
        )}

        {status === 'ok' && clientes.length === 0 && (
          <p className="body body-ash">Nenhum cliente cadastrado ainda.</p>
        )}

        {clientes.map((c) => (
          <article className="row" key={c.id}>
            <div>
              <p className="sub">{c.name}</p>
              {c.dataDeNascimento && (
                <p className="cap cap-ash">{dataBr(c.dataDeNascimento)}</p>
              )}
            </div>
            <div>
              <p className="body">{c.email}</p>
              <p className="body body-ash">{endereco(c) || '—'}</p>
            </div>
          </article>
        ))}
      </div>
    </main>
  );
}
