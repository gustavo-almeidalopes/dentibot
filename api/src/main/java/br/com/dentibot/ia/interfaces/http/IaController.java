package br.com.dentibot.ia.interfaces.http;

import br.com.dentibot.ia.ConfiguracaoDeIa;
import br.com.dentibot.ia.ConsumoDeIa;
import br.com.dentibot.ia.application.GatewayDeIa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia")
public class IaController {

    private final GatewayDeIa gateway;

    public IaController(GatewayDeIa gateway) {
        this.gateway = gateway;
    }

    /** IA-59: o que a clínica gastou com IA no mês (AAAA-MM; padrão, o atual). */
    @GetMapping("/consumo")
    public List<ConsumoDeIa> consumo(@RequestParam(required = false) String mes) {
        return gateway.consumo(mes == null ? YearMonth.now(ZoneOffset.UTC) : YearMonth.parse(mes));
    }

    @GetMapping("/configuracao")
    public ConfiguracaoDeIa configuracao() {
        return gateway.configuracao();
    }

    public record NovaConfiguracao(@NotNull @Size(max = 20) List<String> recursosDesligados,
                                   @NotNull @PositiveOrZero BigDecimal cotaMensalUsd) {
    }

    @PutMapping("/configuracao")
    public ConfiguracaoDeIa salvar(@Valid @RequestBody NovaConfiguracao nova) {
        return gateway.salvarConfiguracao(nova.recursosDesligados(), nova.cotaMensalUsd());
    }

    public record Confirmacao(@NotNull Boolean aceita, @Size(max = 120) String registro) {
    }

    /** A decisão humana sobre a sugestão — uma por chamada. */
    @PostMapping("/chamadas/{idChamada}/confirmacao")
    @ResponseStatus(HttpStatus.CREATED)
    public void confirmar(@PathVariable long idChamada, @Valid @RequestBody Confirmacao c) {
        gateway.confirmar(idChamada, c.aceita(), c.registro());
    }
}
