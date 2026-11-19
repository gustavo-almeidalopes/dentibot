/**
 * O que as telas mostram, separado de como elas montam.
 *
 * <p>Função pura, fora do JSX, para o `node --test` alcançar.
 */

const HORA = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
const EXTENSO = new Intl.DateTimeFormat('pt-BR', {
  weekday: 'long', day: 'numeric', month: 'long',
});

/* Valor do banco → rótulo e tom do selo. O tom é forma, não cor: `riscado` e
   `contorno` continuam legíveis para quem não distingue vermelho de cinza. */
export const STATUS_CONSULTA = {
  agendada: { rotulo: 'Agendada', tom: 'contorno' },
  confirmada: { rotulo: 'Confirmada', tom: 'cheio' },
  em_atendimento: { rotulo: 'Em atendimento', tom: 'alarme' },
  realizada: { rotulo: 'Realizada', tom: 'fraco' },
  cancelada: { rotulo: 'Cancelada', tom: 'riscado' },
  faltou: { rotulo: 'Faltou', tom: 'alarme' },
};

export const STATUS_RECEBIVEL = {
  aberta: { rotulo: 'Em aberto', tom: 'contorno' },
  parcial: { rotulo: 'Parcial', tom: 'contorno' },
  paga: { rotulo: 'Paga', tom: 'fraco' },
  vencida: { rotulo: 'Vencida', tom: 'alarme' },
  cancelada: { rotulo: 'Cancelada', tom: 'riscado' },
};

export const STATUS_PACIENTE = {
  ativo: { rotulo: 'Ativo', tom: 'cheio' },
  inativo: { rotulo: 'Inativo', tom: 'fraco' },
  obito: { rotulo: 'Óbito', tom: 'riscado' },
};

/* Exclusão, falha de login e exportação são as três que precisam saltar numa
   trilha — o resto é o ruído normal de um dia de trabalho. */
export const ACAO_AUDITORIA = {
  leitura: { rotulo: 'Leitura', tom: 'fraco' },
  criacao: { rotulo: 'Criação', tom: 'contorno' },
  alteracao: { rotulo: 'Alteração', tom: 'contorno' },
  exclusao: { rotulo: 'Exclusão', tom: 'alarme' },
  login: { rotulo: 'Login', tom: 'fraco' },
  logout: { rotulo: 'Logout', tom: 'fraco' },
  falha_login: { rotulo: 'Falha de login', tom: 'alarme' },
  exportacao: { rotulo: 'Exportação', tom: 'alarme' },
};

/* Condição de dente. `inteiro` é o lado do `ck_face_coerente` em que a face é
   NULL; as demais exigem face. 'fratura' está no CHECK da coluna mas em nenhum
   dos dois lados do ck_face_coerente, então o banco não aceita lançá-la — fica
   fora até a migration decidir de que lado ela mora. */
export const CONDICAO_DENTE = {
  higido: { rotulo: 'Hígido', inteiro: false },
  carie: { rotulo: 'Cárie', inteiro: false },
  restauracao: { rotulo: 'Restauração', inteiro: false },
  selante: { rotulo: 'Selante', inteiro: false },
  ausente: { rotulo: 'Ausente', inteiro: true },
  extraido: { rotulo: 'Extraído', inteiro: true },
  implante: { rotulo: 'Implante', inteiro: true },
  coroa: { rotulo: 'Coroa', inteiro: true },
  canal: { rotulo: 'Canal', inteiro: true },
  protese: { rotulo: 'Prótese', inteiro: true },
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

export const nomeDoPaciente = (c) => c.nomePaciente ?? `Paciente ${c.idPaciente}`;

/**
 * `id → nome` sobre uma lista carregada, com um rótulo honesto para o id que
 * não está nela. As respostas trazem o id e não o nome (a fronteira entre
 * módulos do back-end), e é a tela que casa os dois lados.
 */
export function nomesPorId(lista, chave, rotulo) {
  const mapa = new Map((lista ?? []).map((item) => [item[chave], item.nomeCompleto]));
  return (id) => mapa.get(id) ?? `${rotulo} ${id}`;
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
 * <p>Se vieram exatamente `limite`, há provavelmente mais, e a única frase
 * verdadeira é "os primeiros".
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

/* ── BALCÃO ─────────────────────────────────────────────────────────────────
   O que a Recepção calcula sobre a lista de consultas que já carregou. */

/* O mesmo WHERE da EXCLUDE `ex_dentista_sem_sobreposicao`: cancelada e faltou
   liberam o horário, realizada não. Divergir daqui é oferecer um horário que o
   banco recusa com 409. */
const OCUPAM_HORARIO = new Set(['agendada', 'confirmada', 'em_atendimento', 'realizada']);

/* Quem ainda vai sentar, ou está sentado, na cadeira. */
const VIVAS = new Set(['agendada', 'confirmada', 'em_atendimento']);

/* ponytail: expediente fixo, 8h às 19h em passos de 30 min. O back-end não
   guarda horário de funcionamento; quando `/clinica` ganhar, estes números
   saem de lá. */
export const EXPEDIENTE = { abre: 8, fecha: 19, passoMin: 30 };

/**
 * A grade do dia de UM dentista: cada início possível, e se cabe.
 *
 * <p>"Cabe" é o intervalo inteiro `[início, início + duração)` livre — o mesmo
 * `[)` do `tstzrange` do banco, então quem termina 10:00 não bloqueia 10:00.
 * Horário que já passou sai da grade: o `@Future` do NovaConsulta o recusaria
 * de qualquer jeito.
 */
export function horarios(dia, consultas, duracaoMin, agora = new Date()) {
  const [ano, mes, d] = dia.split('-').map(Number);
  const ocupadas = consultas
    .filter((c) => OCUPAM_HORARIO.has(c.status))
    .map((c) => [new Date(c.inicioEm).getTime(), new Date(c.terminoEm).getTime()]);

  const grade = [];
  for (let min = EXPEDIENTE.abre * 60; min + duracaoMin <= EXPEDIENTE.fecha * 60;
    min += EXPEDIENTE.passoMin) {
    const inicio = new Date(ano, mes - 1, d, 0, min);
    if (inicio <= agora) continue;
    const fim = new Date(inicio.getTime() + duracaoMin * 60000);
    const ocupado = ocupadas.some(([a, b]) => inicio.getTime() < b && a < fim.getTime());
    grade.push({ hora: HORA.format(inicio), inicio, fim, livre: !ocupado });
  }
  return grade;
}

/**
 * O que a recepção precisa saber sem ler a lista: quem está na cadeira agora
 * e quem é o próximo a chegar.
 */
export function momento(consultas, agora = new Date()) {
  const vivas = consultas
    .filter((c) => VIVAS.has(c.status))
    .sort((a, b) => new Date(a.inicioEm) - new Date(b.inicioEm));
  return {
    naCadeira: vivas.filter((c) =>
      new Date(c.inicioEm) <= agora && agora < new Date(c.terminoEm)),
    proxima: vivas.find((c) => new Date(c.inicioEm) > agora) ?? null,
  };
}

/**
 * O relógio por cima das ações que o back-end já liberou: "Faltou" antes do
 * horário é chute, confirmar ou cancelar depois que começou não faz sentido, e
 * concluir espera o horário acabar.
 */
export function cabeAgora(acao, c, agora = new Date()) {
  const comecou = new Date(c.inicioEm) <= agora;
  if (acao === 'confirmar' || acao === 'cancelar') return !comecou;
  if (acao === 'falta') return comecou;
  if (acao === 'concluir') return new Date(c.terminoEm) <= agora;
  return true;
}

/** Ainda sem desfecho: nem realizada, nem cancelada, nem falta. */
export const semDesfecho = (c) => VIVAS.has(c.status);

/** Saiu do dia: terminou de fato, ou o horário dela já acabou. */
export const jaPassou = (c, agora = new Date()) =>
  !VIVAS.has(c.status) || new Date(c.terminoEm) <= agora;

/** 'em 25 min', 'há 5 min', 'em 1 h 10 min'. */
export function relativo(alvo, agora = new Date()) {
  const min = Math.round((new Date(alvo) - agora) / 60000);
  if (min === 0) return 'agora';
  const abs = Math.abs(min);
  const texto = abs < 60
    ? `${abs} min`
    : `${Math.floor(abs / 60)} h${abs % 60 ? ` ${abs % 60} min` : ''}`;
  return min > 0 ? `em ${texto}` : `há ${texto}`;
}

const semAcento = (s) => String(s ?? '').normalize('NFD').replace(/\p{Diacritic}/gu, '')
  .toLowerCase();

/**
 * A busca da recepção: "joao" acha João, e "98765" acha o celular.
 *
 * <p>Telefone comparado em dígitos dos dois lados — o banco guarda só dígitos e
 * quem digita põe parêntese e traço. Três dígitos no mínimo, senão "1" casaria
 * com todo DDD 11.
 */
export function casaBusca(busca, nome, telefone) {
  const termo = semAcento(busca).trim();
  if (!termo) return true;
  const digitos = termo.replace(/\D/g, '');
  return semAcento(nome).includes(termo)
    || (digitos.length >= 3 && String(telefone ?? '').replace(/\D/g, '').includes(digitos));
}

/** Conversa no WhatsApp com a mensagem pronta, ou null se o número não disca. */
export function whatsappHref(telefone, mensagem) {
  const tel = telHref(telefone);
  return tel
    ? `https://wa.me/${tel.slice('tel:+'.length)}?text=${encodeURIComponent(mensagem)}`
    : null;
}

/** '14:30', no fuso de quem está na clínica. */
export const hora = (instante) => HORA.format(new Date(instante));

/* O back-end recebe `de` e `ate` como instantes ISO. O <input type="date"> dá
   'AAAA-MM-DD', que é dia civil — converter no fuso do browser é o certo: a
   agenda de terça é a terça de quem está na clínica, não a de UTC. */
export const inicioDoDia = (dia) => new Date(`${dia}T00:00:00`).toISOString();
export const fimDoDia = (dia) => new Date(`${dia}T23:59:59.999`).toISOString();

/** O dia civil local de um instante, em 'AAAA-MM-DD'. */
export const diaDe = (instante) => new Date(instante).toLocaleDateString('sv-SE');
