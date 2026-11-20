package br.com.dentibot.lgpd.interfaces.http;

import br.com.dentibot.lgpd.Finalidade;
import br.com.dentibot.lgpd.Preferencia;
import br.com.dentibot.lgpd.application.TitularServico;
import br.com.dentibot.lgpd.application.TitularServico.Painel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * O painel do titular (IA-52, IA-53). Público — a credencial é o link.
 *
 * <p>Tudo POST com o token no corpo: token em URL vai parar em log de acesso,
 * histórico e cabeçalho Referer. No web ele vive no fragmento (#), que o
 * navegador nunca envia.
 */
@RestController
@RequestMapping("/api/v1/titular")
public class TitularController {

    private final TitularServico titular;

    public TitularController(TitularServico titular) {
        this.titular = titular;
    }

    public record ComToken(@NotBlank @Size(max = 64) String token) {
    }

    @PostMapping("/painel")
    public Painel painel(@Valid @RequestBody ComToken pedido) {
        return titular.painel(pedido.token());
    }

    public record NovaPreferencia(@NotBlank @Size(max = 64) String token,
                                  @NotBlank String finalidade, @NotNull Boolean permitido) {
    }

    @PostMapping("/preferencias")
    public List<Preferencia> preferencia(@Valid @RequestBody NovaPreferencia nova) {
        return titular.alterar(nova.token(), Finalidade.de(nova.finalidade()), nova.permitido());
    }

    public record Oposicao(@NotBlank @Size(max = 64) String token, @NotNull Long idEvento,
                           @NotBlank @Size(max = 300) String texto) {
    }

    @PostMapping("/oposicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> opor(@Valid @RequestBody Oposicao oposicao) {
        return Map.of("idSolicitacao",
                titular.opor(oposicao.token(), oposicao.idEvento(), oposicao.texto()));
    }
}
