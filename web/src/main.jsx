import { ptBR } from '@clerk/localizations';
import { ClerkProvider } from '@clerk/react';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import Clientes from './components/Clientes.jsx';
import Login from './components/Login.jsx';
import './style.css';
import { CRIAR, LOGIN } from './rotas.js';

// Três telas, um mapa: a lista tem dado pessoal e o acesso não pertence à
// landing pública. Vale um router quando existir rota com parâmetro.
const ROTAS = { '/clientes': Clientes, '/login': Login };
const Page = ROTAS[window.location.pathname] ?? App;

/* Variáveis em vez de @clerk/themes: o tema daqui é preto, branco e canto
   vivo — sete tokens cobrem isso e não entra dependência para reescrevê-los
   depois. */
const aparencia = {
  variables: {
    colorBackground: '#000000',
    colorForeground: '#ffffff',
    colorMuted: '#000000',
    colorMutedForeground: '#838383',
    colorPrimary: '#ffffff',
    colorPrimaryForeground: '#000000',
    colorInput: '#000000',
    colorInputForeground: '#ffffff',
    colorBorder: 'rgba(255, 255, 255, .26)',
    colorDanger: '#ed1c24',
    borderRadius: '0px',
    fontFamily: "'Inter', 'Neue Haas Grotesk', 'Helvetica Neue', Helvetica, sans-serif",
  },
};

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ClerkProvider
      localization={ptBR}
      appearance={aparencia}
      signInUrl={LOGIN}
      signUpUrl={CRIAR}
      afterSignOutUrl="/"
    >
      <Page />
    </ClerkProvider>
  </StrictMode>,
);
