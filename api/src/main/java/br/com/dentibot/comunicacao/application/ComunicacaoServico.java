package br.com.dentibot.comunicacao.application;

import br.com.dentibot.agenda.AgendaApi;
import br.com.dentibot.clinicas.ClinicasApi;
import br.com.dentibot.comunicacao.Espera;
import br.com.dentibot.comunicacao.Mensagem;
import br.com.dentibot.comunicacao.domain.Cadencia;
import br.com.dentibot.comunicacao.domain.Textos;
import br.com.dentibot.comunicacao.domain.Textos.Orientacao;
import br.com.dentibot.comunicacao.infrastructure.ClienteWhatsApp;
import br.com.dentibot.comunicacao.infrastructure.ComunicacaoRepositorio;
import br.com.dentibot.identidade.Celular;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.pacientes.PacienteResumo;
import br.com.dentibot.pacientes.PacientesApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.Alcance;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O lado da equipe: a fila de quem precisa de gente, a lista de espera, o
 * canal, a orientação ao paciente.
 *
 * <p>Conversa com paciente é parte da agenda — quem tem AGENDA atende o
 * WhatsApp, como a recepção de verdade faz. Orientação pós-procedimento é ato
 * clínico e pede PRONTUARIO; o canal é configuração da clínica.
 */
@Service
public class ComunicacaoServico {

    /** Os que a clínica liga e desliga. Lembrete e reforço andam com a confirmação. */
    public static final List<String> TIPOS_CONFIGURAVEIS =
            List.of("confirmacao", "oferta_vaga", "pos_procedimento");

    private static final Set<String> PERIODOS = Set.of("manha", "tarde", "qualquer");

    private final ComunicacaoRepositorio repo;
    private final PacientesApi pacientes;
    private final AgendaApi agenda;
    private final IdentidadeApi identidade;
    private final ClinicasApi clinicas;
    private final ClienteWhatsApp whatsapp;
    private final AvaliadorDePermissao permissoes;

    public ComunicacaoServico(ComunicacaoRepositorio repo, PacientesApi pacientes,
                              AgendaApi agenda, IdentidadeApi identidade, ClinicasApi clinicas,
                              ClienteWhatsApp whatsapp, AvaliadorDePermissao permissoes) {
        this.repo = repo;
        this.pacientes = pacientes;
        this.agenda = agenda;
        this.identidade = identidade;
        this.clinicas = clinicas;
        this.whatsapp = whatsapp;
        this.permissoes = permissoes;
    }

    public record Item(Mensagem mensagem, String nomePaciente) {
    }

    // ─── Quem precisa de gente ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Item> escaladas() {
        permissoes.exigir(Recurso.AGENDA, Acao.LER);
        return comNomes(repo.escaladas());
    }

    @Transactional
    public void resolver(long idMensagem) {
        permissoes.exigir(Recurso.AGENDA, Acao.ALTERAR);
        if (repo.resolver(idMensagem, ContextoAtual.obter().usuarioId()) == 0) {
            throw new RecursoNaoEncontradoException("mensagem", idMensagem);
        }
    }

    /** A equipe responde pelo mesmo número; sai pela fila, como tudo. */
    @Transactional
    public long responder(long idMensagem, String texto) {
        permissoes.exigir(Recurso.AGENDA, Acao.ALTERAR);
        Mensagem m = repo.buscar(idMensagem)
                .filter(x -> x.sentido().equals("entrada"))
                .orElseThrow(() -> new RecursoNaoEncontradoException("mensagem", idMensagem));
        return repo.agendar(m.idPaciente(), m.telefone(), "resposta", null, texto.strip(),
                Instant.now());
    }

    @Transactional(readOnly = true)
    public List<Item> doPaciente(long idPaciente) {
        permissoes.exigir(Recurso.AGENDA, Acao.LER);
        return comNomes(repo.doPaciente(idPaciente));
    }

    // ─── Lista de espera (IA-18) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Espera> listaDeEspera() {
        permissoes.exigir(Recurso.AGENDA, Acao.LER);
        List<Espera> lista = repo.listaDeEspera();
        Map<Long, PacienteResumo> nomes = pacientes.mapaDeResumos(
                lista.stream().map(Espera::idPaciente).toList());
        return lista.stream()
                .map(e -> e.comNome(nomes.containsKey(e.idPaciente())
                        ? nomes.get(e.idPaciente()).nomeCompleto() : null))
                .toList();
    }

    @Transactional
    public long entrarNaEspera(long idPaciente, Long idDentista, String periodo, int urgencia,
                               String observacao) {
        permissoes.exigir(Recurso.AGENDA, Acao.CRIAR);
        if (!PERIODOS.contains(periodo)) {
            throw new IllegalArgumentException("Período inválido: " + periodo);
        }
        if (!pacientes.existe(idPaciente)) {
            throw new RecursoNaoEncontradoException("paciente", idPaciente);
        }
        return repo.entrarNaEspera(idPaciente, idDentista, periodo, urgencia, observacao);
    }

    @Transactional
    public void sairDaEspera(long idEspera) {
        permissoes.exigir(Recurso.AGENDA, Acao.ALTERAR);
        if (repo.encerrarEspera(idEspera) == 0) {
            throw new RecursoNaoEncontradoException("espera", idEspera);
        }
    }

    // ─── Canal ───────────────────────────────────────────────────────────────

    public record ConfiguracaoDoCanal(String phoneNumberId, boolean ativo,
                                      List<String> tiposDesligados, List<String> tiposDisponiveis,
                                      boolean provedorConfigurado) {
    }

    @Transactional(readOnly = true)
    public ConfiguracaoDoCanal canal() {
        permissoes.exigir(Recurso.CONFIGURACAO, Acao.LER);
        var canal = repo.canal();
        return new ConfiguracaoDoCanal(
                canal.map(ComunicacaoRepositorio.Canal::phoneNumberId).orElse(null),
                canal.map(ComunicacaoRepositorio.Canal::ativo).orElse(false),
                canal.map(ComunicacaoRepositorio.Canal::tiposDesligados).orElse(List.of()),
                TIPOS_CONFIGURAVEIS, whatsapp.configurado());
    }

    @Transactional
    public ConfiguracaoDoCanal salvarCanal(String phoneNumberId, boolean ativo,
                                           List<String> desligados) {
        permissoes.exigir(Recurso.CONFIGURACAO, Acao.ALTERAR);
        if (!TIPOS_CONFIGURAVEIS.containsAll(desligados)) {
            throw new IllegalArgumentException("Tipo de mensagem desconhecido.");
        }
        repo.salvarCanal(phoneNumberId, ativo, desligados);
        return canal();
    }

    // ─── Orientação ao paciente (IA-38) ──────────────────────────────────────

    public record Tema(String chave, String titulo, String texto) {
    }

    public List<Tema> temas() {
        return Textos.ORIENTACOES.entrySet().stream()
                .map(e -> new Tema(e.getKey(), e.getValue().titulo(), e.getValue().texto()))
                .toList();
    }

    /**
     * Texto fixo, revisado, e não gerado: o que o paciente recebe é exatamente
     * o que está na biblioteca. "Lida" — pelo recibo do WhatsApp ou pelo OK do
     * paciente — é a prova de que a orientação foi prestada.
     */
    @Transactional
    public long enviarOrientacao(long idPaciente, String tema) {
        Alcance alcance = permissoes.exigir(Recurso.PRONTUARIO, Acao.CRIAR);
        if (alcance == Alcance.PROPRIOS) {
            Long idUsuario = ContextoAtual.obter().usuarioId();
            long dentista = idUsuario == null ? -1L
                    : identidade.dentistaDoUsuario(idUsuario).orElse(-1L);
            if (!agenda.pacienteAtendidoPor(idPaciente, dentista)) {
                throw new AcessoNegadoException(Recurso.PRONTUARIO, Acao.CRIAR);
            }
        }
        Orientacao o = Textos.ORIENTACOES.get(tema);
        if (o == null) {
            throw new IllegalArgumentException("Orientação desconhecida: " + tema);
        }
        PacienteResumo p = pacientes.mapaDeResumos(List.of(idPaciente)).get(idPaciente);
        if (p == null) {
            throw new RecursoNaoEncontradoException("paciente", idPaciente);
        }
        String telefone = Celular.paraWhatsApp(p.telefoneCelular())
                .orElseThrow(() -> new IllegalArgumentException(
                        "O paciente não tem celular válido no cadastro."));
        if (repo.canal().filter(ComunicacaoRepositorio.Canal::ativo).isEmpty()) {
            throw new IllegalArgumentException(
                    "A clínica não tem WhatsApp configurado. Configure o canal em Conversas.");
        }
        var clinica = clinicas.identificacao();
        return repo.agendar(idPaciente, telefone, "orientacao", null,
                Textos.orientacao(clinica.nome(), o),
                Cadencia.avancarParaHorarioUtil(Instant.now(), clinica.fuso()));
    }

    private List<Item> comNomes(List<Mensagem> mensagens) {
        Map<Long, PacienteResumo> nomes = pacientes.mapaDeResumos(mensagens.stream()
                .map(Mensagem::idPaciente).filter(id -> id != null).distinct().toList());
        return mensagens.stream()
                .map(m -> new Item(m, m.idPaciente() == null || !nomes.containsKey(m.idPaciente())
                        ? null : nomes.get(m.idPaciente()).nomeCompleto()))
                .toList();
    }
}
