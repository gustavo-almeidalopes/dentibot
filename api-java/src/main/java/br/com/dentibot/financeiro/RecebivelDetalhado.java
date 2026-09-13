package br.com.dentibot.financeiro;

import java.util.List;

public record RecebivelDetalhado(RecebivelResumo recebivel, List<LancamentoResumo> extrato) {
}
