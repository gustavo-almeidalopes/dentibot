import { StrictMode, Suspense, lazy } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import App from './App.jsx';
import RecuperaCarga from './components/RecuperaCarga.jsx';
import NaoEncontrada from './paginas/NaoEncontrada.jsx';
/* As fontes vêm do próprio domínio, e não do Google Fonts: duas negociações de
   DNS e TLS a menos no 3G, e o IP de quem abre o site não vai para um terceiro.
   Os mesmos pesos do <link> que saiu do index.html. */
import '@fontsource/antonio/400.css';
import '@fontsource/antonio/700.css';
import '@fontsource/cormorant-sc/400.css';
import '@fontsource/inter/400.css';
import '@fontsource/inter/700.css';
import './style.css';
import './app.css';
import {
  AGENDA, AUDITORIA, CADASTRO, EQUIPE, FINANCEIRO, LOGIN, PACIENTES, PRONTUARIO,
} from './rotas.js';

/* Router de verdade, e não o mapa de `window.location.pathname` que estava
   aqui. O comentário anterior dizia "vale um router quando existir rota com
   parâmetro" — `/pacientes/:id/prontuario` é essa rota. O mapa também não tinha
   404: qualquer caminho desconhecido caía na landing com 200, então um link
   errado parecia funcionar. */

/* A landing e o 404 entram de cara; todo o resto, sob demanda. Quem abre o site
   num 3G baixa só a página de vendas — nem o Clerk, nem Agenda, Prontuário e
   Odontograma. O Clerk mora no ComClerk, que só monta nas rotas que precisam de
   sessão. */
const ComClerk = lazy(() => import('./ComClerk.jsx'));
const Login = lazy(() => import('./components/Login.jsx'));
const Cadastro = lazy(() => import('./paginas/Cadastro.jsx'));
const Layout = lazy(() => import('./paginas/Layout.jsx'));
const Agenda = lazy(() => import('./paginas/Agenda.jsx'));
const Pacientes = lazy(() => import('./paginas/Pacientes.jsx'));
const Prontuario = lazy(() => import('./paginas/Prontuario.jsx'));
const Financeiro = lazy(() => import('./paginas/Financeiro.jsx'));
const Equipe = lazy(() => import('./paginas/Equipe.jsx'));
const Auditoria = lazy(() => import('./paginas/Auditoria.jsx'));

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <RecuperaCarga>
        <Suspense fallback={<p className="body body-ash edge" role="status">Carregando…</p>}>
          <Routes>
            <Route path="/" element={<App />} />

            <Route element={<ComClerk />}>
              <Route path={LOGIN} element={<Login />} />
              {/* Fora do Layout de propósito: quem chega aqui ainda não tem clínica,
                  e o menu do Layout só aponta para telas que responderiam 401. */}
              <Route path={CADASTRO} element={<Cadastro />} />

              {/* Tudo sob o Layout exige sessão — o gate fica lá, uma vez. */}
              <Route element={<Layout />}>
                <Route path={AGENDA} element={<Agenda />} />
                <Route path={PACIENTES} element={<Pacientes />} />
                <Route path={PRONTUARIO} element={<Prontuario />} />
                <Route path={FINANCEIRO} element={<Financeiro />} />
                <Route path={EQUIPE} element={<Equipe />} />
                <Route path={AUDITORIA} element={<Auditoria />} />
              </Route>
            </Route>

            <Route path="*" element={<NaoEncontrada />} />
          </Routes>
        </Suspense>
      </RecuperaCarga>
    </BrowserRouter>
  </StrictMode>,
);
