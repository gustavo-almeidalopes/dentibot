import { Header } from '@/components/landing/Header';
import { Footer } from '@/components/landing/Footer';
import { AnimatedSection } from '@/components/landing/AnimatedSection';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ArrowRight } from 'lucide-react';

const vagas = [
    {
        title: "Engenheiro(a) de Software Pleno (Front-end)",
        location: "Remoto (Brasil)",
        description: "Buscamos um profissional com experiência em React/Next.js e TypeScript para se juntar ao nosso time de produto e ajudar a construir o futuro da gestão odontológica."
    },
    {
        title: "Especialista em Sucesso do Cliente (Customer Success)",
        location: "São Paulo, SP (Híbrido)",
        description: "Você será a voz do DentiBot para nossos clientes, garantindo que eles extraiam o máximo valor da nossa plataforma através de onboarding, treinamento e suporte contínuo."
    },
    {
        title: "Product Manager (SaaS)",
        location: "Remoto (Brasil)",
        description: "Lidere o roadmap e a estratégia de produto, trabalhando próximo aos times de engenharia e design para entregar funcionalidades que encantem nossos usuários."
    }
];

export default function CarreirasPage() {
  return (
    <div className="flex flex-col min-h-screen bg-background">
      <Header />
      <main className="flex-grow py-20 md:py-28">
        <div className="container">
          <AnimatedSection>
            <h1 className="font-headline text-4xl md:text-5xl font-bold text-center">Faça Parte da Nossa Equipe</h1>
            <p className="text-lg text-muted-foreground mt-4 max-w-3xl mx-auto text-center">
              Estamos construindo o futuro da odontologia e procuramos pessoas talentosas e apaixonadas por tecnologia para se juntarem a nós.
            </p>
          </AnimatedSection>

          <AnimatedSection delay={200}>
            <div className="max-w-4xl mx-auto mt-16 space-y-6">
              <h2 className="font-headline text-3xl font-bold">Vagas em Aberto</h2>
              {vagas.map((vaga, index) => (
                <Card key={index}>
                    <CardHeader className="flex-row items-center justify-between">
                        <div>
                            <CardTitle>{vaga.title}</CardTitle>
                            <CardDescription>{vaga.location}</CardDescription>
                        </div>
                        <Button asChild variant="outline">
                            <a href="#">Ver Detalhes <ArrowRight className="ml-2"/></a>
                        </Button>
                    </CardHeader>
                    <CardContent>
                        <p className="text-muted-foreground">{vaga.description}</p>
                    </CardContent>
                </Card>
              ))}
            </div>
             <div className="text-center mt-12 p-8 bg-muted rounded-lg">
                <h3 className="font-bold text-xl">Não encontrou a vaga ideal?</h3>
                <p className="text-muted-foreground mt-2">Envie seu currículo para nosso banco de talentos. Estamos sempre em busca de pessoas incríveis!</p>
                <Button className="mt-4">
                    Enviar Currículo
                </Button>
            </div>
          </AnimatedSection>
        </div>
      </main>
      <Footer />
    </div>
  );
}
