/**
 * O que as telas mostram, separado de como elas montam.
 *
 * <p>Existe porque cada tela tinha o próprio `Intl`, o próprio mapa de status e
 * a própria frase de contagem — e nenhum deles tinha teste, porque estavam
 * dentro do JSX. Aqui em cima é função pura, e `node --test` alcança.
 */

const HORA = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
const EXTENSO = new Intl.DateTimeFormat('pt-BR', {
  weekday: 'long', day: 'numeric', month: 'long',
});

/* Status → rótulo e tom do selo. O tom é forma, não cor: `riscado` e `contorno`
   continuam legíveis para quem não distingue vermelho de cinza. */
export const STATUS_CONSULTA = {
  agendada: { rotulo: 'Agendada', tom: 'contorno' },
  confirmada: { rotulo: 'Confirmada', tom: 'cheio' },
  em_atendimento: { rotulo: 'Em atendimento', tom: 'alarme' },
  realizada: { rotulo: 'Realizada', tom: 'fraco' },
  cancelada: { rotulo: 'Cancelada', tom: 'riscado' },
  faltou: { rotulo: 'Faltou', tom: 'alarme' },
};

export const STATUS_RECEBIVEL = {
  aberto: { rotulo: 'Em aberto', tom: 'contorno' },
  pago: { rotulo: 'Pago', tom: 'fraco' },
  vencido: { rotulo: 'Vencido', tom: 'alarme' },
  cancelado: { rotulo: 'Cancelado', tom: 'riscado' },
};

export const STATUS_MEMBRO = {
  ativo: { rotulo: 'Ativo', tom: 'cheio' },
  bloqueado: { rotulo: 'Bloqueado', tom: 'alarme' },
  desativado: { rotulo: 'Desativado', tom: 'riscado' },
};

/**
 * Traduz um valor de status, sem inventar.
 *
 * <p>Valor que o mapa não conhece aparece como veio. O CHECK do banco pode
 * ganhar um estado antes desta tela, e esconder a linha seria perder
 * informação; mostrar o valor cru é só feio.
 */
export function descrever(mapa, valor) {
  if (valor == null || valor === '') return { rotulo: '—', tom: 'fraco' };
  return mapa[valor] ?? { rotulo: String(valor), tom: 'fraco' };
}

/** `14:00 – 14:45`, ou só o início quando não há término utilizável. */
export function faixaHoraria(inicioEm, terminoEm) {
  const inicio = HORA.format(new Date(inicioEm));
  /* `new Date(null)` é 01/01/1970 e não erro: sem esta guarda, consulta sem
     término mostraria "14:00 – 21:00" com cara de dado verdadeiro. */
  if (!terminoEm) return inicio;
  const fim = new Date(terminoEm);
  if (Number.isNaN(fim.getTime())) return inicio;
  return `${inicio} – ${HORA.format(fim)}`;
}

/**
 * Agrupa o dia por hora cheia, em ordem.
 *
 * <p>A ordem é imposta aqui e não confiada à resposta: a agenda é o eixo do
 * tempo, e uma consulta fora de lugar na calha é pior que uma lista sem calha.
 */
export function agruparPorHora(consultas) {
  const porHora = new Map();

  for (const c of [...consultas].sort((a, b) =>
    new Date(a.inicioEm) - new Date(b.inicioEm))) {
    const hora = String(new Date(c.inicioEm).getHours()).padStart(2, '0');
    if (!porHora.has(hora)) porHora.set(hora, []);
    porHora.get(hora).push(c);
  }

  return [...porHora].map(([hora, lista]) => ({ hora, consultas: lista }));
}

/**
 * Telefone → `tel:`, no formato E.164.
 *
 * <p>Assume Brasil quando não vem código de país, porque o cadastro é de uma
 * clínica brasileira e o campo é `telefoneCelular`. Número curto demais para
 * ser discável devolve null — link que não liga é pior que texto.
 */
export function telHref(telefone) {
  const digitos = String(telefone ?? '').replace(/\D/g, '');
  if (digitos.length < 10) return null;
  return `tel:+${digitos.startsWith('55') ? digitos : `55${digitos}`}`;
}

/** Hoje em 'AAAA-MM-DD'. sv-SE é o atalho de ISO que o toLocaleDateString dá. */
export const hoje = () => new Date().toLocaleDateString('sv-SE');

/**
 * Soma dias a um 'AAAA-MM-DD' sem passar por fuso.
 *
 * <p>`new Date('2026-09-30')` é meia-noite UTC; somar um dia e formatar em São
 * Paulo devolveria o próprio 30. O construtor de três argumentos é local, e
 * ele normaliza mês e ano sozinho.
 */
export function somarDias(dia, n) {
  const [ano, mes, d] = dia.split('-').map(Number);
  return new Date(ano, mes - 1, d + n).toLocaleDateString('sv-SE');
}

/** 'domingo, 13 de setembro'. */
export function porExtenso(dia) {
  const [ano, mes, d] = dia.split('-').map(Number);
  return EXTENSO.format(new Date(ano, mes - 1, d));
}

/**
 * A frase de contagem, honesta quando bateu o teto.
 *
 * <p>A tela pedia 200 — que é o `LIMITE_MAXIMO` do controller — recebia 200 e
 * escrevia "200 no cadastro". Se vieram exatamente `limite`, há provavelmente
 * mais, e a única frase verdadeira é "os primeiros".
 */
export function contagem(n, limite, singular, plural) {
  if (n === 0) {
    /* ponytail: gênero pela terminação. Acerta consulta, parcela e paciente,
       que é o que esta tela usa; um substantivo feminino terminado em -ão
       ("evolução") sairia errado. Vira tabela se um terceiro caso aparecer. */
    return `nenhum${singular.endsWith('a') ? 'a' : ''} ${singular}`;
  }
  if (n >= limite) return `${n} primeiros`;
  return `${n} ${n === 1 ? singular : plural}`;
}

const REAIS = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const QUANTIDADE = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 });
const DATA_CURTA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' });

/** Dinheiro que chega como número do back-end (BigDecimal serializado). */
export const reais = (valor) => REAIS.format(Number(valor ?? 0));

/** Quantidade de estoque: até três casas, sem zero à direita. */
export const quantidade = (valor) => QUANTIDADE.format(Number(valor ?? 0));

/** Instante ISO como data curta; ausente vira travessão, não "Invalid Date". */
export const dataCurta = (instante) => (instante ? DATA_CURTA.format(new Date(instante)) : '—');
