import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ErroApi, api, type Paciente } from '../../api';
import { cor, espaco, tipo } from '../../theme';
import { Carregando, Divisoria, Erro, Rotulo, Titulo, Vazio } from '../../ui';

const PAGINA = 50;

/**
 * Lista de pacientes, somente leitura.
 *
 * Cadastrar paciente é trabalho de recepção, com CPF, convênio e carteirinha —
 * um formulário de sete campos que se digita em teclado, não em vidro. O
 * endpoint existe (`POST /api/v1/pacientes`); a tela não, até alguém pedir.
 */
export default function Pacientes() {
  const inset = useSafeAreaInsets();

  const [lista, setLista] = useState<Paciente[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [buscandoMais, setBuscandoMais] = useState(false);
  const [fim, setFim] = useState(false);

  const primeiraPagina = useCallback(async () => {
    setErro(null);
    try {
      const linhas = await api.pacientes(0, PAGINA);
      setLista(linhas);
      setFim(linhas.length < PAGINA);
    } catch (e) {
      setErro(e instanceof ErroApi ? e.message : 'Não foi possível carregar os pacientes.');
      setLista([]);
    }
  }, []);

  useEffect(() => {
    void primeiraPagina();
  }, [primeiraPagina]);

  /**
   * Keyset: manda o último id visto, não um número de página. Com OFFSET o
   * banco lê e descarta as N primeiras linhas a cada requisição — é o que o
   * `PacienteController` evita de propósito.
   */
  async function proximaPagina() {
    if (fim || buscandoMais || !lista || lista.length === 0) return;
    const ultimo = lista[lista.length - 1];
    if (!ultimo) return;

    setBuscandoMais(true);
    try {
      const linhas = await api.pacientes(ultimo.idPaciente, PAGINA);
      setLista([...lista, ...linhas]);
      if (linhas.length < PAGINA) setFim(true);
    } catch (e) {
      setErro(e instanceof ErroApi ? e.message : 'Não foi possível carregar mais.');
    } finally {
      setBuscandoMais(false);
    }
  }

  return (
    <View style={{ flex: 1, paddingTop: inset.top }}>
      <View style={{ padding: espaco.lg, gap: espaco.md }}>
        <Titulo>Pacientes</Titulo>
        <Erro>{erro}</Erro>
      </View>

      <Divisoria />

      {lista === null ? (
        <Carregando />
      ) : (
        <FlatList
          data={lista}
          keyExtractor={(p) => String(p.idPaciente)}
          ItemSeparatorComponent={Divisoria}
          contentContainerStyle={{ paddingBottom: espaco.xxl }}
          ListEmptyComponent={<Vazio>Nenhum paciente cadastrado.</Vazio>}
          onEndReached={() => void proximaPagina()}
          onEndReachedThreshold={0.4}
          ListFooterComponent={
            buscandoMais ? (
              <View style={{ padding: espaco.lg }}>
                <ActivityIndicator color={cor.osso} />
              </View>
            ) : null
          }
          renderItem={({ item }) => (
            <View style={{ padding: espaco.lg, gap: 4 }}>
              <Text style={{ ...tipo.corpo, color: cor.osso }} numberOfLines={1}>
                {item.nomeCompleto}
              </Text>
              <Text style={{ ...tipo.corpo, color: cor.cinza }}>
                {item.telefoneCelular || 'sem telefone'}
              </Text>
              {item.status !== 'ativo' && <Rotulo>{item.status}</Rotulo>}
            </View>
          )}
        />
      )}
    </View>
  );
}
