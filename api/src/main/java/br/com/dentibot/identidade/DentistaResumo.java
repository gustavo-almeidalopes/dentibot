package br.com.dentibot.identidade;

/** Um dentista ativo, para preencher seletor de agenda e de evolução. */
public record DentistaResumo(
        long idDentista,
        String nomeCompleto,
        String croNumero,
        String croUf,
        String especialidade) {
}
