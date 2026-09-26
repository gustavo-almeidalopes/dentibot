package br.com.dentibot.clinicas.interfaces.http;

import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.ClinicaCriada;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Criação de clínica (auto-cadastro a partir da landing).
 *
 * <p>Fica sob {@code /api/v1/auth} porque é o único endpoint de escrita que roda
 * SEM tenant — e por isso precisa estar na lista de rotas públicas da
 * {@link br.com.dentibot.plataforma.config.CadeiaDeSeguranca}. É também por isso
 * que ele cai no limite mais apertado de rate limit por IP: é um endpoint que
 * cria linha no banco sem ninguém autenticado do outro lado.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class OnboardingController {

    private final OnboardingServico onboarding;

    public OnboardingController(OnboardingServico onboarding) {
        this.onboarding = onboarding;
    }

    public record PedidoCadastro(
            /* CNPJ da clínica ou CPF do dentista autônomo — os dois nascem
               tenant, e o autônomo não tem CNPJ para dar. O CHECK da V20 repete
               esta regra no banco, que é onde ela vale mesmo. */
            @NotBlank
            @Pattern(regexp = "^([0-9]{11}|[0-9]{14})$",
                     message = "informe CPF (11 dígitos) ou CNPJ (14), sem pontuação")
            String cnpj,
            @NotBlank @Size(max = 144) String razaoSocial,
            @NotBlank @Size(max = 60) String nomeFantasia,
            String timezone,
            String plano,
            @NotBlank @Size(max = 150) String nomeAdmin,
            @NotBlank @Email @Size(max = 254) String emailAdmin) {
    }

    /**
     * Público na cadeia de segurança, mas não anônimo: exige um token do Clerk
     * válido, porque é o {@code sub} dele que vira o dono da clínica.
     *
     * <p>"Público" aqui quer dizer só que a cadeia não pede contexto de tenant —
     * e não poderia, já que o tenant é o que este endpoint cria. A checagem que
     * importa é a de baixo, e ela é explícita de propósito: sem ela qualquer um
     * criaria clínicas sem conta nenhuma associada.
     */
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> cadastrar(@Valid @RequestBody PedidoCadastro pedido) {
        String subDoClerk = ContextoAtual.sujeitoExternoObrigatorio();

        ClinicaCriada criada = onboarding.provisionar(new NovaClinica(
                pedido.cnpj(),
                pedido.razaoSocial(),
                pedido.nomeFantasia(),
                ZoneId.of(pedido.timezone() == null ? "America/Sao_Paulo" : pedido.timezone()),
                Plano.de(pedido.plano() == null ? "solo" : pedido.plano()),
                pedido.nomeAdmin(),
                pedido.emailAdmin(),
                subDoClerk));

        return Map.of("idClinica", criada.idClinica(),
                "idUsuarioAdmin", criada.idUsuarioAdmin());
    }
}
