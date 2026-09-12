import { Redirect } from 'expo-router';
import { useAuth } from '../auth';
import { Carregando } from '../ui';

/**
 * Porteiro. Enquanto o provedor pergunta ao backend se existe sessão (um POST
 * /auth/refresh), não há para onde mandar o usuário — mostrar o login aqui
 * faria a tela piscar em quem já estava logado.
 */
export default function Entrada() {
  const { situacao } = useAuth();

  if (situacao === 'carregando') return <Carregando />;
  return <Redirect href={situacao === 'autenticado' ? '/agenda' : '/login'} />;
}
