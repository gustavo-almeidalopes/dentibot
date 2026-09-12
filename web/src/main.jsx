import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import Clientes from './components/Clientes.jsx';
import Login from './components/Login.jsx';
import './style.css';

// Três telas, um mapa: a lista tem dado pessoal e o acesso não pertence à
// landing pública. Vale um router quando existir rota com parâmetro.
const ROTAS = { '/clientes': Clientes, '/login': Login };
const Page = ROTAS[window.location.pathname] ?? App;

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Page />
  </StrictMode>,
);
