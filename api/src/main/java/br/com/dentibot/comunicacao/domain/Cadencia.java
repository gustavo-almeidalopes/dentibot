package br.com.dentibot.comunicacao.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Confirmação proporcional ao risco (IA-16), com limite de insistência (IA-32).
 *
 * <p>O risco é o mesmo "faltou X de Y" que a recepção vê na agenda (IA-15) —
 * contagem conferível, não probabilidade. Quem nunca falta recebe um lembrete;
 * quem falta recebe a confirmação mais cedo e um reforço SÓ se não confirmou,
 * que o despachante confere na hora de enviar. Nada sai de madrugada.
 */
public final class Cadencia {

    public enum Risco { BAIXO, MEDIO, ALTO }

    public record Envio(String tipo, Instant quando) {
    }

    /** Horário útil no fuso da clínica: das 8h às 20h. */
    static final int ABRE = 8;
    static final int FECHA = 20;

    /** Dois avisos a menos de quatro horas um do outro viram cobrança. */
    private static final long INTERVALO_MINIMO_HORAS = 4;

    private Cadencia() {
    }

    public static Risco risco(int faltas, int consultas) {
        if (faltas >= 2 || (consultas >= 3 && faltas * 3 >= consultas)) {
            return Risco.ALTO;
        }
        return faltas == 1 ? Risco.MEDIO : Risco.BAIXO;
    }

    public static List<Envio> plano(Risco risco, Instant inicio, Instant agora, ZoneId fuso) {
        List<Envio> passos = switch (risco) {
            case BAIXO -> List.of(new Envio("lembrete", antes(inicio, 24)));
            case MEDIO -> List.of(new Envio("confirmacao", antes(inicio, 48)),
                    new Envio("reforco", antes(inicio, 24)));
            case ALTO -> List.of(new Envio("confirmacao", antes(inicio, 72)),
                    new Envio("reforco", antes(inicio, 24)),
                    new Envio("reforco", antes(inicio, 3)));
        };

        List<Envio> validos = new ArrayList<>();
        for (Envio e : passos) {
            Instant quando = recuarParaHorarioUtil(e.quando(), fuso);
            if (quando.isAfter(agora) && quando.isBefore(inicio)) {
                validos.add(new Envio(e.tipo(), quando));
            }
        }
        // Marcou em cima da hora: o primeiro aviso já passou. Confirma agora
        // (ou às 8h), se ainda der tempo de o paciente responder.
        if (validos.stream().allMatch(e -> e.tipo().equals("reforco"))) {
            Instant ja = avancarParaHorarioUtil(agora, fuso);
            if (ja.isBefore(inicio.minus(2, ChronoUnit.HOURS))) {
                validos.add(new Envio("confirmacao", ja));
            }
        }

        validos.sort(Comparator.comparing(Envio::quando));
        List<Envio> plano = new ArrayList<>();
        for (Envio e : validos) {
            if (plano.isEmpty() || e.quando().isAfter(
                    plano.getLast().quando().plus(INTERVALO_MINIMO_HORAS, ChronoUnit.HOURS))) {
                plano.add(e);
            }
        }
        return plano;
    }

    /** Pós-procedimento (IA-37): no dia seguinte, às 10h da clínica. */
    public static Instant diaSeguinte(Instant realizada, ZoneId fuso, int dias) {
        return realizada.atZone(fuso).plusDays(dias).withHour(10).truncatedTo(ChronoUnit.HOURS)
                .toInstant();
    }

    /** Aviso programado que cai de madrugada vai para as 19h da véspera. */
    static Instant recuarParaHorarioUtil(Instant t, ZoneId fuso) {
        ZonedDateTime z = t.atZone(fuso);
        if (z.getHour() < ABRE) {
            return z.minusDays(1).withHour(FECHA - 1).truncatedTo(ChronoUnit.HOURS).toInstant();
        }
        if (z.getHour() >= FECHA) {
            return z.withHour(FECHA - 1).truncatedTo(ChronoUnit.HOURS).toInstant();
        }
        return t;
    }

    /** Aviso para "agora" que cai de madrugada espera as 8h. */
    public static Instant avancarParaHorarioUtil(Instant t, ZoneId fuso) {
        ZonedDateTime z = t.atZone(fuso);
        if (z.getHour() < ABRE) {
            return z.withHour(ABRE).truncatedTo(ChronoUnit.HOURS).toInstant();
        }
        if (z.getHour() >= FECHA) {
            return z.plusDays(1).withHour(ABRE).truncatedTo(ChronoUnit.HOURS).toInstant();
        }
        return t;
    }

    private static Instant antes(Instant inicio, long horas) {
        return inicio.minus(horas, ChronoUnit.HOURS);
    }
}
