package br.com.dentibot.auditoria.application;

import br.com.dentibot.auditoria.AuditoriaApi;
import br.com.dentibot.auditoria.infrastructure.AuditoriaRepositorio;
import br.com.dentibot.plataforma.contexto.ContextoAtual;
import br.com.dentibot.plataforma.contexto.ContextoRequisicao;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaServico implements AuditoriaApi {

    private final AuditoriaRepositorio repositorio;
    private final ObjectMapper json;

    public AuditoriaServico(AuditoriaRepositorio repositorio, ObjectMapper json) {
        this.repositorio = repositorio;
        this.json = json;
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
    public void registrarAutenticacao(String acao, Long idClinica, Long idUsuario,
                                      String ipOrigem, String userAgent) {
        repositorio.inserirAutenticacao(acao, idClinica, idUsuario, ipOrigem, userAgent,
                ContextoAtual.correlacao());
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
     * O JSON de antes/depois entra na trilha e PODE conter dado clínico — é o
     * ponto: a auditoria é o único destino onde isso é correto e exigido. O que
     * não pode é esse mesmo JSON vazar para Sentry, PostHog ou Better Stack
     * (invariante 11); por isso ele não passa por log nem por telemetria, só
     * por este caminho até o banco da própria clínica.
     */
    private String serializar(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return json.writeValueAsString(valor);
        } catch (Exception e) {
            return "{\"erro\":\"payload não serializável\"}";
        }
    }
}
