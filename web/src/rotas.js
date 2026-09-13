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
  { href: FINANCEIRO, label: 'Financeiro', recurso: 'FINANCEIRO' },
  { href: EQUIPE, label: 'Equipe', recurso: 'EQUIPE' },
  { href: AUDITORIA, label: 'Auditoria', recurso: 'AUDITORIA' },
];

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
