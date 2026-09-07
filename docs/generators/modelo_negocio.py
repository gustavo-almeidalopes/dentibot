# -*- coding: utf-8 -*-
"""
Modelo financeiro do DentiBot
=============================

Centraliza TODAS as premissas do plano de negócios em um só lugar e deriva
delas o dimensionamento de mercado, a economia unitária e a projeção de 36
meses. O Documento 05 imprime apenas o que este módulo calcula — nenhum número
é digitado à mão no texto, de modo que a alteração de uma premissa se propaga
por todo o documento e a aritmética nunca diverge.

Rode `python3 modelo_negocio.py` para inspecionar o modelo no terminal.
"""

from dataclasses import dataclass, field


# ─── Formatação pt-BR ────────────────────────────────────────────────────────
def brl(valor, casas=0):
    """Formata em real brasileiro: 1234567.8 -> 'R$ 1.234.568'."""
    txt = f"{valor:,.{casas}f}".replace(",", "\x00").replace(".", ",").replace("\x00", ".")
    return f"R$ {txt}"


def num(valor, casas=0):
    """Formata número no padrão pt-BR, sem símbolo monetário."""
    return f"{valor:,.{casas}f}".replace(",", "\x00").replace(".", ",").replace("\x00", ".")


def pct(valor, casas=1):
    return f"{valor * 100:.{casas}f}".replace(".", ",") + "%"


# ─── Premissas ───────────────────────────────────────────────────────────────
@dataclass
class Premissas:
    """Toda premissa do plano, com a fonte que precisa validá-la.

    Os valores são estimativas de ordem de grandeza para planejamento. A coluna
    de validação do Anexo A do Documento 05 diz onde confirmar cada um antes de
    o número sair de um documento interno.
    """

    # Mercado (a validar: CFO, CNES/DataSUS, IBGE/CNAE 8630-5/04)
    estabelecimentos_odonto_brasil: int = 120_000
    fatia_pequeno_medio_porte: float = 0.45
    penetracao_alvo_5_anos: float = 0.02

    # Preços praticados hoje na landing page (index.html)
    preco_solo: int = 97
    preco_clinica: int = 197
    preco_enterprise: int = 497

    # Mix esperado da base em regime
    mix_solo: float = 0.60
    mix_clinica: float = 0.35
    mix_enterprise: float = 0.05

    # Economia unitária
    custo_variavel_mensal: float = 38.0   # infraestrutura + WhatsApp + suporte
    cac: float = 700.0                    # custo de aquisição misto
    churn_mensal: float = 0.035           # 3,5% ao mês
    desconto_anual: float = 0.167         # 2 meses grátis no plano anual

    # Estrutura de custo fixo por ano (R$/mês)
    custo_fixo_ano1: float = 25_000
    custo_fixo_ano2: float = 78_000
    custo_fixo_ano3: float = 155_000

    # Trajetória de clientes ao fim de cada mês-marco
    marcos_clientes: dict = field(default_factory=lambda: {
        4: 0, 6: 8, 9: 25, 12: 60, 18: 175, 24: 340, 30: 560, 36: 820,
    })

    # ── Derivados ────────────────────────────────────────────────────────────
    @property
    def arpu(self):
        """Receita média mensal por cliente, dado o mix de planos."""
        return (self.preco_solo * self.mix_solo
                + self.preco_clinica * self.mix_clinica
                + self.preco_enterprise * self.mix_enterprise)

    @property
    def margem_bruta(self):
        return (self.arpu - self.custo_variavel_mensal) / self.arpu

    @property
    def vida_media_meses(self):
        return 1 / self.churn_mensal

    @property
    def ltv(self):
        return self.arpu * self.margem_bruta * self.vida_media_meses

    @property
    def ltv_cac(self):
        return self.ltv / self.cac

    @property
    def payback_meses(self):
        return self.cac / (self.arpu * self.margem_bruta)

    @property
    def tam_estabelecimentos(self):
        return self.estabelecimentos_odonto_brasil

    @property
    def tam_anual(self):
        return self.tam_estabelecimentos * self.arpu * 12

    @property
    def sam_estabelecimentos(self):
        return int(self.tam_estabelecimentos * self.fatia_pequeno_medio_porte)

    @property
    def sam_anual(self):
        return self.sam_estabelecimentos * self.arpu * 12

    @property
    def som_estabelecimentos(self):
        return int(self.sam_estabelecimentos * self.penetracao_alvo_5_anos)

    @property
    def som_anual(self):
        return self.som_estabelecimentos * self.arpu * 12


# ─── Três cenários ──────────────────────────────────────────────────────────
# O plano não aposta em um número único. Cada cenário troca velocidade de
# crescimento por necessidade de capital, e o Documento 05 imprime os três lado
# a lado para que a decisão de captar (ou não) seja tomada com o custo à vista.

BOOTSTRAP = Premissas(
    # Equipe enxuta, crescimento por indicação e conteúdo, sem capital externo.
    custo_fixo_ano1=12_000, custo_fixo_ano2=20_000, custo_fixo_ano3=32_000,
    cac=350.0,
    marcos_clientes={4: 0, 6: 8, 9: 22, 12: 45, 18: 110, 24: 200, 30: 310, 36: 430},
)

BASE = Premissas(
    # Time pequeno mas remunerado, comercial estruturado, capital moderado.
    custo_fixo_ano1=18_000, custo_fixo_ano2=45_000, custo_fixo_ano3=78_000,
    cac=550.0,
    marcos_clientes={4: 0, 6: 8, 9: 25, 12: 60, 18: 175, 24: 340, 30: 560, 36: 820},
)

ACELERADO = Premissas(
    # Captação semente, time completo, aquisição paga desde o início.
    custo_fixo_ano1=35_000, custo_fixo_ano2=95_000, custo_fixo_ano3=190_000,
    cac=800.0,
    marcos_clientes={4: 0, 6: 15, 9: 55, 12: 130, 18: 380, 24: 750, 30: 1250, 36: 1850},
)

CENARIOS = {"Bootstrap": BOOTSTRAP, "Base": BASE, "Acelerado": ACELERADO}

# Cenário de referência do documento.
P = BASE


# ─── Projeção mês a mês ──────────────────────────────────────────────────────
def _clientes_por_mes(p: Premissas):
    """Interpola linearmente a trajetória de clientes entre os marcos."""
    marcos = sorted(p.marcos_clientes.items())
    serie, anterior_mes, anterior_qtd = [], 0, 0
    for mes_marco, qtd_marco in marcos:
        for mes in range(anterior_mes + 1, mes_marco + 1):
            fracao = (mes - anterior_mes) / (mes_marco - anterior_mes)
            serie.append(round(anterior_qtd + (qtd_marco - anterior_qtd) * fracao))
        anterior_mes, anterior_qtd = mes_marco, qtd_marco
    return serie


def projecao(p: Premissas = P):
    """Retorna 36 linhas com receita, custos e caixa acumulado."""
    clientes = _clientes_por_mes(p)
    linhas, caixa = [], 0.0
    for mes in range(1, 37):
        qtd = clientes[mes - 1]
        anterior = clientes[mes - 2] if mes > 1 else 0
        # Novos brutos = crescimento líquido + reposição do churn do mês.
        novos = max(0, qtd - anterior) + round(anterior * p.churn_mensal)
        mrr = qtd * p.arpu
        custo_var = qtd * p.custo_variavel_mensal
        custo_fixo = (p.custo_fixo_ano1 if mes <= 12
                      else p.custo_fixo_ano2 if mes <= 24 else p.custo_fixo_ano3)
        marketing = novos * p.cac
        resultado = mrr - custo_var - custo_fixo - marketing
        caixa += resultado
        linhas.append({
            "mes": mes, "clientes": qtd, "novos": novos, "mrr": mrr, "arr": mrr * 12,
            "custo_variavel": custo_var, "custo_fixo": custo_fixo, "marketing": marketing,
            "resultado": resultado, "caixa": caixa,
        })
    return linhas


def resumo_trimestral(p: Premissas = P):
    """Agrega a projeção em trimestres, para caber em uma tabela impressa."""
    linhas = projecao(p)
    saida = []
    for t in range(12):
        bloco = linhas[t * 3:(t + 1) * 3]
        fim = bloco[-1]
        saida.append({
            "trimestre": f"T{t % 4 + 1} A{t // 4 + 1}",
            "mes_final": fim["mes"],
            "clientes": fim["clientes"],
            "mrr": fim["mrr"],
            "receita_trimestre": sum(b["mrr"] for b in bloco),
            "custo_trimestre": sum(b["custo_variavel"] + b["custo_fixo"] + b["marketing"]
                                   for b in bloco),
            "resultado_trimestre": sum(b["resultado"] for b in bloco),
            "caixa": fim["caixa"],
        })
    return saida


def pior_caixa(p: Premissas = P):
    """Menor caixa acumulado e mês em que ocorre — dimensiona a necessidade de capital."""
    linhas = projecao(p)
    pior = min(linhas, key=lambda x: x["caixa"])
    return pior["caixa"], pior["mes"]


def mes_ponto_equilibrio(p: Premissas = P):
    """Primeiro mês com resultado mensal positivo (None se não ocorrer em 36 meses)."""
    for linha in projecao(p):
        if linha["resultado"] > 0:
            return linha["mes"]
    return None


def cenario(nome, **ajustes):
    """Constrói uma variação das premissas para análise de sensibilidade."""
    base = Premissas()
    for chave, valor in ajustes.items():
        setattr(base, chave, valor)
    return nome, base


def painel_cenarios():
    """Uma linha por cenário, com o que a diretoria precisa comparar."""
    saida = []
    for nome, prem in CENARIOS.items():
        caixa_min, mes_min = pior_caixa(prem)
        equilibrio = mes_ponto_equilibrio(prem)
        linhas = projecao(prem)
        saida.append({
            "cenario": nome,
            "clientes_36m": linhas[-1]["clientes"],
            "mrr_36m": linhas[-1]["mrr"],
            "arr_36m": linhas[-1]["arr"],
            "cac": prem.cac,
            "ltv_cac": prem.ltv_cac,
            "payback": prem.payback_meses,
            "capital_necessario": abs(min(0.0, caixa_min)),
            "mes_pior_caixa": mes_min,
            "equilibrio": equilibrio,
        })
    return saida


if __name__ == "__main__":
    print(f"ARPU mensal ............. {brl(P.arpu, 2)}")
    print(f"Margem bruta ............ {pct(P.margem_bruta)}")
    print(f"Vida média .............. {P.vida_media_meses:.1f} meses")
    print(f"LTV ..................... {brl(P.ltv)}")
    print(f"CAC ..................... {brl(P.cac)}")
    print(f"LTV/CAC ................. {P.ltv_cac:.1f}x")
    print(f"Payback ................. {P.payback_meses:.1f} meses")
    print(f"TAM ..................... {brl(P.tam_anual)}/ano ({num(P.tam_estabelecimentos)} estab.)")
    print(f"SAM ..................... {brl(P.sam_anual)}/ano ({num(P.sam_estabelecimentos)} estab.)")
    print(f"SOM 5 anos .............. {brl(P.som_anual)}/ano ({num(P.som_estabelecimentos)} estab.)")
    print()
    for c in painel_cenarios():
        eq = f"mês {c['equilibrio']}" if c["equilibrio"] else "após 36 meses"
        print(f"{c['cenario']:<11} clientes36m={c['clientes_36m']:>5}  "
              f"ARR={brl(c['arr_36m']):>14}  capital={brl(c['capital_necessario']):>14}  "
              f"equilíbrio={eq}")
    print()
    for t in resumo_trimestral(BASE):
        print(f"{t['trimestre']}  clientes={t['clientes']:>4}  MRR={brl(t['mrr']):>12}  "
              f"caixa={brl(t['caixa']):>14}")
