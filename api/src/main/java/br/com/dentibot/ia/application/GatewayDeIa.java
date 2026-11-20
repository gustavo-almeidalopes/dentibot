package br.com.dentibot.ia.application;

import br.com.dentibot.ia.ConfiguracaoDeIa;
import br.com.dentibot.ia.ConsumoDeIa;
import br.com.dentibot.ia.IaApi;
import br.com.dentibot.ia.PedidoDeIa;
import br.com.dentibot.ia.RespostaDeIa;
import br.com.dentibot.ia.infrastructure.ClienteAnthropic;
import br.com.dentibot.ia.infrastructure.IaRepositorio;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.erro.FalhaExternaException;
import br.com.dentibot.plataforma.erro.LimiteExcedidoException;
import br.com.dentibot.plataforma.erro.ServicoIndisponivelException;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import br.com.dentibot.plataforma.telemetria.ScrubberDePii;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O gateway (ST-61): a única porta entre o DentiBot e um modelo de linguagem.
 *
 * <p>A ordem é a garantia:
 * <ol>
 *   <li>provedor configurado, recurso ligado na clínica, cota do mês com saldo;</li>
 *   <li>a entrada é redigida — CPF, e-mail e telefone pela forma
 *       ({@link ScrubberDePii}), o nome do paciente porque quem chama o informou;</li>
 *   <li>só então o provedor, FORA de transação: segurar conexão de banco
 *       esperando um modelo por segundos esgotaria o pool;</li>
 *   <li>a trilha grava o que saiu (já redigido), o que voltou, tokens e custo.</li>
 * </ol>
 *
 * <p>Nada aqui escreve em prontuário, orçamento ou qualquer registro oficial. O
 * texto volta para a tela como sugestão; o aceite humano é outra chamada
 * ({@link #confirmar}).
 */
@Service
public class GatewayDeIa implements IaApi {

    private static final String PACIENTE = "[PACIENTE]";
    private static final BigDecimal UM_MILHAO = BigDecimal.valueOf(1_000_000);

    private final IaRepositorio repositorio;
    private final ClienteAnthropic cliente;
    private final ScrubberDePii scrubber;
    private final AvaliadorDePermissao permissoes;
    private final TransactionTemplate transacao;
    private final BigDecimal precoEntradaMtok;
    private final BigDecimal precoSaidaMtok;

    public GatewayDeIa(IaRepositorio repositorio, ClienteAnthropic cliente, ScrubberDePii scrubber,
                       AvaliadorDePermissao permissoes, TransactionTemplate transacao,
                       @Value("${dentibot.ia.preco-entrada-usd-mtok:5.00}") BigDecimal precoEntradaMtok,
                       @Value("${dentibot.ia.preco-saida-usd-mtok:25.00}") BigDecimal precoSaidaMtok) {
        this.repositorio = repositorio;
        this.cliente = cliente;
        this.scrubber = scrubber;
        this.permissoes = permissoes;
        this.transacao = transacao;
        this.precoEntradaMtok = precoEntradaMtok;
        this.precoSaidaMtok = precoSaidaMtok;
    }

    @Override
    public RespostaDeIa executar(PedidoDeIa pedido) {
        if (!RECURSOS.contains(pedido.recurso())) {
            throw new IllegalArgumentException("Recurso de IA desconhecido: " + pedido.recurso());
        }
        if (!cliente.configurado()) {
            throw new ServicoIndisponivelException(
                    "Provedor de IA não configurado nesta instância (DENTIBOT_IA_CHAVE).");
        }
        transacao.executeWithoutResult(s -> exigirLigadoEComSaldo(pedido.recurso()));

        String redigida = redigir(scrubber, pedido.entrada(), pedido.identificadores());
        ClienteAnthropic.Resultado resultado;
        try {
            resultado = cliente.chamar(pedido.instrucoes(), redigida, pedido.esquema(),
                    pedido.esforco() == null ? "low" : pedido.esforco());
        } catch (FalhaExternaException e) {
            transacao.executeWithoutResult(s -> gravar(pedido.recurso(), cliente.modelo(), redigida,
                    null, 0, 0, "erro", e.getMessage()));
            throw e;
        }
        Long id = transacao.execute(s -> gravar(pedido.recurso(), resultado.modelo(), redigida,
                resultado.texto(), resultado.tokensEntrada(), resultado.tokensSaida(), "sucesso", null));
        return new RespostaDeIa(id, resultado.texto());
    }

    @Override
    public void confirmar(long idChamada, boolean aceita, String registro) {
        Long usuario = ContextoAtual.obter().usuarioId();
        if (usuario == null) {
            throw new IllegalArgumentException("Confirmação de sugestão exige um usuário identificado.");
        }
        transacao.executeWithoutResult(s ->
                repositorio.inserirConfirmacao(idChamada, usuario, aceita, registro));
    }

    // ─── IA-59 e configuração por clínica ────────────────────────────────────

    public List<ConsumoDeIa> consumo(YearMonth mes) {
        permissoes.exigir(Recurso.BILLING, Acao.LER);
        Instant de = mes.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant ate = mes.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return transacao.execute(s -> repositorio.consumo(de, ate));
    }

    public ConfiguracaoDeIa configuracao() {
        permissoes.exigir(Recurso.CONFIGURACAO, Acao.LER);
        return transacao.execute(s -> {
            IaRepositorio.Configuracao c = repositorio.configuracao();
            return new ConfiguracaoDeIa(RECURSOS.stream().sorted().toList(), c.recursosDesligados(),
                    c.cotaMensalUsd(), repositorio.gastoDesde(inicioDoMes()),
                    cliente.configurado(), cliente.modelo());
        });
    }

    public ConfiguracaoDeIa salvarConfiguracao(List<String> recursosDesligados, BigDecimal cotaMensalUsd) {
        permissoes.exigir(Recurso.CONFIGURACAO, Acao.ALTERAR);
        if (!RECURSOS.containsAll(recursosDesligados)) {
            throw new IllegalArgumentException("Há recurso de IA desconhecido na lista de desligados.");
        }
        if (cotaMensalUsd == null || cotaMensalUsd.signum() < 0) {
            throw new IllegalArgumentException("A cota mensal não pode ser negativa.");
        }
        transacao.executeWithoutResult(s ->
                repositorio.salvarConfiguracao(recursosDesligados.stream().distinct().toList(), cotaMensalUsd));
        return configuracao();
    }

    // ─── apoio ───────────────────────────────────────────────────────────────

    private void exigirLigadoEComSaldo(String recurso) {
        IaRepositorio.Configuracao c = repositorio.configuracao();
        if (c.recursosDesligados().contains(recurso)) {
            throw new ServicoIndisponivelException(
                    "O recurso de IA '" + recurso + "' está desligado nesta clínica.");
        }
        if (repositorio.gastoDesde(inicioDoMes()).compareTo(c.cotaMensalUsd()) >= 0) {
            throw new LimiteExcedidoException(
                    "A cota mensal de IA da clínica foi atingida. Ajuste em Configuração de IA.");
        }
    }

    private long gravar(String recurso, String modelo, String entrada, String saida,
                        int tokensEntrada, int tokensSaida, String status, String erro) {
        BigDecimal custo = precoEntradaMtok.multiply(BigDecimal.valueOf(tokensEntrada))
                .add(precoSaidaMtok.multiply(BigDecimal.valueOf(tokensSaida)))
                .divide(UM_MILHAO, 6, RoundingMode.HALF_UP);
        return repositorio.inserirChamada(ContextoAtual.obter().usuarioId(), recurso,
                cliente.provedor(), modelo, entrada, sha256(entrada), saida, tokensEntrada,
                tokensSaida, custo, status, erro, ContextoAtual.correlacao());
    }

    private static Instant inicioDoMes() {
        return YearMonth.now(ZoneOffset.UTC).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    /**
     * O que sai para o provedor. Nome inteiro primeiro e partes depois, da maior
     * para a menor — senão "Ana" viraria "[PACIENTE]" dentro de "Ana Souza" e
     * sobraria o sobrenome. Partes de até duas letras ficam ("da", "de"): trocar
     * toda preposição do ditado destruiria o texto e não protegeria ninguém.
     */
    static String redigir(ScrubberDePii scrubber, String texto, List<String> identificadores) {
        String r = scrubber.limparTexto(texto == null ? "" : texto);
        List<String> termos = new ArrayList<>();
        for (String id : identificadores == null ? List.<String>of() : identificadores) {
            if (id == null || id.isBlank()) {
                continue;
            }
            termos.add(id.strip());
            for (String parte : id.strip().split("\\s+")) {
                if (parte.length() > 2) {
                    termos.add(parte);
                }
            }
        }
        termos.sort(Comparator.comparingInt(String::length).reversed());
        for (String termo : termos) {
            r = Pattern.compile("(?<![\\p{L}])" + Pattern.quote(termo) + "(?![\\p{L}])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(r).replaceAll(PACIENTE);
        }
        return r;
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8))).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
