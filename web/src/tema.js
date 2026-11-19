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
