package br.com.dentibot.billing.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.billing.Assinatura;
import br.com.dentibot.billing.BillingApi;
import br.com.dentibot.billing.Fatura;
import br.com.dentibot.billing.LimiteDoPlano;
import br.com.dentibot.billing.Plano;
import br.com.dentibot.billing.SincronizacaoDeAssinatura;
import br.com.dentibot.billing.SincronizacaoDeFatura;
import br.com.dentibot.billing.UsoDoPlano;
import br.com.dentibot.billing.infrastructure.BillingRepositorio;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.outbox.Outbox;
import br.com.dentibot.plataforma.outbox.TiposDeEvento;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingServico implements BillingApi {

    private static final Logger log = LoggerFactory.getLogger(BillingServico.class);

    /** Competência é sempre o dia 1º — a V17 tem CHECK para isso. */
    private static LocalDate competenciaDe(LocalDate dia) {
        return dia.withDayOfMonth(1);
    }

    private final BillingRepositorio billing;
    private final AvaliadorDePermissao permissoes;
    private final AuditoriaApi auditoria;
    private final Outbox outbox;

    public BillingServico(BillingRepositorio billing, AvaliadorDePermissao permissoes,
                          AuditoriaApi auditoria, Outbox outbox) {
        this.billing = billing;
        this.permissoes = permissoes;
        this.auditoria = auditoria;
        this.outbox = outbox;
    }

    /** Catálogo é público dentro do tenant: quem vê a tela de plano precisa dele. */
    @Override
    @Transactional(readOnly = true)
    public List<Plano> listarPlanos() {
        permissoes.exigir(Recurso.BILLING, Acao.LER);
        return billing.listarPlanos();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assinatura> assinaturaAtual() {
        permissoes.exigir(Recurso.BILLING, Acao.LER);
        return billing.buscarViva();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fatura> listarFaturas() {
        permissoes.exigir(Recurso.BILLING, Acao.LER);
        return billing.listarFaturas();
    }

    @Override
    @Transactional(readOnly = true)
    public UsoDoPlano usoAtual() {
        permissoes.exigir(Recurso.BILLING, Acao.LER);

        LocalDate competencia = competenciaDe(LocalDate.now());
        int[] uso = billing.usoDoMes(competencia);
        Plano plano = planoVigente();

        // Os dois números vêm do contador. O de profissionais é fotografia,
        // reescrita pela tela de equipe a cada mudança — que é exatamente
        // quando ele muda.
        return new UsoDoPlano(
                competencia, plano.codigo(),
                uso[0], plano.maxMensagensMes(),
                uso[1], plano.maxProfissionais());
    }

    /**
     * Sem permissão de BILLING de propósito: quem chama é a tela de equipe, e
     * um ADMIN tem as duas — mas a checagem que vale para admitir alguém é a de
     * EQUIPE, feita no {@code IdentidadeServico}. Exigir BILLING aqui negaria a
     * operação para quem a matriz autoriza.
     */
    @Override
    @Transactional(readOnly = true)
    public LimiteDoPlano cabeMaisUmProfissional(int ativos) {
        Plano plano = planoVigente();

        if (ativos < plano.maxProfissionais()) {
            return LimiteDoPlano.liberado(ativos, plano.maxProfissionais());
        }
        return new LimiteDoPlano(false, ativos, plano.maxProfissionais(),
                "O plano %s permite %d profissionais e a clínica já tem %d. Mude de plano para admitir mais."
                        .formatted(plano.nome(), plano.maxProfissionais(), ativos));
    }

    /**
     * Trial no nascimento da clínica. Chamado pelo onboarding, que ainda não tem
     * usuário autenticado — por isso não checa permissão: nesse instante não há
     * papel nenhum para checar.
     */
    @Override
    @Transactional
    public long iniciarTrial(String plano) {
        long id = billing.inserirAssinatura(plano, "stripe", "trial");
        outbox.gravar(TiposDeEvento.ASSINATURA_CRIADA,
                Map.of("idAssinatura", id, "plano", plano, "status", "trial"));
        return id;
    }

    // ─── Espelho do provedor ─────────────────────────────────────────────────

    /**
     * Reage ao que o provedor informou. Sem checagem de permissão porque não há
     * usuário: quem chama é o consumidor do webhook, e a autenticidade da
     * mensagem foi verificada pela ASSINATURA HMAC do provedor, antes do parse —
     * é ela que faz o papel do token aqui.
     */
    @Override
    @Transactional
    public void sincronizarAssinatura(SincronizacaoDeAssinatura s) {
        // Se a clínica ainda estava no trial local, ele sai de cena antes: o
        // índice parcial só admite uma assinatura viva por clínica, e a segunda
        // estouraria unicidade dentro do webhook.
        billing.encerrarTrialSemProvedor(s.idClinica());
        billing.sincronizarAssinatura(s);

        log.info("Assinatura {} da clinica {} sincronizada como {}",
                s.idAssinaturaExterna(), s.idClinica(), s.status());

        outbox.gravar(TiposDeEvento.ASSINATURA_ATUALIZADA,
                Map.of("idClinica", s.idClinica(), "status", s.status(), "plano", s.plano()));

        if ("inadimplente".equals(s.status())) {
            outbox.gravar(TiposDeEvento.ASSINATURA_INADIMPLENTE,
                    Map.of("idClinica", s.idClinica()));
        }
        if ("cancelada".equals(s.status()) || "encerrada".equals(s.status())) {
            outbox.gravar(TiposDeEvento.ASSINATURA_CANCELADA,
                    Map.of("idClinica", s.idClinica(), "status", s.status()));
        }
    }

    @Override
    @Transactional
    public void registrarFatura(SincronizacaoDeFatura f) {
        billing.sincronizarFatura(f);
        log.info("Fatura {} da clinica {} registrada como {}",
                f.idExterno(), f.idClinica(), f.status());
    }

    // ─── apoio ───────────────────────────────────────────────────────────────

    /**
     * O plano vigente, ou o mais barato quando ainda não há assinatura.
     *
     * <p>A clínica recém-criada tem o trial, mas entre o INSERT da clínica e o
     * da assinatura existe um instante sem nenhuma. Cair no plano mais restrito
     * nesse intervalo é o modo certo de errar — o oposto liberaria o limite
     * máximo para quem ainda não assinou nada.
     */
    private Plano planoVigente() {
        Optional<String> codigo = billing.buscarViva().map(Assinatura::plano);
        List<Plano> planos = billing.listarPlanos();
        if (planos.isEmpty()) {
            throw new RecursoNaoEncontradoException("catalogo de planos", "billing.planos");
        }
        return codigo
                .flatMap(billing::buscarPlano)
                // listarPlanos já vem ordenado por preço.
                .orElse(planos.get(0));
    }

    /**
     * Contabiliza mensagens enviadas. Chamado pelo módulo de comunicação quando
     * ele existir; por ora o método já está aqui porque o limite de plano
     * depende dele e o contador não pode nascer depois do uso.
     */
    @Transactional
    public void contabilizarMensagens(long idClinica, int quantas) {
        billing.somarMensagens(idClinica, competenciaDe(LocalDate.now()), quantas);
    }

    /** Fotografa o número de profissionais na competência corrente. */
    @Override
    @Transactional
    public void fotografarProfissionais(int ativos) {
        billing.registrarProfissionais(competenciaDe(LocalDate.now()), ativos);

        Plano plano = planoVigente();
        if (ativos > plano.maxProfissionais()) {
            // Já estourou: não dá para impedir retroativamente, mas dá para
            // avisar quem cobra em vez de descobrir na renovação.
            auditoria.registrarAlteracao("billing.limite", plano.codigo(),
                    Map.of("maxProfissionais", plano.maxProfissionais()),
                    Map.of("profissionaisAtivos", ativos));
            outbox.gravar(TiposDeEvento.LIMITE_PLANO_ATINGIDO,
                    Map.of("plano", plano.codigo(), "limite", plano.maxProfissionais(),
                            "usado", ativos));
        }
    }
}
