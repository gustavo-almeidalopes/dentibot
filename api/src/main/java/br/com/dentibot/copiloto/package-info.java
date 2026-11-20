/**
 * Copiloto (Documento 03, sub-projeto A): o que o sistema já sabe, reunido no
 * momento em que alguém precisa — sem modelo de IA, sem dado novo.
 *
 * <p>Não tem schema nem SQL. Tudo o que ele mostra vem das portas públicas dos
 * outros módulos, e cada porta aplica a matriz de permissão de quem pergunta.
 * É por isso que o mesmo resumo serve à recepção e ao dentista: a parte clínica
 * volta como "sem acesso" para quem não pode vê-la, e o resto chega igual.
 *
 * <p>Os métodos do serviço não são {@code @Transactional} de propósito: uma
 * porta que nega acesso lança exceção, e dentro de uma transação externa isso a
 * marcaria como rollback-only — o resumo inteiro falharia no commit por causa de
 * uma seção que só devia vir vazia.
 */
package br.com.dentibot.copiloto;
