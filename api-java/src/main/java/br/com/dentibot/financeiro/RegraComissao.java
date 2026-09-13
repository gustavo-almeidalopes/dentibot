package br.com.dentibot.financeiro;

import java.math.BigDecimal;

public record RegraComissao(
        long idRegra,
        long idDentista,
        Long idProcedimento,
        String baseCalculo,
        BigDecimal percentual,
        boolean ativo) {
}
