import { Component } from 'react';

/**
 * O que aparece quando um pedaço do site não chega.
 *
 * <p>Com a divisão por rota, cada tela é um arquivo que o navegador busca na
 * hora. No 3G que cai no meio da navegação o `import()` rejeita, e sem este
 * limite o React desmonta tudo: tela branca, sem uma palavra. Aqui vira uma
 * frase e um botão.
 *
 * <p>Classe porque limite de erro no React ainda só existe assim.
 */
export default class RecuperaCarga extends Component {
  state = { falhou: false };

  static getDerivedStateFromError() {
    return { falhou: true };
  }

  render() {
    if (!this.state.falhou) return this.props.children;
    return (
      <main id="main" className="edge" role="alert" style={{ paddingBlock: 'var(--spacing-30)' }}>
        <p className="body">Algo não carregou — a conexão pode ter caído.</p>
        <p style={{ marginTop: 'var(--spacing-20)' }}>
          <button type="button" className="btn" onClick={() => window.location.reload()}>
            Tentar de novo
          </button>
        </p>
      </main>
    );
  }
}
