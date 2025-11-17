'use client';
import Link from "next/link";
import { ToothIcon } from "../ui/icons";
import anime from 'animejs';
import React from 'react';

export function Footer() {
  const currentYear = new Date().getFullYear();
  
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
    <footer className="border-t bg-secondary/20">
      <div className="container py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div className="md:col-span-1">
                 <Link href="/" className="flex items-center gap-2 mb-4">
                    <ToothIcon className="h-7 w-7 text-primary" />
                    <span className="font-headline text-2xl font-bold">DentiBot</span>
                </Link>
                <p className="text-muted-foreground text-sm">O futuro da gestão odontológica, hoje.</p>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-3 md:col-span-3 gap-8">
                <div>
                    <h4 className="font-semibold mb-3">Produto</h4>
                    <ul className="space-y-2 text-sm text-muted-foreground">
                        <li><a href="#features" onClick={(e) => handleScroll(e, '#features')} className="hover:text-primary cursor-pointer">Funcionalidades</a></li>
                        <li><a href="#benefits" onClick={(e) => handleScroll(e, '#benefits')} className="hover:text-primary cursor-pointer">Benefícios</a></li>
                        <li><a href="#testimonials" onClick={(e) => handleScroll(e, '#testimonials')} className="hover:text-primary cursor-pointer">Depoimentos</a></li>
                        <li><a href="#faq" onClick={(e) => handleScroll(e, '#faq')} className="hover:text-primary cursor-pointer">FAQ</a></li>
                    </ul>
                </div>
                 <div>
                    <h4 className="font-semibold mb-3">Empresa</h4>
                    <ul className="space-y-2 text-sm text-muted-foreground">
                        <li><Link href="/sobre-nos" className="hover:text-primary">Sobre Nós</Link></li>
                        <li><Link href="/contato" className="hover:text-primary">Contato</Link></li>
                        <li><Link href="/carreiras" className="hover:text-primary">Carreiras</Link></li>
                    </ul>
                </div>
                 <div>
                    <h4 className="font-semibold mb-3">Legal</h4>
                    <ul className="space-y-2 text-sm text-muted-foreground">
                        <li><Link href="/termos-de-servico" className="hover:text-primary">Termos de Serviço</Link></li>
                        <li><Link href="/politica-de-privacidade" className="hover:text-primary">Política de Privacidade</Link></li>
                    </ul>
                </div>
            </div>
        </div>
        <div className="border-t mt-8 pt-6 text-center">
            <p className="text-sm text-muted-foreground">
            © {currentYear} DentiBot. Todos os direitos reservados.
            </p>
        </div>
      </div>
    </footer>
  );
}
