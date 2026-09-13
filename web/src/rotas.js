/* As rotas num lugar só. O ClerkProvider usa as de auth para redirecionar, o
   widget monta o link "criar conta" a partir delas, e a navegação do app as usa
   para montar href — três consumidores, uma definição. */
export const LOGIN = '/login';
export const CRIAR = '/login?criar=1';

export const AGENDA = '/agenda';
export const PACIENTES = '/pacientes';
export const EQUIPE = '/equipe';
export const AUDITORIA = '/auditoria';
export const FINANCEIRO = '/financeiro';

/** O prontuário é a única rota com parâmetro — e o motivo de haver um router. */
export const prontuarioDe = (idPaciente) => `/pacientes/${idPaciente}/prontuario`;

/** O que aparece na navegação do app, na ordem do dia de trabalho. */
export const MENU = [
  { href: AGENDA, label: 'Agenda' },
  { href: PACIENTES, label: 'Pacientes' },
  { href: FINANCEIRO, label: 'Financeiro' },
  { href: EQUIPE, label: 'Equipe' },
  { href: AUDITORIA, label: 'Auditoria' },
];
