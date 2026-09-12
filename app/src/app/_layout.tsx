import { ClerkProvider } from '@clerk/expo';
import { tokenCache } from '@clerk/expo/token-cache';
import { Slot } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { cor } from '../theme';

const publishableKey = process.env.EXPO_PUBLIC_CLERK_PUBLISHABLE_KEY ?? '';

if (!publishableKey) {
  throw new Error(
    'Falta EXPO_PUBLIC_CLERK_PUBLISHABLE_KEY no .env.local. Rode `clerk env pull` e reinicie o dev server.',
  );
}

/**
 * tokenCache guarda a sessão no Keychain (iOS) / Keystore (Android) via
 * expo-secure-store. Sem ele o usuário reloga a cada abertura — e era esse o
 * trabalho do ProvedorDeAuth, que saiu junto com o refresh manual.
 */
export default function Raiz() {
  return (
    <ClerkProvider publishableKey={publishableKey} tokenCache={tokenCache}>
      <SafeAreaProvider>
        <StatusBar style="light" />
        <View style={{ flex: 1, backgroundColor: cor.obsidiana }}>
          <Slot />
        </View>
      </SafeAreaProvider>
    </ClerkProvider>
  );
}
