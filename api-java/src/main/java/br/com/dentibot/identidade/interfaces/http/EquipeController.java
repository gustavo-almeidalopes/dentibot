package br.com.dentibot.identidade.interfaces.http;

import br.com.dentibot.identidade.DentistaResumo;
import br.com.dentibot.identidade.IdentidadeApi;
import br.com.dentibot.identidade.MembroEquipe;
import br.com.dentibot.identidade.NovoMembro;
import br.com.dentibot.plataforma.contexto.Papel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Equipe da clínica: quem trabalha aqui, com qual papel.
 *
 * <p>Não existe endpoint de senha. Admitir alguém cria a linha com e-mail e
 * papel; a pessoa cria a própria conta no Clerk e o vínculo acontece na primeira
 * entrada dela, por e-mail verificado. Um admin escolher a senha de um
 * subordinado seria conseguir entrar como ele — e a trilha de auditoria
 * registraria os atos no nome errado.
 */
@RestController
@RequestMapping("/api/v1/equipe")
public class EquipeController {

    private final IdentidadeApi identidade;

    public EquipeController(IdentidadeApi identidade) {
        this.identidade = identidade;
    }

    @GetMapping
    public List<MembroEquipe> listar() {
        return identidade.listarEquipe();
    }

    /**
     * Os dentistas ativos, para o seletor da agenda. Fica sob {@code /equipe}
     * por ser o mesmo assunto, mas exige AGENDA-LER e não EQUIPE-LER: quem
     * agenda é a recepção, que não administra a equipe.
     */
    @GetMapping("/dentistas")
    public List<DentistaResumo> dentistas() {
        return identidade.listarDentistas();
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> admitir(@Valid @RequestBody NovoMembro novo) {
        long id = identidade.admitirMembro(novo);
        return ResponseEntity.created(URI.create("/api/v1/equipe/" + id))
                .body(Map.of("idUsuario", id));
    }

    public record PedidoAtualizacao(@NotNull Papel papel, @NotBlank String status) {
    }

    @PutMapping("/{idUsuario}")
    public ResponseEntity<Void> atualizar(@PathVariable long idUsuario,
                                          @Valid @RequestBody PedidoAtualizacao pedido) {
        identidade.atualizarMembro(idUsuario, pedido.papel(), pedido.status());
        return ResponseEntity.noContent().build();
    }
}
