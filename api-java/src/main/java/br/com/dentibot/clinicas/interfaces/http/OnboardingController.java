package br.com.dentibot.clinicas.interfaces.http;

import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.ClinicaCriada;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
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
            @NotBlank @Pattern(regexp = "^[0-9]{14}$", message = "CNPJ deve ter 14 dígitos, sem pontuação")
            String cnpj,
            @NotBlank @Size(max = 144) String razaoSocial,
            @NotBlank @Size(max = 60) String nomeFantasia,
            String timezone,
            String plano,
            @NotBlank @Size(max = 150) String nomeAdmin,
            @NotBlank @Email @Size(max = 254) String emailAdmin,
            // Mínimo de 12: a política de senha vive aqui e no Zod do frontend,
            // que é gerado do mesmo OpenAPI — uma regra, dois lugares que a
            // derivam.
            @NotBlank @Size(min = 12, max = 200) String senhaAdmin) {
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> cadastrar(@Valid @RequestBody PedidoCadastro pedido) {
        ClinicaCriada criada = onboarding.provisionar(new NovaClinica(
                pedido.cnpj(),
                pedido.razaoSocial(),
                pedido.nomeFantasia(),
                ZoneId.of(pedido.timezone() == null ? "America/Sao_Paulo" : pedido.timezone()),
                Plano.de(pedido.plano() == null ? "solo" : pedido.plano()),
                pedido.nomeAdmin(),
                pedido.emailAdmin(),
                pedido.senhaAdmin()));

        return Map.of("idClinica", criada.idClinica(),
                "idUsuarioAdmin", criada.idUsuarioAdmin());
    }
}
