'use strict';

document.addEventListener('DOMContentLoaded', function () {

  var INK = getComputedStyle(document.documentElement)
              .getPropertyValue('--black').trim() || '#2C2C2C';

  /* ══════════════════════════════════════════════════════════════
     SMOOTH SCROLL — compensa altura do header fixo
  ══════════════════════════════════════════════════════════════ */
  document.querySelectorAll('a[href^="#"]:not([href="#"])').forEach(function (a) {
    a.addEventListener('click', function (e) {
      var id = a.getAttribute('href');
      var target = document.querySelector(id);
      if (!target) return;
      e.preventDefault();
      var header = document.querySelector('.site-header');
      var offset = header ? header.offsetHeight : 0;
      var top = target.getBoundingClientRect().top + window.scrollY - offset - 16;
      window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
    });
  });

  /* ══════════════════════════════════════════════════════════════
     MENU MOBILE
  ══════════════════════════════════════════════════════════════ */
  var menuBtn  = document.getElementById('mobile-menu-button');
  var mobileMenu = document.getElementById('mobile-menu');

  if (menuBtn && mobileMenu) {
    menuBtn.addEventListener('click', function () {
      var isOpen = !mobileMenu.classList.contains('hidden');
      mobileMenu.classList.toggle('hidden', isOpen);
      menuBtn.classList.toggle('open', !isOpen);
      menuBtn.setAttribute('aria-expanded', String(!isOpen));
    });

    mobileMenu.querySelectorAll('a').forEach(function (link) {
      link.addEventListener('click', function () {
        mobileMenu.classList.add('hidden');
        menuBtn.classList.remove('open');
        menuBtn.setAttribute('aria-expanded', 'false');
      });
    });

    /* Fecha menu ao clicar fora */
    document.addEventListener('click', function (e) {
      if (!menuBtn.contains(e.target) && !mobileMenu.contains(e.target)) {
        mobileMenu.classList.add('hidden');
        menuBtn.classList.remove('open');
        menuBtn.setAttribute('aria-expanded', 'false');
      }
    });
  }

  /* ══════════════════════════════════════════════════════════════
     HEADER — sombra neobrutalism ao rolar
  ══════════════════════════════════════════════════════════════ */
  var header = document.querySelector('.site-header');
  if (header) {
    var onScroll = function () {
      header.style.boxShadow = window.scrollY > 8
        ? '0 4px 0 0 ' + INK
        : '';
    };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  /* ══════════════════════════════════════════════════════════════
     SCROLLSPY — destaca o link do nav conforme a seção visível
  ══════════════════════════════════════════════════════════════ */
  var navLinks = Array.from(
    document.querySelectorAll('.nav-desktop .nav-link[href^="#"]')
  );

  var spySections = navLinks.reduce(function (acc, link) {
    var sec = document.querySelector(link.getAttribute('href'));
    if (sec) acc.push({ el: sec, link: link });
    return acc;
  }, []);

  if (spySections.length && 'IntersectionObserver' in window) {
    var spyObs = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        navLinks.forEach(function (l) { l.classList.remove('active'); });
        var matched = spySections.find(function (s) { return s.el === entry.target; });
        if (matched) matched.link.classList.add('active');
      });
    }, { rootMargin: '-40% 0px -55% 0px', threshold: 0 });

    spySections.forEach(function (s) { spyObs.observe(s.el); });
  }

  /* ══════════════════════════════════════════════════════════════
     SCROLL-REVEAL — anima elementos ao entrarem na viewport
  ══════════════════════════════════════════════════════════════ */
  if ('IntersectionObserver' in window) {
    var revealObs = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('visible');
        revealObs.unobserve(entry.target);
      });
    }, { threshold: 0.08 });

    document.querySelectorAll('[data-reveal]').forEach(function (el) {
      revealObs.observe(el);
    });
  } else {
    /* Fallback: exibe todos imediatamente (sem IntersectionObserver) */
    document.querySelectorAll('[data-reveal]').forEach(function (el) {
      el.classList.add('visible');
    });
  }

  /* ══════════════════════════════════════════════════════════════
     HERO STATS — animação de "pop" ao entrar na viewport
  ══════════════════════════════════════════════════════════════ */
  var statNums = document.querySelectorAll('.stat-num');
  if (statNums.length && 'IntersectionObserver' in window) {
    var statObs = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('stat-pop');
        entry.target.addEventListener('animationend', function () {
          entry.target.classList.remove('stat-pop');
        }, { once: true });
        statObs.unobserve(entry.target);
      });
    }, { threshold: 1.0 });

    statNums.forEach(function (el) { statObs.observe(el); });
  }

  /* ══════════════════════════════════════════════════════════════
     HERO MINI-CARDS — rotação de mensagens para simular atividade
  ══════════════════════════════════════════════════════════════ */
  var miniCards = document.querySelectorAll('.hero-card-mini');
  if (miniCards.length >= 2) {
    var pool = [
      [
        '<span>✅</span> 3 faltas evitadas hoje',
        '<span>📅</span> 12 consultas confirmadas',
        '<span>⏰</span> Lembrete enviado — 4 pacientes',
        '<span>🔔</span> Retorno agendado — D. Maria'
      ],
      [
        '<span>💰</span> Pix recebido — R$&nbsp;350',
        '<span>📊</span> Faturamento +R$&nbsp;2.100 este mês',
        '<span>✅</span> Novo paciente cadastrado',
        '<span>📦</span> Estoque: reposição solicitada'
      ]
    ];
    var idx = [0, 0];

    function rotateMini(cardIndex) {
      var card = miniCards[cardIndex];
      idx[cardIndex] = (idx[cardIndex] + 1) % pool[cardIndex].length;
      card.style.transition = 'opacity .25s ease, transform .25s ease';
      card.style.opacity    = '0';
      card.style.transform  = 'translateY(6px)';
      setTimeout(function () {
        card.innerHTML = pool[cardIndex][idx[cardIndex]];
        card.style.opacity   = '1';
        card.style.transform = 'translateY(0)';
      }, 270);
    }

    /* Offsets para não sincronizar os dois cards */
    setTimeout(function () {
      setInterval(function () { rotateMini(0); }, 3400);
    }, 1200);
    setTimeout(function () {
      setInterval(function () { rotateMini(1); }, 4000);
    }, 2800);
  }

  /* ══════════════════════════════════════════════════════════════
     PRICING HOVER — pulso na badge "Mais Popular" ao entrar na tela
  ══════════════════════════════════════════════════════════════ */
  var pricingHighlight = document.querySelector('.pricing-card.highlight');
  if (pricingHighlight && 'IntersectionObserver' in window) {
    var priceObs = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        var badge = entry.target.querySelector('.pricing-badge');
        if (badge) {
          badge.style.transition = 'transform .2s';
          badge.style.transform = 'scale(1.08)';
          setTimeout(function () { badge.style.transform = 'scale(1)'; }, 220);
        }
        priceObs.unobserve(entry.target);
      });
    }, { threshold: 0.5 });
    priceObs.observe(pricingHighlight);
  }

});
