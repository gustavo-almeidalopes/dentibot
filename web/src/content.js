/* Conteúdo da landing page em dados, não em JSX — quando o back-end passar a
   servir planos e depoimentos, só esta camada muda. */

export const NAV_LINKS = [
  { href: '#funcionalidades', label: 'Funcionalidades' },
  { href: '#para-quem', label: 'Para Quem' },
  { href: '#planos', label: 'Planos' },
  { href: '#depoimentos', label: 'Depoimentos' },
];

export const TICKER =
  '09:00 Ana Costa · Confirmado — 10:30 João Silva · Em atendimento — ' +
  '11:15 Pedro Almeida · Aguardando — 13:00 Maria Santos · ';

export const TICKER_TAIL =
  ' — 14:30 Beatriz Nunes · Confirmado — 15:45 Rafael Dias · Confirmado — ';

export const WALL_METRICS = [
  { num: '−35%', cap: 'Faltas' },
  { num: '8h', cap: 'Devolvidas por semana' },
  { num: '500+', cap: 'Consultórios ativos' },
];

export const FEATURES = [
  {
    svc: 'svc/appointment',
    title: 'Agenda',
    body: 'Lembrete automático no WhatsApp com confirmação em um toque. Quando um horário abre, o sistema sugere o encaixe.',
  },
  {
    svc: 'svc/patient',
    title: 'Prontuário',
    body: 'Odontograma por elemento e face, anamnese, evolução e radiografias — acessíveis de qualquer dispositivo.',
  },
  {
    svc: 'svc/financial',
    title: 'Cobrança',
    body: 'Pix, boleto e cartão gerados pelo sistema. A cobrança vencida é reenviada sem você lembrar dela.',
  },
  {
    svc: 'svc/inventory',
    title: 'Estoque',
    body: 'Consumo estimado por procedimento e aviso de reposição antes de o anestésico acabar no meio do atendimento.',
  },
  {
    svc: 'svc/communication',
    title: 'Comunicação',
    body: 'WhatsApp Business, SMS e e-mail em uma caixa só. Retorno e pós-consulta saem sozinhos.',
  },
  {
    svc: 'svc/audit',
    title: 'LGPD',
    body: 'Consentimento registrado, portabilidade de dados e trilha de auditoria de cada acesso ao prontuário.',
  },
];

export const WHO = [
  {
    title: 'Sozinho.',
    body: 'Você atende, agenda, cobra e ainda responde o WhatsApp. O DentiBot assume a parte administrativa para você voltar a fazer só a clínica.',
    tags: ['Consultório próprio', 'Recém-formado', '1 cadeira'],
  },
  {
    title: 'Em equipe.',
    body: 'De 2 a 8 dentistas em um sistema só. Agenda por profissional, estoque compartilhado, financeiro consolidado e permissão por perfil.',
    tags: ['2 a 8 dentistas', 'Multi-cadeira', 'Sócio-gestor'],
  },
];

export const PLANS = [
  {
    name: 'Solo',
    price: '97',
    cap: 'Dentista autônomo',
    note: 'Por mês, ou R$ 970 por ano — dois meses grátis.',
    items: [
      '1 profissional',
      'Agendamento ilimitado',
      'Prontuário e odontograma completos',
      '500 lembretes de WhatsApp por mês',
      'Cobrança por Pix e boleto',
      'LGPD e consentimento digital',
    ],
    fill: false,
  },
  {
    name: 'Clínica',
    price: '197',
    cap: 'Mais escolhido',
    note: 'Por mês, ou R$ 1.970 por ano — dois meses grátis.',
    items: [
      'Até 8 profissionais',
      'Tudo do Solo, e mais:',
      'Controle de estoque com alerta',
      'Relatórios financeiros por profissional',
      'Permissão por perfil de acesso',
      'WhatsApp ilimitado e suporte 24/7',
    ],
    fill: true,
  },
];

export const TESTIMONIALS = [
  {
    body: 'Tenho cinco dentistas na clínica. Antes era planilha e grupo de WhatsApp. Hoje agenda, pagamento e estoque estão na mesma tela.',
    name: 'Dra. Ana Mello',
    where: 'Instituto Dental Pro · Recife',
  },
  {
    body: 'A LGPD me tirava o sono. Consentimento digital e trilha de auditoria resolveram isso sem eu contratar ninguém.',
    name: 'Dr. Carlos Souza',
    where: 'Clínica Sorriso+ · Belo Horizonte',
  },
];

/* ── ODONTOGRAMA — notação FDI ─────────────────────────────────────────── */

export const TOOTH_NAMES = {
  1: 'Incisivo central', 2: 'Incisivo lateral', 3: 'Canino',
  4: 'Primeiro pré-molar', 5: 'Segundo pré-molar',
  6: 'Primeiro molar', 7: 'Segundo molar', 8: 'Terceiro molar',
};

export const QUADRANTS = {
  1: 'superior direito', 2: 'superior esquerdo',
  3: 'inferior esquerdo', 4: 'inferior direito',
};

export const UPPER_ARCH = [
  { fdi: '18' }, { fdi: '17' },
  { fdi: '16', detail: 'Restauração em resina · face oclusal · 12/03/2025 · Dra. Ana Mello · R$ 320,00' },
  { fdi: '15' }, { fdi: '14' }, { fdi: '13' }, { fdi: '12' },
  { fdi: '11', detail: 'Faceta em resina composta · 18/06/2025 · Dra. Ana Mello · R$ 890,00' },
  { fdi: '21', s: 'alert', detail: 'Cárie interproximal · face mesial · detectada 04/08/2025 · tratamento pendente' },
  { fdi: '22' }, { fdi: '23' }, { fdi: '24' }, { fdi: '25' },
  { fdi: '26', s: 'alert', detail: 'Cárie oclusal · detectada 04/08/2025 · Dr. João Silva · orçado em R$ 340,00' },
  { fdi: '27' }, { fdi: '28' },
];

export const LOWER_ARCH = [
  { fdi: '48' },
  { fdi: '47', detail: 'Amálgama substituído por resina · 27/05/2025 · Dr. João Silva · R$ 280,00' },
  { fdi: '46', s: 'gone', detail: 'Extraído em 09/2023 · aguardando implante · orçado em R$ 3.400,00' },
  { fdi: '45' }, { fdi: '44' }, { fdi: '43' }, { fdi: '42' }, { fdi: '41' },
  { fdi: '31' }, { fdi: '32' }, { fdi: '33' }, { fdi: '34' }, { fdi: '35' },
  { fdi: '36', detail: 'Implante osseointegrado + coroa · 22/01/2025 · Dr. Carlos Souza · R$ 3.400,00' },
  { fdi: '37' }, { fdi: '38' },
];

export const WHATSAPP_URL =
  'https://wa.me/5511941212737?text=Olá%2C+gostaria+de+saber+mais+sobre+o+DentiBot';

export const LOGIN_HREF = '/login';
