import { Header } from '@/components/landing/Header';
import { Footer } from '@/components/landing/Footer';
import { AnimatedSection } from '@/components/landing/AnimatedSection';
import Image from 'next/image';

export default function SobreNosPage() {
  return (
    <div className="flex flex-col min-h-screen bg-background">
      <Header />
      <main className="flex-grow pt-20 md:pt-28">
        <div className="container">
          <AnimatedSection>
            <h1 className="font-headline text-4xl md:text-5xl font-bold text-center">Sobre o DentiBot</h1>
            <p className="text-lg text-muted-foreground mt-4 max-w-3xl mx-auto text-center">
              Nossa missão é revolucionar a gestão de clínicas odontológicas através da tecnologia e inteligência artificial.
            </p>
          </AnimatedSection>
          
          <AnimatedSection delay={200}>
            <div className="relative aspect-video max-w-4xl mx-auto mt-12 rounded-xl overflow-hidden shadow-2xl">
              <Image 
                src="https://images.unsplash.com/photo-1588776814546-da62b5345169?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3NDE5ODJ8MHwxfHNlYXJjaHwxfHxkZW50YWwlMjB0ZWFtfGVufDB8fHx8MTc2NDA5MDU1Nnww&ixlib=rb-4.1.0&q=80&w=1080" 
                alt="Equipe da clínica odontológica" 
                fill
                className="object-cover"
                data-ai-hint="dental team"
              />
            </div>
          </AnimatedSection>

          <AnimatedSection delay={400}>
            <div className="max-w-3xl mx-auto mt-16 space-y-8 text-muted-foreground">
              <p>
                O DentiBot nasceu da observação de uma necessidade real no mercado: clínicas odontológicas, independentemente do tamanho, enfrentam desafios diários que vão muito além do atendimento ao paciente. Gestão de estoque, controle financeiro, agendamentos, comunicação e conformidade com regulamentações como a LGPD consomem um tempo valioso que poderia ser dedicado ao cuidado e à expansão do negócio.
              </p>
              <p>
                Nossa equipe é formada por especialistas em tecnologia, design de experiência do usuário e profissionais com vivência no setor odontológico. Essa combinação nos permitiu criar uma solução que não é apenas poderosa, mas também intuitiva e perfeitamente alinhada com o fluxo de trabalho de uma clínica moderna.
              </p>
              <h2 className="font-headline text-2xl md:text-3xl font-bold text-foreground !mt-12">Nossos Valores</h2>
              <ul className="space-y-4">
                <li className="flex items-start gap-3">
                  <strong className="text-primary mt-1">Inovação:</strong>
                  <span>Buscamos constantemente as melhores tecnologias para resolver problemas reais, utilizando a inteligência artificial como uma aliada estratégica para nossos clientes.</span>
                </li>
                 <li className="flex items-start gap-3">
                  <strong className="text-primary mt-1">Simplicidade:</strong>
                  <span>Acreditamos que softwares poderosos não precisam ser complicados. Nossa interface é projetada para ser clara, amigável e fácil de usar por toda a equipe da clínica.</span>
                </li>
                 <li className="flex items-start gap-3">
                  <strong className="text-primary mt-1">Segurança:</strong>
                  <span>A confiança dos nossos clientes é fundamental. Tratamos a segurança dos dados com a máxima seriedade, garantindo conformidade e tranquilidade.</span>
                </li>
              </ul>
            </div>
          </AnimatedSection>
        </div>
      </main>
      <Footer />
    </div>
  );
}
