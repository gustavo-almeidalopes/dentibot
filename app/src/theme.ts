/**
 * Os mesmos tokens do `web/src/style.css`, transcritos para React Native.
 *
 * Brutalista: preto absoluto, sem raio de canto, vermelho só para o que é
 * destrutivo ou alarmante. Se o CSS da web mudar, este arquivo muda junto — é
 * a única cópia, e está aqui porque RN não lê custom properties de CSS.
 */
import type { TextStyle } from 'react-native';

export const cor = {
  obsidiana: '#000000',
  osso: '#ffffff',
  cinza: '#838383',
  alarme: '#ed1c24',
  /** Divisórias sobre fundo escuro. */
  fio: 'rgba(255, 255, 255, 0.26)',
} as const;

export const espaco = {
  xs: 6,
  sm: 7,
  md: 15,
  lg: 20,
  xl: 30,
  xxl: 50,
} as const;

/**
 * `Antonio` (display) e `Inter` (UI) não estão embutidas: carregar duas famílias
 * custa peso de bundle e uma tela de splash esperando fonte. O peso e o
 * caixa-alta já dão o tom brutalista com a fonte do sistema.
 *
 * ponytail: fonte do sistema; embutir Antonio via expo-font quando a identidade
 * visual do app virar requisito de marca.
 */
// `satisfies` e não `as const`: mantém os tipos literais E valida cada token
// contra o TextStyle do RN, que é o que pega `fontVariant` readonly.
export const tipo = {
  display: { fontSize: 30, fontWeight: '700', letterSpacing: -0.5 },
  titulo: { fontSize: 20, fontWeight: '700' },
  corpo: { fontSize: 16, fontWeight: '400' },
  rotulo: { fontSize: 12, fontWeight: '700', letterSpacing: 1.2 },
  /** `tabular-nums` alinha os horários da agenda em coluna. */
  mono: { fontSize: 15, fontWeight: '700', fontVariant: ['tabular-nums'] },
} satisfies Record<string, TextStyle>;

/** Cor de cada status de consulta. Os nomes vêm do CHECK em `agenda.consultas`. */
export const corDoStatus: Record<string, string> = {
  agendada: cor.cinza,
  confirmada: cor.osso,
  em_atendimento: cor.alarme,
  realizada: cor.cinza,
  cancelada: cor.cinza,
  faltou: cor.alarme,
};

export const rotuloDoStatus: Record<string, string> = {
  agendada: 'AGENDADA',
  confirmada: 'CONFIRMADA',
  em_atendimento: 'EM ATENDIMENTO',
  realizada: 'REALIZADA',
  cancelada: 'CANCELADA',
  faltou: 'FALTOU',
};
