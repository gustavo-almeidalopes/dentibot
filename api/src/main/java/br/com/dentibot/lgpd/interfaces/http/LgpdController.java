package br.com.dentibot.lgpd.interfaces.http;

import br.com.dentibot.lgpd.Consentimento;
import br.com.dentibot.lgpd.LgpdApi;
import br.com.dentibot.lgpd.NovaSolicitacao;
import br.com.dentibot.lgpd.NovoConsentimento;
import br.com.dentibot.lgpd.NovoTermo;
import br.com.dentibot.lgpd.RespostaSolicitacao;
import br.com.dentibot.lgpd.SolicitacaoTitular;
import br.com.dentibot.lgpd.Termo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * LGPD: termos versionados, consentimentos e direitos do titular.
 *
 * <p>Tudo aqui exige o recurso {@code LGPD}, que na matriz da camada 5 só o
 * ADMIN tem. Isso significa, hoje, que a recepcionista que colhe a assinatura no
 * balcão não consegue registrar o consentimento — ver a nota em
 * {@link #registrarConsentimento}.
 */
@RestController
@RequestMapping("/api/v1/lgpd")
public class LgpdController {

    private final LgpdApi lgpd;

    public LgpdController(LgpdApi lgpd) {
        this.lgpd = lgpd;
    }

    // ─── Termos ──────────────────────────────────────────────────────────────

    @GetMapping("/termos")
    public List<Termo> termos(@RequestParam(defaultValue = "false") boolean somenteAtivos) {
        return lgpd.listarTermos(somenteAtivos);
    }

    @PostMapping("/termos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> publicar(@Valid @RequestBody NovoTermo novo) {
        return Map.of("idTermo", lgpd.publicarTermo(novo));
    }

    /**
     * DELETE que desativa em vez de apagar. O verbo descreve a intenção de quem
     * chama ("tire este termo de circulação"); a tabela é imutável e o texto
     * continua provando o que provava para quem já assinou.
     */
    @DeleteMapping("/termos/{id}")
    public ResponseEntity<Void> desativar(@PathVariable long id) {
        lgpd.desativarTermo(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Consentimentos ──────────────────────────────────────────────────────

    @GetMapping("/pacientes/{idPaciente}/consentimentos")
    public List<Consentimento> consentimentos(@PathVariable long idPaciente) {
        return lgpd.consentimentosDoPaciente(idPaciente);
    }

    /**
     * O IP e o user-agent vêm da requisição, nunca do corpo — um IP enviado pelo
     * cliente é um IP que o cliente escolheu, e o que a V16 exige NOT NULL aqui
     * é prova de quem aceitou de onde.
     *
     * <p>{@code getRemoteAddr()} atrás de proxy devolve o IP do proxy; o
     * {@code forward-headers-strategy: framework} do application.yml faz o
     * Spring resolver o X-Forwarded-For antes de chegar aqui.
     */
    @PostMapping("/consentimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> registrarConsentimento(@Valid @RequestBody NovoConsentimento novo,
                                                    HttpServletRequest req) {
        return Map.of("idConsentimento",
                lgpd.registrarConsentimento(novo, req.getRemoteAddr(),
                        req.getHeader("User-Agent")));
    }

    @DeleteMapping("/consentimentos/{id}")
    public ResponseEntity<Void> revogar(@PathVariable long id) {
        lgpd.revogarConsentimento(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Solicitações do titular ─────────────────────────────────────────────

    @GetMapping("/solicitacoes")
    public List<SolicitacaoTitular> solicitacoes(@RequestParam(required = false) String status) {
        return lgpd.listarSolicitacoes(status == null || status.isBlank() ? null : status);
    }

    @PostMapping("/solicitacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> abrir(@Valid @RequestBody NovaSolicitacao nova) {
        return Map.of("idSolicitacao", lgpd.abrirSolicitacao(nova));
    }

    @PostMapping("/solicitacoes/{id}/responder")
    public ResponseEntity<Void> responder(@PathVariable long id,
                                          @Valid @RequestBody RespostaSolicitacao resposta) {
        lgpd.responderSolicitacao(id, resposta);
        return ResponseEntity.noContent().build();
    }
}
