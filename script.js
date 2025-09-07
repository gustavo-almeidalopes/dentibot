/* Requer: anime.js                                */

document.addEventListener('DOMContentLoaded', () => {

    /* 1) UTILIDADE: Retorna o elemento de scroll do documento */
    const getScrollElement = () => document.scrollingElement || document.documentElement;

    /* 2) SMOOTH SCROLL: Rolagem suave para links de âncora (Mantido) */
    const anchorLinks = document.querySelectorAll('a.nav-link, a[href^="#"]:not([href="#"])');
    anchorLinks.forEach(a => {
        a.addEventListener('click', (ev) => {
            ev.preventDefault();
            const href = a.getAttribute('href');
            const target = document.querySelector(href);
            if (!target) return;

            const header = document.querySelector('header');
            const headerH = header ? header.offsetHeight : 0;
            const targetTop = window.scrollY + target.getBoundingClientRect().top - headerH - 16;

            anime({
                targets: getScrollElement(),
                scrollTop: Math.max(0, targetTop),
                duration: 700,
                easing: 'easeInOutCubic'
            });
        });
    });
    
    /* 3) ANIMAÇÃO CTA: Pequena entrada animada para os botões (Mantido) */
    const ctas = document.querySelectorAll('.button-highlight, .button-primary');
    if (window.anime && ctas.length) {
        anime({
            targets: ctas,
            translateY: [10, 0],
            opacity: [0, 1],
            duration: 700,
            delay: anime.stagger(120, { start: 200 }),
            easing: 'easeOutCubic'
        });
    }

    /* 4) MENU MOBILE: Lógica para o menu hamburger (Mantido) */
    const mobileMenuButton = document.getElementById('mobile-menu-button');
    const mobileMenu = document.getElementById('mobile-menu');
    
    if (mobileMenuButton && mobileMenu) {
        const hamburgerIcon = mobileMenuButton.querySelector('svg.block');
        const closeIcon = mobileMenuButton.querySelector('svg.hidden');

        mobileMenuButton.addEventListener('click', () => {
            const isExpanded = mobileMenuButton.getAttribute('aria-expanded') === 'true';

            mobileMenu.classList.toggle('hidden');
            hamburgerIcon.classList.toggle('hidden');
            closeIcon.classList.toggle('hidden');
            mobileMenuButton.setAttribute('aria-expanded', !isExpanded);
        });

        const mobileMenuLinks = mobileMenu.querySelectorAll('a');
        mobileMenuLinks.forEach(link => {
            link.addEventListener('click', () => {
                if (mobileMenuButton.getAttribute('aria-expanded') === 'true') {
                    mobileMenuButton.click();
                }
            });
        });
    }

});