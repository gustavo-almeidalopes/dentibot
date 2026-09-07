#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Regenera os cinco documentos estratégicos do DentiBot.

    python3 docs/generators/build_all.py

Requisitos: reportlab e a família de fontes Liberation (pacote fonts-liberation
na maioria das distribuições). Sem as fontes, o toolkit cai para Helvetica e o
documento continua sendo gerado, com aparência um pouco diferente.
"""

import os
import runpy
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))

GERADORES = [
    "gen_01_stack.py",
    "gen_02_uxui.py",
    "gen_03_funcionalidades_ia.py",
    "gen_04_ods.py",
    "gen_05_plano_negocios.py",
]


def main():
    sys.path.insert(0, AQUI)
    print("Gerando documentos do DentiBot...\n")
    for gerador in GERADORES:
        runpy.run_path(os.path.join(AQUI, gerador), run_name="__main__")
    print("\nConcluído. Os PDFs estão em docs/.")


if __name__ == "__main__":
    main()
