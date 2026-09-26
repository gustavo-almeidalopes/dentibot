package br.com.dentibot.copiloto.application;

import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.agenda.ConsultaResumo;
import br.com.dentibot.agenda.SituacaoNaAgenda;
import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.copiloto.Exportacao;
import br.com.dentibot.copiloto.Pendencia;
import br.com.dentibot.copiloto.ResumoDoPaciente;
import br.com.dentibot.copiloto.Secao;
import br.com.dentibot.copiloto.SituacaoFinanceira;
import br.com.dentibot.copiloto.TratamentoParado;
import br.com.dentibot.financeiro.FiltroFinanceiro;
import br.com.dentibot.financeiro.FinanceiroApi;
import br.com.dentibot.financeiro.RecebivelResumo;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.lgpd.LgpdApi;
import br.com.dentibot.orcamento.ItemDePlano;
import br.com.dentibot.orcamento.OrcamentoApi;
import br.com.dentibot.pacientes.PacienteResumo;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import br.com.dentibot.prontuario.ProntuarioApi;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Ver {@link br.com.dentibot.copiloto}. Sem {@code @Transactional}: cada porta
 * abre a própria transação, com o tenant e a permissão de quem pergunta.
 */
@Service
public class CopilotoServico {

    private static final Set<String> RECEBIVEL_EM_ABERTO = Set.of("aberta", "parcial", "vencida");

    private final PacientesApi pacientes;
    private final ProntuarioApi prontuario;
    private final OrcamentoApi orcamento;
    private final FinanceiroApi financeiro;
    private final AgendaApi agenda;
    private final IdentidadeApi identidade;
    private final LgpdApi lgpd;
    private final AuditoriaApi auditoria;
    private final AvaliadorDePermissao permissoes;
    private final ObjectMapper json;
    private final Clock relogio;

    public CopilotoServico(PacientesApi pacientes, ProntuarioApi prontuario, OrcamentoApi orcamento,
                           FinanceiroApi financeiro, AgendaApi agenda, IdentidadeApi identidade,
                           LgpdApi lgpd, AuditoriaApi auditoria, AvaliadorDePermissao permissoes,
                           ObjectMapper json) {
        this.pacientes = pacientes;
        this.prontuario = prontuario;
        this.orcamento = orcamento;
        this.financeiro = financeiro;
        this.agenda = agenda;
        this.identidade = identidade;
        this.lgpd = lgpd;
        this.auditoria = auditoria;
        this.permissoes = permissoes;
        this.json = json;
        this.relogio = Clock.systemUTC();
    }

    // ─── IA-06: resumo de retorno ────────────────────────────────────────────

    public ResumoDoPaciente resumo(long idPaciente) {
        PacienteResumo paciente = pacienteExistente(idPaciente);
        return new ResumoDoPaciente(
                idPaciente,
                paciente.nomeCompleto(),
                Secao.ler(() -> prontuario.alertas(idPaciente)),
                Secao.ler(() -> prontuario.historico(idPaciente).stream().findFirst().orElse(null)),
                Secao.ler(() -> orcamento.itensEmAberto(idPaciente)),
                Secao.ler(() -> situacaoFinanceira(idPaciente)),
                Secao.ler(() -> agenda.situacaoDosPacientes(List.of(idPaciente)).get(idPaciente)));
    }

    private SituacaoFinanceira situacaoFinanceira(long idPaciente) {
        LocalDate hoje = LocalDate.now(relogio);
        BigDecimal emAberto = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        int parcelas = 0;
        for (RecebivelResumo r : financeiro.listarRecebiveis(
                new FiltroFinanceiro(null, null, null, idPaciente, 0, 500))) {
            if (!RECEBIVEL_EM_ABERTO.contains(r.status())) {
                continue;
            }
            parcelas++;
            emAberto = emAberto.add(r.saldoDevedor());
            if ("vencida".equals(r.status())
                    || (r.vencimentoEm() != null && r.vencimentoEm().isBefore(hoje))) {
                vencido = vencido.add(r.saldoDevedor());
            }
        }
        return new SituacaoFinanceira(emAberto, vencido, parcelas);
    }

    // ─── IA-35: radar de abandono ────────────────────────────────────────────

    /**
     * Quem tem plano aprovado com item por fazer, nenhuma consulta marcada, e
     * nada acontecendo há pelo menos {@code dias}. Ordenado pelo que está em
     * jogo: valor em aberto, depois tempo parado.
     */
    public List<TratamentoParado> tratamentosParados(int dias) {
        Map<Long, List<ItemDePlano>> porPaciente = orcamento.itensEmAberto(null).stream()
                .collect(Collectors.groupingBy(ItemDePlano::idPaciente, LinkedHashMap::new,
                        Collectors.toList()));
        if (porPaciente.isEmpty()) {
            return List.of();
        }
        Map<Long, SituacaoNaAgenda> agendas = agenda.situacaoDosPacientes(porPaciente.keySet());
        Map<Long, PacienteResumo> nomes = pacientes.mapaDeResumos(porPaciente.keySet());
        Instant agora = relogio.instant();
        Instant limite = agora.minus(Duration.ofDays(Math.max(dias, 0)));

        List<TratamentoParado> parados = new ArrayList<>();
        porPaciente.forEach((idPaciente, itens) -> {
            SituacaoNaAgenda situacao = agendas.get(idPaciente);
            if (situacao != null && situacao.proximaMarcada() != null) {
                return;
            }
            Instant desde = itens.stream().map(ItemDePlano::aprovadoEm)
                    .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
            if (situacao != null && situacao.ultimaRealizada() != null
                    && (desde == null || situacao.ultimaRealizada().isAfter(desde))) {
                desde = situacao.ultimaRealizada();
            }
            if (desde == null || desde.isAfter(limite)) {
                return;
            }
            PacienteResumo p = nomes.get(idPaciente);
            parados.add(new TratamentoParado(
                    idPaciente,
                    p == null ? null : p.nomeCompleto(),
                    p == null ? null : p.telefoneCelular(),
                    itens.size(),
                    itens.stream().map(ItemDePlano::valorCobrado).reduce(BigDecimal.ZERO, BigDecimal::add),
                    itens.stream().map(CopilotoServico::rotulo).distinct().toList(),
                    desde,
                    Duration.between(desde, agora).toDays()));
        });
        parados.sort(Comparator.comparing(TratamentoParado::valorEmAberto).reversed()
                .thenComparing(Comparator.comparingLong(TratamentoParado::diasParado).reversed()));
        return parados;
    }

    // ─── IA-05: registro que falta ───────────────────────────────────────────

    /**
     * Atendimento dos últimos {@code dias} sem evolução no prontuário. Pergunta
     * clínica: quem não lê prontuário (recepção, financeiro) recebe 403 aqui,
     * mesmo que a lista estivesse vazia.
     */
    public List<Pendencia> pendencias(int dias) {
        permissoes.exigir(Recurso.PRONTUARIO, Acao.LER);
        Instant ate = relogio.instant();
        Instant de = ate.minus(Duration.ofDays(Math.max(dias, 1)));

        List<ConsultaResumo> realizadas = agenda.listar(de, ate, null).stream()
                .filter(c -> "realizada".equals(c.status()))
                .toList();
        List<ItemDePlano> concluidos = orcamento.itensConcluidos(de, ate);

        Set<Long> consultas = new HashSet<>();
        realizadas.forEach(c -> consultas.add(c.idConsulta()));
        concluidos.stream().map(ItemDePlano::idConsulta).filter(Objects::nonNull)
                .forEach(consultas::add);
        Set<Long> comEvolucao = prontuario.consultasComEvolucao(consultas);

        List<Pendencia> pendencias = new ArrayList<>();
        Set<Long> jaApontadas = new HashSet<>();
        for (ConsultaResumo c : realizadas) {
            if (!comEvolucao.contains(c.idConsulta())) {
                jaApontadas.add(c.idConsulta());
                pendencias.add(new Pendencia(Pendencia.CONSULTA_SEM_EVOLUCAO, c.idPaciente(),
                        c.nomePaciente(), c.idConsulta(), null, c.inicioEm(),
                        "Consulta realizada sem evolução no prontuário."));
            }
        }
        Map<Long, PacienteResumo> nomes = concluidos.isEmpty() ? Map.of()
                : pacientes.mapaDeResumos(concluidos.stream().map(ItemDePlano::idPaciente)
                        .collect(Collectors.toSet()));
        for (ItemDePlano item : concluidos) {
            PacienteResumo p = nomes.get(item.idPaciente());
            String nome = p == null ? null : p.nomeCompleto();
            if (item.idConsulta() == null) {
                pendencias.add(new Pendencia(Pendencia.PROCEDIMENTO_SEM_CONSULTA, item.idPaciente(),
                        nome, null, item.idItem(), item.executadoEm(),
                        rotulo(item) + " concluído sem atendimento vinculado."));
            } else if (!comEvolucao.contains(item.idConsulta())
                    && !jaApontadas.contains(item.idConsulta())) {
                pendencias.add(new Pendencia(Pendencia.PROCEDIMENTO_SEM_EVOLUCAO, item.idPaciente(),
                        nome, item.idConsulta(), item.idItem(), item.executadoEm(),
                        rotulo(item) + " concluído sem evolução no prontuário."));
            }
        }
        pendencias.sort(Comparator.comparing(Pendencia::quando,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return pendencias;
    }

    // ─── IA-47: portabilidade ────────────────────────────────────────────────

    /**
     * Tudo o que a clínica tem sobre o paciente, num JSON legível, com o SHA-256
     * dos bytes. Só o admin (permissão LGPD): é resposta a direito do titular,
     * não consulta do dia a dia.
     */
    public Exportacao exportar(long idPaciente) {
        permissoes.exigir(Recurso.LGPD, Acao.CRIAR);
        PacienteResumo paciente = pacienteExistente(idPaciente);
        Instant agora = relogio.instant();

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("formato", "dentibot.portabilidade.v1");
        dados.put("geradoEm", agora);
        dados.put("paciente", paciente);
        dados.put("dadosPessoais", identidade.dadosPessoais(paciente.idPessoa()).orElse(null));
        dados.put("triagemDeSaude", pacientes.anamnese(idPaciente).orElse(null));
        dados.put("consultas", agenda.historicoDoPaciente(idPaciente));
        dados.put("evolucoes", prontuario.historico(idPaciente));
        dados.put("odontograma", prontuario.odontograma(idPaciente));
        dados.put("orcamentos", orcamento.listar(idPaciente, null).stream()
                .map(o -> orcamento.detalhar(o.idOrcamento()))
                .toList());
        dados.put("recebiveis", financeiro.listarRecebiveis(
                new FiltroFinanceiro(null, null, null, idPaciente, 0, 500)));
        dados.put("consentimentos", lgpd.consentimentosDoPaciente(idPaciente));

        byte[] conteudo = json.writerWithDefaultPrettyPrinter().writeValueAsBytes(dados);
        String sha256 = sha256(conteudo);
        auditoria.registrarExportacao("paciente", String.valueOf(idPaciente),
                Map.of("sha256", sha256, "bytes", conteudo.length));

        String data = agora.atOffset(ZoneOffset.UTC).toLocalDate().toString();
        return new Exportacao(conteudo, sha256, "paciente-%d-%s.json".formatted(idPaciente, data));
    }

    // ─── apoio ───────────────────────────────────────────────────────────────

    private PacienteResumo pacienteExistente(long idPaciente) {
        permissoes.exigir(Recurso.PACIENTE, Acao.LER);
        PacienteResumo p = pacientes.mapaDeResumos(List.of(idPaciente)).get(idPaciente);
        if (p == null) {
            throw new RecursoNaoEncontradoException("Paciente", idPaciente);
        }
        return p;
    }

    private static String rotulo(ItemDePlano item) {
        String nome = item.nomeProcedimento() == null ? "Procedimento" : item.nomeProcedimento();
        return item.dente() == null ? nome : nome + " (dente " + item.dente() + ")";
    }

    static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM é obrigada a ter SHA-256 (JCA); chegar aqui é JVM quebrada.
            throw new IllegalStateException(e);
        }
    }
}
