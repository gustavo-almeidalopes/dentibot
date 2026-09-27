/**
 * O vocabulário do odontograma: o valor que o banco aceita (CHECK da V12) e o
 * rótulo que a tela mostra.
 *
 * <p>Antes a tela mandava o rótulo ('cárie', 'restaurado') como valor, o CHECK
 * recusava e a API respondia 409 — só ausente, implante e coroa conseguiam ser
 * lançados. `odontograma.test.mjs` confere esta lista contra o SQL.
 */
export const CONDICOES = [
  { valor: 'higido', rotulo: 'Hígido' },
  { valor: 'carie', rotulo: 'Cárie' },
  { valor: 'restauracao', rotulo: 'Restauração' },
  { valor: 'ausente', rotulo: 'Ausente' },
  { valor: 'implante', rotulo: 'Implante' },
  { valor: 'coroa', rotulo: 'Coroa' },
  { valor: 'canal', rotulo: 'Canal' },
  { valor: 'fratura', rotulo: 'Fratura' },
  { valor: 'extraido', rotulo: 'Extraído' },
  { valor: 'selante', rotulo: 'Selante' },
  { valor: 'protese', rotulo: 'Prótese' },
];

export const rotuloDaCondicao = (valor) =>
  CONDICOES.find((c) => c.valor === valor)?.rotulo ?? valor;
