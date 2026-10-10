import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/* A API publicada pelo render.yaml. Vale só no build da Vercel, que exporta
   VERCEL=1, e só se VITE_API_BASE não veio de Settings → Environment
   Variables: a variável, quando existe, continua mandando. Fora da Vercel
   (dev, e2e, CI, `npm run a11y`, `vite preview`) nada muda: o bundle chama
   /api/v1 no próprio host. A origem tem de estar no connect-src dos dois
   vercel.json, e o borda.test.mjs confere. Escrever em process.env aqui
   funciona porque o Vite só lê as VITE_* depois de carregar este arquivo. */
const API_NA_VERCEL = 'https://dentibot-api.onrender.com/api/v1';
if (process.env.VERCEL && !process.env.VITE_API_BASE) process.env.VITE_API_BASE = API_NA_VERCEL;

// /api vai para o back-end em dev, então o front nunca lida com CORS nem
// precisa saber a porta.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // 8080: é a porta do Spring Boot (`server.port` em application.yml). Era
      // 8000, da API em ASP.NET que saiu na V2 — o proxy apontava para porta
      // sem ninguém ouvindo.
      '/api': { target: process.env.VITE_API_URL || 'http://localhost:8080', changeOrigin: true },
    },
  },
});
