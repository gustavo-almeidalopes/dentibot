package br.com.dentibot.identidade;

import br.com.dentibot.plataforma.contexto.Papel;

/**
 * Um membro da equipe, como a tela de equipe o mostra.
 *
 * <p>{@code vinculado} é a resposta à pergunta que a tela precisa fazer e que
 * antes não existia: "esta pessoa já entrou alguma vez?". Com o Clerk, o
 * cadastro pela clínica cria a linha sem conta associada, e o vínculo acontece
 * na primeira entrada. Sem este campo, um admin que cadastrou alguém não teria
 * como distinguir "ainda não entrou" de "está com problema para entrar".
 *
 * <p>Sem CPF e sem endereço, como {@link PessoaResumo}: a tela de equipe lista
 * quem trabalha na clínica, não o cadastro pessoal de cada um.
 */
public record MembroEquipe(
        long idUsuario,
        String nomeCompleto,
        String email,
        Papel papel,
        String status,
        boolean vinculado,
        Long idDentista,
        String croNumero,
        String croUf,
        String especialidade) {
}
