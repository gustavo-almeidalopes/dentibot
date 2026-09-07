# -*- coding: utf-8 -*-
"""
Toolkit de documentos DentiBot
==============================

Camada de apresentacao compartilhada pelos cinco documentos estrategicos do
projeto (Stack, UX/UI, Funcionalidades & IA, ODS da ONU e Plano de Negocios).

Padroniza:
  * paleta Pantone extraida de ``style.css`` (fonte unica da identidade visual);
  * capa, folha de rosto, sumario, cabecalho e rodape com numeracao "X de Y";
  * componentes de conteudo: secoes, tabelas de melhorias, blocos de destaque,
    tabelas genericas, KPIs e notas metodologicas.

Uso:
    from dentibot_doc import Documento, Melhoria
    doc = Documento(caminho="saida.pdf", numero="01", titulo="...", ...)
    doc.capa(); doc.sumario(); doc.secao("..."); doc.melhorias([...]); doc.build()
"""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from datetime import date

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT, TA_RIGHT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    KeepTogether,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)

# ---------------------------------------------------------------------------
# Paleta Pantone do DentiBot (espelha :root de style.css)
# ---------------------------------------------------------------------------
VERDE = colors.HexColor("#00DF76")   # Pantone 802 C
AMARELO = colors.HexColor("#FFC72C")  # Pantone 116 C
LARANJA = colors.HexColor("#FE5000")  # Pantone 021 C
VERMELHO = colors.HexColor("#DA291C")  # Pantone 485 C
PRETO = colors.HexColor("#2C2C2C")   # Pantone Black C
FUNDO = colors.HexColor("#F4F5F0")   # Pantone 11-0601 TPX
BRANCO = colors.HexColor("#FAFAF5")  # Pantone Bright White
CINZA = colors.HexColor("#97999B")   # Pantone Cool Gray 7 C
BORDA = colors.HexColor("#D0D0CC")   # Pantone Cool Gray 3 C

VERDE_CLARO = colors.HexColor("#E4FBF0")
AMARELO_CLARO = colors.HexColor("#FFF6DF")
LARANJA_CLARO = colors.HexColor("#FFEAE0")
CINZA_CLARO = colors.HexColor("#ECEDE8")
AZUL = colors.HexColor("#1F5F8B")
AZUL_CLARO = colors.HexColor("#E3EEF6")

VERSAO_DOC = "1.0"
DATA_DOC = date.today().strftime("%d/%m/%Y")
PROJETO = "DentiBot \u2014 Sistema de Gest\u00e3o Odontol\u00f3gica"

# ---------------------------------------------------------------------------
# Fontes: DejaVu cobre todo o repertorio latino acentuado + simbolos usados.
# ---------------------------------------------------------------------------
_FONTES_DIR = "/usr/share/fonts/truetype/liberation"


def _registrar_fontes() -> tuple[str, str, str, str]:
    """Registra a familia Liberation Sans/Mono (cobre todo o latim acentuado do
    portugues, travessoes, aspas curvas e setas). Volta as fontes base do PDF se
    o sistema nao tiver os arquivos TrueType."""
    try:
        faces = {
            "DB": "LiberationSans-Regular.ttf",
            "DB-B": "LiberationSans-Bold.ttf",
            "DB-I": "LiberationSans-Italic.ttf",
            "DB-BI": "LiberationSans-BoldItalic.ttf",
            "DB-M": "LiberationMono-Regular.ttf",
        }
        for nome, arquivo in faces.items():
            pdfmetrics.registerFont(TTFont(nome, f"{_FONTES_DIR}/{arquivo}"))
        pdfmetrics.registerFontFamily(
            "DB", normal="DB", bold="DB-B", italic="DB-I", boldItalic="DB-BI")
        return "DB", "DB-B", "DB-I", "DB-M"
    except Exception:  # pragma: no cover - fallback defensivo
        return "Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Courier"


FONTE, FONTE_B, FONTE_I, FONTE_M = _registrar_fontes()

MARGEM = 18 * mm
LARGURA_UTIL = A4[0] - 2 * MARGEM  # ~173 mm

# ---------------------------------------------------------------------------
# Estilos de paragrafo
# ---------------------------------------------------------------------------
E = {
    "corpo": ParagraphStyle(
        "corpo", fontName=FONTE, fontSize=9.3, leading=13.6, textColor=PRETO,
        alignment=TA_JUSTIFY, spaceAfter=6,
    ),
    "corpo_pequeno": ParagraphStyle(
        "corpo_pequeno", fontName=FONTE, fontSize=8.1, leading=11.4, textColor=PRETO,
        alignment=TA_JUSTIFY,
    ),
    "lead": ParagraphStyle(
        "lead", fontName=FONTE, fontSize=10.4, leading=15.6, textColor=PRETO,
        alignment=TA_JUSTIFY, spaceAfter=9,
    ),
    "h1": ParagraphStyle(
        "h1", fontName=FONTE_B, fontSize=17, leading=21, textColor=PRETO, spaceAfter=4,
    ),
    "h2": ParagraphStyle(
        "h2", fontName=FONTE_B, fontSize=12.4, leading=16, textColor=PRETO,
        spaceBefore=10, spaceAfter=5,
    ),
    "h3": ParagraphStyle(
        "h3", fontName=FONTE_B, fontSize=10, leading=13.5, textColor=PRETO,
        spaceBefore=7, spaceAfter=3,
    ),
    "tag": ParagraphStyle(
        "tag", fontName=FONTE_B, fontSize=7.6, leading=10, textColor=CINZA,
    ),
    "celula": ParagraphStyle(
        "celula", fontName=FONTE, fontSize=8.1, leading=11.2, textColor=PRETO,
    ),
    "celula_b": ParagraphStyle(
        "celula_b", fontName=FONTE_B, fontSize=8.1, leading=11.2, textColor=PRETO,
    ),
    "celula_c": ParagraphStyle(
        "celula_c", fontName=FONTE, fontSize=7.7, leading=10.4, textColor=PRETO,
        alignment=TA_CENTER,
    ),
    "celula_ch": ParagraphStyle(
        "celula_ch", fontName=FONTE_B, fontSize=7.7, leading=10.4, textColor=BRANCO,
        alignment=TA_CENTER,
    ),
    "celula_h": ParagraphStyle(
        "celula_h", fontName=FONTE_B, fontSize=7.9, leading=10.6, textColor=BRANCO,
    ),
    "kpi_valor": ParagraphStyle(
        "kpi_valor", fontName=FONTE_B, fontSize=16, leading=19, textColor=PRETO,
        alignment=TA_CENTER,
    ),
    "kpi_rotulo": ParagraphStyle(
        "kpi_rotulo", fontName=FONTE, fontSize=7.2, leading=9.4, textColor=CINZA,
        alignment=TA_CENTER,
    ),
    "id": ParagraphStyle(
        "id", fontName=FONTE_B, fontSize=8.4, leading=11, textColor=PRETO,
        alignment=TA_CENTER,
    ),
    "item_titulo": ParagraphStyle(
        "item_titulo", fontName=FONTE_B, fontSize=8.6, leading=11.6, textColor=PRETO,
        spaceAfter=1.5,
    ),
    "item_desc": ParagraphStyle(
        "item_desc", fontName=FONTE, fontSize=8.0, leading=10.9, textColor=PRETO,
        alignment=TA_LEFT,
    ),
    "lista": ParagraphStyle(
        "lista", fontName=FONTE, fontSize=9.1, leading=13.2, textColor=PRETO,
        leftIndent=11, bulletIndent=2, spaceAfter=3, alignment=TA_JUSTIFY,
    ),
    "nota": ParagraphStyle(
        "nota", fontName=FONTE, fontSize=8.2, leading=11.6, textColor=PRETO,
        alignment=TA_JUSTIFY,
    ),
    "capa_titulo": ParagraphStyle(
        "capa_titulo", fontName=FONTE_B, fontSize=30, leading=35, textColor=BRANCO,
    ),
    "capa_sub": ParagraphStyle(
        "capa_sub", fontName=FONTE, fontSize=12.6, leading=18, textColor=BRANCO,
    ),
    "capa_tag": ParagraphStyle(
        "capa_tag", fontName=FONTE_B, fontSize=9, leading=12, textColor=VERDE,
    ),
    "sumario": ParagraphStyle(
        "sumario", fontName=FONTE, fontSize=9.4, leading=13, textColor=PRETO,
    ),
    "sumario_b": ParagraphStyle(
        "sumario_b", fontName=FONTE_B, fontSize=9.4, leading=13, textColor=PRETO,
    ),
    "rodape": ParagraphStyle(
        "rodape", fontName=FONTE, fontSize=7.3, leading=9, textColor=CINZA,
    ),
}


# ---------------------------------------------------------------------------
# Flowables auxiliares
# ---------------------------------------------------------------------------
class Regua(Flowable):
    """Linha horizontal fina usada como separador de secao."""

    def __init__(self, largura=LARGURA_UTIL, espessura=0.6, cor=BORDA, espaco=3):
        super().__init__()
        self.largura, self.espessura, self.cor, self.espaco = largura, espessura, cor, espaco
        self.height = espessura + espaco

    def draw(self):
        self.canv.setStrokeColor(self.cor)
        self.canv.setLineWidth(self.espessura)
        self.canv.line(0, self.espaco, self.largura, self.espaco)


class EspacoAte(Flowable):
    """Espacador que consome exatamente o necessario para que o proximo flowable
    comece na altura ``y_alvo`` (medida a partir da base da pagina).

    Torna a capa robusta a titulos e subtitulos de tamanhos diferentes: o bloco
    de metadados sempre pousa abaixo da faixa escura, nunca sobre ela."""

    def __init__(self, y_alvo, y_base_frame=18 * mm):
        super().__init__()
        self.y_alvo, self.y_base_frame = y_alvo, y_base_frame
        self.height = 0

    def wrap(self, disponivel_w, disponivel_h):
        cursor = self.y_base_frame + disponivel_h
        self.height = max(0.0, cursor - self.y_alvo)
        return (0, self.height)

    def draw(self):
        pass


class BarraSecao(Flowable):
    """Cabecalho de secao: numero em bloco colorido + titulo + subtitulo."""

    def __init__(self, numero, titulo, subtitulo="", cor=VERDE, largura=LARGURA_UTIL):
        super().__init__()
        self.numero, self.titulo, self.subtitulo, self.cor = numero, titulo, subtitulo, cor
        self.largura = largura
        self.height = 30 if subtitulo else 24

    def draw(self):
        c, h = self.canv, self.height
        c.setFillColor(self.cor)
        c.rect(0, 0, 26, h, stroke=0, fill=1)
        c.setFillColor(PRETO if self.cor in (VERDE, AMARELO) else BRANCO)
        c.setFont(FONTE_B, 13)
        c.drawCentredString(13, h / 2 - 4.6, str(self.numero))
        c.setFillColor(PRETO)
        c.setFont(FONTE_B, 13.4)
        base = h - 14 if self.subtitulo else h / 2 - 4.6
        c.drawString(34, base, self.titulo)
        if self.subtitulo:
            c.setFillColor(CINZA)
            c.setFont(FONTE, 8.4)
            c.drawString(34, base - 12, self.subtitulo)
        c.setStrokeColor(BORDA)
        c.setLineWidth(0.6)
        c.line(0, -5, self.largura, -5)


def _p(txt, estilo="corpo"):
    return Paragraph(txt, E[estilo])


# ---------------------------------------------------------------------------
# Modelo de dados de uma melhoria
# ---------------------------------------------------------------------------
@dataclass
class Melhoria:
    """Uma linha acionavel do backlog apresentado no documento."""

    id: str
    titulo: str
    descricao: str
    alvo: str = ""          # arquivo/componente/ODS/area alvo
    impacto: str = "Medio"  # Alto | Medio | Baixo
    esforco: str = "M"      # P | M | G
    extra: str = ""         # coluna livre (indicador, KPI, fase...)


_TINTAS_IMPACTO = {
    "alto": (VERDE_CLARO, PRETO),
    "muito alto": (VERDE_CLARO, PRETO),
    "medio": (AMARELO_CLARO, PRETO),
    "médio": (AMARELO_CLARO, PRETO),
    "baixo": (CINZA_CLARO, PRETO),
}
_TINTAS_ESFORCO = {
    "p": (VERDE_CLARO, PRETO),
    "m": (AMARELO_CLARO, PRETO),
    "g": (LARANJA_CLARO, PRETO),
}


# ---------------------------------------------------------------------------
# Documento
# ---------------------------------------------------------------------------
class Documento:
    """Constroi um documento DentiBot completo com capa, sumario e conteudo."""

    def __init__(self, caminho, numero, titulo, subtitulo, resumo,
                 cor=VERDE, rotulo="Documento estrategico",
                 col_extra="Indicador / Resultado esperado"):
        self.caminho = caminho
        self.numero = numero
        self.titulo = titulo
        self.subtitulo = subtitulo
        self.resumo = resumo
        self.cor = cor
        self.rotulo = rotulo
        self.col_extra = col_extra
        self.story: list = []
        self._secoes: list[tuple[str, str]] = []
        self._n_secao = 0
        self._total_melhorias = 0

    # -- infraestrutura de pagina -------------------------------------------
    def _fundo_capa(self, canv, doc):
        canv.saveState()
        canv.setFillColor(PRETO)
        canv.rect(0, A4[1] - 172 * mm, A4[0], 172 * mm, stroke=0, fill=1)
        canv.setFillColor(FUNDO)
        canv.rect(0, 0, A4[0], A4[1] - 172 * mm, stroke=0, fill=1)
        # faixa cromatica da marca
        faixa_y = A4[1] - 172 * mm - 6
        for i, cor in enumerate((VERDE, AMARELO, LARANJA, VERMELHO)):
            canv.setFillColor(cor)
            canv.rect(i * A4[0] / 4, faixa_y, A4[0] / 4, 6, stroke=0, fill=1)
        # marca "D"
        canv.setFillColor(self.cor)
        canv.roundRect(MARGEM, A4[1] - 52 * mm, 17 * mm, 17 * mm, 4 * mm, stroke=0, fill=1)
        canv.setFillColor(PRETO if self.cor in (VERDE, AMARELO) else BRANCO)
        canv.setFont(FONTE_B, 26)
        canv.drawCentredString(MARGEM + 8.5 * mm, A4[1] - 46.5 * mm, "D")
        canv.setFillColor(BRANCO)
        canv.setFont(FONTE_B, 15)
        canv.drawString(MARGEM + 22 * mm, A4[1] - 44 * mm, "DentiBot")
        canv.setFillColor(CINZA)
        canv.setFont(FONTE, 8.6)
        canv.drawString(MARGEM + 22 * mm, A4[1] - 49 * mm, "Gest\u00e3o odontol\u00f3gica acess\u00edvel")
        # rodape da capa
        canv.setFillColor(CINZA)
        canv.setFont(FONTE, 7.8)
        canv.drawString(MARGEM, 14 * mm,
                        "Universidade da Cidade de S\u00e3o Paulo (UNICID) \u2014 Engenharia de Software"
                        " \u2014 Prof. Jadir Cust\u00f3dio Mendon\u00e7a Junior")
        canv.drawRightString(A4[0] - MARGEM, 14 * mm, f"v{VERSAO_DOC} \u2014 {DATA_DOC}")
        canv.restoreState()

    def _cabecalho_rodape(self, canv, doc):
        canv.saveState()
        # cabecalho
        canv.setFillColor(CINZA)
        canv.setFont(FONTE, 7.4)
        canv.drawString(MARGEM, A4[1] - 12 * mm, f"DentiBot \u2014 Documento {self.numero}")
        canv.drawRightString(A4[0] - MARGEM, A4[1] - 12 * mm, self.titulo[:74])
        canv.setStrokeColor(BORDA)
        canv.setLineWidth(0.5)
        canv.line(MARGEM, A4[1] - 14 * mm, A4[0] - MARGEM, A4[1] - 14 * mm)
        canv.setFillColor(self.cor)
        canv.rect(MARGEM, A4[1] - 14 * mm - 1.6, 24, 1.6, stroke=0, fill=1)
        # rodape
        canv.line(MARGEM, 14 * mm, A4[0] - MARGEM, 14 * mm)
        canv.setFillColor(CINZA)
        canv.setFont(FONTE, 7.2)
        canv.drawString(MARGEM, 10 * mm, PROJETO)
        canv.drawCentredString(A4[0] / 2, 10 * mm,
                               f"v{VERSAO_DOC} \u2014 {DATA_DOC}")
        canv.drawRightString(A4[0] - MARGEM, 10 * mm,
                             f"P\u00e1gina {doc.page - 1} de {getattr(doc, '_total_paginas', '?')}")
        canv.restoreState()

    # -- blocos de conteudo -------------------------------------------------
    def capa(self):
        self.story.append(Spacer(1, 62 * mm))
        self.story.append(_p(f"DOCUMENTO {self.numero}  |  {self.rotulo.upper()}", "capa_tag"))
        self.story.append(Spacer(1, 7))
        self.story.append(_p(self.titulo, "capa_titulo"))
        self.story.append(Spacer(1, 9))
        self.story.append(_p(self.subtitulo, "capa_sub"))
        # A faixa escura termina a 172 mm do topo; a ficha pousa 14 mm abaixo dela.
        self.story.append(EspacoAte(A4[1] - 186 * mm))
        ficha = [
            ["Projeto", PROJETO],
            ["Reposit\u00f3rio", "github.com/gustavo-almeidalopes/dentibot"],
            ["Branch de trabalho", "claude/stacks-improvements-business-plan-7gz5q8"],
            ["Respons\u00e1vel", "Gustavo Roberto de Almeida Lopes (l\u00edder de projeto)"],
            ["Equipe", "B. L. T. Chuma, M. Balsi Filho, S. S. Ferraz, G. R. A. Lopes"],
            ["Vers\u00e3o / Data", f"{VERSAO_DOC} \u2014 {DATA_DOC}"],
            ["Classifica\u00e7\u00e3o", "Uso interno \u2014 documento de planejamento"],
        ]
        t = Table([[_p(a, "celula_b"), _p(b, "celula")] for a, b in ficha],
                  colWidths=[38 * mm, LARGURA_UTIL - 38 * mm])
        t.setStyle(TableStyle([
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 3.4),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 3.4),
            ("LEFTPADDING", (0, 0), (-1, -1), 0),
            ("LINEBELOW", (0, 0), (-1, -2), 0.4, BORDA),
        ]))
        self.story.append(t)
        self.story.append(NextPageTemplate("conteudo"))
        self.story.append(PageBreak())

    def resumo_executivo(self, paragrafos, destaques=None):
        self.story.append(_p("Resumo executivo", "h1"))
        self.story.append(Regua(cor=self.cor, espessura=2))
        self.story.append(Spacer(1, 8))
        for i, par in enumerate(paragrafos):
            self.story.append(_p(par, "lead" if i == 0 else "corpo"))
        if destaques:
            self.story.append(Spacer(1, 4))
            self.kpis(destaques)

    def kpis(self, itens):
        """Faixa de indicadores no topo do resumo: lista de (valor, rotulo)."""
        n = len(itens)
        larg = LARGURA_UTIL / n
        linha_valor = [_p(v, "kpi_valor") for v, _ in itens]
        linha_rotulo = [_p(r, "kpi_rotulo") for _, r in itens]
        t = Table([linha_valor, linha_rotulo], colWidths=[larg] * n)
        t.setStyle(TableStyle([
            ("VALIGN", (0, 0), (-1, 0), "BOTTOM"),
            ("VALIGN", (0, 1), (-1, 1), "TOP"),
            ("BACKGROUND", (0, 0), (-1, -1), CINZA_CLARO),
            ("TOPPADDING", (0, 0), (-1, 0), 9),
            ("BOTTOMPADDING", (0, 0), (-1, 0), 2),
            ("TOPPADDING", (0, 1), (-1, 1), 0),
            ("BOTTOMPADDING", (0, 1), (-1, 1), 9),
            ("LEFTPADDING", (0, 0), (-1, -1), 3),
            ("RIGHTPADDING", (0, 0), (-1, -1), 3),
            ("LINEBEFORE", (1, 0), (-1, -1), 0.8, BRANCO),
            ("LINEABOVE", (0, 0), (-1, 0), 2.2, self.cor),
        ]))
        self.story.append(t)
        self.story.append(Spacer(1, 14))

    def sumario(self, extras=()):
        """Sumario simples (sem numero de pagina, evita segunda passagem)."""
        self.story.append(_p("Sum\u00e1rio", "h1"))
        self.story.append(Regua(cor=self.cor, espessura=2))
        self.story.append(Spacer(1, 8))
        self._sumario_idx = len(self.story)
        self._sumario_extras = list(extras)
        self.story.append(Spacer(1, 1))  # placeholder trocado no build()
        self.story.append(PageBreak())

    def _montar_sumario(self):
        linhas = []
        for num, titulo in self._secoes:
            linhas.append([_p(str(num), "celula_b"), _p(titulo, "sumario")])
        for titulo in self._sumario_extras:
            linhas.append([_p("", "celula"), _p(titulo, "sumario_b")])
        t = Table(linhas, colWidths=[13 * mm, LARGURA_UTIL - 13 * mm])
        t.setStyle(TableStyle([
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 4.2),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4.2),
            ("LEFTPADDING", (0, 0), (0, -1), 0),
            ("LINEBELOW", (0, 0), (-1, -1), 0.4, BORDA),
            ("TEXTCOLOR", (0, 0), (0, -1), self.cor if self.cor != AMARELO else LARANJA),
        ]))
        self.story[self._sumario_idx] = t

    def secao(self, titulo, subtitulo="", quebra=True):
        self._n_secao += 1
        rotulo = f"{self._n_secao:02d}"
        self._secoes.append((rotulo, titulo))
        if quebra and self._n_secao > 1:
            self.story.append(PageBreak())
        self.story.append(BarraSecao(rotulo, titulo, subtitulo, self.cor))
        self.story.append(Spacer(1, 12))
        return rotulo

    def texto(self, *paragrafos, estilo="corpo"):
        for par in paragrafos:
            self.story.append(_p(par, estilo))

    def subsecao(self, txt):
        """Titulo de nivel 2 dentro de uma secao."""
        self.story.append(_p(txt, "h2"))

    def subsub(self, txt):
        """Titulo de nivel 3."""
        self.story.append(_p(txt, "h3"))

    def lista(self, itens, marcador="•"):
        for it in itens:
            self.story.append(Paragraph(it, E["lista"], bulletText=marcador))
        self.story.append(Spacer(1, 4))

    def destaque(self, titulo, texto, cor=None):
        cor = cor or self.cor
        t = Table([[Paragraph(f"<b>{titulo}</b><br/><br/>{texto}", E["nota"])]],
                  colWidths=[LARGURA_UTIL])
        t.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), CINZA_CLARO),
            ("LINEBEFORE", (0, 0), (0, -1), 3, cor),
            ("LEFTPADDING", (0, 0), (-1, -1), 10),
            ("RIGHTPADDING", (0, 0), (-1, -1), 10),
            ("TOPPADDING", (0, 0), (-1, -1), 8),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
        ]))
        self.story.append(KeepTogether([t, Spacer(1, 9)]))

    def tabela(self, cabecalho, linhas, larguras=None, alinhar_centro=(), zebra=True):
        """Tabela generica com cabecalho escuro e zebra."""
        larguras = larguras or [LARGURA_UTIL / len(cabecalho)] * len(cabecalho)
        dados = [[_p(h, "celula_ch" if i in alinhar_centro else "celula_h")
                  for i, h in enumerate(cabecalho)]]
        for linha in linhas:
            dados.append([_p(str(c), "celula_c" if i in alinhar_centro else "celula")
                          for i, c in enumerate(linha)])
        t = Table(dados, colWidths=larguras, repeatRows=1)
        estilo = [
            ("BACKGROUND", (0, 0), (-1, 0), PRETO),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 4.6),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4.6),
            ("LEFTPADDING", (0, 0), (-1, -1), 5),
            ("RIGHTPADDING", (0, 0), (-1, -1), 5),
            ("LINEBELOW", (0, 0), (-1, -1), 0.35, BORDA),
            ("LINEABOVE", (0, 1), (-1, 1), 0, PRETO),
        ]
        if zebra:
            for i in range(1, len(dados)):
                if i % 2 == 0:
                    estilo.append(("BACKGROUND", (0, i), (-1, i), CINZA_CLARO))
        t.setStyle(TableStyle(estilo))
        self.story.append(t)
        self.story.append(Spacer(1, 10))

    def melhorias(self, itens, col_extra=None, larg_alvo=30 * mm, larg_extra=34 * mm):
        """Tabela do backlog: ID | Melhoria + descricao | Alvo | Impacto | Esforco | Extra."""
        col_extra = col_extra if col_extra is not None else self.col_extra
        usa_extra = any(i.extra for i in itens)
        self._total_melhorias += len(itens)

        larg_id = 12 * mm
        larg_imp = 14 * mm
        larg_esf = 11 * mm
        fixo = larg_id + larg_alvo + larg_imp + larg_esf + (larg_extra if usa_extra else 0)
        larg_desc = LARGURA_UTIL - fixo

        cab = [_p("ID", "celula_ch"), _p("Melhoria proposta", "celula_h"),
               _p("Onde se aplica", "celula_h"), _p("Impacto", "celula_ch"),
               _p("Esf.", "celula_ch")]
        larguras = [larg_id, larg_desc, larg_alvo, larg_imp, larg_esf]
        if usa_extra:
            cab.append(_p(col_extra, "celula_h"))
            larguras.append(larg_extra)

        dados = [cab]
        tintas = []
        for n, it in enumerate(itens, start=1):
            linha = [
                _p(it.id, "id"),
                [Paragraph(it.titulo, E["item_titulo"]), Paragraph(it.descricao, E["item_desc"])],
                _p(it.alvo, "celula"),
                _p(it.impacto, "celula_c"),
                _p(it.esforco, "celula_c"),
            ]
            if usa_extra:
                linha.append(_p(it.extra, "celula"))
            dados.append(linha)
            bg_i = _TINTAS_IMPACTO.get(it.impacto.strip().lower())
            bg_e = _TINTAS_ESFORCO.get(it.esforco.strip().lower())
            if bg_i:
                tintas.append(("BACKGROUND", (3, n), (3, n), bg_i[0]))
            if bg_e:
                tintas.append(("BACKGROUND", (4, n), (4, n), bg_e[0]))

        t = Table(dados, colWidths=larguras, repeatRows=1)
        estilo = [
            ("BACKGROUND", (0, 0), (-1, 0), PRETO),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 5),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ("LEFTPADDING", (0, 0), (-1, -1), 4.5),
            ("RIGHTPADDING", (0, 0), (-1, -1), 4.5),
            ("LINEBELOW", (0, 0), (-1, -1), 0.35, BORDA),
            ("TEXTCOLOR", (0, 1), (0, -1), CINZA),
        ] + tintas
        t.setStyle(TableStyle(estilo))
        self.story.append(t)
        self.story.append(Spacer(1, 10))

    def nota_metodologica(self, texto):
        self.story.append(Spacer(1, 2))
        t = Table([[Paragraph(texto, E["corpo_pequeno"])]], colWidths=[LARGURA_UTIL])
        t.setStyle(TableStyle([
            ("BOX", (0, 0), (-1, -1), 0.6, BORDA),
            ("LEFTPADDING", (0, 0), (-1, -1), 9),
            ("RIGHTPADDING", (0, 0), (-1, -1), 9),
            ("TOPPADDING", (0, 0), (-1, -1), 7),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ("BACKGROUND", (0, 0), (-1, -1), BRANCO),
        ]))
        self.story.append(t)
        self.story.append(Spacer(1, 8))

    def legenda_backlog(self):
        self.destaque(
            "Como ler as tabelas de melhorias",
            "<b>ID</b> identifica a melhoria de forma est\u00e1vel e pode ser referenciada em issues, "
            "commits e pull requests. <b>Onde se aplica</b> aponta o arquivo, servi\u00e7o ou \u00e1rea "
            "do reposit\u00f3rio afetada. <b>Impacto</b> estima o ganho percebido por usu\u00e1rio ou "
            "neg\u00f3cio (Alto / M\u00e9dio / Baixo). <b>Esf.</b> \u00e9 o esfor\u00e7o estimado de "
            "implementa\u00e7\u00e3o: <b>P</b> at\u00e9 3 dias-pessoa, <b>M</b> de 4 a 15 dias-pessoa, "
            "<b>G</b> acima de 15 dias-pessoa. As estimativas s\u00e3o de planejamento e devem ser "
            "refinadas em refinamento t\u00e9cnico antes de virarem compromisso de sprint.")

    # -- build ---------------------------------------------------------------
    def build(self):
        self._montar_sumario()
        doc = BaseDocTemplate(
            self.caminho, pagesize=A4,
            leftMargin=MARGEM, rightMargin=MARGEM,
            topMargin=20 * mm, bottomMargin=18 * mm,
            title=f"DentiBot {self.numero} \u2014 {self.titulo}",
            author="Equipe DentiBot \u2014 UNICID",
            subject=self.subtitulo,
            creator="DentiBot docs toolkit",
        )
        frame_capa = Frame(MARGEM, 18 * mm, LARGURA_UTIL, A4[1] - 36 * mm, id="capa",
                           leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
        frame_conteudo = Frame(MARGEM, 18 * mm, LARGURA_UTIL, A4[1] - 38 * mm, id="conteudo",
                               leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
        doc.addPageTemplates([
            PageTemplate(id="capa", frames=[frame_capa], onPage=self._fundo_capa),
            PageTemplate(id="conteudo", frames=[frame_conteudo], onPage=self._cabecalho_rodape),
        ])

        # 1a passagem: descobre o total de paginas; 2a passagem: imprime "X de Y".
        import copy
        ensaio = copy.deepcopy(self.story)
        doc._total_paginas = "?"
        doc.build(ensaio)
        total = doc.page - 1
        doc2 = BaseDocTemplate(
            self.caminho, pagesize=A4,
            leftMargin=MARGEM, rightMargin=MARGEM,
            topMargin=20 * mm, bottomMargin=18 * mm,
            title=f"DentiBot {self.numero} \u2014 {self.titulo}",
            author="Equipe DentiBot \u2014 UNICID",
            subject=self.subtitulo,
            creator="DentiBot docs toolkit",
        )
        doc2.addPageTemplates([
            PageTemplate(id="capa", frames=[frame_capa], onPage=self._fundo_capa),
            PageTemplate(id="conteudo", frames=[frame_conteudo], onPage=self._cabecalho_rodape),
        ])
        doc2._total_paginas = total
        doc2.build(self.story)
        tamanho = os.path.getsize(self.caminho) / 1024
        print(f"  OK  {os.path.basename(self.caminho)} \u2014 {total + 1} p\u00e1ginas, "
              f"{self._total_melhorias} melhorias, {tamanho:.0f} KB")
        return self.caminho
