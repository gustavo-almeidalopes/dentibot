import { useAuth } from '@clerk/expo';
import { Redirect } from 'expo-router';
import { Carregando } from '../ui';

/**
 * Porteiro. isLoaded antes de isSignedIn não é zelo: enquanto o Clerk lê o
 * token do Keychain, isSignedIn é false — mandar para o login aí faria a tela
 * piscar em quem já estava logado.
 */
export default function Entrada() {
  const { isLoaded, isSignedIn } = useAuth();

  if (!isLoaded) return <Carregando />;
  return <Redirect href={isSignedIn ? '/agenda' : '/login'} />;
}
