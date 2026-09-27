/**
 * O texto da triagem de saúde da ficha, fora do JSX.
 *
 * <p>É o trecho que o próprio paciente lê no pré-cadastro, e por isso passa
 * pelo teste de legibilidade (legibilidade.test.mjs), que só consegue ler o
 * que não está preso no JSX.
 */
export const TRIAGEM = {
  aviso: 'Suas respostas sobre saúde são protegidas pela LGPD. Só a equipe que cuida de você lê.',
  emTratamentoMedico: 'Faz algum tratamento médico agora?',
  condicaoSistemica: 'Tem problema de coração, diabetes ou pressão alta?',
  medicamentoContinuo: 'Toma algum remédio sempre, mesmo que não seja todo dia? Qual?',
  alergia: 'Tem alergia a remédio ou a látex? Qual?',
  gravidez: 'Está grávida?',
  motivoConsulta: 'Por que você quer a consulta?',
  sensibilidade: 'Sente dor nos dentes com frio, calor ou doce?',
  sangramentoGengival: 'A gengiva sangra quando você escova ou passa fio dental?',
};
