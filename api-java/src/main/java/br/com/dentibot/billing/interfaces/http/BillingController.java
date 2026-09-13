package br.com.dentibot.billing.interfaces.http;

import br.com.dentibot.billing.Assinatura;
import br.com.dentibot.billing.BillingApi;
import br.com.dentibot.billing.Fatura;
import br.com.dentibot.billing.LimiteDoPlano;
import br.com.dentibot.billing.Plano;
import br.com.dentibot.billing.UsoDoPlano;
import br.com.dentibot.identidade.IdentidadeApi;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Assinatura da clínica no DentiBot.
 *
 * <p>Só leitura. Não existe endpoint para ativar, mudar de plano ou marcar
 * fatura como paga: quem decide isso é o provedor, e a API só espelha o que ele
 * informa por webhook. Um POST que ativasse assinatura aqui seria a porta para
 * usar o produto de graça.
 *
 * <p>Mudança de plano e cancelamento acontecem no portal do próprio provedor —
 * o front manda o cliente para lá com uma sessão do Customer Portal, e o
 * resultado volta pelo webhook.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingApi billing;
    private final IdentidadeApi identidade;

    public BillingController(BillingApi billing, IdentidadeApi identidade) {
        this.billing = billing;
        this.identidade = identidade;
    }

    @GetMapping("/planos")
    public List<Plano> planos() {
        return billing.listarPlanos();
    }

    @GetMapping("/assinatura")
    public Map<String, Object> assinatura() {
        // Mapa e não 404 quando não há assinatura: "esta clínica não tem
        // assinatura" é uma resposta legítima da tela, não um recurso ausente.
        return billing.assinaturaAtual()
                .<Map<String, Object>>map(a -> Map.of("assinatura", (Object) a))
                .orElseGet(Map::of);
    }

    @GetMapping("/faturas")
    public List<Fatura> faturas() {
        return billing.listarFaturas();
    }

    @GetMapping("/uso")
    public UsoDoPlano uso() {
        return billing.usoAtual();
    }

    /** A tela de equipe chama antes de abrir o formulário de admissão. */
    @GetMapping("/limites/profissionais")
    public LimiteDoPlano limiteDeProfissionais() {
        // A contagem vem de identidade e entra por parâmetro: billing não a
        // busca, para não fechar ciclo com quem já depende dele.
        return billing.cabeMaisUmProfissional(identidade.contarProfissionaisAtivos());
    }
}
