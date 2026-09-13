package br.com.dentibot.pacientes.interfaces.http;

import br.com.dentibot.pacientes.NovoPaciente;
import br.com.dentibot.pacientes.application.PreCadastroServico;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ficha de quem criou conta como paciente — inclusive pelo Google, Microsoft ou
 * Apple, que não pedem nada além do e-mail.
 *
 * <p>Fica sob {@code /api/v1/auth} pelo mesmo motivo que o cadastro de clínica:
 * é escrita SEM tenant, e por isso precisa estar na lista de rotas que a
 * {@link br.com.dentibot.plataforma.config.CadeiaDeSeguranca} libera do contexto
 * de clínica. "Público" ali quer dizer "sem tenant", não "sem token" — o
 * {@code sub} do Clerk é obrigatório e é o dono da ficha.
 *
 * <p>Não há {@code GET} par deste {@code POST}, e não é omissão: a V20 não
 * concede {@code SELECT} nesta tabela à aplicação. O fluxo em que uma clínica
 * reivindica a ficha pelo CPF é que vai precisar de leitura, e ele trará junto o
 * privilégio e a regra de quem pode ler o quê.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class PreCadastroController {

    private final PreCadastroServico preCadastro;

    public PreCadastroController(PreCadastroServico preCadastro) {
        this.preCadastro = preCadastro;
    }

    @PostMapping("/pre-cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@Valid @RequestBody NovoPaciente ficha) {
        preCadastro.registrar(ContextoAtual.sujeitoExternoObrigatorio(), ficha);
    }
}
