package br.com.dentibot.plataforma.seguranca;

import static br.com.dentibot.plataforma.seguranca.Acao.ALTERAR;
import static br.com.dentibot.plataforma.seguranca.Acao.CRIAR;
import static br.com.dentibot.plataforma.seguranca.Acao.EXCLUIR;
import static br.com.dentibot.plataforma.seguranca.Acao.LER;
import static br.com.dentibot.plataforma.seguranca.Alcance.NENHUM;
import static br.com.dentibot.plataforma.seguranca.Alcance.PROPRIOS;
import static br.com.dentibot.plataforma.seguranca.Alcance.TODOS;
import static br.com.dentibot.plataforma.seguranca.Recurso.AGENDA;
import static br.com.dentibot.plataforma.seguranca.Recurso.AUDITORIA;
import static br.com.dentibot.plataforma.seguranca.Recurso.BILLING;
import static br.com.dentibot.plataforma.seguranca.Recurso.CONFIGURACAO;
import static br.com.dentibot.plataforma.seguranca.Recurso.EQUIPE;
import static br.com.dentibot.plataforma.seguranca.Recurso.ESTOQUE;
import static br.com.dentibot.plataforma.seguranca.Recurso.FINANCEIRO;
import static br.com.dentibot.plataforma.seguranca.Recurso.LGPD;
import static br.com.dentibot.plataforma.seguranca.Recurso.ORCAMENTO;
import static br.com.dentibot.plataforma.seguranca.Recurso.PACIENTE;
import static br.com.dentibot.plataforma.seguranca.Recurso.PRONTUARIO;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.contexto.StaffPapel;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * O único lugar do sistema que decide se alguém pode fazer alguma coisa
 * (invariante 3). Se aparecer uma segunda checagem de permissão fora daqui, é
 * bug — mesmo que ela esteja certa, porque duas fontes de verdade divergem com
 * o tempo e ninguém percebe qual está valendo.
 *
 * <p>Isto NÃO substitui o RLS. São camadas diferentes: o RLS responde "de qual
 * clínica é esta linha"; esta classe responde "este papel pode ver este tipo de
 * coisa". Nenhuma das duas cobre a outra.
 *
 * <p>Deny by default: recurso ausente da matriz é negado. Papel novo sem linha
 * na matriz é negado. A falta de uma entrada nunca significa permissão.
 */
@Component
public class AvaliadorDePermissao {

    /** Matriz do eixo A, transcrita da camada 5 da arquitetura. */
    private static final Map<Papel, Map<Recurso, Map<Acao, Alcance>>> MATRIZ = construirMatriz();

    /** Eixo B: o que cada papel de plataforma alcança. Nada clínico, em nenhum caso. */
    private static final Map<StaffPapel, Set<Recurso>> STAFF = Map.of(
            // Suporte N1 confirma que a clínica existe e qual é o plano. Só isso.
            StaffPapel.SUPORTE_N1, Set.of(CONFIGURACAO),
            StaffPapel.OPS_BILLING, Set.of(CONFIGURACAO, BILLING),
            // Engenharia idem; acesso a tenant exige JIT aprovado (Fase 4) e
            // passa a agir COMO usuário da clínica, registrado em auditoria.
            StaffPapel.ENGENHARIA, Set.of(CONFIGURACAO, BILLING));

    public Alcance alcance(Recurso recurso, Acao acao) {
        return alcance(ContextoAtual.obter(), recurso, acao);
    }

    public Alcance alcance(ContextoRequisicao ctx, Recurso recurso, Acao acao) {
        if (ctx.modoWorker()) {
            // Worker roda operação de sistema, não de usuário. Ele é limitado
            // pelo que consegue alcançar no banco (política modo_worker), não
            // por papel — não existe "papel" para um job.
            return TODOS;
        }
        if (ctx.staffPapel() != null) {
            boolean pode = STAFF.getOrDefault(ctx.staffPapel(), Set.of()).contains(recurso);
            // Staff lê; alterar cadastro de clínica é operação de billing.
            if (!pode) {
                return NENHUM;
            }
            return (acao == LER || recurso == BILLING) ? TODOS : NENHUM;
        }
        if (ctx.papel() == null) {
            return NENHUM;
        }
        return MATRIZ.getOrDefault(ctx.papel(), Map.of())
                .getOrDefault(recurso, Map.of())
                .getOrDefault(acao, NENHUM);
    }

    /** Lança 403 quando negado. Use no início do método de serviço. */
    public Alcance exigir(Recurso recurso, Acao acao) {
        Alcance alcance = alcance(recurso, acao);
        if (!alcance.permite()) {
            throw new AcessoNegadoException(recurso, acao);
        }
        return alcance;
    }

    public static class AcessoNegadoException extends RuntimeException {
        private final transient Recurso recurso;
        private final transient Acao acao;

        public AcessoNegadoException(Recurso recurso, Acao acao) {
            super("Acesso negado: %s em %s".formatted(acao, recurso));
            this.recurso = recurso;
            this.acao = acao;
        }

        public Recurso recurso() {
            return recurso;
        }

        public Acao acao() {
            return acao;
        }
    }

    // ─── a matriz ────────────────────────────────────────────────────────────

    private static Map<Papel, Map<Recurso, Map<Acao, Alcance>>> construirMatriz() {
        Map<Papel, Map<Recurso, Map<Acao, Alcance>>> m = new EnumMap<>(Papel.class);

        // Dono/Admin: tudo dentro da própria clínica.
        Map<Recurso, Map<Acao, Alcance>> admin = new EnumMap<>(Recurso.class);
        for (Recurso r : Recurso.values()) {
            admin.put(r, todasAcoes(TODOS));
        }
        // Nem o admin altera a trilha de auditoria — ela é append-only no banco,
        // e a matriz diz a mesma coisa para a resposta ser 403 e não 500.
        admin.put(AUDITORIA, somenteLeitura(TODOS));
        m.put(Papel.ADMIN, admin);

        // Dentista: a própria agenda, o prontuário dos seus pacientes, nada de dinheiro.
        Map<Recurso, Map<Acao, Alcance>> dentista = new EnumMap<>(Recurso.class);
        dentista.put(AGENDA, todasAcoes(PROPRIOS));
        dentista.put(PACIENTE, Map.of(LER, TODOS, CRIAR, TODOS, ALTERAR, TODOS, EXCLUIR, NENHUM));
        dentista.put(PRONTUARIO, Map.of(LER, PROPRIOS, CRIAR, PROPRIOS, ALTERAR, NENHUM, EXCLUIR, NENHUM));
        dentista.put(ORCAMENTO, Map.of(LER, PROPRIOS, CRIAR, PROPRIOS, ALTERAR, PROPRIOS, EXCLUIR, NENHUM));
        dentista.put(ESTOQUE, somenteLeitura(TODOS));
        dentista.put(CONFIGURACAO, somenteLeitura(TODOS));
        m.put(Papel.DENTISTA, dentista);

        // Recepcionista: agenda inteira, paciente para identificar e contatar,
        // prontuário NUNCA. A linha "só nome e horário" da camada 5 é resolvida
        // na projeção da consulta (PacienteResumo), não limpando campo depois.
        Map<Recurso, Map<Acao, Alcance>> recepcao = new EnumMap<>(Recurso.class);
        recepcao.put(AGENDA, todasAcoes(TODOS));
        recepcao.put(PACIENTE, Map.of(LER, TODOS, CRIAR, TODOS, ALTERAR, TODOS, EXCLUIR, NENHUM));
        recepcao.put(PRONTUARIO, todasAcoes(NENHUM));
        recepcao.put(ORCAMENTO, somenteLeitura(TODOS));
        // "Financeiro parcial": registra recebimento no balcão, não mexe em
        // comissão, despesa nem conciliação.
        recepcao.put(FINANCEIRO, Map.of(LER, TODOS, CRIAR, TODOS, ALTERAR, NENHUM, EXCLUIR, NENHUM));
        recepcao.put(ESTOQUE, somenteLeitura(TODOS));
        recepcao.put(CONFIGURACAO, somenteLeitura(TODOS));
        m.put(Papel.RECEPCIONISTA, recepcao);

        // Financeiro: dinheiro inteiro, zero prontuário.
        Map<Recurso, Map<Acao, Alcance>> financeiro = new EnumMap<>(Recurso.class);
        financeiro.put(AGENDA, somenteLeitura(TODOS));
        financeiro.put(PACIENTE, somenteLeitura(TODOS));
        financeiro.put(PRONTUARIO, todasAcoes(NENHUM));
        financeiro.put(ORCAMENTO, Map.of(LER, TODOS, CRIAR, NENHUM, ALTERAR, TODOS, EXCLUIR, NENHUM));
        financeiro.put(FINANCEIRO, todasAcoes(TODOS));
        financeiro.put(BILLING, somenteLeitura(TODOS));
        financeiro.put(ESTOQUE, somenteLeitura(TODOS));
        financeiro.put(CONFIGURACAO, somenteLeitura(TODOS));
        m.put(Papel.FINANCEIRO, financeiro);

        // Auxiliar/ASB: apoia o atendimento. Lê prontuário para preparar a sala;
        // não escreve evolução, que é ato do profissional (CFO-226).
        Map<Recurso, Map<Acao, Alcance>> auxiliar = new EnumMap<>(Recurso.class);
        auxiliar.put(AGENDA, somenteLeitura(TODOS));
        auxiliar.put(PACIENTE, somenteLeitura(TODOS));
        auxiliar.put(PRONTUARIO, somenteLeitura(TODOS));
        auxiliar.put(ESTOQUE, todasAcoes(TODOS));
        auxiliar.put(CONFIGURACAO, somenteLeitura(TODOS));
        m.put(Papel.AUXILIAR, auxiliar);

        return m;
    }

    private static Map<Acao, Alcance> todasAcoes(Alcance alcance) {
        return Map.of(LER, alcance, CRIAR, alcance, ALTERAR, alcance, EXCLUIR, alcance);
    }

    private static Map<Acao, Alcance> somenteLeitura(Alcance alcance) {
        return Map.of(LER, alcance, CRIAR, NENHUM, ALTERAR, NENHUM, EXCLUIR, NENHUM);
    }
}
