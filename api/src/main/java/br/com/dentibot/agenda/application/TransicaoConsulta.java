package br.com.dentibot.agenda.application;

import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import java.util.Arrays;
import java.util.List;

/**
 * As mudanças de status de uma consulta, num lugar só.
 *
 * <p>O {@code caminho} é a mesma palavra no endpoint
 * ({@code POST /consultas/{id}/{caminho}}) e na lista {@code acoes} de cada
 * {@code ConsultaResumo}: os clientes mostram o que vem ali em vez de manter a
 * própria cópia desta tabela.
 */
enum TransicaoConsulta {

    CONFIRMAR("confirmar", List.of("agendada"), "confirmada",
            TiposDeEvento.CONSULTA_CONFIRMADA, "Só é possível confirmar consulta agendada."),
    CANCELAR("cancelar", List.of("agendada", "confirmada"), "cancelada",
            TiposDeEvento.CONSULTA_CANCELADA,
            "Só é possível cancelar consulta agendada ou confirmada."),
    FALTA("falta", List.of("agendada", "confirmada"), "faltou",
            // Alimenta o score de falta do svc-python (Fase 5).
            TiposDeEvento.CONSULTA_FALTOU,
            "Só é possível registrar falta em consulta agendada ou confirmada."),
    CONCLUIR("concluir", List.of("agendada", "confirmada", "em_atendimento"), "realizada",
            TiposDeEvento.CONSULTA_REALIZADA, "Consulta não está em estado que permita concluir.");

    final String caminho;
    final List<String> origens;
    final String destino;
    final String evento;
    final String recusa;

    TransicaoConsulta(String caminho, List<String> origens, String destino, String evento,
                      String recusa) {
        this.caminho = caminho;
        this.origens = origens;
        this.destino = destino;
        this.evento = evento;
        this.recusa = recusa;
    }

    boolean exigeMotivo() {
        return this == CANCELAR;
    }

    static TransicaoConsulta doCaminho(String caminho) {
        return Arrays.stream(values())
                .filter(t -> t.caminho.equals(caminho))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("transição", caminho));
    }

    static List<String> disponiveis(String status) {
        return Arrays.stream(values())
                .filter(t -> t.origens.contains(status))
                .map(t -> t.caminho)
                .toList();
    }
}
