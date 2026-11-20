package br.com.dentibot.comunicacao;

import java.time.Instant;

/** Uma mensagem de WhatsApp, de ida ou de volta, com o estado da entrega. */
public record Mensagem(
        long idMensagem,
        Long idPaciente,
        String telefone,
        String sentido,
        String tipo,
        Long idConsulta,
        String texto,
        String status,
        Instant enviarEm,
        Instant enviadaEm,
        Instant lidaEm,
        String motivo,
        String classificacao,
        String escalada,
        Instant resolvidaEm,
        Instant criadaEm) {
}
