import { RedirectToSignIn, Show, SignOutButton, UserButton, useUser } from '@clerk/react';
import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../api.js';
import FichaPaciente from '../components/FichaPaciente.jsx';
import { Lines, Reveal } from '../components/Reveal.jsx';
import { Aviso } from '../components/primitivos.jsx';
import { WHATSAPP_PACIENTE } from '../content.js';
import { useAcao } from '../dados.js';
import { AGENDA } from '../rotas.js';

const DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long' });

/**
 * Quem é paciente, pela conta do Clerk.
 *
 * <p>O back-end não tem identidade de paciente: o pré-cadastro grava a ficha e
 * não devolve nada, e o /eu responde 403 a quem não tem clínica. Sem uma marca
 * em algum lugar, todo login do paciente terminaria de novo no "CPF ou CNPJ" do
 * /cadastro.
 *
 * <p>`unsafeMetadata` porque é o campo que o próprio usuário pode escrever pelo
 * front — e é só roteamento. Quem forjar `perfil: 'paciente'` ganha esta página,
 * que não lê dado nenhum do servidor; toda autorização continua sendo do
 * back-end. Quando ele ganhar a identidade de paciente, esta marca sai.
 */
export const ehPaciente = (user) => user?.unsafeMetadata?.perfil === 'paciente';

export async function marcarPaciente(user) {
  try {
    await user?.update({
      unsafeMetadata: {
        ...user.unsafeMetadata,
        perfil: 'paciente',
        fichaEnviadaEm: new Date().toISOString(),
      },
    });
  } catch {
    /* A ficha já foi gravada; falhar aqui só custa, no próximo login, passar
       pelo /cadastro uma vez a mais. Não vale travar quem acabou de enviar. */
  }
}

/**
 * A área de quem é atendido — a outra ponta da tela de Pacientes, que é da
 * recepção.
 *
 * <p>Mostra só o que é verdade hoje: a conta, a ficha enviada e o que falta
 * para o resto aparecer. Consultas, orçamentos e parcelas dependem do vínculo
 * com a clínica, que o back-end ainda não faz; a página diz isso em vez de
 * desenhar caixas vazias.
 */
export default function AreaPaciente() {
  return (
    <>
      <a href="#main" className="skip-link">Ir para o conteúdo</a>
      <Show when="signed-out">
        <RedirectToSignIn />
      </Show>
      <Show when="signed-in">
        <Conteudo />
      </Show>
    </>
  );
}

function Conteudo() {
  const { user, isLoaded } = useUser();
  const [atualizando, setAtualizando] = useState(false);
  const { executar, enviando, erro, sucesso } = useAcao(async () => {
    await marcarPaciente(user);
    setAtualizando(false);
  });

  if (!isLoaded) return null;
  /* Dentista ou recepção que caiu aqui por um link: o Layout sabe para onde
     cada um vai, então a decisão fica com ele. */
  if (!ehPaciente(user)) return <Navigate to={AGENDA} replace />;

  const email = user.primaryEmailAddress?.emailAddress;
  const enviadaEm = user.unsafeMetadata?.fichaEnviadaEm;
  const etapas = [
    {
      estado: 'Feito',
      titulo: 'Conta',
      texto: `Você entra com ${email ?? 'o seu e-mail'}. A senha — ou o acesso pelo Google, `
        + 'Microsoft ou Apple — fica com o provedor de login; o DentiBot não a vê.',
    },
    {
      estado: 'Feito',
      titulo: 'Ficha de saúde',
      texto: 'Dados pessoais, endereço e a triagem que muda o atendimento: alergias, '
        + 'remédios de uso contínuo, gravidez, pressão e diabetes.',
      nota: enviadaEm ? `Última versão enviada em ${DATA.format(new Date(enviadaEm))}` : null,
    },
    {
      estado: 'Aguardando',
      titulo: 'Sua clínica',
      texto: 'Quando a clínica que te atende vincular a ficha ao seu CPF, o histórico chega '
        + 'lá pronto. Você não repete nada na recepção.',
    },
    {
      estado: 'Depois do vínculo',
      titulo: 'Consultas e pagamentos',
      texto: 'Próxima consulta, orçamento para aprovar e parcelas passam a aparecer nesta '
        + 'página quando a clínica estiver vinculada.',
    },
  ];

  return (
    <>
      <header className="auth-top area-topo edge">
        <a href="/" className="mbar-mark" aria-label="DentiBot — página inicial">DentiBot</a>
        <div className="acoes">
          <SignOutButton>
            <button type="button" className="btn btn-sm">Sair</button>
          </SignOutButton>
          <UserButton />
        </div>
      </header>

      <main id="main" className="area edge">
        <section className="area-abre">
          <Lines as="h1" className="display" lines={['Ficha', 'guardada.']} />
          <Reveal as="p" className="credit" delay="60ms">
            Área do paciente{user.fullName ? ` · ${user.fullName}` : ''}
          </Reveal>
          <Reveal as="p" className="body" delay="120ms">
            Seus dados e a sua triagem de saúde ficam guardados com o seu CPF. É a ficha
            que a clínica lê antes de você sentar na cadeira.
          </Reveal>
        </section>

        <section className="area-sec" aria-labelledby="onde">
          <Lines as="h2" id="onde" className="display" lines={['Onde você está.']} />
          <ol className="lista">
            {etapas.map((e) => (
              <Reveal as="li" key={e.titulo} className="etapa"
                      data-feita={e.estado === 'Feito' ? 'sim' : 'nao'}>
                <div>
                  <span className="selo" data-tom={e.estado === 'Feito' ? 'cheio' : 'contorno'}>
                    {e.estado}
                  </span>
                  <p className="row-title">{e.titulo}</p>
                </div>
                <div>
                  <p className="body">{e.texto}</p>
                  {e.nota && <p className="credit">{e.nota}</p>}
                </div>
              </Reveal>
            ))}
          </ol>
        </section>

        <section className="area-sec" aria-labelledby="mudou">
          <Lines as="h2" id="mudou" className="display" lines={['Mudou', 'alguma coisa?']} />
          <Reveal as="p" className="body">
            Começou um remédio, descobriu uma alergia, engravidou? Envie a ficha de novo —
            a versão mais recente é a que vale. O formulário vem em branco: a ficha enviada
            não volta para o navegador.
          </Reveal>

          <Aviso texto={sucesso} />

          {atualizando ? (
            <FichaPaciente
              inicial={{ nomeCompleto: user.fullName ?? '', email: email ?? '' }}
              enviando={enviando}
              erro={erro}
              rotuloEnviar="Enviar ficha atualizada"
              onEnviar={(corpo) => executar(api.post('/auth/pre-cadastro', corpo), 'Ficha atualizada.')}
            >
              <button type="button" className="btn" onClick={() => setAtualizando(false)}>
                Cancelar
              </button>
            </FichaPaciente>
          ) : (
            <div className="area-acoes">
              <button type="button" className="btn btn-lg btn-fill"
                      onClick={() => setAtualizando(true)}>
                Atualizar minha ficha
              </button>
            </div>
          )}
        </section>

        <section className="area-sec" aria-labelledby="dados">
          <Lines as="h2" id="dados" className="display" lines={['Seus dados.']} />
          <div className="area-direitos">
            <Reveal>
              <p className="sub">Quem lê</p>
              <p className="body body-ash">
                Triagem de saúde é dado sensível (LGPD, art. 11). Só a equipe clínica da
                clínica vinculada lê, e cada abertura do prontuário fica registrada com o
                nome de quem abriu.
              </p>
            </Reveal>
            <Reveal delay="80ms">
              <p className="sub">Corrigir</p>
              <p className="body body-ash">
                Envie a ficha de novo, aqui em cima. Não tem fila nem formulário à parte.
              </p>
            </Reveal>
            <Reveal delay="160ms">
              <p className="sub">Cópia ou exclusão</p>
              <p className="body body-ash">
                Peça pelo WhatsApp do DentiBot, com o e-mail desta conta.
              </p>
              <p className="area-acoes">
                <a href={WHATSAPP_PACIENTE} className="btn" target="_blank" rel="noopener">
                  Falar no WhatsApp
                </a>
              </p>
            </Reveal>
          </div>
        </section>

        <footer className="foot">
          <p className="credit">DentiBot · Área do paciente</p>
          <div className="foot-links">
            <a href="/politica-de-privacidade.pdf" className="credit">Privacidade</a>
            <a href="/" className="credit">Voltar ao site</a>
          </div>
        </footer>
      </main>
    </>
  );
}
