import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { contagem, telHref } from '../apresentacao.js';
import FichaPaciente from '../components/FichaPaciente.jsx';
import { Aviso, Celula, Selo, Tabela } from '../components/primitivos.jsx';
import { useAcao, useRecurso } from '../dados.js';
import { prontuarioDe } from '../rotas.js';
import { Cabecalho, Estado, usePode } from './Layout.jsx';

/* O `LIMITE_MAXIMO` do PacienteController é 200, e pedir mais não traz mais.
   A constante existe para a frase de contagem saber quando a lista bateu o
   teto — sem isso a tela escrevia "200 no cadastro" para uma clínica de 900. */
const LIMITE = 200;

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
  const pacientes = useRecurso(`/pacientes?limite=${LIMITE}`);
  const lista = pacientes.dados ?? [];

  const filtrada = busca.trim()
    ? lista.filter((p) => `${p.nomeCompleto ?? ''} ${p.telefoneCelular ?? ''}`
      .toLowerCase().includes(busca.trim().toLowerCase()))
    : lista;

  return (
    <>
      <Cabecalho
        titulo="Pacientes."
        detalhe={pacientes.status === 'ok'
          ? contagem(lista.length, LIMITE, 'paciente', 'pacientes')
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
            ? 'Nenhum paciente carregado corresponde a esta busca.'
            : 'Nenhum paciente cadastrado ainda.')
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
              <Celula rotulo="Nome">{p.nomeCompleto ?? '—'}</Celula>
              <Celula rotulo="Telefone" num>
                {telHref(p.telefoneCelular)
                  ? <a href={telHref(p.telefoneCelular)}>{p.telefoneCelular}</a>
                  : '—'}
              </Celula>
              {/* Mapa vazio de propósito: os valores do CHECK de
                  `pacientes.status` não foram conferidos contra o banco, e um
                  rótulo inventado esconderia um estado real atrás de um
                  travessão. `descrever` mostra o valor como veio. */}
              <Celula rotulo="Status"><Selo mapa={{}} valor={p.status} /></Celula>
              <Celula rotulo="">
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
              </Celula>
            </tr>
          ))}
        </Tabela>
      </Estado>
    </>
  );
}

/**
 * O mesmo componente do auto-cadastro em /cadastro.
 *
 * <p>Antes eram quatro campos aqui — nome, CPF, celular, e-mail — e a ficha
 * completa em lugar nenhum. Faltava tudo o que muda conduta: data de
 * nascimento (dose de anestésico), alergia, condição sistêmica, gravidez.
 */
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
