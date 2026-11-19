import { createContext, useContext } from 'react';

/* Recurso → ação → alcance, como o back-end calculou para ESTE token (a
   resposta do /eu). O Layout fornece; qualquer tela ou componente lê. */
export const Permissoes = createContext(null);

/* Traduzir "NENHUM" em booleano é o limite do que o front sabe sobre permissão:
   quem decidiu já foi o AvaliadorDePermissao do back-end, e qualquer regra a
   mais aqui seria a segunda fonte de verdade que a invariante 3 proíbe. */
export const podeCom = (permissoes) => (recurso, acao = 'LER') =>
  (permissoes?.[recurso]?.[acao] ?? 'NENHUM') !== 'NENHUM';

/** `const pode = usePode(); pode('PACIENTE', 'CRIAR')`. */
export function usePode() {
  return podeCom(useContext(Permissoes));
}
