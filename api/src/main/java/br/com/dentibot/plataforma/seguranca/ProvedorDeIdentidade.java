package br.com.dentibot.plataforma.seguranca;

import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduz o sujeito de um token já validado no contexto da aplicação.
 *
 * <p>A interface mora aqui, em {@code plataforma}, e quem a implementa é o
 * módulo {@code identidade}. É inversão de dependência de propósito: o filtro de
 * autenticação é infraestrutura de borda e não deve importar um módulo de
 * domínio, mas a tradução {@code sub → (clínica, papel)} é conhecimento de
 * identidade e de mais ninguém.
 *
 * <p>O que esta interface deixa claro pela assinatura: o token responde só
 * "quem é você". "De qual clínica" e "com qual papel" é consulta a este banco,
 * onde o dado é versionado por migration e coberto por auditoria — e não um
 * claim que um dashboard externo pode reescrever.
 */
public interface ProvedorDeIdentidade {

    /**
     * @param sujeito         o {@code sub} do token validado
     * @param emailVerificado o e-mail do token, <b>somente</b> quando o próprio
     *                        Clerk o marcou como verificado; nulo caso
     *                        contrário. Serve para vincular a conta do Clerk ao
     *                        usuário que já foi cadastrado pela clínica na tela
     *                        de equipe e ainda não tinha {@code sub} — e por
     *                        isso não pode aceitar e-mail não verificado, que
     *                        qualquer um digita.
     * @return o contexto correspondente, ou vazio quando não há conta ativa para
     *         esse sujeito. Conta inexistente e conta bloqueada devolvem a mesma
     *         coisa, pela mesma razão que o login antigo tinha resposta única:
     *         a diferença entre as duas é informação sobre quem existe.
     */
    Optional<ContextoRequisicao> resolver(String sujeito, String emailVerificado, UUID correlacao);
}
