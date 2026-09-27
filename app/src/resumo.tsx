import * as Speech from 'expo-speech';
import { useEffect, useState } from 'react';
import { Text, View } from 'react-native';
import { ErroApi, api, type ResumoDoPaciente } from './api';
import { cor, espaco, tipo } from './theme';
import { Botao, Carregando, Erro, Rotulo } from './ui';

/**
 * Resumo de retorno (IA-06) com os alertas falados (IA-27), na cadeira.
 *
 * <p>A voz é a do aparelho (expo-speech → TTS nativo): nenhum texto clínico sai
 * para serviço de terceiros. As frases vêm prontas do back-end, as mesmas que o
 * web mostra e fala.
 */
export function ResumoPaciente({ idPaciente }: { idPaciente: number }) {
  const [resumo, setResumo] = useState<ResumoDoPaciente | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let vivo = true;
    api
      .resumo(idPaciente)
      .then((r) => {
        if (!vivo) return;
        setResumo(r);
        falar(r.alertas.dados?.avisos ?? []);
      })
      .catch((e) => vivo && setErro(e instanceof ErroApi ? e.message : 'Resumo indisponível.'));
    return () => {
      vivo = false;
      Speech.stop();
    };
  }, [idPaciente]);

  if (erro) return <Erro>{erro}</Erro>;
  if (!resumo) return <Carregando />;

  const avisos = resumo.alertas.dados?.avisos ?? [];
  const plano = resumo.planoEmAberto.dados ?? [];
  const agenda = resumo.agenda.dados;

  return (
    <View style={{ gap: espaco.md, borderTopWidth: 1, borderTopColor: cor.fio, paddingTop: espaco.md }}>
      {!resumo.alertas.permitido ? (
        <Rotulo>Alertas clínicos: sem acesso</Rotulo>
      ) : avisos.length === 0 ? (
        <Rotulo>Nenhum alerta na triagem</Rotulo>
      ) : (
        <View style={{ borderLeftWidth: 3, borderLeftColor: cor.alarme, paddingLeft: espaco.md, gap: 4 }}>
          <Text style={{ ...tipo.rotulo, color: cor.alarme, textTransform: 'uppercase' }}>
            Alertas
          </Text>
          {avisos.map((a) => (
            <Text key={a} style={{ ...tipo.corpo, color: cor.osso, fontWeight: '700' }}>
              {a}
            </Text>
          ))}
          <View style={{ alignSelf: 'flex-start', marginTop: espaco.sm }}>
            <Botao variante="contorno" onPress={() => falar(avisos)}>
              Ouvir de novo
            </Botao>
          </View>
        </View>
      )}

      <Linha rotulo="Última conduta"
             valor={!resumo.ultimaEvolucao.permitido ? 'Sem acesso'
               : resumo.ultimaEvolucao.dados?.descricao ?? 'Nenhuma'} />
      <Linha rotulo="Plano em aberto"
             valor={!resumo.planoEmAberto.permitido ? 'Sem acesso'
               : plano.length === 0 ? 'Nada pendente'
                 : plano.map((i) => `${i.nomeProcedimento ?? 'Procedimento'}${i.dente ? ` (${i.dente})` : ''}`).join(' · ')} />
      <Linha rotulo="Próxima consulta"
             valor={!resumo.agenda.permitido ? 'Sem acesso'
               : agenda?.proximaMarcada ? new Date(agenda.proximaMarcada).toLocaleDateString() : 'Nada marcado'} />
    </View>
  );
}

function Linha({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <View style={{ gap: 2 }}>
      <Rotulo>{rotulo}</Rotulo>
      <Text style={{ ...tipo.corpo, color: cor.osso }} numberOfLines={3}>
        {valor}
      </Text>
    </View>
  );
}

function falar(frases: string[]) {
  if (frases.length === 0) return;
  Speech.stop();
  Speech.speak(`Atenção. ${frases.join(' ')}`, { language: 'pt-BR' });
}
