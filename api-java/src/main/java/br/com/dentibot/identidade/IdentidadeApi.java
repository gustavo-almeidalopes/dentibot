package br.com.dentibot.identidade;

import br.com.dentibot.plataforma.contexto.Papel;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Porta pública do módulo identidade.
 *
 * <p>É a única coisa que outros módulos enxergam daqui. Nada de
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

    /** O mesmo, indexado por id — conveniência para montar listagens. */
    Map<Long, PessoaResumo> mapaDeResumos(Collection<Long> idsPessoa);

    /**
     * Cadastra uma pessoa física na clínica. Quem tem paciente, dentista ou
     * responsável para registrar chama isto — o dado pessoal mora num lugar só.
     */
    long criarPessoa(String nomeCompleto, String cpf, String telefoneCelular, String email);

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
     * O dentista correspondente a um usuário, quando houver.
     *
     * <p>Necessário porque {@code id_usuario} e {@code id_dentista} são coisas
     * diferentes: nem todo usuário é dentista (recepcionista não é) e nem todo
     * dentista tem login. Quem filtra agenda por "a própria" precisa do
     * id_dentista, não do id_usuario — confundir os dois mostra a agenda de
     * outra pessoa, silenciosamente, sempre que os números coincidirem.
     */
    java.util.Optional<Long> dentistaDoUsuario(long idUsuario);

    /** Quantos profissionais contam para o limite do plano (camada 12). */
    int contarProfissionaisAtivos();

    /** Os dentistas ativos da clínica, para preencher seletor de agenda. */
    List<DentistaResumo> listarDentistas();

    // ─── Tela de equipe ──────────────────────────────────────────────────────

    List<MembroEquipe> listarEquipe();

    /** Cadastra um membro da equipe. Devolve o id do usuário criado. */
    long admitirMembro(NovoMembro novo);

    /** Muda papel e/ou status de um membro. */
    void atualizarMembro(long idUsuario, Papel papel, String status);
}
