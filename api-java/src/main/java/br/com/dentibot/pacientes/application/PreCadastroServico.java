package br.com.dentibot.pacientes.application;

import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.infrastructure.PreCadastroRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A ficha do paciente que criou conta antes de qualquer clínica conhecê-lo.
 *
 * <p>É o segundo fluxo do sistema que roda sem tenant — o primeiro é o
 * onboarding de clínica. A diferença é que este não cria tenant nenhum: a linha
 * fica fora de qualquer clínica até alguém vinculá-la.
 *
 * <p>{@code @Transactional} não é decoração: o {@link
 * br.com.dentibot.plataforma.tenant.GuardaDeTransacao} recusa acesso a
 * repositório fora de transação.
 */
@Service
public class PreCadastroServico {

    private final PreCadastroRepositorio pre;

    public PreCadastroServico(PreCadastroRepositorio pre) {
        this.pre = pre;
    }

    @Transactional
    public void registrar(String clerkUserId, NovoPaciente ficha) {
        pre.salvar(clerkUserId, ficha);
    }
}
