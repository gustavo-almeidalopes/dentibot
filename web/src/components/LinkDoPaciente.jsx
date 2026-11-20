import { Link } from 'react-router-dom';
import { usePode } from '../paginas/Layout.jsx';
import { prontuarioDe } from '../rotas.js';

/**
 * O nome do paciente, levando ao prontuário de quem pode abri-lo.
 *
 * <p>Toda tela que lista paciente — agenda, conversas, cobranças,
 * acompanhamento — liga ao prontuário por aqui, e não cada uma do seu jeito:
 * antes só duas ligavam, e o dentista que via o nome na agenda voltava à lista
 * de pacientes para procurá-lo.
 *
 * <p>Recepção e financeiro não alcançam prontuário (dado de saúde, LGPD art.
 * 11): para eles o nome fica texto, e a tela não convida ao 403. O nome vai por
 * `state` para o cabeçalho do prontuário não esperar o resumo para dizer de
 * quem é.
 */
export default function LinkDoPaciente({ idPaciente, nome }) {
  const pode = usePode();
  const rotulo = nome ?? `Paciente ${idPaciente}`;
  if (idPaciente == null || !pode('PRONTUARIO')) return rotulo;
  return <Link to={prontuarioDe(idPaciente)} state={{ nomePaciente: nome }}>{rotulo}</Link>;
}
