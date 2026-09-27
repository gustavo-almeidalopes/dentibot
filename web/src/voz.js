/**
 * Alertas falados (IA-27).
 *
 * <p>O dentista está de luva e olhando para a boca do paciente: alerta que
 * precisa ser lido na tela compete com o paciente pela atenção. Falado, não.
 *
 * <p>speechSynthesis é nativo do navegador e roda local — nenhum áudio nem texto
 * clínico sai do computador da clínica. As frases vêm prontas do back-end
 * (AlertasClinicos.avisos), então tela e voz dizem a mesma coisa.
 */
export const vozDisponivel = () => typeof globalThis.speechSynthesis !== 'undefined';

export function falar(frases, synth = globalThis.speechSynthesis,
                      Fala = globalThis.SpeechSynthesisUtterance) {
  if (!synth || !Fala || !frases?.length) return false;
  // Um aviso por vez: abrir outro paciente interrompe o anterior.
  synth.cancel();
  const fala = new Fala(`Atenção. ${frases.join(' ')}`);
  fala.lang = 'pt-BR';
  synth.speak(fala);
  return true;
}
