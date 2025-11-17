'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { ToothIcon } from '@/components/ui/icons';
import { Menu } from 'lucide-react';
import { Sheet, SheetContent, SheetTrigger } from '../ui/sheet';
import anime from 'animejs';

const navLinks = [
    { href: "#features", label: "Funcionalidades" },
    { href: "#benefits", label: "Benefícios" },
    { href: "#testimonials", label: "Depoimentos" },
    { href: "#faq", label: "FAQ" },
];

export function Header() {
  const handleScroll = (e: React.MouseEvent<HTMLAnchorElement>, target: string) => {
    // Check if it's a link to the homepage sections
    if (target.startsWith("#") && window.location.pathname === '/') {
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
    } else if (target.startsWith("#")) {
        // If on another page, just navigate to the homepage + hash
        window.location.href = `/${target}`;
    }
    // For other links (like /sobre-nos), let the default Link behavior handle it
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur-sm">
      <div className="container flex h-16 items-center">
        <Link href="/" className="flex items-center gap-2 mr-auto">
          <ToothIcon className="h-6 w-6 text-primary" />
          <span className="font-headline text-xl font-bold">DentiBot</span>
        </Link>
        
        <nav className="hidden md:flex items-center gap-6">
            {navLinks.map(link => (
                <Link key={link.label} href={`/${link.href}`} onClick={(e) => handleScroll(e, link.href)} className="text-sm font-medium text-muted-foreground transition-colors hover:text-primary">
                    {link.label}
                </Link>
            ))}
        </nav>

        <div className="flex items-center gap-2 ml-4">
          <Button asChild>
            <Link href="/#login" onClick={(e) => handleScroll(e, '#login')}>Acessar</Link>
          </Button>
          <div className="md:hidden">
            <Sheet>
                <SheetTrigger asChild>
                    <Button variant="outline" size="icon">
                        <Menu className="h-5 w-5" />
                        <span className="sr-only">Abrir menu</span>
                    </Button>
                </SheetTrigger>
                <SheetContent side="right">
                    <nav className="flex flex-col gap-6 pt-10">
                        {navLinks.map(link => (
                             <Link key={link.label} href={`/${link.href}`} onClick={(e) => handleScroll(e, link.href)} className="text-lg font-medium text-foreground transition-colors hover:text-primary">
                                {link.label}
                            </Link>
                        ))}
                    </nav>
                </SheetContent>
            </Sheet>
          </div>
        </div>
      </div>
    </header>
  );
}
