package br.com.dentibot.identidade;

import br.com.dentibot.plataforma.contexto.Papel;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A única coisa que outros módulos enxergam daqui. Nada de
 * {@code br.com.dentibot.identidade.infrastructure} fora deste módulo, e nada de
 * {@code JOIN} contra o schema {@code identidade} vindo de outro schema — as
 * duas regras têm teste (ArchUnit para o import, varredura de SQL para o JOIN).
 *
 * <p>É por isso que {@code agenda} monta a listagem chamando
 * {@link #buscarResumos(Collection)} em vez de juntar {@code consultas} com
 * {@code pessoas}: o JOIN funcionaria hoje e é exatamente o que impediria
 * extrair a agenda depois.
 */
public interface IdentidadeApi {

    /** Dados mínimos de exibição de uma pessoa. Sem CPF, sem endereço. */
    List<PessoaResumo> buscarResumos(Collection<Long> idsPessoa);

    Map<Long, PessoaResumo> mapaDeResumos(Collection<Long> idsPessoa);

    /**
     * Cadastra uma pessoa física na clínica. Quem tem paciente, dentista ou
     * responsável para registrar chama isto — o dado pessoal mora num lugar só.
     */
    long criarPessoa(DadosPessoais dados);

    /**
     * Cria pessoa + usuário com acesso.
     *
     * <p>{@code clerkUserId} nulo é o caso normal da tela de equipe: a clínica
     * cadastra a pessoa, e o vínculo com a conta do Clerk acontece na primeira
     * entrada dela, por e-mail verificado. O onboarding é a exceção — lá quem
     * cadastra JÁ está autenticado, e o {@code sub} é conhecido na hora.
     */
    long criarUsuario(String nomeCompleto, String email, Papel papel, String clerkUserId);

    /**
     * O dentista de quem está fazendo a requisição, quando ele for um.
     *
     * <p>Vem do usuário do contexto, nunca de parâmetro: quem decide "quem é
     * você" é o token. E {@code id_usuario} não é {@code id_dentista} (nem todo
     * usuário é dentista, nem todo dentista tem login) — confundir os dois
     * mostra o registro de outra pessoa sempre que os números coincidirem.
     */
    java.util.Optional<Long> dentistaCorrente();

    /** Quantos profissionais contam para o limite do plano (camada 12). */
    int contarProfissionaisAtivos();

    /** Os dentistas ativos da clínica, para preencher seletor de agenda. */
    List<DentistaResumo> listarDentistas();

    // ─── Tela de equipe ──────────────────────────────────────────────────────

    List<MembroEquipe> listarEquipe();

    /** Cadastra um membro da equipe. Devolve o id do usuário criado. */
    long admitirMembro(NovoMembro novo);

    void atualizarMembro(long idUsuario, Papel papel, String status);
}
