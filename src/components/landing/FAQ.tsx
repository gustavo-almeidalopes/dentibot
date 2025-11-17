import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from "@/components/ui/accordion"
import { AnimatedSection } from "./AnimatedSection";

const faqs = [
    {
        question: "O DentiBot é adequado para clínicas de todos os tamanhos?",
        answer: "Sim! O DentiBot foi projetado para ser escalável, atendendo desde consultórios individuais até grandes clínicas com múltiplas especialidades e profissionais."
    },
    {
        question: "Preciso instalar algum software no meu computador?",
        answer: "Não. O DentiBot é uma plataforma 100% baseada na nuvem. Você pode acessá-lo de qualquer dispositivo com um navegador de internet, seja um computador, tablet ou smartphone."
    },
    {
        question: "Meus dados estão seguros na plataforma?",
        answer: "Absolutamente. A segurança é nossa prioridade máxima. Utilizamos criptografia de ponta e seguimos as melhores práticas de segurança de dados, incluindo conformidade com a LGPD, para garantir que suas informações e as de seus pacientes estejam sempre protegidas."
    },
    {
        question: "Como funciona a integração com convênios?",
        answer: "Nosso sistema simula a comunicação com os principais convênios do mercado. Você pode registrar os dados do paciente, o código do procedimento (TUSS) e enviar para autorização, recebendo um status de 'Aprovado', 'Pendente' ou 'Glosado' diretamente no painel."
    },
];

export function FAQ() {
    return (
        <section id="faq" className="py-20 md:py-28">
            <div className="container max-w-3xl mx-auto">
                <AnimatedSection className="text-center mb-12">
                    <h2 className="font-headline text-3xl md:text-4xl font-bold">Perguntas Frequentes</h2>
                    <p className="text-lg text-muted-foreground mt-2">
                        Tire suas dúvidas sobre o DentiBot.
                    </p>
                </AnimatedSection>

                <AnimatedSection delay={200}>
                    <Accordion type="single" collapsible className="w-full">
                        {faqs.map((faq, index) => (
                            <AccordionItem value={`item-${index}`} key={index}>
                                <AccordionTrigger className="text-lg font-medium text-left">{faq.question}</AccordionTrigger>
                                <AccordionContent className="text-base text-muted-foreground">
                                    {faq.answer}
                                </AccordionContent>
                            </AccordionItem>
                        ))}
                    </Accordion>
                </AnimatedSection>
            </div>
        </section>
    );
}
