package br.com.dentibot.comunicacao.domain;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * O que o paciente quis dizer (IA-28, IA-31) — por regra, não por modelo.
 *
 * <p>A urgência vem primeiro e vence tudo: "1" depois de "estou com sangramento"
 * não é confirmação. E a regra erra para o lado seguro — um falso alarme custa
 * um minuto da recepção; uma urgência perdida custa o paciente. Por isso não há
 * IA aqui: um modelo que "entende melhor" também erra de jeitos que ninguém
 * consegue listar num teste.
 *
 * <p>{@code contexto} é o tipo da última mensagem que a clínica mandou: o mesmo
 * "1" confirma consulta depois de um lembrete e diz "estou bem" depois do
 * acompanhamento pós-procedimento.
 */
public final class Triagem {

    public enum Intencao {
        URGENCIA, RECLAMACAO, CLINICO, PARAR, VOLTAR,
        CONFIRMAR, REMARCAR, CANCELAR, HUMANO,
        ACEITAR, RECUSAR,
        POS_BEM, POS_LEVE, POS_PREOCUPANTE,
        CIENTE, MENU;

        /** Precisa de gente — o robô responde com segurança e passa adiante. */
        public boolean escala() {
            return this == URGENCIA || this == RECLAMACAO || this == CLINICO
                    || this == HUMANO || this == REMARCAR || this == POS_PREOCUPANTE;
        }

        public boolean urgente() {
            return this == URGENCIA || this == POS_PREOCUPANTE;
        }
    }

    private static final Pattern URGENCIA = Pattern.compile(
            "\\b(urgente|urgencia|emergencia|socorro|sangr\\w*|hemorrag\\w*|inchad\\w*|inchac\\w*"
            + "|incha|inchou|edema|trauma\\w*|febre|pus|abscesso|acidente|arrancou|quebr\\w*"
            + "|dente mole|nao consigo (abrir|engolir|respirar|comer|dormir)"
            + "|dor (muito )?(forte|intensa|insuportavel|demais)|muita dor)\\b");

    private static final Pattern RECLAMACAO = Pattern.compile(
            "\\b(reclam\\w*|insatisfeit\\w*|pessim\\w*|absurdo|procon|advogad\\w*|processar"
            + "|descaso|horrivel|cobranca indevida|cobraram errado|falta de respeito)\\b");

    private static final Pattern CLINICO = Pattern.compile(
            "\\b(dor|doendo|doi|dolorido|sensibilidade|sensivel|remedio|medicamento|antibiotico"
            + "|analgesico|pontos?|cirurgia|gengiva|ferida|afta|machucando|latejando)\\b");

    private static final Pattern PARAR = Pattern.compile(
            "^(parar|pare|sair|stop|descadastrar)$"
            + "|\\b(nao (quero|desejo) (mais )?receber|nao me mande|parem de mandar|descadastr\\w*)\\b");

    private static final Pattern SIM = Pattern.compile(
            "^(1|sim|s|confirmo|confirmado|confirmar|confirmada|ok|okay|vou|estarei|combinado)\\b");
    private static final Pattern REMARCAR = Pattern.compile(
            "^2\\b|\\b(remarc\\w*|reagend\\w*|outro horario|outro dia|mudar (o )?horario|trocar (o )?horario)\\b");
    private static final Pattern CANCELAR = Pattern.compile(
            "^3\\b|^cancel\\w*|\\b(nao vou|nao poderei|nao posso ir|nao vou conseguir|desmarc\\w*)\\b");
    private static final Pattern HUMANO = Pattern.compile(
            "^4\\b|\\b(atendente|humano|recepcao|recepcionista|falar com (alguem|uma pessoa|voces))\\b");

    private Triagem() {
    }

    public static Intencao interpretar(String contexto, String texto) {
        String t = normalizar(texto);
        if (t.isEmpty()) {
            return Intencao.MENU;
        }
        if (URGENCIA.matcher(t).find()) {
            return Intencao.URGENCIA;
        }
        if ("pos_procedimento".equals(contexto)) {
            if (t.startsWith("3")) {
                return Intencao.POS_PREOCUPANTE;
            }
            if (t.startsWith("2")) {
                return Intencao.POS_LEVE;
            }
            if (t.startsWith("1") || t.matches("^(bem|tudo bem|estou bem|to bem|otimo|ok)\\b.*")) {
                return Intencao.POS_BEM;
            }
        }
        if (RECLAMACAO.matcher(t).find()) {
            return Intencao.RECLAMACAO;
        }
        if (t.length() <= 60 && PARAR.matcher(t).find()) {
            return Intencao.PARAR;
        }
        if (t.equals("voltar")) {
            return Intencao.VOLTAR;
        }
        if (CLINICO.matcher(t).find()) {
            return Intencao.CLINICO;
        }
        if ("oferta_vaga".equals(contexto)) {
            if (t.matches("^(sim|s|quero|aceito|pode ser|pode|1)\\b.*")) {
                return Intencao.ACEITAR;
            }
            if (t.matches("^(nao|n|2)\\b.*")) {
                return Intencao.RECUSAR;
            }
        }
        if ("orientacao".equals(contexto) && t.matches("^(ok|li|lido|entendi|ciente|certo|sim)\\b.*")) {
            return Intencao.CIENTE;
        }
        if (HUMANO.matcher(t).find()) {
            return Intencao.HUMANO;
        }
        if (CANCELAR.matcher(t).find()) {
            return Intencao.CANCELAR;
        }
        if (REMARCAR.matcher(t).find()) {
            return Intencao.REMARCAR;
        }
        if (SIM.matcher(t).find()) {
            return Intencao.CONFIRMAR;
        }
        return Intencao.MENU;
    }

    /** Minúsculas, sem acento, espaço único: "Não VOU" e "nao vou" são a mesma coisa. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }
}
