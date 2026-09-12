import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// /api vai para o back-end em dev, então o front nunca lida com CORS nem
// precisa saber a porta. Em produção o proxy é do nginx/host.
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
