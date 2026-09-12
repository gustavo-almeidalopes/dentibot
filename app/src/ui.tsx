/**
 * Primitivas visuais. Só o que as telas usam de fato — cada componente aqui
 * existe porque aparece em mais de um lugar.
 *
 * Alvos de toque têm 44pt de altura mínima (o número das duas plataformas), e
 * todo controle carrega papel e rótulo de acessibilidade. Isso não é
 * enfeite: é o que faz o app funcionar com VoiceOver e TalkBack ligados.
 */
import {
  ActivityIndicator,
  Pressable,
  Text,
  TextInput,
  View,
  type TextInputProps,
} from 'react-native';
import { cor, espaco, tipo } from './theme';

export function Rotulo({ children }: { children: string }) {
  return (
    <Text style={{ ...tipo.rotulo, color: cor.cinza, textTransform: 'uppercase' }}>
      {children}
    </Text>
  );
}

export function Titulo({ children }: { children: string }) {
  return (
    <Text style={{ ...tipo.display, color: cor.osso, textTransform: 'uppercase' }}>
      {children}
    </Text>
  );
}

type BotaoProps = {
  children: string;
  onPress: () => void;
  variante?: 'solido' | 'contorno' | 'alarme';
  ocupado?: boolean;
  desabilitado?: boolean;
};

export function Botao({
  children,
  onPress,
  variante = 'solido',
  ocupado = false,
  desabilitado = false,
}: BotaoProps) {
  const inerte = desabilitado || ocupado;
  const cores = {
    solido: { fundo: cor.osso, texto: cor.obsidiana, borda: cor.osso },
    contorno: { fundo: 'transparent', texto: cor.osso, borda: cor.fio },
    alarme: { fundo: 'transparent', texto: cor.alarme, borda: cor.alarme },
  }[variante];

  return (
    <Pressable
      onPress={onPress}
      disabled={inerte}
      accessibilityRole="button"
      accessibilityLabel={children}
      accessibilityState={{ disabled: inerte, busy: ocupado }}
      style={({ pressed }) => ({
        minHeight: 44,
        paddingHorizontal: espaco.lg,
        paddingVertical: espaco.md,
        backgroundColor: cores.fundo,
        borderWidth: 1,
        borderColor: cores.borda,
        // Sem raio: --radius-buttons é 0px na web.
        borderRadius: 0,
        opacity: inerte ? 0.4 : pressed ? 0.7 : 1,
        alignItems: 'center',
        justifyContent: 'center',
      })}
    >
      {ocupado ? (
        <ActivityIndicator color={cores.texto} />
      ) : (
        <Text
          style={{
            ...tipo.rotulo,
            color: cores.texto,
            textTransform: 'uppercase',
          }}
        >
          {children}
        </Text>
      )}
    </Pressable>
  );
}

type CampoProps = TextInputProps & { rotulo: string };

export function Campo({ rotulo, ...props }: CampoProps) {
  return (
    <View style={{ gap: espaco.sm }}>
      <Rotulo>{rotulo}</Rotulo>
      <TextInput
        accessibilityLabel={rotulo}
        placeholderTextColor={cor.cinza}
        {...props}
        style={{
          ...tipo.corpo,
          minHeight: 44,
          color: cor.osso,
          borderWidth: 1,
          borderColor: cor.fio,
          borderRadius: 0,
          paddingHorizontal: espaco.md,
          paddingVertical: espaco.md,
        }}
      />
    </View>
  );
}

/** Mensagem de erro. `role="alert"` para que o leitor de tela a anuncie. */
export function Erro({ children }: { children: string | null }) {
  if (!children) return null;
  return (
    <View
      accessibilityRole="alert"
      style={{
        borderLeftWidth: 3,
        borderLeftColor: cor.alarme,
        paddingLeft: espaco.md,
        paddingVertical: espaco.sm,
      }}
    >
      <Text style={{ ...tipo.corpo, color: cor.alarme }}>{children}</Text>
    </View>
  );
}

export function Vazio({ children }: { children: string }) {
  return (
    <View style={{ paddingVertical: espaco.xxl, alignItems: 'center' }}>
      <Text style={{ ...tipo.corpo, color: cor.cinza, textAlign: 'center' }}>{children}</Text>
    </View>
  );
}

export function Carregando() {
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <ActivityIndicator color={cor.osso} accessibilityLabel="Carregando" />
    </View>
  );
}

export function Divisoria() {
  return <View style={{ height: 1, backgroundColor: cor.fio }} />;
}
