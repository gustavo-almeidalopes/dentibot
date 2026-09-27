package br.com.dentibot.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.dentibot.TesteIntegracao;
import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.ia.application.GatewayDeIa;
import br.com.dentibot.ia.infrastructure.ClienteAnthropic;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.erro.LimiteExcedidoException;
import br.com.dentibot.plataforma.erro.ServicoIndisponivelException;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Os controles do Doc 03 sobre o gateway (ST-61), com banco de verdade e o
 * provedor substituído: nenhuma chamada sai para a Anthropic num teste.
 */
@DisplayName("Gateway de IA com governança (Doc 03-B)")
class GatewayDeIaTest extends TesteIntegracao {

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000);

    @MockitoBean private ClienteAnthropic cliente;
    @Autowired private GatewayDeIa gateway;
    @Autowired private OnboardingServico onboarding;
    @Autowired private JdbcClient jdbc;
    @Autowired private TransactionTemplate transacao;

    private long idClinica;
    private long idAdmin;

    @BeforeEach
    void clinica() {
        long n = SEQ.incrementAndGet();
        ContextoAtual.definir(ContextoRequisicao.semConta("user_ia_" + n, UUID.randomUUID()));
        var criada = onboarding.provisionar(new NovaClinica(
                String.format("%014d", n), "Clinica IA LTDA", "Clinica IA",
                ZoneId.of("America/Sao_Paulo"), Plano.CLINICA,
                "Admin IA", "admin.ia" + n + "@teste.local", "user_ia_" + n));
        idClinica = criada.idClinica();
        idAdmin = criada.idUsuarioAdmin();
        como(Papel.ADMIN);

        given(cliente.configurado()).willReturn(true);
        given(cliente.provedor()).willReturn("anthropic");
        given(cliente.modelo()).willReturn("claude-opus-5");
        given(cliente.chamar(anyString(), anyString(), any(), anyString()))
                .willReturn(new ClienteAnthropic.Resultado("{\"ok\":true}", 1000, 500, "claude-opus-5"));
    }

    @AfterEach
    void limpar() {
        ContextoAtual.limpar();
    }

    @Test
    @DisplayName("nome e CPF saem antes do provedor; a trilha guarda o redigido, tokens e custo")
    void redigeEGravaTrilha() {
        RespostaDeIa r = gateway.executar(pedido(
                "Ana Souza, CPF 123.456.789-09, restauração no 36. Ana voltou bem.", "Ana Souza"));

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(cliente).chamar(anyString(), enviado.capture(), any(), anyString());
        assertThat(enviado.getValue())
                .doesNotContain("Ana", "Souza", "123.456.789-09")
                .contains("[PACIENTE]", "restauração no 36");

        var trilha = transacao.execute(s -> jdbc.sql("""
                        SELECT entrada_redigida, tokens_entrada, custo_usd, status FROM ia.chamadas
                        WHERE id_chamada = :id""")
                .param("id", r.idChamada())
                .query((rs, n) -> List.of(rs.getString(1), rs.getInt(2), rs.getBigDecimal(3), rs.getString(4)))
                .single());
        assertThat((String) trilha.get(0)).isEqualTo(enviado.getValue());
        assertThat(trilha.get(1)).isEqualTo(1000);
        // 1000 × US$5/M + 500 × US$25/M = 0,005 + 0,0125 (Opus 5).
        assertThat((BigDecimal) trilha.get(2)).isEqualByComparingTo("0.017500");
        assertThat(trilha.get(3)).isEqualTo("sucesso");
    }

    @Test
    @DisplayName("recurso desligado na clínica: nada sai para o provedor")
    void recursoDesligado() {
        gateway.salvarConfiguracao(List.of("nota_clinica"), new BigDecimal("20"));

        assertThatThrownBy(() -> gateway.executar(pedido("texto", null)))
                .isInstanceOf(ServicoIndisponivelException.class);
        verify(cliente, never()).chamar(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("cota do mês atingida: 429, e nada sai para o provedor")
    void cotaAtingida() {
        gateway.salvarConfiguracao(List.of(), new BigDecimal("0.01"));
        gateway.executar(pedido("primeira", null));

        assertThatThrownBy(() -> gateway.executar(pedido("segunda", null)))
                .isInstanceOf(LimiteExcedidoException.class);
    }

    @Test
    @DisplayName("sem provedor configurado: 503 antes de tocar no banco")
    void semProvedor() {
        given(cliente.configurado()).willReturn(false);
        assertThatThrownBy(() -> gateway.executar(pedido("texto", null)))
                .isInstanceOf(ServicoIndisponivelException.class);
    }

    @Test
    @DisplayName("uma decisão humana por sugestão, e o consumo do mês mostra o que virou registro")
    void confirmacaoEConsumo() {
        long aceita = gateway.executar(pedido("um", null)).idChamada();
        long descartada = gateway.executar(pedido("dois", null)).idChamada();
        gateway.confirmar(aceita, true, "prontuario.evolucao:1");
        gateway.confirmar(descartada, false, null);

        assertThatThrownBy(() -> gateway.confirmar(aceita, false, null))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(gateway.consumo(YearMonth.now(ZoneOffset.UTC))).singleElement().satisfies(c -> {
            assertThat(c.recurso()).isEqualTo("nota_clinica");
            assertThat(c.chamadas()).isEqualTo(2);
            assertThat(c.aceitas()).isEqualTo(1);
            assertThat(c.descartadas()).isEqualTo(1);
            assertThat(c.custoUsd()).isEqualByComparingTo("0.035000");
        });
    }

    @Test
    @DisplayName("custo e configuração de IA são do admin: dentista não vê nem mexe")
    void soAdminGovernaIa() {
        como(Papel.DENTISTA);
        assertThatThrownBy(() -> gateway.consumo(YearMonth.now(ZoneOffset.UTC)))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> gateway.salvarConfiguracao(List.of(), BigDecimal.ONE))
                .isInstanceOf(AcessoNegadoException.class);
    }

    private static PedidoDeIa pedido(String entrada, String nome) {
        return new PedidoDeIa("nota_clinica", "instrucoes", entrada,
                nome == null ? List.of() : List.of(nome), null, "low");
    }

    private void como(Papel papel) {
        ContextoAtual.definir(ContextoRequisicao.deClinica(idClinica, idAdmin, papel, UUID.randomUUID()));
    }
}
