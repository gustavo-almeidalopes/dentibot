package br.com.dentibot.orcamento.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.clinicas.ClinicasApi;
import br.com.dentibot.clinicas.ProcedimentoResumo;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.orcamento.ItemResumo;
import br.com.dentibot.orcamento.NovoItem;
import br.com.dentibot.orcamento.NovoOrcamento;
import br.com.dentibot.orcamento.OrcamentoApi;
import br.com.dentibot.orcamento.OrcamentoDetalhado;
import br.com.dentibot.orcamento.OrcamentoResumo;
import br.com.dentibot.orcamento.infrastructure.OrcamentoRepositorio;
import br.com.dentibot.orcamento.infrastructure.OrcamentoRepositorio.LinhaItem;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.outbox.Outbox;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.Alcance;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrcamentoServico implements OrcamentoApi {

    private final OrcamentoRepositorio orcamentos;
    private final ClinicasApi clinicas;
    private final IdentidadeApi identidade;
    private final AvaliadorDePermissao permissoes;
    private final AuditoriaApi auditoria;
    private final Outbox outbox;

    public OrcamentoServico(OrcamentoRepositorio orcamentos, ClinicasApi clinicas,
                            IdentidadeApi identidade, AvaliadorDePermissao permissoes,
                            AuditoriaApi auditoria, Outbox outbox) {
        this.orcamentos = orcamentos;
        this.clinicas = clinicas;
        this.identidade = identidade;
        this.permissoes = permissoes;
        this.auditoria = auditoria;
        this.outbox = outbox;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrcamentoResumo> listar(Long idPaciente, String status) {
        Alcance alcance = permissoes.exigir(Recurso.ORCAMENTO, Acao.LER);
        List<OrcamentoResumo> todos = orcamentos.listar(idPaciente, status);
        return filtrarPorAlcance(todos, alcance);
    }

    @Override
    @Transactional(readOnly = true)
    public OrcamentoDetalhado detalhar(long idOrcamento) {
        Alcance alcance = permissoes.exigir(Recurso.ORCAMENTO, Acao.LER);

        OrcamentoResumo cabecalho = orcamentos.buscar(idOrcamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("orcamento", idOrcamento));
        exigirAlcance(cabecalho, alcance);

        return new OrcamentoDetalhado(cabecalho, montarItens(idOrcamento),
                orcamentos.observacoes(idOrcamento));
    }

    @Override
    @Transactional
    public long criar(NovoOrcamento novo) {
        Alcance alcance = permissoes.exigir(Recurso.ORCAMENTO, Acao.CRIAR);
        long idDentista = autorPermitido(novo.idDentista(), alcance);

        long id = orcamentos.inserir(novo.idPaciente(), idDentista,
                novo.validadeEm(), novo.observacoes());
        if (novo.valorDesconto() != null && novo.valorDesconto().signum() > 0) {
            orcamentos.definirDesconto(id, novo.valorDesconto());
        }

        auditoria.registrarCriacao("orcamento", String.valueOf(id), novo);
        outbox.gravar(TiposDeEvento.ORCAMENTO_CRIADO,
                Map.of("idOrcamento", id, "idPaciente", novo.idPaciente()));
        return id;
    }

    /**
     * O preço entra CONGELADO: ou o negociado, ou o de tabela no instante em que
     * o item é adicionado. Guardar só o {@code id_procedimento} e resolver o
     * preço na leitura faria um orçamento já aprovado mudar de valor sozinho no
     * dia em que a clínica reajustasse a tabela.
     */
    @Override
    @Transactional
    public long adicionarItem(long idOrcamento, NovoItem item) {
        Alcance alcance = permissoes.exigir(Recurso.ORCAMENTO, Acao.ALTERAR);
        exigirRascunho(carregar(idOrcamento, alcance), "adicionar item");

        ProcedimentoResumo procedimento = clinicas.buscarProcedimento(item.idProcedimento())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "procedimento", item.idProcedimento()));

        BigDecimal valor = item.valorCobrado() == null
                ? procedimento.precoParticular()
                : item.valorCobrado();

        long idItem = orcamentos.inserirItem(idOrcamento, item.idProcedimento(),
                item.dente(), item.face(), valor);
        orcamentos.recalcularTotais(idOrcamento);

        // Os dois valores na trilha: desconto no item é a forma mais comum de
        // furar a tabela de preços sem ninguém ver.
        auditoria.registrarCriacao("orcamento.item", String.valueOf(idItem),
                Map.of("idOrcamento", idOrcamento,
                        "idProcedimento", item.idProcedimento(),
                        "precoTabela", procedimento.precoParticular(),
                        "valorCobrado", valor));
        return idItem;
    }

    @Override
    @Transactional
    public void removerItem(long idOrcamento, long idItem) {
        Alcance alcance = permissoes.exigir(Recurso.ORCAMENTO, Acao.ALTERAR);
        exigirRascunho(carregar(idOrcamento, alcance), "remover item");

        if (orcamentos.removerItem(idOrcamento, idItem) == 0) {
            throw new RecursoNaoEncontradoException("item de orcamento", idItem);
        }
        orcamentos.recalcularTotais(idOrcamento);
        auditoria.registrarExclusao("orcamento.item", String.valueOf(idItem),
                Map.of("idOrcamento", idOrcamento));
    }

    @Override
    @Transactional
    public void enviar(long idOrcamento) {
        carregar(idOrcamento, permissoes.exigir(Recurso.ORCAMENTO, Acao.ALTERAR));
        if (orcamentos.transicionar(idOrcamento, "rascunho", "enviado", false) == 0) {
            throw new TransicaoInvalidaException("Só um orçamento em rascunho pode ser enviado.");
        }
        auditoria.registrarAlteracao("orcamento", String.valueOf(idOrcamento),
                Map.of("status", "rascunho"), Map.of("status", "enviado"));
    }

    /**
     * Aprovação. A transição condicionada no UPDATE é o que impede dois cliques
     * simultâneos de gerarem dois eventos — e, portanto, dois recebíveis para o
     * mesmo tratamento.
     *
     * <p>O evento vai para o outbox na MESMA transação: se o commit falhar, nem
     * a aprovação nem o aviso acontecem. É a diferença entre isto e publicar
     * direto num broker.
     */
    @Override
    @Transactional
    public void aprovar(long idOrcamento) {
        carregar(idOrcamento, permissoes.exigir(Recurso.ORCAMENTO, Acao.ALTERAR));

        if (orcamentos.transicionar(idOrcamento, "enviado", "aprovado", true) == 0) {
            throw new TransicaoInvalidaException(
                    "Só um orçamento enviado pode ser aprovado. Envie-o ao paciente antes.");
        }
        OrcamentoResumo aprovado = orcamentos.buscar(idOrcamento).orElseThrow();

        auditoria.registrarAlteracao("orcamento", String.valueOf(idOrcamento),
                Map.of("status", "enviado"), Map.of("status", "aprovado"));
        outbox.gravar(TiposDeEvento.ORCAMENTO_APROVADO, Map.of(
                "idOrcamento", idOrcamento,
                "idPaciente", aprovado.idPaciente(),
                "idDentista", aprovado.idDentista(),
                "valorFinal", aprovado.valorFinal()));
    }

    @Override
    @Transactional
    public void recusar(long idOrcamento, String motivo) {
        carregar(idOrcamento, permissoes.exigir(Recurso.ORCAMENTO, Acao.ALTERAR));
        if (orcamentos.registrarRecusa(idOrcamento, motivo) == 0) {
            throw new TransicaoInvalidaException(
                    "Só um orçamento em rascunho ou enviado pode ser recusado.");
        }
        auditoria.registrarAlteracao("orcamento", String.valueOf(idOrcamento),
                Map.of("status", "enviado"), Map.of("status", "recusado", "motivo", motivo));
    }

    @Override
    @Transactional
    public void concluirItem(long idOrcamento, long idItem, Long idConsulta) {
        carregar(idOrcamento, permissoes.exigir(Recurso.ORCAMENTO, Acao.ALTERAR));
        if (orcamentos.concluirItem(idItem, idOrcamento, idConsulta) == 0) {
            throw new RecursoNaoEncontradoException("item de orcamento", idItem);
        }
        auditoria.registrarAlteracao("orcamento.item", String.valueOf(idItem),
                Map.of("statusExecucao", "pendente"), Map.of("statusExecucao", "concluido"));
    }

    // ─── apoio ───────────────────────────────────────────────────────────────

    /**
     * Nome do procedimento pela porta de {@code clinicas}, não por JOIN: preço e
     * nome moram naquele módulo, e o JOIN entre schemas é o que impediria
     * extrair qualquer um dos dois depois (camada 7).
     */
    private List<ItemResumo> montarItens(long idOrcamento) {
        List<LinhaItem> linhas = orcamentos.itens(idOrcamento);
        if (linhas.isEmpty()) {
            return List.of();
        }
        Map<Long, ProcedimentoResumo> catalogo = clinicas.listarProcedimentos(false).stream()
                .collect(Collectors.toMap(ProcedimentoResumo::idProcedimento, Function.identity()));

        return linhas.stream()
                .map(l -> {
                    ProcedimentoResumo p = catalogo.get(l.idProcedimento());
                    return new ItemResumo(
                            l.idItem(), l.idProcedimento(),
                            p == null ? null : p.nomeServico(),
                            l.dente(), l.face(), l.valorCobrado(),
                            l.statusExecucao(), l.executadoEm(), l.idConsulta());
                })
                .toList();
    }

    /**
     * Porta única das escritas: carrega a linha e confere o alcance antes de
     * qualquer mutação.
     *
     * <p>Existe porque o alcance vinha sendo obtido e jogado fora nos seis
     * mutadores — {@code exigir()} devolve {@link Alcance}, {@code PROPRIOS}
     * satisfaz {@code permite()}, e nada mais olhava para o {@code id_dentista}.
     * Na prática um dentista aprovava o orçamento de um colega da mesma clínica
     * pelo id, e o evento {@code orcamento.aprovado} saía com o
     * {@code idDentista} do colega — abrindo recebível e comissão para um plano
     * que ele nunca aprovou. O RLS não pega isso: ele fecha o eixo do tenant, e
     * os dois dentistas estão no mesmo.
     *
     * <p>Todo mutador passa por aqui. Um caminho de escrita que não chame este
     * método é o bug voltando.
     */
    private OrcamentoResumo carregar(long idOrcamento, Alcance alcance) {
        OrcamentoResumo o = orcamentos.buscar(idOrcamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("orcamento", idOrcamento));
        exigirAlcance(o, alcance);
        return o;
    }

    /**
     * De quem é o orçamento que está nascendo.
     *
     * <p>Com alcance {@code PROPRIOS} o campo do corpo é ignorado e a autoria
     * vem do contexto: aceitar {@code idDentista} do cliente deixaria um
     * dentista lançar orçamento em nome de outro, e a comissão — que é chaveada
     * por {@code id_dentista} — seria paga à pessoa errada.
     */
    private long autorPermitido(Long idDentistaPedido, Alcance alcance) {
        if (alcance != Alcance.PROPRIOS) {
            return idDentistaPedido;
        }
        Long proprio = dentistaCorrente();
        if (proprio == null) {
            throw new AcessoNegadoException(Recurso.ORCAMENTO, Acao.CRIAR);
        }
        if (idDentistaPedido != null && !proprio.equals(idDentistaPedido)) {
            throw new AcessoNegadoException(Recurso.ORCAMENTO, Acao.CRIAR);
        }
        return proprio;
    }

    private void exigirRascunho(OrcamentoResumo o, String acao) {
        if (!"rascunho".equals(o.status())) {
            // Mexer na composição depois de enviado mudaria o documento que o
            // paciente já viu — e, se já aprovado, o valor que virou recebível.
            throw new TransicaoInvalidaException(
                    "Não é possível %s: o orçamento já saiu de rascunho.".formatted(acao));
        }
    }

    /**
     * O dentista alcança os PRÓPRIOS orçamentos (camada 5). O filtro é por
     * {@code id_dentista}, nunca por {@code id_usuario}: são números diferentes,
     * e trocá-los mostra o orçamento de outra pessoa sempre que coincidirem.
     */
    private List<OrcamentoResumo> filtrarPorAlcance(List<OrcamentoResumo> todos, Alcance alcance) {
        if (alcance != Alcance.PROPRIOS) {
            return todos;
        }
        Long idDentista = dentistaCorrente();
        return idDentista == null
                ? List.of()
                : todos.stream().filter(o -> o.idDentista() == idDentista).toList();
    }

    private void exigirAlcance(OrcamentoResumo o, Alcance alcance) {
        if (alcance == Alcance.PROPRIOS) {
            Long idDentista = dentistaCorrente();
            if (idDentista == null || o.idDentista() != idDentista) {
                // 404 e não 403: confirmar que o orçamento existe já é informação
                // sobre um paciente de outra pessoa.
                throw new RecursoNaoEncontradoException("orcamento", o.idOrcamento());
            }
        }
    }

    /**
     * O id_dentista de quem está falando, resolvido a partir do contexto.
     *
     * <p>Vem do contexto e não de parâmetro de propósito: quem decide "quem é
     * você" é o token, e um id_dentista vindo da requisição seria exatamente a
     * forma de ver a agenda alheia.
     */
    private Long dentistaCorrente() {
        Long idUsuario = ContextoAtual.obter().usuarioId();
        return idUsuario == null ? null : identidade.dentistaDoUsuario(idUsuario).orElse(null);
    }

    public static class TransicaoInvalidaException extends RuntimeException {
        public TransicaoInvalidaException(String mensagem) {
            super(mensagem);
        }
    }
}
