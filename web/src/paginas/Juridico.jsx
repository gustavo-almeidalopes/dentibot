import { Link } from 'react-router-dom';
import { LOGIN, PRIVACIDADE, TERMOS } from '../rotas.js';

/**
 * Termos de Uso, Política de Privacidade e o aceite que aponta para os dois.
 *
 * <p>Um arquivo só porque as três coisas mudam juntas: reescrever um termo é
 * subir {@link VERSAO_TERMOS}, e o aceite registra a versão que a pessoa leu.
 * Sem isso, "aceitou os termos" não prova QUAL texto (LGPD art. 8º §2º: o ônus
 * da prova do consentimento é do controlador).
 */
export const VERSAO_TERMOS = '2026-11-20';

/* ponytail: dados da empresa ainda não definidos. Preencher antes de publicar —
   a LGPD exige identificação e contato do controlador (art. 9º III e IV) e do
   encarregado (art. 41 §1º) de forma pública. */
const EMPRESA = {
  razaoSocial: 'DentiBot Tecnologia Ltda. (razão social a definir)',
  cnpj: '00.000.000/0001-00 (a definir)',
  endereco: 'endereço da sede a definir',
  email: 'contato@dentibot.com.br',
  encarregado: 'Encarregado de Dados (nome a definir)',
  emailEncarregado: 'privacidade@dentibot.com.br',
};

function Documento({ titulo, linhas, children }) {
  return (
    <>
      <a href="#main" className="skip-link">Ir para o conteúdo</a>
      <header className="auth-top edge">
        <a href="/" className="mbar-mark" aria-label="DentiBot — página inicial">DentiBot</a>
      </header>
      <main id="main" className="lei edge">
        <h1 className="display">{linhas.map((l) => <span key={l} className="lei-linha">{l}</span>)}</h1>
        <p className="credit lei-versao">
          {titulo} · versão {VERSAO_TERMOS} · {EMPRESA.razaoSocial} · CNPJ {EMPRESA.cnpj}
        </p>
        <div className="lei-corpo">{children}</div>
        <footer className="foot">
          <p className="credit">DentiBot · {titulo}</p>
          <div className="foot-links">
            <Link to={TERMOS} className="credit">Termos de uso</Link>
            <Link to={PRIVACIDADE} className="credit">Privacidade</Link>
            <a href="/" className="credit">Voltar ao site</a>
          </div>
        </footer>
      </main>
    </>
  );
}

function Secao({ titulo, children }) {
  return (
    <section className="lei-secao">
      <h2 className="sub">{titulo}</h2>
      {children}
    </section>
  );
}

export function Privacidade() {
  return (
    <Documento titulo="Política de privacidade" linhas={['Privaci-', 'dade.']}>
      <Secao titulo="Em uma frase">
        <p className="body">
          Guardamos o mínimo para a sua consulta acontecer, usamos só para isso, não vendemos
          nada e você pode pedir cópia, correção ou exclusão quando quiser — de graça.
        </p>
      </Secao>

      <Secao titulo="Quem é responsável pelos seus dados">
        <p className="body body-ash">
          <b>Paciente de uma clínica:</b> a clínica é a <i>controladora</i> — é ela quem decide
          o que fazer com os seus dados e responde por eles. O DentiBot é o <i>operador</i>:
          guarda e processa os dados em nome da clínica, conforme as instruções dela (LGPD,
          art. 5º VI e VII, e art. 39).
        </p>
        <p className="body body-ash">
          <b>Pré-cadastro sem clínica vinculada, e dados da conta de quem administra uma
          clínica:</b> o controlador é o DentiBot — {EMPRESA.razaoSocial}, CNPJ {EMPRESA.cnpj},
          {' '}{EMPRESA.endereco}, {EMPRESA.email}.
        </p>
      </Secao>

      <Secao titulo="O que coletamos, para quê e com qual base legal">
        <div className="lei-rolagem">
          <table className="lei-tabela">
            <thead>
              <tr><th scope="col">Dado</th><th scope="col">Finalidade</th><th scope="col">Base legal (LGPD)</th></tr>
            </thead>
            <tbody>
              <tr>
                <td>Nome, e-mail, login social (Google, Microsoft, Apple)</td>
                <td>Criar e proteger a sua conta</td>
                <td>Execução de contrato — art. 7º V</td>
              </tr>
              <tr>
                <td>CPF, RG, data de nascimento, endereço, celular, profissão</td>
                <td>Identificar o paciente, montar o prontuário, confirmar consultas</td>
                <td>Execução de contrato e obrigação legal de manter prontuário — art. 7º II e V</td>
              </tr>
              <tr>
                <td>Triagem de saúde (medicamentos, alergias, gravidez, condições sistêmicas)</td>
                <td>Segurança do atendimento odontológico — escolha de anestésico, material e conduta</td>
                <td>
                  Dado sensível. No auto-cadastro: consentimento específico e destacado — art. 11 I.
                  Na clínica: tutela da saúde por profissional de saúde — art. 11 II f
                </td>
              </tr>
              <tr>
                <td>Nome do responsável legal (paciente menor)</td>
                <td>Autorizar o tratamento e o atendimento do menor</td>
                <td>Consentimento de um dos pais ou responsável — art. 14 §1º</td>
              </tr>
              <tr>
                <td>CNPJ ou CPF, razão social e nome da clínica</td>
                <td>Contratar o serviço e emitir cobrança</td>
                <td>Execução de contrato — art. 7º V</td>
              </tr>
              <tr>
                <td>Endereço IP, data e hora de acesso, navegador</td>
                <td>Segurança, prevenção a fraude e prova do consentimento</td>
                <td>Obrigação legal (Marco Civil, art. 15) e legítimo interesse — art. 7º II e IX</td>
              </tr>
              <tr>
                <td>Preferência de receber novidades</td>
                <td>Enviar campanhas e novidades, só se você marcar</td>
                <td>Consentimento — art. 7º I. Desmarque quando quiser</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p className="body body-ash">
          Lembrete de consulta marcada não é propaganda: faz parte do atendimento que você pediu.
          Você pode não fornecer um dado opcional ou negar um consentimento; a consequência é só
          a de não termos aquele dado — por exemplo, sem autorizar a triagem online, ela é feita
          na própria consulta (art. 18 VIII).
        </p>
      </Secao>

      <Secao titulo="Com quem compartilhamos">
        <p className="body body-ash">
          Com a clínica que você vincular, e só ela. Com fornecedores que operam a
          infraestrutura — autenticação (Clerk), hospedagem (Vercel e Render) e banco de dados
          (Neon) —, todos sob contrato de confidencialidade e apenas para manter o serviço no ar.
          Alguns ficam fora do Brasil; a transferência segue o art. 33 da LGPD, com cláusulas
          contratuais de proteção. Dado de saúde nunca é compartilhado para obter vantagem
          econômica (art. 11 §4º), e não vendemos dados a ninguém.
        </p>
      </Secao>

      <Secao titulo="Por quanto tempo guardamos">
        <ul className="lei-lista body body-ash">
          <li>Prontuário odontológico: no mínimo 20 anos após o último registro (Lei 13.787/2018, art. 6º). Esse prazo legal prevalece sobre um pedido de exclusão.</li>
          <li>Registros de acesso (IP, data e hora): 6 meses (Marco Civil da Internet, art. 15).</li>
          <li>Pré-cadastro nunca vinculado a uma clínica: até você pedir a exclusão ou encerrar a conta.</li>
          <li>Registro do consentimento: enquanto durar o tratamento e pelo prazo de prescrição, para provar o que foi aceito.</li>
        </ul>
      </Secao>

      <Secao titulo="Seus direitos (LGPD, art. 18)">
        <ul className="lei-lista body body-ash">
          <li>Confirmar se tratamos dados seus e acessar uma cópia deles.</li>
          <li>Corrigir dado incompleto, errado ou desatualizado.</li>
          <li>Pedir anonimização, bloqueio ou eliminação do que for desnecessário ou excessivo.</li>
          <li>Levar os seus dados para outro fornecedor (portabilidade).</li>
          <li>Eliminar o que foi tratado com base no seu consentimento, salvo guarda legal.</li>
          <li>Saber com quem compartilhamos.</li>
          <li>Saber que pode negar o consentimento, e o que acontece se negar.</li>
          <li>Revogar o consentimento, a qualquer momento, de forma gratuita e simples (art. 8º §5º).</li>
          <li>Reclamar à Autoridade Nacional de Proteção de Dados — ANPD (art. 18 §1º).</li>
        </ul>
        <p className="body body-ash">
          Peça pelo e-mail do encarregado abaixo, ou ao balcão da sua clínica. O pedido é gratuito
          e respondido em até 15 dias.
        </p>
      </Secao>

      <Secao titulo="Encarregado de dados (DPO)">
        <p className="body">
          {EMPRESA.encarregado} — <a href={`mailto:${EMPRESA.emailEncarregado}`}>{EMPRESA.emailEncarregado}</a>
        </p>
        <p className="body body-ash">
          Recebe reclamações e pedidos dos titulares e as comunicações da ANPD (art. 41).
        </p>
      </Secao>

      <Secao titulo="Crianças e adolescentes">
        <p className="body body-ash">
          A ficha de quem tem menos de 18 anos só é aceita com o nome do responsável legal e o
          consentimento dele, dado em destaque (art. 14). Pedimos apenas o necessário para o
          atendimento.
        </p>
      </Secao>

      <Secao titulo="Cookies">
        <p className="body body-ash">
          Usamos só cookies estritamente necessários: os de sessão do login e a preferência de
          tema claro ou escuro. Não há cookie de publicidade nem de rastreamento — por isso não
          pedimos aceite de cookies.
        </p>
      </Secao>

      <Secao titulo="Segurança">
        <p className="body body-ash">
          Conexão criptografada, dados separados por clínica no banco, acesso ao prontuário só
          para a equipe clínica e registro de quem abriu cada prontuário (art. 46). Se houver um
          incidente que possa causar risco ou dano relevante, avisamos você e a ANPD (art. 48).
        </p>
      </Secao>

      <Secao titulo="Mudanças nesta política">
        <p className="body body-ash">
          Mudança de finalidade, de forma de tratamento ou de compartilhamento é avisada com
          destaque antes de valer. Se ela depender de consentimento, você pode revogá-lo
          (art. 8º §6º). A versão em vigor está sempre no topo desta página.
        </p>
      </Secao>
    </Documento>
  );
}

export function TermosDeUso() {
  return (
    <Documento titulo="Termos de uso" linhas={['Termos', 'de uso.']}>
      <Secao titulo="O que é o DentiBot">
        <p className="body body-ash">
          Software de gestão para consultórios e clínicas odontológicas — agenda, prontuário,
          cobrança, estoque — e a área onde o paciente envia a própria ficha. O DentiBot não
          presta serviço de saúde: diagnóstico, tratamento e conduta são do cirurgião-dentista.
          Fornecedor: {EMPRESA.razaoSocial}, CNPJ {EMPRESA.cnpj}, {EMPRESA.endereco}.
        </p>
      </Secao>

      <Secao titulo="Sua conta">
        <ul className="lei-lista body body-ash">
          <li>Use dados verdadeiros e mantenha-os atualizados.</li>
          <li>A conta é pessoal. Não compartilhe a senha; cada pessoa da equipe tem a sua.</li>
          <li>Quem cadastra uma clínica declara ter poderes para representá-la e contratar em nome dela.</li>
          <li>Menor de 18 anos usa o DentiBot pelo responsável legal.</li>
        </ul>
      </Secao>

      <Secao titulo="Clínica: controladora dos dados dos pacientes">
        <p className="body body-ash">
          Ao cadastrar a clínica você concorda que ela é a controladora dos dados dos seus
          pacientes e o DentiBot, o operador (LGPD, art. 39). Isso quer dizer que a clínica:
        </p>
        <ul className="lei-lista body body-ash">
          <li>define a finalidade do tratamento e colhe os consentimentos que dependerem dela;</li>
          <li>responde aos pedidos dos pacientes — o DentiBot dá as ferramentas (exportar, corrigir, registrar);</li>
          <li>guarda o prontuário pelo prazo legal e mantém o sigilo profissional (Código de Ética Odontológica);</li>
          <li>dá acesso a cada pessoa da equipe só ao que o papel dela exige.</li>
        </ul>
        <p className="body body-ash">
          O DentiBot trata os dados apenas para prestar o serviço, nunca para fins próprios, e
          avisa a clínica de qualquer incidente de segurança.
        </p>
      </Secao>

      <Secao titulo="Teste, plano e cancelamento">
        <ul className="lei-lista body body-ash">
          <li>Trinta dias de teste, sem cartão. Ao fim, a clínica escolhe se contrata.</li>
          <li>Contratou pela internet? Pode desistir em até 7 dias da contratação, com devolução integral (Código de Defesa do Consumidor, art. 49).</li>
          <li>Cancele quando quiser. Antes de encerrar, você pode exportar os dados da clínica; o prontuário segue guardado pelo prazo legal, sob responsabilidade da clínica.</li>
          <li>Preço e reajuste ficam claros antes da contratação; nada é cobrado sem o seu aceite.</li>
        </ul>
      </Secao>

      <Secao titulo="O que não é permitido">
        <ul className="lei-lista body body-ash">
          <li>Cadastrar dados de terceiros sem base legal para isso.</li>
          <li>Tentar acessar dados de outra clínica, burlar a segurança ou sobrecarregar o sistema.</li>
          <li>Usar o DentiBot para enviar mensagens em massa que o paciente não pediu.</li>
        </ul>
        <p className="body body-ash">
          Descumprir estes termos pode suspender a conta, com aviso e direito de resposta,
          exceto em risco imediato à segurança dos dados.
        </p>
      </Secao>

      <Secao titulo="Disponibilidade e responsabilidade">
        <p className="body body-ash">
          Trabalhamos para o sistema estar no ar o tempo todo e avisamos as paradas programadas.
          Respondemos pelos danos que causarmos, nos termos do Código de Defesa do Consumidor e da
          LGPD (art. 42). Não respondemos pela conduta clínica, que é do profissional.
        </p>
      </Secao>

      <Secao titulo="Seus dados">
        <p className="body body-ash">
          O tratamento de dados pessoais segue a <Link to={PRIVACIDADE}>Política de privacidade</Link>,
          que faz parte destes termos.
        </p>
      </Secao>

      <Secao titulo="Mudanças, lei e foro">
        <p className="body body-ash">
          Mudanças relevantes são avisadas com antecedência, e a versão em vigor fica no topo da
          página. Vale a lei brasileira. Para consumidor, o foro é o do domicílio dele (CDC,
          art. 101 I).
        </p>
        <p className="body body-ash">
          Dúvidas: <a href={`mailto:${EMPRESA.email}`}>{EMPRESA.email}</a>.
          {' '}Já tem conta? <Link to={LOGIN}>Entrar</Link>.
        </p>
      </Secao>
    </Documento>
  );
}

/**
 * Uma caixa de aceite. Nunca vem marcada: consentimento pré-marcado é vício de
 * consentimento (art. 8º §3º), e a caixa marcada por padrão não prova vontade.
 */
export function Aceite({ marcado, aoMudar, obrigatorio = false, children }) {
  return (
    <label className="aceite">
      <input type="checkbox" required={obrigatorio} checked={marcado}
             onChange={(e) => aoMudar(e.target.checked)} />
      <span className="body">
        {children}
        {!obrigatorio && <span className="body-ash"> (opcional)</span>}
      </span>
    </label>
  );
}

/** Os links abrem em outra aba: sair do formulário no meio apagaria o que foi digitado. */
export const LinkTermos = () => <a href={TERMOS} target="_blank" rel="noopener">Termos de uso</a>;
export const LinkPrivacidade = () => <a href={PRIVACIDADE} target="_blank" rel="noopener">Política de privacidade</a>;
