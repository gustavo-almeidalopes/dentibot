package br.com.dentibot.comunicacao.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tudo o que a clínica diz ao paciente, num lugar só e em linguagem simples.
 *
 * <p>Uma linha por mensagem, sem quebra: fora da janela de 24 horas a Meta só
 * entrega modelo aprovado, e parâmetro de modelo não aceita quebra de linha.
 * Texto fixo e não gerado: o que sai para o paciente é o que está aqui, e a
 * clínica consegue ler antes.
 */
public final class Textos {

    private static final DateTimeFormatter DIA_E_HORA =
            DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", Locale.of("pt", "BR"));

    private static final String MENU_CONSULTA =
            "Responda 1 para confirmar, 2 para remarcar ou 3 para cancelar.";

    public static final String MENU = "Olá! Responda com o número: 1 confirmar sua próxima"
            + " consulta · 2 remarcar · 3 cancelar · 4 falar com a recepção. Em caso de dor forte,"
            + " sangramento ou inchaço, escreva URGENTE.";

    public static final String URGENCIA = "Recebemos sua mensagem e avisamos a equipe agora, com"
            + " prioridade. Se o sangramento não parar, se o inchaço dificultar respirar ou engolir,"
            + " ou se a dor for insuportável, procure um pronto-socorro ou ligue 192.";

    public static final String ESCALADA = "Recebemos. Uma pessoa da equipe vai responder por aqui"
            + " assim que possível.";

    public static final String REMARCAR = "Certo. A recepção vai mandar opções de horário por"
            + " aqui assim que possível.";

    public static final String SEM_CONSULTA = "Não encontramos consulta marcada para este número."
            + " Uma pessoa da equipe vai responder por aqui.";

    public static final String PARADO = "Pronto: você não vai mais receber mensagens automáticas"
            + " da clínica. Se mudar de ideia, responda VOLTAR.";

    public static final String VOLTOU = "Combinado: você volta a receber lembretes e avisos da"
            + " clínica. Para parar, responda PARAR.";

    public static final String OFERTA_PERDIDA = "Que pena, esse horário acabou de ser preenchido."
            + " Você continua na lista de espera.";

    public static final String OFERTA_RECUSADA = "Tudo bem. Você continua na lista de espera.";

    public static final String POS_BEM = "Que bom! Qualquer mudança, é só escrever aqui.";

    public static final String POS_LEVE = "Um desconforto leve nos primeiros dias é comum. Siga"
            + " as orientações que recebeu. Vamos perguntar de novo em dois dias; se piorar antes,"
            + " escreva aqui.";

    public static final String CIENTE = "Obrigado por confirmar a leitura.";

    private Textos() {
    }

    public static String quando(Instant instante, ZoneId fuso) {
        return DIA_E_HORA.format(instante.atZone(fuso));
    }

    public static String lembrete(String clinica, String quando) {
        return "Olá! Lembrete da %s: sua consulta é %s. %s".formatted(clinica, quando, MENU_CONSULTA);
    }

    public static String confirmacao(String clinica, String quando) {
        return "Olá! A %s tem uma consulta marcada para você %s. Pode confirmar? %s"
                .formatted(clinica, quando, MENU_CONSULTA);
    }

    public static String reforco(String clinica, String quando) {
        return ("Ainda não recebemos sua confirmação para %s na %s. %s Se não quiser mais receber"
                + " estas mensagens, responda PARAR.").formatted(quando, clinica, MENU_CONSULTA);
    }

    public static String oferta(String clinica, String quando) {
        return ("Abriu um horário na %s: %s. Quer ficar com ele? Responda SIM ou NÃO. Vale para"
                + " quem responder primeiro.").formatted(clinica, quando);
    }

    public static String posProcedimento(String clinica) {
        return ("Olá! Aqui é da %s. Como você está depois do atendimento? Responda 1 se está tudo"
                + " bem, 2 se sente um desconforto leve ou 3 se tem dor forte, sangramento ou"
                + " inchaço.").formatted(clinica);
    }

    public static String confirmado(String quando) {
        return "Consulta confirmada: " + quando + ". Até lá!";
    }

    public static String cancelado(String quando) {
        return "Consulta de " + quando + " cancelada. Se quiser remarcar, responda 2.";
    }

    public static String ofertaAceita(String quando) {
        return "Horário garantido: " + quando + ". Até lá!";
    }

    /** Orientações (IA-38): o que se diz depois de cada procedimento, verificável. */
    public record Orientacao(String titulo, String texto) {
    }

    public static final Map<String, Orientacao> ORIENTACOES = orientacoes();

    public static String orientacao(String clinica, Orientacao o) {
        return "%s — %s: %s Responda OK para confirmar que leu.".formatted(clinica, o.titulo(), o.texto());
    }

    private static Map<String, Orientacao> orientacoes() {
        Map<String, Orientacao> m = new LinkedHashMap<>();
        m.put("pos_extracao", new Orientacao("Cuidados depois da extração",
                "Morda a gaze por 30 minutos. Nas primeiras 24 horas, não bocheche, não cuspa com"
                + " força, não use canudo e não fume. Use compressa fria no rosto, 15 minutos sim e"
                + " 15 não, nas primeiras horas. Prefira alimentos frios e pastosos e mastigue do"
                + " outro lado. Escove os dentes com cuidado, sem encostar no local. Tome só os"
                + " remédios que o dentista receitou. Se o sangramento não parar, se o inchaço"
                + " aumentar depois de 3 dias ou se tiver febre, fale com a clínica."));
        m.put("pos_restauracao", new Orientacao("Cuidados depois da restauração",
                "Se tomou anestesia, espere passar para comer, para não morder a bochecha ou a"
                + " língua. Um pouco de sensibilidade ao frio nos primeiros dias é comum. Se a"
                + " mordida parecer alta ou a sensibilidade aumentar com o tempo, fale com a"
                + " clínica para um ajuste."));
        m.put("pos_canal", new Orientacao("Cuidados depois do tratamento de canal",
                "É normal o dente ficar dolorido ao morder por alguns dias. Evite mastigar"
                + " alimentos duros desse lado até a restauração definitiva. Tome só os remédios"
                + " que o dentista receitou. Se tiver inchaço, febre ou dor que piora, fale com a"
                + " clínica."));
        m.put("pos_clareamento", new Orientacao("Cuidados durante o clareamento",
                "Nas próximas 48 horas, evite café, chá preto, vinho, refrigerante escuro, açaí,"
                + " beterraba, molhos escuros e cigarro. Sensibilidade nos dentes é comum e passa;"
                + " se incomodar muito, fale com a clínica."));
        m.put("higiene", new Orientacao("Cuidados em casa",
                "Escove pelo menos duas vezes por dia, por dois minutos, com creme dental com"
                + " flúor. Use fio dental uma vez por dia, passando dos dois lados de cada dente."
                + " Troque a escova a cada três meses. Sangramento na gengiva ao escovar é sinal"
                + " para voltar à clínica."));
        return Collections.unmodifiableMap(m);
    }
}
