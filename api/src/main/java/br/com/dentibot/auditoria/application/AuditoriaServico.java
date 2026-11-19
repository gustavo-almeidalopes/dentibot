package br.com.dentibot.auditoria.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.auditoria.EventoAuditoria;
import br.com.dentibot.auditoria.FiltroDeTrilha;
import br.com.dentibot.auditoria.infrastructure.AuditoriaRepositorio;
import br.com.dentibot.auditoria.infrastructure.AuditoriaRepositorio.LinhaEvento;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import br.com.dentibot.plataforma.seguranca.Acao;
import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao;
import br.com.dentibot.plataforma.seguranca.Recurso;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaServico implements AuditoriaApi {

    private final AuditoriaRepositorio repositorio;
    private final ObjectMapper json;
    private final AvaliadorDePermissao permissoes;

    public AuditoriaServico(AuditoriaRepositorio repositorio, ObjectMapper json,
                            AvaliadorDePermissao permissoes) {
        this.repositorio = repositorio;
        this.json = json;
        this.permissoes = permissoes;
    }

    @Override
    @Transactional
    public void registrarLeitura(String recurso, String idRecurso) {
        gravar("leitura", recurso, idRecurso, null, null);
    }

    @Override
    @Transactional
    public void registrarCriacao(String recurso, String idRecurso, Object dadosPosteriores) {
        gravar("criacao", recurso, idRecurso, null, serializar(dadosPosteriores));
    }

    @Override
    @Transactional
    public void registrarAlteracao(String recurso, String idRecurso,
                                   Object dadosAnteriores, Object dadosPosteriores) {
        gravar("alteracao", recurso, idRecurso,
                serializar(dadosAnteriores), serializar(dadosPosteriores));
    }

    @Override
    @Transactional
    public void registrarExclusao(String recurso, String idRecurso, Object dadosAnteriores) {
        gravar("exclusao", recurso, idRecurso, serializar(dadosAnteriores), null);
    }

    @Override
    @Transactional
    public List<EventoAuditoria> consultar(FiltroDeTrilha filtro) {
        permissoes.exigir(Recurso.AUDITORIA, Acao.LER);

        List<LinhaEvento> linhas = repositorio.consultar(
                filtro.de(), filtro.ate(), filtro.acao(), filtro.recurso(),
                filtro.idUsuario(), filtro.apos(), filtro.limite());

        gravar("leitura", "auditoria.trilha", null, null,
                serializar(Map.of("de", filtro.de().toString(), "ate", filtro.ate().toString())));

        return linhas.stream()
                .map(l -> new EventoAuditoria(
                        l.idEvento(), l.ocorridoEm(), l.idUsuario(), l.staffPapel(),
                        l.acao(), l.recurso(), l.idRecurso(), l.ipOrigem(), l.correlacaoId()))
                .toList();
    }

    private void gravar(String acao, String recurso, String idRecurso,
                        String antes, String depois) {
        ContextoRequisicao ctx = ContextoAtual.obter();
        repositorio.inserir(
                ContextoAtual.clinicaObrigatoria(),
                ctx.usuarioId(),
                ctx.staffPapel() == null ? null : ctx.staffPapel().valorBanco(),
                acao, recurso, idRecurso, antes, depois, ctx.correlacaoId());
    }

    /**
     * O JSON de antes/depois PODE conter dado clínico: a trilha é o único destino
     * onde isso é correto. Ele não pode passar por log nem telemetria (Sentry,
     * PostHog, Better Stack — invariante 11).
     */
    private String serializar(Object valor) {
        if (valor == null) {
            return null;
        }
        // Sem try/catch: uma linha de trilha com um placeholder no lugar do
        // antes/depois é uma trilha que mente. Falhar aqui desfaz a transação.
        return json.writeValueAsString(valor);
    }
}
