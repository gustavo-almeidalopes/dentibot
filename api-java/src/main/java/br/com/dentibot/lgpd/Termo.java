package br.com.dentibot.lgpd;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma versão publicada de um termo.
 *
 * <p>{@code textoIntegral} é imutável: a V16 tira UPDATE e DELETE da aplicação e
 * devolve só {@code UPDATE (ativo)}. Não é zelo — é o que faz um consentimento
 * significar alguma coisa. Se o texto pudesse ser editado depois de assinado,
 * "o paciente aceitou a versão 2.0" não provaria o conteúdo do que ele aceitou.
 */
public record Termo(
        long idTermo,
        String tipo,
        String versao,
        String textoIntegral,
        LocalDate ativoDesde,
        boolean ativo,
        Instant criadoEm) {
}
