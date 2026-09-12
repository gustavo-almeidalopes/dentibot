import Constants from 'expo-constants';
import { useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '../../auth';
import { cor, espaco, tipo } from '../../theme';
import { Botao, Divisoria, Rotulo, Titulo } from '../../ui';

export default function Perfil() {
  const inset = useSafeAreaInsets();
  const auth = useAuth();
  const [saindo, setSaindo] = useState(false);

  const identidade = auth.situacao === 'autenticado' ? auth.identidade : null;

  return (
    <ScrollView
      style={{ flex: 1 }}
      contentContainerStyle={{
        paddingTop: inset.top + espaco.lg,
        padding: espaco.lg,
        gap: espaco.xl,
      }}
    >
      <Titulo>Perfil</Titulo>

      <View style={{ gap: espaco.md }}>
        <Dado rotulo="Papel" valor={identidade?.papel ?? '—'} />
        <Divisoria />
        <Dado rotulo="Clínica" valor={identidade?.clinicaId ? `#${identidade.clinicaId}` : '—'} />
        <Divisoria />
        <Dado rotulo="Usuário" valor={identidade?.usuarioId ? `#${identidade.usuarioId}` : '—'} />
      </View>

      <Botao
        variante="alarme"
        ocupado={saindo}
        onPress={() => {
          setSaindo(true);
          // `sair` não lança: falha de rede ainda derruba a sessão local.
          void auth.sair().finally(() => setSaindo(false));
        }}
      >
        Sair
      </Botao>

      <View style={{ gap: espaco.xs }}>
        <Rotulo>Versão</Rotulo>
        <Text style={{ ...tipo.corpo, color: cor.cinza }}>
          {Constants.expoConfig?.version ?? '—'}
        </Text>
      </View>
    </ScrollView>
  );
}

function Dado({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <View style={{ gap: 4 }}>
      <Rotulo>{rotulo}</Rotulo>
      <Text style={{ ...tipo.corpo, color: cor.osso }}>{valor}</Text>
    </View>
  );
}
