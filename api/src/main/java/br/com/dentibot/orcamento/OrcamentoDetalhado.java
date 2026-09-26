package br.com.dentibot.orcamento;

import java.util.List;

public record OrcamentoDetalhado(OrcamentoResumo cabecalho, List<ItemResumo> itens,
                                 String observacoes) {
}
