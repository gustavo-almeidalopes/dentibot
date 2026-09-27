// ST-46. E2E contra o web de verdade: `npm run e2e`. O servidor de dev faz
// proxy de /api para a API local, então as jornadas logadas pedem a API de pé
// (iniciar.bat) — as públicas, não.
import { defineConfig } from '@playwright/test';
import { loadEnv } from 'vite';

// As chaves do Clerk moram em .env.local, que o Playwright não lê sozinho.
// Variável já definida no ambiente ganha, como no resto do projeto.
Object.assign(process.env, { ...loadEnv('development', import.meta.dirname, ''), ...process.env });

// Sem chave (CI sem a variável), um domínio .invalid: o Clerk não carrega, mas
// as telas públicas sobem — sem chave nenhuma o main.jsx mostra só o aviso.
process.env.VITE_CLERK_PUBLISHABLE_KEY ||= `pk_test_${btoa('clerk.invalid$')}`;

const URL = process.env.E2E_URL || 'http://localhost:5173';

export default defineConfig({
  testDir: 'e2e',
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  // O Chrome instalado, e não o baixado pelo Playwright: o runner do GitHub
  // já tem, e aqui não entram 150 MB de navegador.
  use: { baseURL: URL, channel: 'chrome', trace: 'retain-on-failure' },
  // Com E2E_URL (um deploy), testa o que está lá e não sobe nada.
  webServer: process.env.E2E_URL ? undefined : {
    command: 'npm run dev -- --strictPort',
    url: URL,
    reuseExistingServer: !process.env.CI,
  },
});
