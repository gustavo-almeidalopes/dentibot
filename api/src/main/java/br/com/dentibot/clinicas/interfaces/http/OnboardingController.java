package br.com.dentibot.clinicas.interfaces.http;

import br.com.dentibot.clinicas.application.OnboardingServico;
import br.com.dentibot.clinicas.application.OnboardingServico.ClinicaCriada;
import br.com.dentibot.clinicas.application.OnboardingServico.NovaClinica;
import br.com.dentibot.clinicas.domain.Plano;
import br.com.dentibot.lgpd.AceiteDeCadastro;
import br.com.dentibot.lgpd.LgpdApi;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private final LgpdApi lgpd;

    public OnboardingController(OnboardingServico onboarding, LgpdApi lgpd) {
        this.onboarding = onboarding;
        this.lgpd = lgpd;
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
            /* Sem aceite não há contrato — e sem a linha gravada não há como
               provar o aceite (LGPD art. 8º §2º). */
            @NotNull @Valid AceiteDeCadastro aceites) {
        // Sem emailAdmin: o e-mail do admin é o que o Clerk verificou, lido do
        // token. Vindo do corpo, qualquer conta ocupava o endereço de outra
        // pessoa — identidade.usuarios.email é UNIQUE global, e a dona de
        // verdade tomava 409 ao se cadastrar ou ser convidada. Cliente que
        // ainda manda o campo não quebra: propriedade desconhecida é ignorada.
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
    public Map<String, Long> cadastrar(@Valid @RequestBody PedidoCadastro pedido,
                                       HttpServletRequest req) {
        String subDoClerk = ContextoAtual.sujeitoExternoObrigatorio();
        String email = ContextoAtual.obter().emailVerificado();
        if (email == null) {
            // Os claims email e email_verified vêm da customização do token de
            // sessão no Clerk; ver dentibot.clerk no application.yml.
            throw new IllegalArgumentException(
                    "Confirme o e-mail da sua conta antes de cadastrar a clínica.");
        }

        // ANTES de provisionar, e em transação própria: se o provisionamento
        // falhar (CNPJ repetido), sobra um aceite sem clínica — que continua
        // sendo prova verdadeira do que a pessoa aceitou. O contrário, clínica
        // sem aceite, é o que não pode existir.
        lgpd.registrarAceiteDeCadastro(subDoClerk, "clinica", pedido.aceites(),
                req.getRemoteAddr(), req.getHeader("User-Agent"));

        ClinicaCriada criada = onboarding.provisionar(new NovaClinica(
                pedido.cnpj(),
                pedido.razaoSocial(),
                pedido.nomeFantasia(),
                ZoneId.of(pedido.timezone() == null ? "America/Sao_Paulo" : pedido.timezone()),
                Plano.de(pedido.plano() == null ? "solo" : pedido.plano()),
                pedido.nomeAdmin(),
                email,
                subDoClerk));

        return Map.of("idClinica", criada.idClinica(),
                "idUsuarioAdmin", criada.idUsuarioAdmin());
    }
}
