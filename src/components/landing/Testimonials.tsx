import { Card, CardContent } from "@/components/ui/card";
import { AnimatedSection } from "./AnimatedSection";
import { Star } from "lucide-react";
import Image from "next/image";

const testimonials = [
  {
    name: "Dr. Ana Costa",
    role: "Dentista, Clínica Sorriso Digital",
    quote: "O DentiBot revolucionou a forma como gerenciamos nossa clínica. O tempo que economizamos com agendamentos e estoque é inacreditável. Recomendo a todos!",
  },
  {
    name: "Carlos Andrade",
    role: "Recepcionista Chefe, Odonto Prime",
    quote: "A interface é tão intuitiva que a equipe inteira aprendeu a usar em um dia. O painel da recepção é perfeito para o nosso fluxo de trabalho.",
  },
   {
    name: "Juliana Ferreira",
    role: "Coordenadora, Clínica DenteBelo",
    quote: "A capacidade de auditar e gerenciar usuários de forma centralizada nos deu a segurança e o controle que precisávamos. É uma ferramenta de gestão completa.",
  },
];

export function Testimonials() {
  return (
    <section id="testimonials" className="py-20 md:py-28 bg-secondary/20">
      <div className="container">
        <AnimatedSection className="text-center mb-12">
          <h2 className="font-headline text-3xl md:text-4xl font-bold">Amado por Profissionais da Odontologia</h2>
          <p className="text-lg text-muted-foreground mt-2 max-w-2xl mx-auto">
            Veja o que nossos clientes estão dizendo sobre a transformação que o DentiBot trouxe para suas clínicas.
          </p>
        </AnimatedSection>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {testimonials.map((testimonial, index) => (
            <AnimatedSection key={testimonial.name} delay={index * 150}>
              <Card className="h-full flex flex-col">
                <CardContent className="p-6 flex flex-col flex-grow">
                  <div className="flex mb-4">
                    {[...Array(5)].map((_, i) => (
                      <Star key={i} className="w-5 h-5 text-yellow-400 fill-yellow-400" />
                    ))}
                  </div>
                  <blockquote className="italic text-muted-foreground flex-grow">“{testimonial.quote}”</blockquote>
                  <div className="flex items-center mt-6">
                    <div>
                      <p className="font-semibold">{testimonial.name}</p>
                      <p className="text-sm text-muted-foreground">{testimonial.role}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </AnimatedSection>
          ))}
        </div>
      </div>
    </section>
  );
}
