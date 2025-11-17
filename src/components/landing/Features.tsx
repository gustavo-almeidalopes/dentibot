import { Card, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { LayoutDashboard, Users, Boxes, BarChartBig, Bot, ShieldCheck } from "lucide-react";
import { AnimatedSection } from "./AnimatedSection";

const features = [
  {
    icon: <LayoutDashboard className="w-10 h-10 text-primary" />,
    title: "Painéis por Função",
    description: "Visões personalizadas para dentistas, recepcionistas, e gestores, mostrando apenas o que é importante para cada um.",
  },
  {
    icon: <Bot className="w-10 h-10 text-primary" />,
    title: "Gráficos com IA",
    description: "Utilize IA para gerar gráficos realistas para acompanhamento financeiro, métricas de pacientes e muito mais.",
  },
  {
    icon: <Boxes className="w-10 h-10 text-primary" />,
    title: "Controle de Estoque",
    description: "Acompanhe os níveis de estoque com alertas inteligentes, garantindo que você nunca fique sem suprimentos essenciais.",
  },
  {
    icon: <ShieldCheck className="w-10 h-10 text-primary" />,
    title: "Segurança e LGPD",
    description: "Gestão de permissões, logs de auditoria e ferramentas de conformidade para garantir a segurança dos dados.",
  },
];

export function Features() {
  return (
    <section id="features" className="py-20 md:py-28 bg-secondary/20">
      <div className="container">
        <AnimatedSection className="text-center mb-12">
          <h2 className="font-headline text-3xl md:text-4xl font-bold">Tudo o que Você Precisa em um Só Lugar</h2>
          <p className="text-lg text-muted-foreground mt-2 max-w-2xl mx-auto">
            Descubra os recursos poderosos que tornam o DentiBot a ferramenta definitiva para clínicas odontológicas modernas.
          </p>
        </AnimatedSection>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {features.map((feature, index) => (
            <AnimatedSection key={feature.title} delay={index * 150}>
              <Card className="h-full text-center hover:shadow-primary/20 hover:border-primary/50 transition-all duration-300 transform hover:-translate-y-2">
                <CardHeader>
                  <div className="flex justify-center mb-4">{feature.icon}</div>
                  <CardTitle className="font-headline text-xl">{feature.title}</CardTitle>
                  <CardDescription className="pt-2">{feature.description}</CardDescription>
                </CardHeader>
              </Card>
            </AnimatedSection>
          ))}
        </div>
      </div>
    </section>
  );
}
