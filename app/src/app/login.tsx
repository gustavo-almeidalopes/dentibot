import { useAuth, useSignIn, useSignUp } from '@clerk/expo';
import { Redirect, router, type Href } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { BackHandler, KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { cor, espaco, tipo } from '../theme';
import { Botao, Campo, Erro, Titulo } from '../ui';

/**
 * Fluxo combinado: uma tela que entra ou cria conta. O usuário digita e-mail e
 * senha; se o e-mail não existe, o mesmo par vira cadastro e a tela pede o
 * código que o Clerk mandou por e-mail. Duas telas separadas obrigariam a
 * escolher "já tenho conta?" antes de o app saber a resposta — e ele sabe.
 */

/**
 * Tarefa de sessão pendente (MFA obrigatório, escolha de organização) não pode
 * cair na agenda: o Clerk ainda não terminou. Nenhuma está ligada hoje, então o
 * caminho certo é simplesmente não navegar.
 */
/**
 * signUp.status é um sinal: muda durante o await logo acima da leitura, mas o
 * TypeScript guarda a narrowing de antes e passa a jurar que 'complete' é
 * impossível ali. Ler por aqui devolve o valor de verdade.
 */
const statusAtual = (r: { status: string }) => r.status;

const navegarAposAuth = ({ session, decorateUrl }: any) => {
  if (session?.currentTask) return;
  router.replace(decorateUrl('/agenda') as Href);
};

export default function Login() {
  const { isLoaded, isSignedIn } = useAuth();
  const { signIn, fetchStatus: statusEntrada } = useSignIn();
  const { signUp, fetchStatus: statusCadastro } = useSignUp();
  const inset = useSafeAreaInsets();

  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [codigo, setCodigo] = useState('');
  const [verificando, setVerificando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const voltarParaCredenciais = useCallback(() => {
    setVerificando(false);
    setCodigo('');
    setErro(null);
  }, []);

  /* Android tem um voltar que o iOS não tem. Sem isto ele sai da tela inteira
     no meio da verificação, e o código que acabou de chegar por e-mail não tem
     mais onde ser digitado. Antes dos returns condicionais: hook não pode
     ficar atrás de um if. */
  useEffect(() => {
    if (!verificando) return;
    const inscricao = BackHandler.addEventListener('hardwareBackPress', () => {
      voltarParaCredenciais();
      return true; // consumido: o sistema não fecha a tela.
    });
    return () => inscricao.remove();
  }, [verificando, voltarParaCredenciais]);

  const ocupado = statusEntrada === 'fetching' || statusCadastro === 'fetching';

  if (!isLoaded) return null;
  if (isSignedIn) return <Redirect href="/agenda" />;

  const podeEnviar = email.trim().length > 0 && senha.length > 0 && !ocupado;
  const podeVerificar = codigo.trim().length > 0 && !ocupado;

  async function enviar() {
    setErro(null);

    const { error } = await signIn.password({ emailAddress: email.trim(), password: senha });

    if (error) {
      // E-mail desconhecido não é falha: é a bifurcação para o cadastro. Os
      // demais ficam com a mensagem do Clerk, que é deliberadamente vaga em
      // credencial — não distinguir "não existe" de "senha errada" é o que
      // impede enumerar contas. A UI não tenta ser mais específica.
      if (error.code !== 'form_identifier_not_found') {
        setErro(error.longMessage ?? 'Não foi possível entrar.');
        return;
      }

      const { error: erroCadastro } = await signUp.password({
        emailAddress: email.trim(),
        password: senha,
      });
      if (erroCadastro) {
        setErro(erroCadastro.longMessage ?? 'Não foi possível criar a conta.');
        return;
      }

      await signUp.verifications.sendEmailCode();
      if (signUp.unverifiedFields?.includes('email_address')) setVerificando(true);
      return;
    }

    if (signIn.status === 'complete') {
      await signIn.finalize({ navigate: navegarAposAuth });
    } else if (signIn.status === 'needs_client_trust') {
      // Aparelho novo: o Clerk confirma por código antes de liberar a sessão.
      const fator = signIn.supportedSecondFactors?.find((f) => f.strategy === 'email_code');
      if (fator) {
        await signIn.mfa.sendEmailCode();
        setVerificando(true);
      }
    } else {
      setErro('Este acesso pede uma etapa que o app ainda não cobre.');
    }
  }

  async function verificar() {
    setErro(null);

    // O mesmo campo serve aos dois caminhos — conta recém-criada e aparelho não
    // reconhecido —, então quem decide é qual fluxo está aberto.
    if (signUp.status === 'missing_requirements') {
      const { error } = await signUp.verifications.verifyEmailCode({ code: codigo.trim() });
      if (error) {
        setErro(error.longMessage ?? 'Código inválido.');
        return;
      }
      if (statusAtual(signUp) === 'complete') {
        await signUp.finalize({ navigate: navegarAposAuth });
      } else {
        setErro('O cadastro ainda pede informação que esta tela não coleta.');
      }
      return;
    }

    const { error } = await signIn.mfa.verifyEmailCode({ code: codigo.trim() });
    if (error) {
      setErro(error.longMessage ?? 'Código inválido.');
      return;
    }
    if (signIn.status === 'complete') await signIn.finalize({ navigate: navegarAposAuth });
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
            {verificando
              ? 'Digite o código que enviamos para ' + email.trim() + '.'
              : 'Agenda e pacientes da sua clínica.'}
          </Text>
        </View>

        {verificando ? (
          <View style={{ gap: espaco.lg }}>
            <Campo
              rotulo="Código"
              value={codigo}
              onChangeText={setCodigo}
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="number-pad"
              textContentType="oneTimeCode"
              autoComplete="one-time-code"
              editable={!ocupado}
              returnKeyType="go"
              onSubmitEditing={() => {
                if (podeVerificar) void verificar();
              }}
            />
            <Erro>{erro}</Erro>
            <Botao onPress={() => void verificar()} ocupado={ocupado} desabilitado={!podeVerificar}>
              Confirmar
            </Botao>
            <Botao
              variante="contorno"
              onPress={voltarParaCredenciais}
              desabilitado={ocupado}
            >
              Voltar
            </Botao>
          </View>
        ) : (
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
              editable={!ocupado}
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
              editable={!ocupado}
              returnKeyType="go"
              onSubmitEditing={() => {
                if (podeEnviar) void enviar();
              }}
            />
            <Erro>{erro}</Erro>
            <Botao onPress={() => void enviar()} ocupado={ocupado} desabilitado={!podeEnviar}>
              Entrar ou criar conta
            </Botao>
          </View>
        )}

        {/* Ponto de montagem do desafio anti-bot do Clerk. Vem ligado por padrão
            e o cadastro falha sem esta View — mesmo ela nunca aparecendo. */}
        <View nativeID="clerk-captcha" />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
