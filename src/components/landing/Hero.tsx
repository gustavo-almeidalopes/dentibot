import { RoleSelector } from '@/components/auth/RoleSelector';
import { AnimatedSection } from './AnimatedSection';

export function Hero() {
  return (
    <section id="login" className="relative w-full py-20 md:py-32 lg:py-40">
      <div className="absolute inset-0 bg-gradient-to-t from-background via-background/80 to-transparent z-[-1]"></div>

      <div className="container grid md:grid-cols-2 gap-8 items-center">
        <AnimatedSection className="space-y-4">
          <h1 className="font-headline text-4xl md:text-5xl lg:text-6xl font-bold tracking-tighter">
            O Futuro da Gestão Odontológica Chegou.
          </h1>
          <p className="text-lg md:text-xl text-muted-foreground">
            DentiBot otimiza sua clínica com insights de IA, gestão de estoque e painéis personalizados por função.
          </p>
        </AnimatedSection>
        <AnimatedSection delay={200} className="flex justify-center">
          <RoleSelector />
        </AnimatedSection>
      </div>
    </section>
  );
}
