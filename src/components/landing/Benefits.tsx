import { AnimatedSection } from "./AnimatedSection";
import { CheckCircle2, Bot, Clock, BarChartHorizontal } from "lucide-react";

const benefits = [
    {
        icon: <Clock className="w-8 h-8 mb-4 text-primary" />,
        title: "Economia de Tempo",
        description: "Automatize agendamentos, confirmações e controle de estoque, liberando sua equipe para focar no que realmente importa: seus pacientes."
    },
    {
        icon: <Bot className="w-8 h-8 mb-4 text-primary" />,
        title: "Inteligência Artificial",
        description: "Utilize IA para gerar relatórios, prever demandas de estoque e obter insights valiosos para a gestão da sua clínica."
    },
    {
        icon: <BarChartHorizontal className="w-8 h-8 mb-4 text-primary" />,
        title: "Gestão Centralizada",
        description: "Tenha controle total sobre todos os setores da clínica — financeiro, estoque, pacientes e equipe — em uma única plataforma."
    },
];

export function Benefits() {
    return (
        <section id="benefits" className="py-20 md:py-28 bg-background">
            <div className="container">
                <AnimatedSection className="text-center mb-12">
                    <h2 className="font-headline text-3xl md:text-4xl font-bold">Benefícios que Vão Além do Sorriso</h2>
                    <p className="text-lg text-muted-foreground mt-2 max-w-2xl mx-auto">
                        Veja como o DentiBot pode impactar positivamente o dia a dia da sua clínica.
                    </p>
                </AnimatedSection>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {benefits.map((benefit, index) => (
                        <AnimatedSection key={benefit.title} delay={index * 150}>
                            <div className="p-8 border rounded-lg h-full bg-card">
                                {benefit.icon}
                                <h3 className="font-bold text-xl mb-2">{benefit.title}</h3>
                                <p className="text-muted-foreground">{benefit.description}</p>
                            </div>
                        </AnimatedSection>
                    ))}
                </div>
            </div>
        </section>
    );
}
