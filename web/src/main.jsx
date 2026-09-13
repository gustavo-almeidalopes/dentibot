import { ptBR } from '@clerk/localizations';
import { ClerkProvider } from '@clerk/react';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import App from './App.jsx';
import Login from './components/Login.jsx';
import Agenda from './paginas/Agenda.jsx';
import Auditoria from './paginas/Auditoria.jsx';
import Cadastro from './paginas/Cadastro.jsx';
import Equipe from './paginas/Equipe.jsx';
import Layout from './paginas/Layout.jsx';
import NaoEncontrada from './paginas/NaoEncontrada.jsx';
import Pacientes from './paginas/Pacientes.jsx';
import Prontuario from './paginas/Prontuario.jsx';
import './style.css';
import { AGENDA, AUDITORIA, CADASTRO, CRIAR, EQUIPE, LOGIN, PACIENTES } from './rotas.js';

/* Router de verdade, e não o mapa de `window.location.pathname` que estava
   aqui. O comentário anterior dizia "vale um router quando existir rota com
   parâmetro" — `/pacientes/:id/prontuario` é essa rota. O mapa também não tinha
   404: qualquer caminho desconhecido caía na landing com 200, então um link
   errado parecia funcionar. */

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

/* Sem a chave o ClerkProvider não carrega NADA e não reclama: o próprio SDK faz
   `else if (this.#publishableKey) this.getEntryChunks()` — chave ausente é um
   ramo vazio. Aí todo <Show> devolve null para sempre e /login sobe com a
   metade direita em branco, sem os botões do Google/Microsoft/Apple. Era um
   sintoma sem nenhuma mensagem em lugar nenhum; agora a falta da variável no
   build aparece na tela em vez de virar depuração de página vazia. */
const chaveClerk = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY;

createRoot(document.getElementById('root')).render(
  !chaveClerk ? (
    <main className="edge" style={{ padding: 'var(--spacing-30)' }} role="alert">
      <h1 className="display display-sm">Configuração ausente.</h1>
      <p className="body body-ash">
        Este build subiu sem <code>VITE_CLERK_PUBLISHABLE_KEY</code>. Sem ela não há login:
        defina a variável no ambiente do build (Vercel → Environment Variables, ou
        <code> web/.env.local</code> em dev) e publique de novo.
      </p>
    </main>
  ) : (
  <StrictMode>
    <BrowserRouter>
      <ClerkProvider
        localization={ptBR}
        publishableKey={chaveClerk}
        appearance={aparencia}
        signInUrl={LOGIN}
        signUpUrl={CRIAR}
        afterSignOutUrl="/"
      >
        <Routes>
          <Route path="/" element={<App />} />
          <Route path={LOGIN} element={<Login />} />
          {/* Fora do Layout de propósito: quem chega aqui ainda não tem clínica,
              e o menu do Layout só aponta para telas que responderiam 401. */}
          <Route path={CADASTRO} element={<Cadastro />} />

          {/* Tudo sob o Layout exige sessão — o gate fica lá, uma vez. */}
          <Route element={<Layout />}>
            <Route path={AGENDA} element={<Agenda />} />
            <Route path={PACIENTES} element={<Pacientes />} />
            <Route path="/pacientes/:idPaciente/prontuario" element={<Prontuario />} />
            <Route path={EQUIPE} element={<Equipe />} />
            <Route path={AUDITORIA} element={<Auditoria />} />
          </Route>

          <Route path="*" element={<NaoEncontrada />} />
        </Routes>
      </ClerkProvider>
    </BrowserRouter>
  </StrictMode>
  ),
);
