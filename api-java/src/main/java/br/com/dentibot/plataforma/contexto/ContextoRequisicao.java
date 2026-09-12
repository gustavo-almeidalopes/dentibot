package br.com.dentibot.plataforma.contexto;

import java.util.UUID;

/**
 * Quem está falando com a API nesta requisição, e em nome de qual clínica.
 *
 * <p>Os dois eixos de autorização da camada 5 são mutuamente exclusivos por
 * construção: ou o contexto tem {@code clinicaId + papel} (usuário de clínica),
 * ou tem {@code staffPapel} (staff da plataforma). O construtor recusa qualquer
 * outra combinação — se um token conseguisse carregar os dois, o staff herdaria
 * o acesso clínico de um tenant, que é exatamente o que a separação existe para
 * impedir.
 *
 * @param clinicaId    tenant; nulo para staff da plataforma
 * @param usuarioId    usuário da clínica; nulo para staff
 * @param papel        papel dentro da clínica; nulo para staff
 * @param staffPapel   papel na plataforma; nulo para usuário de clínica
 * @param staffId      staff da plataforma; nulo para usuário de clínica
 * @param correlacaoId atravessa requisição → evento → worker → banco
 * @param modoWorker   processo de background, sem tenant fixo
 */
public record ContextoRequisicao(
        Long clinicaId,
        Long usuarioId,
        Papel papel,
        StaffPapel staffPapel,
        Long staffId,
        UUID correlacaoId,
        boolean modoWorker) {

    public ContextoRequisicao {
        boolean temClinica = clinicaId != null;
        boolean temStaff = staffPapel != null;

        if (temClinica && temStaff) {
            throw new IllegalArgumentException(
                    "Contexto não pode ter clinicaId e staffPapel ao mesmo tempo (camada 5).");
        }
        if (temClinica && papel == null) {
            throw new IllegalArgumentException("Usuário de clínica sem papel definido.");
        }
        if (modoWorker && (temClinica || temStaff)) {
            throw new IllegalArgumentException("Worker não age como usuário nem como staff.");
        }
    }

    public static ContextoRequisicao deClinica(long clinicaId, long usuarioId, Papel papel, UUID correlacaoId) {
        return new ContextoRequisicao(clinicaId, usuarioId, papel, null, null, correlacaoId, false);
    }

    public static ContextoRequisicao deStaff(long staffId, StaffPapel staffPapel, UUID correlacaoId) {
        return new ContextoRequisicao(null, null, null, staffPapel, staffId, correlacaoId, false);
    }

    public static ContextoRequisicao deWorker(UUID correlacaoId) {
        return new ContextoRequisicao(null, null, null, null, null, correlacaoId, true);
    }

    /**
     * Contexto de quem ainda não se autenticou. Não tem tenant, então toda
     * consulta devolve zero linhas — que é o comportamento desejado: o fluxo de
     * login resolve a clínica por função SECURITY DEFINER e só então promove o
     * contexto.
     */
    public static ContextoRequisicao anonimo(UUID correlacaoId) {
        return new ContextoRequisicao(null, null, null, null, null, correlacaoId, false);
    }

    public boolean autenticado() {
        return clinicaId != null || staffPapel != null;
    }
}
