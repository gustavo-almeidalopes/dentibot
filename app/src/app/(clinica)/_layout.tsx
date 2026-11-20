import { useAuth } from '@clerk/expo';
import { Redirect, Tabs } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { cor, espaco, tipo } from '../../theme';
import { Carregando } from '../../ui';

/**
 * Tudo neste grupo exige sessão. O guarda está no layout, e não em cada tela,
 * para que uma tela nova nasça protegida — o mesmo princípio do
 * `anyRequest().authenticated()` na cadeia de segurança do backend.
 *
 * Sem ícones de propósito: o rótulo em caixa-alta é a linguagem visual do
 * produto, e uma biblioteca de ícones seria uma dependência a mais para dizer
 * o que três palavras já dizem.
 */
export default function LayoutClinica() {
  const { isLoaded, isSignedIn } = useAuth();
  const inset = useSafeAreaInsets();

  if (!isLoaded) return <Carregando />;
  if (!isSignedIn) return <Redirect href="/login" />;

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: cor.osso,
        tabBarInactiveTintColor: cor.cinza,
        tabBarStyle: {
          backgroundColor: cor.obsidiana,
          borderTopWidth: 1,
          borderTopColor: cor.fio,
          // Altura padrão do RN não acomoda o rótulo sem ícone. Altura fixa é
          // usada como está, e o paddingBottom do inset fica DENTRO dela: sem
          // somar o inset, a navegação de 3 botões do Android (48dp, e o
          // edge-to-edge é obrigatório no SDK 57) deixa 9dp para o rótulo.
          height: 64 + inset.bottom,
          paddingTop: espaco.sm,
        },
        tabBarLabelStyle: {
          ...tipo.rotulo,
          textTransform: 'uppercase',
        },
        tabBarIcon: () => null,
      }}
    >
      <Tabs.Screen name="agenda" options={{ title: 'Agenda' }} />
      <Tabs.Screen name="pacientes" options={{ title: 'Pacientes' }} />
      <Tabs.Screen name="perfil" options={{ title: 'Perfil' }} />
    </Tabs>
  );
}
