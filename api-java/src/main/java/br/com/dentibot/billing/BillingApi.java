package br.com.dentibot.billing;

import java.util.List;

/**
 * Porta pública do módulo billing — a assinatura da clínica no DentiBot.
 *
 * <p>Não confundir com {@code financeiro}: lá o paciente paga a clínica, aqui a
 * clínica paga a nós. São dinheiros, provedores e obrigações fiscais diferentes,
 * e é por isso que são dois módulos e dois schemas.
 *
 * <p>Nada aqui "marca como pago". O estado da assinatura e das faturas é um
 * espelho do que o provedor diz, sincronizado por webhook — quem decide se o
 * cartão passou é o Stripe, e a única forma de saber é ele contar. Um endpoint
 * que permitisse ativar uma assinatura à mão seria uma porta para usar o produto
 * de graça.
 */
public interface BillingApi {

    List<Plano> listarPlanos();

    /** A assinatura viva da clínica, ou vazio se nunca houve uma. */
    java.util.Optional<Assinatura> assinaturaAtual();

    List<Fatura> listarFaturas();

    /** Consumo do mês corrente contra os limites do plano. */
    UsoDoPlano usoAtual();

    /**
     * Pode admitir mais um profissional?
     *
     * <p>Chamado pela tela de equipe ANTES de criar o usuário. Devolver o
     * resultado em vez de lançar deixa a interface explicar o limite e oferecer
     * o upgrade, em vez de mostrar um erro genérico.
     *
     * <p>A contagem vem por PARÂMETRO e não é buscada aqui: quem a conhece é
     * {@code identidade}, que já depende deste módulo para checar o limite.
     * Buscá-la daqui fecharia um ciclo de construtor que o Spring recusa no
     * boot — e a inversão custa um argumento.
     */
    LimiteDoPlano cabeMaisUmProfissional(int profissionaisAtivos);

    /**
     * Fotografa a contagem de profissionais na competência corrente. Chamado
     * pela equipe sempre que ela muda — é quando o número muda, e é o único
     * momento em que o contador pode ficar certo sem um job varrendo tudo.
     */
    void fotografarProfissionais(int profissionaisAtivos);

    /** Cria a assinatura em trial no cadastro da clínica. */
    long iniciarTrial(String plano);

    /**
     * Espelha o que o provedor informou. Idempotente por
     * {@code idAssinaturaExterna}: o Stripe reentrega webhook, e reagir duas vezes
     * ao mesmo evento não pode produzir dois estados.
     */
    void sincronizarAssinatura(SincronizacaoDeAssinatura sincronizacao);

    /** Idem para fatura, idempotente por {@code idExterno}. */
    void registrarFatura(SincronizacaoDeFatura sincronizacao);
}
