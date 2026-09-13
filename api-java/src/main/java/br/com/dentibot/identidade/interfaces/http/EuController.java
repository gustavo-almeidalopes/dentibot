package br.com.dentibot.identidade.interfaces.http;

import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.Alcance;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quem é o dono deste token, e o que ele alcança.
 *
 * <p>Existe para o front NÃO reimplementar a matriz em JavaScript. Um menu que
 * decidisse sozinho quais itens mostrar seria a segunda fonte de verdade que a
 * invariante 3 proíbe: divergiria da matriz na primeira vez que alguém mexesse
 * só de um lado, e ninguém perceberia qual das duas está valendo. Aqui a
 * resposta é CALCULADA pelo {@link AvaliadorDePermissao} — o front lê o
 * resultado, nunca a regra.
 *
 * <p>Esconder item de menu não é controle de acesso, é cortesia: poupa o clique
 * que daria 403. Quem digitar a URL continua batendo na checagem do serviço, que
 * é onde a decisão vale. Por isso este endpoint pode ser generoso — ele não
 * concede nada, só descreve.
 *
 * <p>Sem conta nesta base (token do Clerk válido, cadastro de clínica ainda não
 * feito) o contexto não é autenticado e a cadeia responde 401 antes de chegar
 * aqui. É o que o front usa para mandar a pessoa ao /cadastro.
 */
@RestController
@RequestMapping("/api/v1/eu")
public class EuController {

    private final AvaliadorDePermissao permissao;

    public EuController(AvaliadorDePermissao permissao) {
        this.permissao = permissao;
    }

    /**
     * @param papel      papel na clínica; nulo para staff da plataforma
     * @param staffPapel papel na plataforma; nulo para usuário de clínica
     * @param permissoes recurso → ação → alcance, para o token desta requisição
     */
    public record Eu(String papel, String staffPapel,
                     Map<Recurso, Map<Acao, Alcance>> permissoes) {
    }

    @GetMapping
    public Eu eu() {
        ContextoRequisicao ctx = ContextoAtual.obter();

        Map<Recurso, Map<Acao, Alcance>> permissoes = new EnumMap<>(Recurso.class);
        for (Recurso recurso : Recurso.values()) {
            Map<Acao, Alcance> porAcao = new EnumMap<>(Acao.class);
            for (Acao acao : Acao.values()) {
                porAcao.put(acao, permissao.alcance(ctx, recurso, acao));
            }
            permissoes.put(recurso, porAcao);
        }

        return new Eu(
                ctx.papel() == null ? null : ctx.papel().name(),
                ctx.staffPapel() == null ? null : ctx.staffPapel().name(),
                permissoes);
    }
}
