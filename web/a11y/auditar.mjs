/**
 * O gate de acessibilidade no navegador (ODS-17).
 *
 *   cd web && npm run a11y
 *
 * Sobe o dist/ com o preview() do próprio Vite, abre o Chrome já instalado
 * (o playwright-core não baixa navegador) e, em cada rota pública:
 *
 *   - roda o axe-core com as regras A e AA das WCAG 2.0, 2.1 e 2.2, e reprova
 *     com QUALQUER violação, não só as críticas;
 *   - confere o reflow: a 320px de largura, nada de rolagem horizontal (1.4.10).
 *
 * E confere a carga que falha: com o chunk do ComClerk bloqueado, o /login
 * mostra a recuperação, não uma tela branca.
 *
 * O relatório vai para a11y-relatorio.json — é a evidência do indicador do
 * ODS-17 ("violações críticas = zero, origem: CI (axe-core)"). As telas
 * autenticadas não passam por aqui: exigiriam o segredo do Clerk no CI e um
 * mock por endpoint. Elas têm o roteiro manual de docs/acessibilidade.md.
 */
import AxeBuilder from '@axe-core/playwright';
import { appendFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright-core';
import { loadEnv, preview } from 'vite';

const RAIZ = fileURLToPath(new URL('..', import.meta.url));
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

/* O mesmo env que o build leu (.env.local e variáveis do processo). Sem a chave,
   o /login é só "Configuração ausente" — auditá-lo seria auditar o aviso. */
const TEM_CHAVE = Boolean(loadEnv('production', RAIZ, 'VITE_').VITE_CLERK_PUBLISHABLE_KEY);

const ROTAS = [
  { nome: 'landing', caminho: '/' },
  { nome: '404', caminho: '/nao-existe' },
  /* O widget vem de um script remoto do Clerk: espera o botão principal. O
     "Registre-se" (.cl-footerActionLink) tem de existir e ficar dentro da
     auditoria: foi ele, em teal a 3,84:1, que motivou a Task 7. */
  {
    nome: 'login',
    caminho: '/login',
    precisaDaChave: true,
    esperar: '.cl-formButtonPrimary',
    auditado: '.cl-footerActionLink',
  },
];

/* O selo "Development mode" é da instância de teste do Clerk e não existe em
   produção. É a única coisa que o axe não olha — e o selo não é só o texto: em
   modo de desenvolvimento o Clerk pinta de laranja (#f36b16) a faixa do rodapé
   do widget, e o "Secured by" cinza sobre ela dava 1,25:1. Sai da auditoria só
   essa faixa: o "Não possui uma conta? Registre-se" (.cl-footerAction) fica
   dentro. O CI roda em localhost, onde a chave de produção do Clerk não vale,
   então esta exclusão é permanente ali — o rodapé de produção fica com o
   roteiro manual. */
async function marcarSeloDeDesenvolvimento(pagina) {
  await pagina.evaluate(() => {
    for (const el of document.querySelectorAll('body *')) {
      if (el.children.length === 0 && el.textContent.trim() === 'Development mode') {
        const rodape = el.closest('.cl-footer');
        const alvos = rodape ? rodape.querySelectorAll(':scope > :not(.cl-footerAction)') : [el];
        for (const alvo of alvos) alvo.setAttribute('data-a11y-fora', '');
      }
    }
  });
}

async function auditarRota(contexto, base, rota) {
  const pagina = await contexto.newPage();
  try {
    await pagina.goto(new URL(rota.caminho, base).href, { waitUntil: 'load' });
    if (rota.esperar) await pagina.waitForSelector(rota.esperar, { timeout: 30_000 });
    await marcarSeloDeDesenvolvimento(pagina);
    /* null = não achou o elemento; true = a exclusão o engoliu. */
    const auditadoFora = rota.auditado
      ? await pagina.evaluate((sel) => {
        const el = document.querySelector(sel);
        return el ? Boolean(el.closest('[data-a11y-fora]')) : null;
      }, rota.auditado)
      : false;

    const r = await new AxeBuilder({ page: pagina }).withTags(TAGS).exclude('[data-a11y-fora]').analyze();
    const largura = await larguraEm320(contexto, base, rota);

    return {
      rota: rota.nome,
      caminho: rota.caminho,
      axe: r.testEngine.version,
      aprovadas: r.passes.length,
      incompletas: r.incomplete.length,
      violacoes: r.violations.map((v) => ({
        regra: v.id,
        impacto: v.impact,
        ajuda: v.help,
        onde: v.nodes.map((n) => n.target.join(' ')),
        /* A mensagem do axe traz o que ele mediu (no contraste: frente, fundo
           e razão) — sem ela, a violação no relatório é só um seletor. */
        detalhe: v.nodes.map((n) => (n.any[0] ?? n.all[0] ?? n.none[0])?.message ?? n.failureSummary),
      })),
      reflow: { largura, passou: largura <= 320 },
      auditado: rota.auditado ? { seletor: rota.auditado, fora: auditadoFora } : null,
    };
  } finally {
    await pagina.close();
  }
}

/* A largura que o conteúdo ocupa, para o reflow (1.4.10). O html e o body têm
   overflow-x: clip (style.css): o que passa da borda é cortado, sem barra de
   rolagem, e o scrollWidth devolve sempre a largura da janela. O corte sai antes
   da medida — o que reprova o 1.4.10 aqui é conteúdo cortado, não barra. */
async function larguraDoConteudo(pagina) {
  return pagina.evaluate(() => {
    document.documentElement.style.overflowX = 'visible';
    document.body.style.overflowX = 'visible';
    return document.documentElement.scrollWidth;
  });
}

/* O reflow se mede numa página nova, já aberta a 320px — o cenário do 1.4.10 —,
   e não na que o axe acabou de auditar: o axe rola a página e mexe no DOM para
   medir contraste, e a mesma landing que dá 320px limpa dava 518px depois
   dele. Abrir largo e estreitar depois (o zoom de quem já estava na página)
   também dá 320px sem o axe no meio — conferido. */
async function larguraEm320(contexto, base, rota) {
  const pagina = await contexto.newPage();
  try {
    await pagina.setViewportSize({ width: 320, height: 640 });
    await pagina.goto(new URL(rota.caminho, base).href, { waitUntil: 'load' });
    if (rota.esperar) await pagina.waitForSelector(rota.esperar, { timeout: 30_000 });
    return await larguraDoConteudo(pagina);
  } finally {
    await pagina.close();
  }
}

/* O medidor precisa enxergar transbordo — senão "320px" não prova nada. Antes
   de valer a medida das rotas, um bloco de 2000px numa página real tem de
   aparecer nela. */
async function calibrarReflow(contexto, base) {
  const pagina = await contexto.newPage();
  try {
    await pagina.setViewportSize({ width: 320, height: 640 });
    await pagina.goto(new URL('/', base).href, { waitUntil: 'load' });
    await pagina.evaluate(() => {
      const largo = document.createElement('div');
      largo.style.width = '2000px';
      largo.style.height = '1px';
      document.body.appendChild(largo);
    });
    const largura = await larguraDoConteudo(pagina);
    return { passou: largura > 320, largura };
  } finally {
    await pagina.close();
  }
}

/* Dividir em chunks cria uma falha que o bundle único não tinha: o 3G cai no
   meio da navegação e o import() rejeita. Tem de aparecer a recuperação. */
async function cargaQueFalha(contexto, base) {
  const pagina = await contexto.newPage();
  try {
    await pagina.route(/\/assets\/ComClerk[-.][^/]*\.js$/, (r) => r.abort());
    await pagina.goto(new URL('/login', base).href);
    await pagina.getByText('Algo não carregou').waitFor({ timeout: 10_000 });
    return { passou: true };
  } catch (erro) {
    return { passou: false, erro: erro.message.split('\n')[0] };
  } finally {
    await pagina.close();
  }
}

const servidor = await preview({ root: RAIZ, preview: { port: 4173, strictPort: true }, logLevel: 'warn' });
const base = servidor.resolvedUrls.local[0];
const navegador = await chromium.launch({ channel: 'chrome' });

const relatorio = {
  gerado: new Date().toISOString(), tags: TAGS, parcial: !TEM_CHAVE, rotas: [], calibracaoReflow: null, cargaQueFalha: null,
};
try {
  /* Movimento reduzido: o [data-reveal] da landing aparece de uma vez, e o axe
     avalia o estado final, não o meio de uma animação. */
  const contexto = await navegador.newContext({ reducedMotion: 'reduce' });
  for (const rota of ROTAS) {
    if (rota.precisaDaChave && !TEM_CHAVE) continue;
    relatorio.rotas.push(await auditarRota(contexto, base, rota));
  }
  relatorio.calibracaoReflow = await calibrarReflow(contexto, base);
  relatorio.cargaQueFalha = await cargaQueFalha(contexto, base);
} finally {
  await navegador.close();
  await servidor.close();
}

await writeFile(new URL('../a11y-relatorio.json', import.meta.url), `${JSON.stringify(relatorio, null, 2)}\n`);

const linhas = [];
let falhou = false;
for (const r of relatorio.rotas) {
  const auditadoOk = !r.auditado || r.auditado.fora === false;
  const ok = r.violacoes.length === 0 && r.reflow.passou && auditadoOk;
  falhou ||= !ok;
  linhas.push(`${ok ? 'ok   ' : 'FALHA'} ${r.caminho} — ${r.violacoes.length} violação(ões), `
    + `${r.aprovadas} regras aprovadas, reflow a 320px: ${r.reflow.largura}px`);
  if (!auditadoOk) {
    linhas.push(`      ${r.auditado.seletor} ${r.auditado.fora === null ? 'não foi achado' : 'ficou dentro da exclusão do axe'}`);
  }
  for (const v of r.violacoes) {
    linhas.push(`      ${v.regra} (${v.impacto}): ${v.ajuda} → ${v.onde.join(' | ')}`);
    for (const d of v.detalhe) linhas.push(`        ${d}`);
  }
}
const calibracao = relatorio.calibracaoReflow;
falhou ||= !calibracao.passou;
linhas.push(`${calibracao.passou ? 'ok   ' : 'FALHA'} o medidor de reflow enxerga transbordo `
  + `(um bloco de 2000px mediu ${calibracao.largura}px)`);
const carga = relatorio.cargaQueFalha;
falhou ||= !carga.passou;
linhas.push(`${carga.passou ? 'ok   ' : 'FALHA'} carga que falha no /login${carga.erro ? ` — ${carga.erro}` : ''}`);
if (relatorio.parcial) {
  linhas.push('AUDITORIA PARCIAL: /login pulado — o build não tem VITE_CLERK_PUBLISHABLE_KEY.');
}

const resumo = linhas.join('\n');
console.log(resumo);
if (process.env.GITHUB_STEP_SUMMARY) {
  await appendFile(process.env.GITHUB_STEP_SUMMARY, `### Acessibilidade\n\n\`\`\`\n${resumo}\n\`\`\`\n`);
}
if (falhou) process.exitCode = 1;
