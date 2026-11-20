import { useAuth } from '@clerk/react';
import { useEffect, useSyncExternalStore } from 'react';

const CACHE_DA_AGENDA = 'dentibot-agenda';
const DONO = 'dentibot-dono-da-agenda-salva';

/**
 * A agenda salva para uso offline (ST-54) pertence a quem a abriu. Saiu ou
 * trocou de usuário, ela é apagada — recepção costuma dividir computador.
 * O service worker só é registrado no build: em dev ele disputaria com o HMR.
 */
export default function SessaoOffline() {
  const { isLoaded, userId } = useAuth();

  useEffect(() => {
    if (import.meta.env.PROD && 'serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(() => {});
    }
  }, []);

  useEffect(() => {
    if (!isLoaded || typeof caches === 'undefined') return;
    let dono = null;
    try { dono = localStorage.getItem(DONO); } catch { /* sem storage: apaga sempre */ }
    if (!userId || dono !== userId) {
      caches.delete(CACHE_DA_AGENDA).catch(() => {});
      try {
        if (userId) localStorage.setItem(DONO, userId);
        else localStorage.removeItem(DONO);
      } catch { /* idem */ }
    }
  }, [isLoaded, userId]);

  return null;
}

const assinar = (avisar) => {
  window.addEventListener('online', avisar);
  window.addEventListener('offline', avisar);
  return () => {
    window.removeEventListener('online', avisar);
    window.removeEventListener('offline', avisar);
  };
};

/** Está sem rede agora? Vivo, sem estado próprio. */
export function useSemConexao() {
  return useSyncExternalStore(assinar, () => !navigator.onLine, () => false);
}
