'use client';
import { AnimatedSection } from "./AnimatedSection";
import { Button } from "../ui/button";
import { MoveRight } from "lucide-react";
import anime from 'animejs';
import React from 'react';

export function CTA() {
    const handleScroll = (e: React.MouseEvent<HTMLAnchorElement>, target: string) => {
        e.preventDefault();
        const targetElement = document.querySelector(target);
        if (targetElement) {
            const targetOffset = targetElement.getBoundingClientRect().top + window.scrollY - 80; // 80px offset for header
            anime({
                targets: 'html, body',
                scrollTop: targetOffset,
                duration: 800,
                easing: 'easeInOutQuad'
            });
        }
    };

    return (
        <section className="py-20 md:py-28">
            <div className="container text-center">
                <AnimatedSection>
                    <h2 className="font-headline text-3xl md:text-4xl font-bold">Pronto para Transformar sua Clínica?</h2>
                    <p className="text-lg text-muted-foreground mt-3 max-w-2xl mx-auto">
                        Junte-se a centenas de clínicas que já estão otimizando sua gestão, economizando tempo e melhorando o atendimento com o DentiBot.
                    </p>
                    <div className="mt-8">
                        <Button asChild size="lg">
                            <a href="#login" onClick={(e) => handleScroll(e, '#login')}>
                                Comece Agora Gratuitamente
                                <MoveRight className="ml-2" />
                            </a>
                        </Button>
                        <p className="text-xs text-muted-foreground mt-3">Não é necessário cartão de crédito.</p>
                    </div>
                </AnimatedSection>
            </div>
        </section>
    );
}
