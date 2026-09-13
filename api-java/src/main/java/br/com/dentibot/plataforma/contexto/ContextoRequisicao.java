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
 * @param clinicaId       tenant; nulo para staff da plataforma
 * @param usuarioId       usuário da clínica; nulo para staff
 * @param papel           papel dentro da clínica; nulo para staff
 * @param staffPapel      papel na plataforma; nulo para usuário de clínica
 * @param staffId         staff da plataforma; nulo para usuário de clínica
 * @param correlacaoId    atravessa requisição → evento → worker → banco
 * @param modoWorker      processo de background, sem tenant fixo
 * @param sujeitoExterno  o {@code sub} do Clerk, quando o token foi validado —
 *                        independente de existir conta correspondente aqui. São
 *                        duas perguntas diferentes: "quem é você" é do Clerk, "o
 *                        que você é nesta clínica" é deste banco, e entre criar
 *                        a conta no Clerk e o cadastro da clínica existe um
 *                        intervalo legítimo em que a primeira tem resposta e a
 *                        segunda não. É esse intervalo que o onboarding ocupa.
 */
public record ContextoRequisicao(
        Long clinicaId,
        Long usuarioId,
        Papel papel,
        StaffPapel staffPapel,
        Long staffId,
        UUID correlacaoId,
        boolean modoWorker,
        String sujeitoExterno) {

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

    public static ContextoRequisicao deClinica(long clinicaId, long usuarioId, Papel papel,
                                               UUID correlacaoId, String sujeitoExterno) {
        return new ContextoRequisicao(clinicaId, usuarioId, papel, null, null, correlacaoId,
                false, sujeitoExterno);
    }

    public static ContextoRequisicao deClinica(long clinicaId, long usuarioId, Papel papel,
                                               UUID correlacaoId) {
        return deClinica(clinicaId, usuarioId, papel, correlacaoId, null);
    }

    public static ContextoRequisicao deStaff(long staffId, StaffPapel staffPapel,
                                             UUID correlacaoId, String sujeitoExterno) {
        return new ContextoRequisicao(null, null, null, staffPapel, staffId, correlacaoId,
                false, sujeitoExterno);
    }

    public static ContextoRequisicao deStaff(long staffId, StaffPapel staffPapel,
                                             UUID correlacaoId) {
        return deStaff(staffId, staffPapel, correlacaoId, null);
    }

    public static ContextoRequisicao deWorker(UUID correlacaoId) {
        return new ContextoRequisicao(null, null, null, null, null, correlacaoId, true, null);
    }

    /**
     * Contexto de quem ainda não se autenticou. Não tem tenant, então toda
     * consulta devolve zero linhas — que é o comportamento desejado: o fluxo de
     * cadastro resolve a clínica por função SECURITY DEFINER e só então promove
     * o contexto.
     */
    public static ContextoRequisicao anonimo(UUID correlacaoId) {
        return new ContextoRequisicao(null, null, null, null, null, correlacaoId, false, null);
    }

    /**
     * Token do Clerk válido, sem conta correspondente neste banco.
     *
     * <p>Não é "autenticado": não há tenant, então o RLS continua devolvendo
     * zero linhas e {@code anyRequest().authenticated()} continua recusando. A
     * única coisa que este contexto habilita é o cadastro de clínica, que
     * precisa saber a qual conta do Clerk vincular o admin que vai nascer.
     */
    public static ContextoRequisicao semConta(String sujeitoExterno, UUID correlacaoId) {
        return new ContextoRequisicao(null, null, null, null, null, correlacaoId, false,
                sujeitoExterno);
    }

    public boolean autenticado() {
        return clinicaId != null || staffPapel != null;
    }
}
