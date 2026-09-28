/* As rotas num lugar só. O ClerkProvider usa as de auth para redirecionar, o
   widget monta o link "criar conta" a partir delas, e a navegação do app as usa
   para montar href — três consumidores, uma definição. */
export const LOGIN = '/login';
export const CRIAR = '/login?criar=1';

/* Onde a conta recém-criada aterrissa. Não é decoração de fluxo: sem passar por
   aqui não existe linha em `identidade.usuarios`, e o back-end responde 401 a
   tudo — inclusive para quem entrou com Google, Microsoft ou Apple. */
export const CADASTRO = '/cadastro';

export const AGENDA = '/agenda';
export const PACIENTES = '/pacientes';
export const EQUIPE = '/equipe';
export const AUDITORIA = '/auditoria';
export const FINANCEIRO = '/financeiro';
export const ACOMPANHAMENTO = '/acompanhamento';
export const ESTOQUE = '/estoque';
export const IA = '/ia';
export const CONVERSAS = '/conversas';
/* Público: o paciente, sem conta, pelo link que a clínica entregou. */
export const TITULAR = '/titular';

/** O prontuário é a única rota com parâmetro — e o motivo de haver um router. */
export const PRONTUARIO = '/pacientes/:idPaciente/prontuario';
export const prontuarioDe = (idPaciente) => PRONTUARIO.replace(':idPaciente', idPaciente);

/**
 * O que aparece na navegação do app, na ordem do dia de trabalho.
 *
 * <p>`recurso` é o nome do enum `Recurso` do back-end, e é o que decide se o
 * item aparece: o Layout pergunta ao `/eu` — que responde pela matriz — em vez
 * de repetir a regra aqui. O nome é singular em PACIENTE porque o enum é; a
 * tentação de "consertar" para PACIENTES quebra a correspondência.
 */
export const MENU = [
  { href: AGENDA, label: 'Agenda', recurso: 'AGENDA' },
  { href: PACIENTES, label: 'Pacientes', recurso: 'PACIENTE' },
  // AGENDA: responder paciente no WhatsApp é parte de cuidar da agenda.
  { href: CONVERSAS, label: 'Conversas', recurso: 'AGENDA' },
  // ORCAMENTO porque o radar é sobre plano aprovado; a parte clínica da tela
  // (registro pendente) pergunta PRONTUARIO por conta própria.
  { href: ACOMPANHAMENTO, label: 'Acompanhamento', recurso: 'ORCAMENTO' },
  { href: FINANCEIRO, label: 'Financeiro', recurso: 'FINANCEIRO' },
  { href: ESTOQUE, label: 'Estoque', recurso: 'ESTOQUE' },
  { href: EQUIPE, label: 'Equipe', recurso: 'EQUIPE' },
  { href: AUDITORIA, label: 'Auditoria', recurso: 'AUDITORIA' },
  // BILLING porque a tela é sobre o que a clínica paga; a configuração, que o
  // back-end guarda em CONFIGURACAO, também é de quem paga.
  { href: IA, label: 'IA', recurso: 'BILLING' },
];

/**
 * Os itens do menu para o estado da resposta do `/eu`.
 *
 * <p>Carregando, nenhum: montar todos e tirar os que o papel não alcança quando
 * a resposta chegasse mostraria, por um instante, telas que a pessoa não
 * alcança — e um clique é mais rápido que um instante.
 *
 * <p>Com erro, todos. Sem o `/eu` não se sabe o papel, e esconder o menu deixava
 * quem acabou de entrar preso na /agenda: sem ir para outra tela e sem o botão de
 * sair. O menu é cortesia — quem nega é o back-end —, e o Layout continua sem
 * montar tela nenhuma até o papel ser conhecido.
 */
export function itensDoMenu(status, pode) {
  if (status === 'erro') return MENU;
  if (status !== 'ok') return [];
  return MENU.filter((item) => pode(item.recurso));
}

/* Único caminho com parâmetro, então uma regex resolve — e ela é conferida
   ANTES da tabela porque `/pacientes/7/prontuario` é PRONTUARIO, não PACIENTE.
   A recepcionista tem PACIENTE e não tem PRONTUARIO: casar pelo prefixo
   `/pacientes` abriria para ela exatamente a tela que a camada 5 fecha. */
const PADRAO_PRONTUARIO = /^\/pacientes\/[^/]+\/prontuario\/?$/;

/**
 * Qual recurso da matriz esta tela exige para ser lida. `null` = tela sem
 * recurso associado; o Layout deixa passar e quem barra é o back-end.
 */
export function recursoDaTela(pathname) {
  if (PADRAO_PRONTUARIO.test(pathname)) return 'PRONTUARIO';
  return MENU.find((item) => item.href === pathname)?.recurso ?? null;
}
