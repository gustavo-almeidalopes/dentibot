package br.com.dentibot.copiloto;

import br.com.dentibot.plataforma.seguranca.AvaliadorDePermissao.AcessoNegadoException;
import java.util.function.Supplier;

/**
 * Uma parte do resumo, ou o aviso de que quem pergunta não pode vê-la.
 *
 * <p>{@code permitido=false} é diferente de {@code dados=null}: o primeiro diz
 * "você não vê isto", o segundo "não há nada aqui". A tela precisa dos dois —
 * "sem alergia registrada" e "sem acesso à ficha clínica" não são o mesmo aviso.
 */
public record Secao<T>(boolean permitido, T dados) {

    public static <T> Secao<T> ler(Supplier<T> leitura) {
        try {
            return new Secao<>(true, leitura.get());
        } catch (AcessoNegadoException e) {
            return new Secao<>(false, null);
        }
    }
}
