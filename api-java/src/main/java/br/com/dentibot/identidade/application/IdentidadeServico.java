package br.com.dentibot.identidade.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.billing.BillingApi;
import br.com.dentibot.billing.LimiteDoPlano;
import br.com.dentibot.identidade.DentistaResumo;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.MembroEquipe;
import br.com.dentibot.identidade.NovoMembro;
import br.com.dentibot.identidade.PessoaResumo;
import br.com.dentibot.identidade.infrastructure.DentistaRepositorio;
import br.com.dentibot.identidade.infrastructure.DentistaRepositorio.LinhaDentista;
import br.com.dentibot.identidade.infrastructure.PessoaRepositorio;
import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio;
import br.com.dentibot.identidade.infrastructure.UsuarioRepositorio.LinhaUsuario;
import br.com.dentibot.plataforma.contexto.Papel;
import br.com.dentibot.plataforma.erro.RecursoNaoEncontradoException;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentidadeServico implements IdentidadeApi {

    /** Os status que {@code identidade.usuarios} aceita, no CHECK da V3. */
    private static final List<String> STATUS_VALIDOS = List.of("ativo", "bloqueado", "desativado");

    private final PessoaRepositorio pessoas;
    private final UsuarioRepositorio usuarios;
    private final DentistaRepositorio dentistas;
    private final AvaliadorDePermissao permissoes;
    private final AuditoriaApi auditoria;
    private final BillingApi billing;

    public IdentidadeServico(PessoaRepositorio pessoas, UsuarioRepositorio usuarios,
                             DentistaRepositorio dentistas, AvaliadorDePermissao permissoes,
                             AuditoriaApi auditoria, BillingApi billing) {
        this.pessoas = pessoas;
        this.usuarios = usuarios;
        this.dentistas = dentistas;
        this.permissoes = permissoes;
        this.auditoria = auditoria;
        this.billing = billing;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PessoaResumo> buscarResumos(Collection<Long> idsPessoa) {
        return pessoas.buscarResumos(idsPessoa);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PessoaResumo> mapaDeResumos(Collection<Long> idsPessoa) {
        return pessoas.buscarResumos(idsPessoa).stream()
                .collect(Collectors.toMap(PessoaResumo::idPessoa, Function.identity()));
    }

    @Override
    @Transactional
    public long criarPessoa(String nomeCompleto, String cpf, String telefoneCelular, String email) {
        return pessoas.inserir(nomeCompleto, cpf, telefoneCelular, email);
    }

    /**
     * {@code REQUIRED} (o padrão): participa da transação de quem chamou. O
     * onboarding precisa que clínica, configurações, pessoa e usuário nasçam
     * juntos ou não nasçam — meia clínica criada, sem ninguém que consiga
     * entrar, é pior que nenhuma.
     */
    @Override
    @Transactional
    public long criarUsuario(String nomeCompleto, String email, Papel papel, String clerkUserId) {
        long idPessoa = pessoas.inserir(nomeCompleto, null, null, email);
        return usuarios.inserir(idPessoa, email, papel, clerkUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> dentistaDoUsuario(long idUsuario) {
        return dentistas.idPorUsuario(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public int contarProfissionaisAtivos() {
        return usuarios.contarProfissionaisAtivos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DentistaResumo> listarDentistas() {
        // Escolher o dentista de uma consulta é parte de usar a agenda, não de
        // administrar a equipe: exigir EQUIPE aqui tiraria o seletor da
        // recepcionista, que é justamente quem agenda.
        permissoes.exigir(Recurso.AGENDA, Acao.LER);

        List<LinhaDentista> linhas = dentistas.listarAtivos();
        Map<Long, PessoaResumo> nomes = mapaDeResumos(
                linhas.stream().map(LinhaDentista::idPessoa).toList());

        return linhas.stream()
                .map(d -> new DentistaResumo(
                        d.idDentista(),
                        nome(nomes, d.idPessoa()),
                        d.croNumero(),
                        d.croUf(),
                        d.especialidade()))
                .toList();
    }

    // ─── Tela de equipe ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MembroEquipe> listarEquipe() {
        permissoes.exigir(Recurso.EQUIPE, Acao.LER);

        List<LinhaUsuario> linhas = usuarios.listar();
        Map<Long, PessoaResumo> nomes = mapaDeResumos(
                linhas.stream().map(LinhaUsuario::idPessoa).toList());
        // Um dentista por usuário, no máximo — uq_dentistas_usuario garante.
        Map<Long, LinhaDentista> porUsuario = dentistas.listar().stream()
                .filter(d -> d.idUsuario() != null)
                .collect(Collectors.toMap(LinhaDentista::idUsuario, Function.identity()));

        return linhas.stream()
                .map(u -> {
                    LinhaDentista d = porUsuario.get(u.idUsuario());
                    return new MembroEquipe(
                            u.idUsuario(),
                            nome(nomes, u.idPessoa()),
                            u.email(),
                            u.papel(),
                            u.status(),
                            u.vinculado(),
                            d == null ? null : d.idDentista(),
                            d == null ? null : d.croNumero(),
                            d == null ? null : d.croUf(),
                            d == null ? null : d.especialidade());
                })
                .toList();
    }

    /**
     * Admite um membro. Sem senha: a pessoa cria a própria conta no Clerk e o
     * vínculo acontece na primeira entrada dela — ver
     * {@link ResolvedorDeAcessoClerk}.
     */
    @Override
    @Transactional
    public long admitirMembro(NovoMembro novo) {
        permissoes.exigir(Recurso.EQUIPE, Acao.CRIAR);

        if (novo.papel() == Papel.DENTISTA && (vazio(novo.croNumero()) || vazio(novo.croUf()))) {
            // CRO é o registro profissional que autoriza o ato clínico. Um
            // "dentista" sem ele assinaria evolução em prontuário sem habilitação
            // registrada — e a evolução é append-only, então não há como
            // desfazer depois.
            throw new IllegalArgumentException("Dentista exige número e UF do CRO.");
        }

        // O limite do plano é checado ANTES de gravar qualquer linha: admitir
        // alguém e só depois descobrir que não cabia deixaria a clínica com um
        // usuário que ela não pode usar e não sabe por quê.
        if (contaParaOLimite(novo.papel())) {
            LimiteDoPlano limite = billing.cabeMaisUmProfissional(
                    usuarios.contarProfissionaisAtivos());
            if (!limite.cabe()) {
                throw new IllegalArgumentException(limite.motivo());
            }
        }

        long idPessoa = pessoas.inserir(novo.nomeCompleto(), null, null, novo.email());
        long idUsuario = usuarios.inserir(idPessoa, novo.email(), novo.papel(), null);

        if (novo.papel() == Papel.DENTISTA) {
            dentistas.inserir(idPessoa, idUsuario, novo.croNumero(),
                    novo.croUf().toUpperCase(java.util.Locale.ROOT), novo.especialidade());
        }

        billing.fotografarProfissionais(usuarios.contarProfissionaisAtivos());
        auditoria.registrarCriacao("usuario", String.valueOf(idUsuario),
                Map.of("email", novo.email(), "papel", novo.papel().valorBanco()));
        return idUsuario;
    }

    @Override
    @Transactional
    public void atualizarMembro(long idUsuario, Papel papel, String status) {
        permissoes.exigir(Recurso.EQUIPE, Acao.ALTERAR);

        if (!STATUS_VALIDOS.contains(status)) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }
        LinhaUsuario antes = usuarios.buscar(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("usuario", idUsuario));

        boolean deixaDeSerAdminAtivo =
                antes.papel() == Papel.ADMIN && antes.status().equals("ativo")
                        && (papel != Papel.ADMIN || !"ativo".equals(status));
        if (deixaDeSerAdminAtivo && usuarios.contarAdminsAtivos() <= 1) {
            // Sem esta guarda, o último admin consegue se rebaixar e a clínica
            // fica sem ninguém capaz de administrar a própria equipe — um estado
            // do qual só se sai por suporte mexendo no banco.
            throw new IllegalArgumentException(
                    "A clínica ficaria sem administrador ativo. Promova outro antes.");
        }

        usuarios.atualizar(idUsuario, papel, status);
        // Desativar alguém libera vaga no plano; promover a dentista ocupa uma.
        // Refotografar aqui é o que mantém o contador honesto sem um job.
        billing.fotografarProfissionais(usuarios.contarProfissionaisAtivos());
        // O dentista acompanha: a V3 só aceita 'ativo'/'inativo' nessa tabela.
        dentistas.atualizarStatusPorUsuario(idUsuario, "ativo".equals(status) ? "ativo" : "inativo");

        auditoria.registrarAlteracao("usuario", String.valueOf(idUsuario),
                Map.of("papel", antes.papel().valorBanco(), "status", antes.status()),
                Map.of("papel", papel.valorBanco(), "status", status));
    }

    private static String nome(Map<Long, PessoaResumo> nomes, long idPessoa) {
        PessoaResumo p = nomes.get(idPessoa);
        return p == null ? null : p.nomeCompleto();
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Quem conta para o limite do plano: admin e dentista, os mesmos que o
     * {@code contarProfissionaisAtivos} soma. Recepção, financeiro e auxiliar
     * não são "profissionais" para efeito de cobrança — e contá-los faria uma
     * clínica de um dentista com duas recepcionistas estourar o plano Solo.
     */
    private static boolean contaParaOLimite(Papel papel) {
        return papel == Papel.ADMIN || papel == Papel.DENTISTA;
    }
}
