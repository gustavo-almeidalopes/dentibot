import { useClerk, useUser } from '@clerk/expo';
import Constants from 'expo-constants';
import { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api, type Identidade } from '../../api';
import { cor, espaco, tipo } from '../../theme';
import { Botao, Divisoria, Rotulo, Titulo } from '../../ui';

export default function Perfil() {
  const inset = useSafeAreaInsets();
  const { user } = useUser();
  const { signOut } = useClerk();
  const [saindo, setSaindo] = useState(false);

  // Quem é a pessoa vem do Clerk; qual o papel dela na clínica vem do backend,
  // que é o dono dessa informação. Falha vira "—", como antes: o perfil não é
  // tela de erro, e papel é detalhe ao lado do botão de sair.
  const [identidade, setIdentidade] = useState<Identidade | null>(null);
  useEffect(() => {
    let vivo = true;
    api
      .eu()
      .then((i) => {
        if (vivo) setIdentidade(i);
      })
      .catch(() => {});
    return () => {
      vivo = false;
    };
  }, []);

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
        <Dado rotulo="Conta" valor={user?.primaryEmailAddress?.emailAddress ?? '—'} />
        <Divisoria />
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
          // signOut limpa o tokenCache no Keychain/Keystore. A troca de estado
          // do Clerk derruba o guarda do layout, que redireciona para /login —
          // não é preciso navegar daqui.
          void signOut().finally(() => setSaindo(false));
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
