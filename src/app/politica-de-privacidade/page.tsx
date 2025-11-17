import { Header } from '@/components/landing/Header';
import { Footer } from '@/components/landing/Footer';
import { AnimatedSection } from '@/components/landing/AnimatedSection';

export default function PrivacidadePage() {
  return (
    <div className="flex flex-col min-h-screen bg-background">
      <Header />
      <main className="flex-grow py-20 md:py-28">
        <div className="container max-w-4xl mx-auto">
          <AnimatedSection>
            <h1 className="font-headline text-4xl md:text-5xl font-bold">Política de Privacidade</h1>
            <p className="text-sm text-muted-foreground mt-2">Última atualização: 31 de Julho de 2024</p>
          </AnimatedSection>

          <AnimatedSection delay={200}>
            <div className="prose prose-lg dark:prose-invert mt-12 space-y-6 text-muted-foreground">
              <p>A DentiBot Tecnologia LTDA ("nós", "nosso") opera a plataforma DentiBot (o "Serviço"). Esta página informa sobre nossas políticas relativas à coleta, uso e divulgação de dados pessoais quando você usa nosso Serviço e as escolhas que você tem associadas a esses dados.</p>
              
              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">1. Coleta e Uso de Informações</h2>
              <p>Coletamos vários tipos diferentes de informações para várias finalidades, para fornecer e melhorar nosso Serviço para você. Em conformidade com a Lei Geral de Proteção de Dados (LGPD), atuamos como Operador dos dados de pacientes inseridos por sua clínica, enquanto sua clínica atua como Controladora.</p>
              
              <h3 className="font-headline text-xl font-bold text-foreground">Tipos de Dados Coletados</h3>
              <ul>
                <li><strong>Dados Pessoais:</strong> Ao usar nosso Serviço, podemos solicitar que você nos forneça certas informações de identificação pessoal que podem ser usadas para contatar ou identificar você ("Dados Pessoais"). Isso inclui, mas não se limita a: nome, endereço de e-mail, telefone, e dados da clínica.</li>
                <li><strong>Dados do Cliente (Dados de Pacientes):</strong> Sua clínica é responsável por inserir e gerenciar os dados de seus pacientes no sistema. Nós apenas processamos esses dados conforme suas instruções e para a prestação do Serviço.</li>
                <li><strong>Dados de Uso:</strong> Podemos coletar informações sobre como o Serviço é acessado e usado.</li>
              </ul>
              
              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">2. Segurança dos Dados</h2>
              <p>A segurança dos seus dados é importante para nós. Utilizamos medidas de segurança administrativas, técnicas e físicas para proteger seus dados pessoais contra acesso não autorizado, uso ou divulgação. Todas as informações são criptografadas em trânsito e em repouso.</p>
              
              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">3. Compartilhamento de Dados</h2>
              <p>Não vendemos nem alugamos seus dados pessoais a terceiros. Podemos compartilhar seus dados com prestadores de serviços terceirizados que nos ajudam a operar nosso Serviço, como provedores de nuvem e serviços de análise, mas eles são contratualmente obrigados a proteger os dados e usá-los apenas para os fins para os quais os divulgamos.</p>

              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">4. Seus Direitos de Proteção de Dados (LGPD)</h2>
              <p>Como usuário, você tem o direito de acessar, corrigir ou excluir suas informações pessoais. Como Controlador dos dados de seus pacientes, sua clínica é responsável por atender às solicitações dos titulares de dados (seus pacientes). O DentiBot fornecerá as ferramentas necessárias para que você possa cumprir essas obrigações.</p>

              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">5. Alterações a Esta Política de Privacidade</h2>
              <p>Podemos atualizar nossa Política de Privacidade de tempos em tempos. Notificaremos você sobre quaisquer alterações, publicando a nova Política de Privacidade nesta página e atualizando a data de "última atualização".</p>
              
              <p>Se você tiver alguma dúvida sobre esta Política de Privacidade, entre em contato conosco em <a href="mailto:privacidade@dentibot.com" className="text-primary hover:underline">privacidade@dentibot.com</a>.</p>
            </div>
          </AnimatedSection>
        </div>
      </main>
      <Footer />
    </div>
  );
}
