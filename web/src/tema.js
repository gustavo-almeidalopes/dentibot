/**
 * Claro ou escuro, nas telas do sistema.
 *
 * <p>Escuro é o padrão porque é a marca: a landing é obsidiana, e o sistema
 * que ela vende tem a mesma cara. Claro é escolha de quem trabalha numa
 * recepção com janela de frente para a tela.
 *
 * <p>O atributo vai no `<html>` e o CSS decide com
 * `:root[data-tema="claro"]:has(.app-main)` — a landing e o 404 continuam pretos
 * sozinhos, porque não montam `.app-main`.
 *
 * <p>localStorage e não o perfil do Clerk: preferência de tela é do aparelho
 * (o computador da recepção e o celular do dentista pedem coisas diferentes),
 * e ler a escolha não pode esperar uma requisição — o primeiro quadro já sai
 * no tema certo porque `main.jsx` importa este módulo antes de montar nada.
 */
const CHAVE = 'dentibot.tema';

/* As duas paletas, para o Clerk. Ele não lê variável CSS nossa: estes valores
   espelham os tokens de `:root` em style.css (escuro) e de
   `:root[data-tema="claro"]:has(.app-main)` em app.css (claro). Variáveis em
   vez de @clerk/themes: preto, branco e canto vivo cabem em poucos tokens. */
const COMUM = {
  colorDanger: '#ed1c24',
  borderRadius: '0px',
  fontFamily: "'Inter', 'Neue Haas Grotesk', 'Helvetica Neue', Helvetica, sans-serif",
};

export const APARENCIA = {
  escuro: {
    ...COMUM,
    colorBackground: '#000000',
    colorForeground: '#ffffff',
    colorMuted: '#000000',
    colorMutedForeground: '#838383',
    colorPrimary: '#ffffff',
    colorPrimaryForeground: '#000000',
    colorInput: '#000000',
    colorInputForeground: '#ffffff',
    colorBorder: 'rgba(255, 255, 255, .26)',
  },
  claro: {
    ...COMUM,
    colorBackground: '#ffffff',
    colorForeground: '#111111',
    colorMuted: '#faf9f7',
    colorMutedForeground: '#5c5c5c',
    colorPrimary: '#111111',
    colorPrimaryForeground: '#ffffff',
    colorInput: '#ffffff',
    colorInputForeground: '#111111',
    colorBorder: 'rgba(0, 0, 0, .22)',
  },
};

/* try/catch porque o localStorage lança em aba anônima com cookies bloqueados.
   Sem preferência legível, o padrão vale. */
export function lerTema() {
  try {
    return localStorage.getItem(CHAVE) === 'claro' ? 'claro' : 'escuro';
  } catch {
    return 'escuro';
  }
}

export function aplicarTema(tema) {
  document.documentElement.dataset.tema = tema;
  try {
    localStorage.setItem(CHAVE, tema);
  } catch {
    /* Sem armazenamento a troca vale até recarregar, e isso basta. */
  }
}

if (typeof document !== 'undefined') document.documentElement.dataset.tema = lerTema();
