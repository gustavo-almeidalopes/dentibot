import { useState } from 'react';
import { Link } from 'react-router-dom';
import { LIMITE_LISTA, api } from '../api.js';
import { STATUS_PACIENTE, casaBusca, contagem } from '../apresentacao.js';
import FichaPaciente from '../components/FichaPaciente.jsx';
import {
  Aviso, Cabecalho, Celula, Estado, Selo, Tabela, Telefone,
} from '../components/primitivos.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { usePode } from '../permissoes.js';
import { RECEPCAO, prontuarioDe } from '../rotas.js';

/**
 * Lista de pacientes.
 *
 * <p>O que o back-end devolve aqui é deliberadamente pouco: id, nome, telefone e
 * status. A camada 5 diz que a recepção vê "só nome e horário", e a forma
 * correta de implementar isso é a consulta NÃO TRAZER o resto — campo que não
 * veio do banco não vaza em log, em erro nem em telemetria.
 */
export default function Pacientes() {
  const [criando, setCriando] = useState(false);
  const [busca, setBusca] = useState('');
  const pode = usePode();
  const pacientes = useRecurso(`/pacientes?limite=${LIMITE_LISTA}`);
  const lista = pacientes.dados ?? [];

  const filtrada = lista.filter((p) => casaBusca(busca, p.nomeCompleto, p.telefoneCelular));

  return (
    <>
      <Cabecalho
        titulo="Pacientes."
        detalhe={pacientes.status === 'ok'
          ? contagem(lista.length, LIMITE_LISTA, 'paciente', 'pacientes')
          : 'Cadastro da clínica'}
        /* Financeiro e auxiliar leem o cadastro e não criam. Mostrar o botão
           para eles seria oferecer um formulário que termina em 403. */
        acao={pode('PACIENTE', 'CRIAR') && (
          <button type="button" className="btn btn-fill"
                  onClick={() => setCriando((c) => !c)}>
            {criando ? 'Cancelar' : 'Novo paciente'}
          </button>
        )}
      />

      <div className="filtros filtros-linha">
        <label className="campo-app" style={{ maxWidth: '24rem' }}>
          <span className="cap cap-ash">Buscar</span>
          <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)}
                 placeholder="Nome ou telefone" />
        </label>
        {/* Dizer o alcance da busca em vez de prometer o cadastro inteiro: o
            back-end tem keyset mas não tem busca, e uma caixa que parece
            procurar em tudo faria alguém concluir que o paciente não existe. */}
        <p className="cap cap-ash">Filtra os {lista.length} carregados nesta tela.</p>
      </div>

      {criando && (
        <NovoPaciente onCriado={() => { setCriando(false); pacientes.recarregar(); }} />
      )}

      <Estado
        status={pacientes.status}
        erro={pacientes.erro}
        onTentarDeNovo={pacientes.recarregar}
        esqueleto={{ linhas: 8, colunas: 4 }}
        vazio={filtrada.length === 0
          ? (busca
            ? { titulo: 'Ninguém com esse nome.', texto: 'Nenhum paciente carregado corresponde a esta busca.' }
            : { titulo: 'Cadastro vazio.', texto: 'Nenhum paciente cadastrado ainda. O primeiro entra por Novo paciente.' })
          : null}
      >
        <Tabela colunas={[
          { chave: 'nome', rotulo: 'Nome' },
          { chave: 'telefone', rotulo: 'Telefone' },
          { chave: 'status', rotulo: 'Status' },
          { chave: 'acoes', rotulo: '' },
        ]}>
          {filtrada.map((p) => (
            <tr key={p.idPaciente}>
              <Celula rotulo="Nome" className="celula-nome">{p.nomeCompleto ?? '—'}</Celula>
              <Celula rotulo="Telefone" num>
                <Telefone numero={p.telefoneCelular} vazio="—" />
              </Celula>
              <Celula rotulo="Status"><Selo mapa={STATUS_PACIENTE} valor={p.status} /></Celula>
              <Celula rotulo="">
                <div className="acoes">
                  {/* Do cadastro direto para o horário, com o paciente já escolhido
                      no painel de agendar da Recepção. */}
                  {pode('AGENDA', 'CRIAR') && (
                    <Link className="btn btn-sm" to={RECEPCAO} state={{ paciente: p }}>
                      Agendar
                    </Link>
                  )}
                  {/* Recepção e financeiro não alcançam prontuário — dado de
                      saúde, LGPD art. 11. Sem o link a tela não convida ao 403. */}
                  {pode('PRONTUARIO') && (
                    <Link className="btn btn-sm"
                          to={prontuarioDe(p.idPaciente)}
                          /* O nome já está aqui. Passar por state evita o
                             "Paciente 7" no cabeçalho do prontuário sem custar
                             uma requisição. */
                          state={{ nomePaciente: p.nomeCompleto }}>
                      Prontuário
                    </Link>
                  )}
                </div>
              </Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}

/** O mesmo componente do auto-cadastro em /cadastro. */
function NovoPaciente({ onCriado }) {
  const { executar, enviando, erro, sucesso } = useAcao(onCriado);

  return (
    <>
      <Aviso texto={sucesso} />
      <FichaPaciente
        enviando={enviando}
        erro={erro}
        onEnviar={(corpo) => executar(api.post('/pacientes', corpo), 'Paciente cadastrado.')}
      />
    </>
  );
}
