/**
 * Bandeira, formatação e validação de cartão — funções puras.
 *
 * <p>Separado do componente de propósito: é a única parte disto que tem regra
 * de verdade, e regra sem teste apodrece. Ver cartao.test.mjs.
 *
 * IMPORTANTE, e vale antes de qualquer uso: se o meio de pagamento for Stripe
 * Elements, NADA aqui toca o número real. Os campos do Stripe moram em iframes
 * de outra origem e o seu JavaScript não consegue lê-los — isso não é limitação
 * de API, é a mesma política de origem que mantém você em PCI SAQ A. Estas
 * funções servem para (a) o modo de demonstração, (b) integrações em que a
 * tokenização é feita por outro SDK que aceita campos próprios, e (c) formatar
 * o `last4` que volta do provedor depois da cobrança.
 */

/* Prefixos por bandeira. Elo e Hipercard estão aqui porque a lista de qualquer
   biblioteca gringa não as tem, e no Brasil elas são uma fatia grande demais do
   balcão de uma clínica para cair no "desconhecido".

   A lista de BINs da Elo é longa e muda; a autoritativa é a da adquirente. Esta
   cobre as faixas comuns e serve para ESCOLHER O LOGO, nunca para decidir se a
   transação vai passar — quem decide isso é o provedor. */
const BANDEIRAS = [
  { id: 'visa',       nome: 'Visa',        re: /^4/,                                   digitos: [16, 19], cvv: 3 },
  { id: 'mastercard', nome: 'Mastercard',  re: /^(5[1-5]|2[2-7])/,                     digitos: [16],     cvv: 3 },
  { id: 'amex',       nome: 'Amex',        re: /^3[47]/,                               digitos: [15],     cvv: 4 },
  { id: 'elo',        nome: 'Elo',         re: /^(4011|4312|4389|4514|4576|5041|5066|5090|6277|6362|6363|650|651|655)/, digitos: [16], cvv: 3 },
  { id: 'hipercard',  nome: 'Hipercard',   re: /^(606282|3841)/,                       digitos: [16, 19], cvv: 3 },
  { id: 'diners',     nome: 'Diners',      re: /^3(0[0-5]|[68])/,                      digitos: [14],     cvv: 3 },
  { id: 'discover',   nome: 'Discover',    re: /^(6011|64[4-9]|65)/,                   digitos: [16],     cvv: 3 },
];

/* Elo e Visa colidem em 4011/4312/4389/4514/4576, e Elo e Discover em 65. A
   ordem do array não resolveria sozinha (o /^4/ da Visa casa primeiro), então a
   busca roda da mais específica para a mais genérica. */
const POR_ESPECIFICIDADE = [...BANDEIRAS].sort(
  (a, b) => b.re.source.length - a.re.source.length,
);

export const somenteDigitos = (valor) => (valor ?? '').replace(/\D/g, '');

/** A bandeira, ou null enquanto não houver prefixo suficiente para decidir. */
export function detectarBandeira(numero) {
  const d = somenteDigitos(numero);
  if (!d) return null;
  return POR_ESPECIFICIDADE.find((b) => b.re.test(d)) ?? null;
}

/** Agrupamento dos dígitos: Amex é 4-6-5 e Diners 4-6-4, o resto é 4-4-4-4. */
function grupos(bandeira) {
  if (bandeira?.id === 'amex') return [4, 6, 5];
  if (bandeira?.id === 'diners') return [4, 6, 4];
  return [4, 4, 4, 4];
}

/**
 * Quantas casas o cartão MOSTRA agora.
 *
 * <p>Não é o máximo da bandeira: a Visa aceita 16 ou 19, e usar o máximo faria
 * todo cartão Visa nascer com 19 bolinhas — três a mais do que 99% deles têm.
 * O esqueleto é o menor comprimento que ainda cabe o que já foi digitado, então
 * ele começa em 16 e só cresce se a pessoa realmente passar disso.
 */
function comprimentoExibido(bandeira, digitados) {
  if (!bandeira) return 16;
  return bandeira.digitos.find((n) => digitados <= n) ?? Math.max(...bandeira.digitos);
}

/** Quantos dígitos o cartão tem no máximo, para travar o input. */
export function maximoDeDigitos(numero) {
  const b = detectarBandeira(numero);
  return b ? Math.max(...b.digitos) : 19;
}

/** '4111111111111111' → '4111 1111 1111 1111'. Respeita o grupo da bandeira. */
export function formatarNumero(numero) {
  const b = detectarBandeira(numero);
  const d = somenteDigitos(numero).slice(0, maximoDeDigitos(numero));

  const partes = [];
  let i = 0;
  for (const tamanho of grupos(b)) {
    if (i >= d.length) break;
    partes.push(d.slice(i, i + tamanho));
    i += tamanho;
  }
  // Sobra de cartão de 19 dígitos: o que passar dos grupos vai num bloco final.
  if (i < d.length) partes.push(d.slice(i));
  return partes.join(' ');
}

/**
 * O número como o cartão o exibe, com os não-digitados virando •.
 *
 * <p>É isto que dá a sensação de "preenchendo" — o cartão já nasce com a forma
 * final e os dígitos ocupam os lugares, em vez de crescer da esquerda.
 */
export function numeroExibido(numero, { mascarar = false } = {}) {
  const b = detectarBandeira(numero);
  const d = somenteDigitos(numero).slice(0, maximoDeDigitos(numero));
  const total = comprimentoExibido(b, d.length);

  const preenchido = d.padEnd(total, '•');
  const visivel = mascarar
    // Só os quatro últimos, que é o que se pode guardar e mostrar depois.
    ? preenchido.slice(0, -4).replace(/\d/g, '•') + preenchido.slice(-4)
    : preenchido;

  const partes = [];
  let i = 0;
  for (const tamanho of grupos(b)) {
    if (i >= visivel.length) break;
    partes.push(visivel.slice(i, i + tamanho));
    i += tamanho;
  }
  if (i < visivel.length) partes.push(visivel.slice(i));
  return partes.join(' ');
}

/** '1230' → '12/30'. Aceita o que o usuário digita, com ou sem barra. */
export function formatarValidade(valor) {
  const d = somenteDigitos(valor).slice(0, 4);
  if (d.length <= 2) return d;
  return `${d.slice(0, 2)}/${d.slice(2)}`;
}

/**
 * Validade no futuro. Mês inválido (00, 13+) é recusado antes da data.
 *
 * <p>Compara com o ÚLTIMO dia do mês: um cartão que vence em 09/2026 vale o mês
 * de setembro inteiro, e recusá-lo no dia 1º é o erro clássico aqui.
 */
export function validadeNoFuturo(valor, agora = new Date()) {
  const d = somenteDigitos(valor);
  if (d.length !== 4) return false;

  const mes = Number(d.slice(0, 2));
  if (mes < 1 || mes > 12) return false;

  const ano = 2000 + Number(d.slice(2));
  // Dia 0 do mês seguinte = último dia deste mês, 23:59:59.999.
  const fim = new Date(ano, mes, 0, 23, 59, 59, 999);
  return fim >= agora;
}

/** CVV com o tamanho que a bandeira pede — Amex são 4, o resto 3. */
export function cvvCompleto(cvv, numero) {
  const esperado = detectarBandeira(numero)?.cvv ?? 3;
  return somenteDigitos(cvv).length === esperado;
}

/**
 * Luhn. Pega dígito trocado e a maioria das transposições — não diz que o
 * cartão existe nem que tem saldo, só que o número não foi digitado errado.
 * Serve para avisar antes de gastar uma tentativa com a adquirente.
 */
export function luhnValido(numero) {
  const d = somenteDigitos(numero);
  if (d.length < 12) return false;

  let soma = 0;
  let dobra = false;
  for (let i = d.length - 1; i >= 0; i--) {
    let n = d.charCodeAt(i) - 48;
    if (dobra) {
      n *= 2;
      if (n > 9) n -= 9;
    }
    soma += n;
    dobra = !dobra;
  }
  return soma % 10 === 0;
}

/** Tudo o que o cartão precisa para ser enviado. Não valida saldo nem emissor. */
export function cartaoCompleto({ numero, nome, validade, cvv }) {
  const b = detectarBandeira(numero);
  return Boolean(
    b
      && b.digitos.includes(somenteDigitos(numero).length)
      && luhnValido(numero)
      && (nome ?? '').trim().length >= 2
      && validadeNoFuturo(validade)
      && cvvCompleto(cvv, numero),
  );
}
