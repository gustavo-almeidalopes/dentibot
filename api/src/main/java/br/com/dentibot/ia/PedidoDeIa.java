package br.com.dentibot.ia;

import java.util.List;

/**
 * Uma chamada ao modelo, como o módulo que a pede a descreve.
 *
 * @param recurso        nome estável do recurso ({@code nota_clinica}); é por ele
 *                       que a clínica desliga a funcionalidade e vê o custo
 * @param instrucoes     o papel e o formato da resposta (prompt de sistema)
 * @param entrada        o texto do usuário — ainda com PII; o gateway redige
 * @param identificadores nomes que precisam sair antes do envio (o do paciente):
 *                       o ScrubberDePii pega CPF e telefone pela forma, mas nome
 *                       próprio no meio de um ditado só sai se alguém disser qual é
 */
public record PedidoDeIa(String recurso, String instrucoes, String entrada,
                         List<String> identificadores, int maxTokens) {
}
