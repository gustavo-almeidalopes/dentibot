import { Redirect } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ErroApi } from '../api';
import { useAuth } from '../auth';
import { cor, espaco, tipo } from '../theme';
import { Botao, Campo, Erro, Titulo } from '../ui';

export default function Login() {
  const { situacao, entrar } = useAuth();
  const inset = useSafeAreaInsets();

  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [entrando, setEntrando] = useState(false);

  if (situacao === 'autenticado') return <Redirect href="/agenda" />;

  const podeEnviar = email.trim().length > 0 && senha.length > 0 && !entrando;

  async function enviar() {
    setErro(null);
    setEntrando(true);
    try {
      await entrar(email, senha);
    } catch (e) {
      // A mensagem vem do servidor, que é deliberadamente vago em falha de
      // credencial — não distinguir "não existe" de "senha errada" é o que
      // impede enumerar contas. A UI não tenta melhorar isso.
      setErro(e instanceof ErroApi ? e.message : 'Não foi possível entrar.');
    } finally {
      setEntrando(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView
        contentContainerStyle={{
          flexGrow: 1,
          justifyContent: 'center',
          padding: espaco.lg,
          paddingTop: inset.top + espaco.xxl,
          paddingBottom: inset.bottom + espaco.lg,
          gap: espaco.xl,
        }}
        keyboardShouldPersistTaps="handled"
      >
        <View style={{ gap: espaco.xs }}>
          <Titulo>DentiBot</Titulo>
          <Text style={{ ...tipo.corpo, color: cor.cinza }}>
            Agenda e pacientes da sua clínica.
          </Text>
        </View>

        <View style={{ gap: espaco.lg }}>
          <Campo
            rotulo="E-mail"
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="email-address"
            textContentType="username"
            autoComplete="email"
            inputMode="email"
            editable={!entrando}
            returnKeyType="next"
          />
          <Campo
            rotulo="Senha"
            value={senha}
            onChangeText={setSenha}
            secureTextEntry
            autoCapitalize="none"
            autoCorrect={false}
            textContentType="password"
            autoComplete="current-password"
            editable={!entrando}
            returnKeyType="go"
            onSubmitEditing={() => {
              if (podeEnviar) void enviar();
            }}
          />
          <Erro>{erro}</Erro>
          <Botao onPress={() => void enviar()} ocupado={entrando} desabilitado={!podeEnviar}>
            Entrar
          </Botao>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
