package br.com.dentibot.pacientes.application;

import br.com.dentibot.lgpd.AceiteDeCadastro;
import br.com.dentibot.lgpd.LgpdApi;
import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.infrastructure.PreCadastroRepositorio;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final PreCadastroRepositorio pre;
    private final LgpdApi lgpd;

    public PreCadastroServico(PreCadastroRepositorio pre, LgpdApi lgpd) {
        this.pre = pre;
        this.lgpd = lgpd;
    }

    /**
     * Ficha e aceite na MESMA transação: ficha gravada sem a prova do aceite é
     * exatamente o tratamento que a LGPD não permite.
     *
     * <p>As duas checagens repetem o front de propósito — o front é sugestão,
     * a regra vale aqui.
     */
    @Transactional
    public void registrar(String clerkUserId, NovoPaciente ficha, AceiteDeCadastro aceite,
                          String ipOrigem, String userAgent) {
        if (ficha.anamnese() != null && !Boolean.TRUE.equals(aceite.dadosDeSaude())) {
            // Art. 11 I: dado de saúde sem consentimento específico não entra.
            throw new IllegalArgumentException(
                    "A triagem de saúde só é guardada com a sua autorização específica.");
        }
        boolean menor = ficha.dataNascimento() != null
                && ficha.dataNascimento().plusYears(18).isAfter(LocalDate.now(BRASILIA));
        if (menor && !Boolean.TRUE.equals(aceite.responsavelLegal())) {
            // Art. 14 §1º: o consentimento do menor é dado pelo responsável.
            throw new IllegalArgumentException(
                    "Paciente menor de idade: o responsável legal precisa consentir.");
        }
        lgpd.registrarAceiteDeCadastro(clerkUserId, "paciente", aceite, ipOrigem, userAgent);
        pre.salvar(clerkUserId, ficha);
    }
}
