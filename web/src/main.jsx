import { ptBR } from '@clerk/localizations';
import { ClerkProvider } from '@clerk/react';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import App from './App.jsx';
import Login from './components/Login.jsx';
import Agenda from './paginas/Agenda.jsx';
import AreaPaciente from './paginas/AreaPaciente.jsx';
import Auditoria from './paginas/Auditoria.jsx';
import Cadastro from './paginas/Cadastro.jsx';
import Equipe from './paginas/Equipe.jsx';
import { Privacidade, TermosDeUso } from './paginas/Juridico.jsx';
import Financeiro from './paginas/Financeiro.jsx';
import Layout from './paginas/Layout.jsx';
import NaoEncontrada from './paginas/NaoEncontrada.jsx';
import Pacientes from './paginas/Pacientes.jsx';
import Prontuario from './paginas/Prontuario.jsx';
import Recepcao from './paginas/Recepcao.jsx';
import './style.css';
import './app.css';
/* Antes do primeiro render: o atributo do tema já está no <html> quando a
   primeira tela pinta, e o tema claro não pisca preto ao recarregar. */
import { APARENCIA } from './tema.js';
import {
  AGENDA, AREA_PACIENTE, AUDITORIA, CADASTRO, CRIAR, EQUIPE, FINANCEIRO, LOGIN, PACIENTES,
  PRIVACIDADE, PRONTUARIO, RECEPCAO, TERMOS,
} from './rotas.js';

/* Sem a chave o ClerkProvider não carrega NADA e não reclama: todo <Show>
   devolve null para sempre e /login sobe com a metade direita em branco. Por
   isso a falta da variável no build aparece na tela. */
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
        appearance={{ variables: APARENCIA.escuro }}
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
          {/* Também fora: paciente não tem clínica, e o /eu responderia 403. */}
          <Route path={AREA_PACIENTE} element={<AreaPaciente />} />
          <Route path={TERMOS} element={<TermosDeUso />} />
          <Route path={PRIVACIDADE} element={<Privacidade />} />

          {/* Tudo sob o Layout exige sessão — o gate fica lá, uma vez. */}
          <Route element={<Layout />}>
            <Route path={RECEPCAO} element={<Recepcao />} />
            <Route path={AGENDA} element={<Agenda />} />
            <Route path={PACIENTES} element={<Pacientes />} />
            <Route path={PRONTUARIO} element={<Prontuario />} />
            <Route path={FINANCEIRO} element={<Financeiro />} />
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
