import { Header } from '@/components/landing/Header';
import { Footer } from '@/components/landing/Footer';
import { AnimatedSection } from '@/components/landing/AnimatedSection';

export default function TermosPage() {
  return (
    <div className="flex flex-col min-h-screen bg-background">
      <Header />
      <main className="flex-grow py-20 md:py-28">
        <div className="container max-w-4xl mx-auto">
          <AnimatedSection>
            <h1 className="font-headline text-4xl md:text-5xl font-bold">Termos de Serviço</h1>
            <p className="text-sm text-muted-foreground mt-2">Última atualização: 31 de Julho de 2024</p>
          </AnimatedSection>

          <AnimatedSection delay={200}>
            <div className="prose prose-lg dark:prose-invert mt-12 space-y-6 text-muted-foreground">
              <p>Bem-vindo ao DentiBot! Estes Termos de Serviço ("Termos") governam o seu uso da nossa plataforma de gestão para clínicas odontológicas ("Serviço"), oferecida pela DentiBot Tecnologia LTDA. Ao acessar ou usar o nosso Serviço, você concorda em cumprir estes Termos.</p>
              
              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">1. Uso do Serviço</h2>
              <p>Você concorda em usar o Serviço apenas para fins legais e de acordo com estes Termos. Você é responsável por garantir que o uso do Serviço por sua clínica e seus usuários (dentistas, recepcionistas, etc.) esteja em conformidade com todas as leis e regulamentos aplicáveis, incluindo a Lei Geral de Proteção de Dados (LGPD).</p>
              
              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">2. Contas de Usuário</h2>
              <p>Para acessar o Serviço, você deve criar uma conta. Você é responsável por manter a confidencialidade de sua senha e por todas as atividades que ocorrem em sua conta. Você concorda em nos notificar imediatamente sobre qualquer uso não autorizado de sua conta.</p>
              
              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">3. Propriedade Intelectual</h2>
              <p>O Serviço e seu conteúdo original, recursos e funcionalidades são e permanecerão propriedade exclusiva da DentiBot Tecnologia LTDA e de seus licenciadores. O Serviço é protegido por direitos autorais, marcas registradas e outras leis do Brasil e de países estrangeiros.</p>
              
              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">4. Dados do Cliente</h2>
              <p>Você retém todos os direitos de propriedade sobre os dados que insere no Serviço ("Dados do Cliente"). Ao usar o Serviço, você nos concede uma licença limitada para usar, processar e transmitir os Dados do Cliente conforme necessário para fornecer e melhorar o Serviço. Garantimos que trataremos seus dados com a máxima confidencialidade e segurança.</p>

              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">5. Limitação de Responsabilidade</h2>
              <p>Em nenhuma circunstância a DentiBot, nem seus diretores, funcionários, parceiros ou agentes, serão responsáveis por quaisquer danos indiretos, incidentais, especiais, consequenciais ou punitivos, incluindo, sem limitação, perda de lucros, dados, uso ou outras perdas intangíveis, resultantes do seu acesso ou uso ou incapacidade de acessar ou usar o Serviço.</p>

              <h2 className="font-headline text-2xl font-bold text-foreground !mt-8">6. Alterações nos Termos</h2>
              <p>Reservamo-nos o direito, a nosso exclusivo critério, de modificar ou substituir estes Termos a qualquer momento. Se uma revisão for material, tentaremos fornecer um aviso com pelo menos 30 dias de antecedência antes que quaisquer novos termos entrem em vigor. O que constitui uma alteração material será determinado a nosso exclusivo critério.</p>
              
              <p>Se você tiver alguma dúvida sobre estes Termos, entre em contato conosco em <a href="mailto:legal@dentibot.com" className="text-primary hover:underline">legal@dentibot.com</a>.</p>
            </div>
          </AnimatedSection>
        </div>
      </main>
      <Footer />
    </div>
  );
}
