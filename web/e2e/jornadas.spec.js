// ST-46: as jornadas que não podem quebrar, no navegador, contra a API de verdade.
//
// A regra de negócio de cada uma já tem teste de integração na API. O que só
// aparece aqui é a costura: Clerk → token → proxy → API → RLS → tela.
import { clerk, clerkSetup } from '@clerk/testing/playwright';
import { expect, test } from '@playwright/test';

test.describe('público', () => {
  for (const [rota, titulo] of [
    ['/', /DentiBot/],
    ['/login', /Entrar\./],
    ['/nao-existe', /encontrado\./],
    // Sem o token no fragmento, o titular não vê nada de ninguém.
    ['/titular', /inválido\./],
  ]) {
    test(`${rota} abre`, async ({ page }) => {
      await page.goto(rota);
      await expect(page.getByRole('heading', { level: 1 })).toHaveText(titulo);
    });
  }
});

test.describe('clínica', () => {
  // Um usuário do Clerk de dev que já passou pelo /cadastro e não tem 2FA — o
  // login por ticket do @clerk/testing não passa por segundo fator.
  test.skip(!process.env.E2E_EMAIL, 'defina E2E_EMAIL para as jornadas logadas');

  test.beforeAll(() => clerkSetup());
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await clerk.signIn({ page, emailAddress: process.env.E2E_EMAIL });
  });

  for (const [rota, titulo] of [
    ['/agenda', 'Agenda.'],
    ['/pacientes', 'Pacientes.'],
    ['/financeiro', 'Financeiro.'],
  ]) {
    test(`${titulo} carrega`, async ({ page }) => {
      await page.goto(rota);
      await expect(page.getByRole('heading', { name: titulo })).toBeVisible();
      await expect(page.getByText('Carregando…')).toHaveCount(0);
      // Toda falha de carga no Layout termina neste botão.
      await expect(page.getByRole('button', { name: 'Tentar de novo' })).toHaveCount(0);
    });
  }

  test('cadastra paciente e registra evolução no prontuário', async ({ page, baseURL }) => {
    test.skip(!/\/\/(localhost|127\.0\.0\.1)[:/]/.test(baseURL), 'grava dados: só em ambiente local');
    const nome = `E2E ${Date.now()}`;

    await page.goto('/pacientes');
    await page.getByRole('button', { name: 'Novo paciente' }).click();
    await page.getByLabel('Nome completo *').fill(nome);
    await page.getByLabel('Data de nascimento *').fill('1990-05-17');
    await page.getByLabel('Celular / WhatsApp *').fill(`119${String(Date.now()).slice(-8)}`);
    // O id vem da resposta, não da lista: ela carrega uma página só, e o
    // paciente novo pode não estar nela numa base cheia de execuções anteriores.
    const criado = page.waitForResponse((r) =>
      r.request().method() === 'POST' && r.url().endsWith('/api/v1/pacientes'));
    await page.getByRole('button', { name: 'Cadastrar' }).click();
    const { idPaciente } = await (await criado).json();
    await expect(page.getByText('Paciente cadastrado.')).toBeVisible();

    await page.goto(`/pacientes/${idPaciente}/prontuario`);
    const evolucao = `Profilaxia de ${nome}. Sem intercorrências.`;
    await page.getByLabel('Nova evolução').fill(evolucao);
    await page.getByRole('button', { name: 'Registrar evolução' }).click();
    await expect(page.getByText(evolucao)).toBeVisible();
  });
});
