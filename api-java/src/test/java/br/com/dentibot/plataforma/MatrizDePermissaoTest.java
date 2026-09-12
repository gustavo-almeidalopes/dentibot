package br.com.dentibot.plataforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.contexto.StaffPapel;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.Alcance;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A matriz da camada 5, transcrita como asserção.
 *
 * <p>Sem banco e sem Spring: a matriz é uma decisão de negócio pura, e um teste
 * que sobe contexto para conferir uma tabela de permissão é lento sem ganhar
 * nada.
 *
 * <p>Cada caso de NEGAÇÃO vem com um de PERMISSÃO ao lado — a matriz inteira
 * devolvendo NENHUM passaria em qualquer teste que só verificasse negações.
 */
@DisplayName("Matriz de permissão (camada 5)")
class MatrizDePermissaoTest {

    private final AvaliadorDePermissao avaliador = new AvaliadorDePermissao();

    private static ContextoRequisicao comPapel(Papel papel) {
        return ContextoRequisicao.deClinica(1L, 10L, papel, UUID.randomUUID());
    }

    private static ContextoRequisicao comStaff(StaffPapel papel) {
        return ContextoRequisicao.deStaff(99L, papel, UUID.randomUUID());
    }

    @Nested
    @DisplayName("Recepcionista")
    class Recepcionista {

        private final ContextoRequisicao ctx = comPapel(Papel.RECEPCIONISTA);

        @Test
        @DisplayName("não alcança prontuário de jeito nenhum")
        void semProntuario() {
            for (Acao acao : Acao.values()) {
                assertThat(avaliador.alcance(ctx, Recurso.PRONTUARIO, acao))
                        .as("recepcionista %s prontuário", acao)
                        .isEqualTo(Alcance.NENHUM);
            }
        }

        @Test
        @DisplayName("mas administra a agenda inteira — é o trabalho dela")
        void agendaInteira() {
            assertThat(avaliador.alcance(ctx, Recurso.AGENDA, Acao.LER)).isEqualTo(Alcance.TODOS);
            assertThat(avaliador.alcance(ctx, Recurso.AGENDA, Acao.CRIAR)).isEqualTo(Alcance.TODOS);
        }

        @Test
        @DisplayName("financeiro parcial: registra recebimento, não altera lançamento")
        void financeiroParcial() {
            assertThat(avaliador.alcance(ctx, Recurso.FINANCEIRO, Acao.CRIAR)).isEqualTo(Alcance.TODOS);
            assertThat(avaliador.alcance(ctx, Recurso.FINANCEIRO, Acao.ALTERAR)).isEqualTo(Alcance.NENHUM);
        }
    }

    @Nested
    @DisplayName("Dentista")
    class Dentista {

        private final ContextoRequisicao ctx = comPapel(Papel.DENTISTA);

        @Test
        @DisplayName("agenda e prontuário são PROPRIOS, não TODOS")
        void apenasOsProprios() {
            assertThat(avaliador.alcance(ctx, Recurso.AGENDA, Acao.LER)).isEqualTo(Alcance.PROPRIOS);
            assertThat(avaliador.alcance(ctx, Recurso.PRONTUARIO, Acao.LER)).isEqualTo(Alcance.PROPRIOS);
        }

        @Test
        @DisplayName("não toca em dinheiro")
        void semFinanceiro() {
            assertThat(avaliador.alcance(ctx, Recurso.FINANCEIRO, Acao.LER)).isEqualTo(Alcance.NENHUM);
        }

        @Test
        @DisplayName("não altera evolução já registrada — correção é adendo (CFO-226)")
        void naoAlteraEvolucao() {
            assertThat(avaliador.alcance(ctx, Recurso.PRONTUARIO, Acao.ALTERAR))
                    .isEqualTo(Alcance.NENHUM);
            assertThat(avaliador.alcance(ctx, Recurso.PRONTUARIO, Acao.CRIAR))
                    .isEqualTo(Alcance.PROPRIOS);
        }
    }

    @Nested
    @DisplayName("Financeiro")
    class Financeiro {

        private final ContextoRequisicao ctx = comPapel(Papel.FINANCEIRO);

        @Test
        @DisplayName("dinheiro inteiro, prontuário nenhum")
        void dinheiroSim_prontuarioNao() {
            assertThat(avaliador.alcance(ctx, Recurso.FINANCEIRO, Acao.ALTERAR)).isEqualTo(Alcance.TODOS);
            assertThat(avaliador.alcance(ctx, Recurso.PRONTUARIO, Acao.LER)).isEqualTo(Alcance.NENHUM);
        }
    }

    @Nested
    @DisplayName("Admin")
    class Admin {

        private final ContextoRequisicao ctx = comPapel(Papel.ADMIN);

        @Test
        @DisplayName("alcança tudo dentro da própria clínica")
        void tudo() {
            assertThat(avaliador.alcance(ctx, Recurso.PRONTUARIO, Acao.LER)).isEqualTo(Alcance.TODOS);
            assertThat(avaliador.alcance(ctx, Recurso.FINANCEIRO, Acao.ALTERAR)).isEqualTo(Alcance.TODOS);
            assertThat(avaliador.alcance(ctx, Recurso.CONFIGURACAO, Acao.ALTERAR)).isEqualTo(Alcance.TODOS);
        }

        @Test
        @DisplayName("nem o admin altera a trilha de auditoria")
        void auditoriaSomenteLeitura() {
            assertThat(avaliador.alcance(ctx, Recurso.AUDITORIA, Acao.LER)).isEqualTo(Alcance.TODOS);
            assertThat(avaliador.alcance(ctx, Recurso.AUDITORIA, Acao.ALTERAR)).isEqualTo(Alcance.NENHUM);
            assertThat(avaliador.alcance(ctx, Recurso.AUDITORIA, Acao.EXCLUIR)).isEqualTo(Alcance.NENHUM);
        }
    }

    @Nested
    @DisplayName("Staff da plataforma (eixo B)")
    class Staff {

        @Test
        @DisplayName("suporte N1 não alcança NENHUM dado clínico")
        void suporteSemDadoClinico() {
            ContextoRequisicao ctx = comStaff(StaffPapel.SUPORTE_N1);
            for (Recurso r : new Recurso[]{Recurso.PRONTUARIO, Recurso.PACIENTE,
                    Recurso.AGENDA, Recurso.ORCAMENTO, Recurso.LGPD}) {
                assertThat(avaliador.alcance(ctx, r, Acao.LER))
                        .as("suporte N1 lendo %s", r)
                        .isEqualTo(Alcance.NENHUM);
            }
            // Permissão ao lado da negação: ele alcança ALGUMA coisa, senão o
            // teste passaria com a matriz de staff vazia.
            assertThat(avaliador.alcance(ctx, Recurso.CONFIGURACAO, Acao.LER))
                    .isEqualTo(Alcance.TODOS);
        }

        @Test
        @DisplayName("ops/billing resolve cobrança sem alcançar prontuário")
        void billingSemProntuario() {
            ContextoRequisicao ctx = comStaff(StaffPapel.OPS_BILLING);
            assertThat(avaliador.alcance(ctx, Recurso.BILLING, Acao.ALTERAR)).isEqualTo(Alcance.TODOS);
            assertThat(avaliador.alcance(ctx, Recurso.PRONTUARIO, Acao.LER)).isEqualTo(Alcance.NENHUM);
        }
    }

    @Test
    @DisplayName("contexto anônimo é negado em tudo — deny by default")
    void anonimoNaoAlcancaNada() {
        ContextoRequisicao anonimo = ContextoRequisicao.anonimo(UUID.randomUUID());
        for (Recurso r : Recurso.values()) {
            for (Acao a : Acao.values()) {
                assertThat(avaliador.alcance(anonimo, r, a))
                        .as("anônimo %s %s", a, r)
                        .isEqualTo(Alcance.NENHUM);
            }
        }
    }

    @Test
    @DisplayName("token com os dois eixos é impossível de construir")
    void eixosSaoExclusivos() {
        assertThatThrownBy(() -> new ContextoRequisicao(
                1L, 10L, Papel.ADMIN, StaffPapel.ENGENHARIA, 99L, UUID.randomUUID(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("camada 5");
    }

    @Test
    @DisplayName("exigir() lança 403 quando o alcance é NENHUM")
    void exigirLancaQuandoNegado() {
        // Fora de uma requisição o contexto é anônimo, então tudo é negado.
        assertThatThrownBy(() -> avaliador.exigir(Recurso.PRONTUARIO, Acao.LER))
                .isInstanceOf(AvaliadorDePermissao.AcessoNegadoException.class);
    }
}
