import { Header } from '@/components/landing/Header';
import { Footer } from '@/components/landing/Footer';
import { AnimatedSection } from '@/components/landing/AnimatedSection';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Mail, Phone, MapPin } from 'lucide-react';

export default function ContatoPage() {
  return (
    <div className="flex flex-col min-h-screen bg-background">
      <Header />
      <main className="flex-grow py-20 md:py-28">
        <div className="container">
          <AnimatedSection>
            <h1 className="font-headline text-4xl md:text-5xl font-bold text-center">Entre em Contato</h1>
            <p className="text-lg text-muted-foreground mt-4 max-w-2xl mx-auto text-center">
              Tem alguma dúvida ou sugestão? Nossa equipe está pronta para ajudar.
            </p>
          </AnimatedSection>

          <div className="grid md:grid-cols-2 gap-12 mt-16">
            <AnimatedSection delay={200}>
              <h2 className="font-headline text-2xl font-bold mb-6">Envie uma Mensagem</h2>
              <form className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="name">Seu Nome</Label>
                  <Input id="name" placeholder="Nome Completo" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">Seu E-mail</Label>
                  <Input id="email" type="email" placeholder="seu@email.com" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="message">Sua Mensagem</Label>
                  <Textarea id="message" placeholder="Como podemos ajudar?" rows={5} />
                </div>
                <Button type="submit" className="w-full">Enviar Mensagem</Button>
              </form>
            </AnimatedSection>

            <AnimatedSection delay={400}>
               <h2 className="font-headline text-2xl font-bold mb-6">Outros Canais</h2>
               <div className="space-y-6 text-muted-foreground">
                 <div className="flex items-start gap-4">
                    <Mail className="w-6 h-6 text-primary mt-1"/>
                    <div>
                        <h3 className="font-semibold text-foreground">E-mail</h3>
                        <p>Para dúvidas gerais ou suporte, envie um e-mail para:</p>
                        <a href="mailto:contato@dentibot.com" className="text-primary hover:underline">contato@dentibot.com</a>
                    </div>
                 </div>
                 <div className="flex items-start gap-4">
                    <Phone className="w-6 h-6 text-primary mt-1"/>
                    <div>
                        <h3 className="font-semibold text-foreground">Telefone</h3>
                        <p>Nosso time de vendas está disponível de segunda a sexta, das 9h às 18h.</p>
                        <p className="text-primary">(11) 4004-1234</p>
                    </div>
                 </div>
                 <div className="flex items-start gap-4">
                    <MapPin className="w-6 h-6 text-primary mt-1"/>
                    <div>
                        <h3 className="font-semibold text-foreground">Endereço</h3>
                        <p>Rua da Inovação, 123, Sala 45</p>
                        <p>Tecnopólis, São Paulo - SP</p>
                        <p>CEP: 01234-567</p>
                    </div>
                 </div>
               </div>
            </AnimatedSection>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
