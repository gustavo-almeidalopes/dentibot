package br.com.dentibot.auditoria.interfaces.http;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.auditoria.EventoAuditoria;
import br.com.dentibot.auditoria.FiltroDeTrilha;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trilha de auditoria da própria clínica.
 *
 * <p>O módulo escrevia desde a V5 e não tinha por onde ser lido: a LGPD e a
 * CFO-226 exigem a trilha, e uma trilha que ninguém consegue consultar cumpre a
 * letra da norma e nada do propósito dela.
 *
 * <p>Só ADMIN alcança, e só para LER — nem o admin altera a trilha. Isso está na
 * matriz do {@code AvaliadorDePermissao} e é imposto no serviço; aqui não há
 * segunda checagem, que a invariante 3 proíbe.
 */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private static final int LIMITE_MAXIMO = 200;
    private static final Duration JANELA_PADRAO = Duration.ofDays(30);

    private final AuditoriaApi auditoria;

    public AuditoriaController(AuditoriaApi auditoria) {
        this.auditoria = auditoria;
    }

    /**
     * A faixa de datas tem default em vez de ser obrigatória na assinatura, mas
     * nunca fica ausente: a tabela é particionada por {@code ocorrido_em} e uma
     * consulta sem intervalo varre todas as partições.
     */
    @GetMapping
    public List<EventoAuditoria> consultar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String recurso,
            @RequestParam(required = false) Long idUsuario,
            @RequestParam(defaultValue = "0") long apos,
            @RequestParam(defaultValue = "100") int limite) {

        Instant fim = ate == null ? Instant.now() : ate;
        Instant inicio = de == null ? fim.minus(JANELA_PADRAO) : de;

        return auditoria.consultar(new FiltroDeTrilha(
                inicio, fim, vazioViraNulo(acao), vazioViraNulo(recurso), idUsuario,
                apos, Math.min(Math.max(limite, 1), LIMITE_MAXIMO)));
    }

    /** Um filtro em branco vindo do formulário é "sem filtro", não "igual a ''". */
    private static String vazioViraNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
