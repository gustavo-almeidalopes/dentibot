import { Slot } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ProvedorDeAuth } from '../auth';
import { cor } from '../theme';

export default function Raiz() {
  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      <View style={{ flex: 1, backgroundColor: cor.obsidiana }}>
        <ProvedorDeAuth>
          <Slot />
        </ProvedorDeAuth>
      </View>
    </SafeAreaProvider>
  );
}
