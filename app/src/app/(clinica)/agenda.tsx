import { useCallback, useEffect, useState } from 'react';
import { FlatList, Pressable, RefreshControl, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  ACOES_POR_STATUS,
  ErroApi,
  api,
  type Acao,
  type Consulta,
} from '../../api';
import { cor, corDoStatus, espaco, rotuloDoStatus, tipo } from '../../theme';
import { Botao, Campo, Carregando, Divisoria, Erro, Rotulo, Titulo, Vazio } from '../../ui';

const ROTULO_DA_ACAO: Record<Acao, string> = {
  confirmar: 'Confirmar',
  concluir: 'Concluir',
  falta: 'Faltou',
  cancelar: 'Cancelar',
};

/** Meia-noite local do dia pedido até a meia-noite seguinte, em UTC. */
function janelaDoDia(dia: Date): { de: Date; ate: Date } {
  const de = new Date(dia.getFullYear(), dia.getMonth(), dia.getDate());
  const ate = new Date(de);
  ate.setDate(ate.getDate() + 1);
  return { de, ate };
}

/**
 * ponytail: renderiza no fuso do aparelho. A clínica tem `timezone` em
 * `clinicas.clinicas`, mas `/auth/me` não o devolve — quando devolver, formatar
 * com `timeZone` para que um dentista em viagem veja o horário da clínica.
 */
function hora(iso: string): string {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function nomeDoDia(dia: Date): string {
  const hoje = new Date();
  const mesmoDia = (a: Date, b: Date) => a.toDateString() === b.toDateString();
  if (mesmoDia(dia, hoje)) return 'Hoje';
  const ontem = new Date(hoje);
  ontem.setDate(ontem.getDate() - 1);
  if (mesmoDia(dia, ontem)) return 'Ontem';
  const amanha = new Date(hoje);
  amanha.setDate(amanha.getDate() + 1);
  if (mesmoDia(dia, amanha)) return 'Amanhã';
  return dia.toLocaleDateString([], { day: '2-digit', month: '2-digit' });
}

export default function Agenda() {
  const inset = useSafeAreaInsets();

  const [dia, setDia] = useState(() => new Date());
  const [consultas, setConsultas] = useState<Consulta[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [recarregando, setRecarregando] = useState(false);
  /** Id da consulta com ação em curso: trava só a linha, não a tela. */
  const [ocupada, setOcupada] = useState<number | null>(null);
  /** Id da consulta cujo campo de motivo está aberto. */
  const [cancelando, setCancelando] = useState<number | null>(null);
  const [motivo, setMotivo] = useState('');

  const carregar = useCallback(async (alvo: Date) => {
    setErro(null);
    try {
      const { de, ate } = janelaDoDia(alvo);
      const linhas = await api.agenda(de, ate);
      setConsultas([...linhas].sort((a, b) => a.inicioEm.localeCompare(b.inicioEm)));
    } catch (e) {
      // 401 não cai aqui: o cliente já derrubou a sessão e o layout redireciona.
      setErro(e instanceof ErroApi ? e.message : 'Não foi possível carregar a agenda.');
      setConsultas([]);
    }
  }, []);

  useEffect(() => {
    void carregar(dia);
  }, [dia, carregar]);

  async function agir(consulta: Consulta, acao: Acao) {
    if (acao === 'cancelar' && cancelando !== consulta.idConsulta) {
      setCancelando(consulta.idConsulta);
      setMotivo('');
      return;
    }

    setOcupada(consulta.idConsulta);
    setErro(null);
    try {
      if (acao === 'confirmar') await api.confirmar(consulta.idConsulta);
      else if (acao === 'concluir') await api.concluir(consulta.idConsulta);
      else if (acao === 'falta') await api.registrarFalta(consulta.idConsulta);
      else await api.cancelar(consulta.idConsulta, motivo.trim());

      setCancelando(null);
      setMotivo('');
      // Relê em vez de adivinhar o novo estado: o backend é a autoridade da
      // transição, e um 409 aqui significa que a tela estava desatualizada.
      await carregar(dia);
    } catch (e) {
      setErro(e instanceof ErroApi ? e.message : 'A ação falhou.');
    } finally {
      setOcupada(null);
    }
  }

  function mover(dias: number) {
    const proximo = new Date(dia);
    proximo.setDate(proximo.getDate() + dias);
    setDia(proximo);
    setConsultas(null);
    setCancelando(null);
  }

  return (
    <View style={{ flex: 1, paddingTop: inset.top }}>
      <View style={{ padding: espaco.lg, gap: espaco.md }}>
        <Titulo>Agenda</Titulo>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: espaco.md }}>
          <Seta rotulo="Dia anterior" glifo="←" onPress={() => mover(-1)} />
          <Text style={{ ...tipo.titulo, color: cor.osso, flex: 1, textAlign: 'center' }}>
            {nomeDoDia(dia)}
          </Text>
          <Seta rotulo="Próximo dia" glifo="→" onPress={() => mover(1)} />
        </View>
        <Erro>{erro}</Erro>
      </View>

      <Divisoria />

      {consultas === null ? (
        <Carregando />
      ) : (
        <FlatList
          data={consultas}
          keyExtractor={(c) => String(c.idConsulta)}
          ItemSeparatorComponent={Divisoria}
          contentContainerStyle={{ paddingBottom: espaco.xxl }}
          ListEmptyComponent={<Vazio>Nenhuma consulta neste dia.</Vazio>}
          refreshControl={
            <RefreshControl
              refreshing={recarregando}
              tintColor={cor.osso}
              onRefresh={async () => {
                setRecarregando(true);
                await carregar(dia);
                setRecarregando(false);
              }}
            />
          }
          renderItem={({ item }) => (
            <Linha
              consulta={item}
              ocupada={ocupada === item.idConsulta}
              cancelando={cancelando === item.idConsulta}
              motivo={motivo}
              setMotivo={setMotivo}
              onCancelarCancelamento={() => setCancelando(null)}
              onAgir={(acao) => void agir(item, acao)}
            />
          )}
        />
      )}
    </View>
  );
}

function Seta({
  glifo,
  rotulo,
  onPress,
}: {
  glifo: string;
  rotulo: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={rotulo}
      style={({ pressed }) => ({
        width: 44,
        height: 44,
        borderWidth: 1,
        borderColor: cor.fio,
        alignItems: 'center',
        justifyContent: 'center',
        opacity: pressed ? 0.6 : 1,
      })}
    >
      <Text style={{ ...tipo.titulo, color: cor.osso }}>{glifo}</Text>
    </Pressable>
  );
}

type LinhaProps = {
  consulta: Consulta;
  ocupada: boolean;
  cancelando: boolean;
  motivo: string;
  setMotivo: (v: string) => void;
  onCancelarCancelamento: () => void;
  onAgir: (acao: Acao) => void;
};

function Linha({
  consulta,
  ocupada,
  cancelando,
  motivo,
  setMotivo,
  onCancelarCancelamento,
  onAgir,
}: LinhaProps) {
  const acoes = ACOES_POR_STATUS[consulta.status];

  return (
    <View style={{ padding: espaco.lg, gap: espaco.md }}>
      <View style={{ flexDirection: 'row', gap: espaco.md, alignItems: 'baseline' }}>
        <Text style={{ ...tipo.mono, color: cor.osso }}>{hora(consulta.inicioEm)}</Text>
        <View style={{ flex: 1, gap: 2 }}>
          <Text style={{ ...tipo.corpo, color: cor.osso }} numberOfLines={1}>
            {consulta.nomePaciente}
          </Text>
          <Text style={{ ...tipo.rotulo, color: corDoStatus[consulta.status] ?? cor.cinza }}>
            {rotuloDoStatus[consulta.status] ?? consulta.status.toUpperCase()}
          </Text>
        </View>
      </View>

      {cancelando && (
        <View style={{ gap: espaco.md }}>
          <Campo
            rotulo="Motivo do cancelamento"
            value={motivo}
            onChangeText={setMotivo}
            maxLength={255}
            autoFocus
            editable={!ocupada}
          />
          <View style={{ flexDirection: 'row', gap: espaco.sm }}>
            <View style={{ flex: 1 }}>
              <Botao
                variante="alarme"
                onPress={() => onAgir('cancelar')}
                ocupado={ocupada}
                desabilitado={motivo.trim().length === 0}
              >
                Confirmar cancelamento
              </Botao>
            </View>
            <Botao variante="contorno" onPress={onCancelarCancelamento} desabilitado={ocupada}>
              Voltar
            </Botao>
          </View>
        </View>
      )}

      {!cancelando && acoes.length > 0 && (
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: espaco.sm }}>
          {acoes.map((acao) => (
            <Botao
              key={acao}
              variante={
                acao === 'confirmar' ? 'solido' : acao === 'concluir' ? 'contorno' : 'alarme'
              }
              onPress={() => onAgir(acao)}
              ocupado={ocupada}
            >
              {ROTULO_DA_ACAO[acao]}
            </Botao>
          ))}
        </View>
      )}

      {!cancelando && acoes.length === 0 && <Rotulo>Sem ações</Rotulo>}
    </View>
  );
}
