package br.com.dentibot.plataforma.idempotencia;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Buffer do corpo da requisição para que ele possa ser lido duas vezes: uma pelo
 * filtro de idempotência, para calcular o hash, e outra pelo controller.
 *
 * <p>O corpo de uma requisição HTTP é um stream de passagem única — depois de
 * lido, acabou. Sem este wrapper, o filtro consumiria o corpo e o controller
 * receberia vazio.
 *
 * <p>{@code ContentCachingRequestWrapper} do Spring não serve aqui: ele guarda o
 * que já foi lido para inspeção posterior, mas não repõe o stream para quem vem
 * depois.
 */
final class RequisicaoComCorpoRelido extends HttpServletRequestWrapper {

    private final byte[] corpo;

    RequisicaoComCorpoRelido(HttpServletRequest original) throws IOException {
        super(original);
        this.corpo = original.getInputStream().readAllBytes();
    }

    byte[] corpo() {
        return corpo;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream fonte = new ByteArrayInputStream(corpo);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return fonte.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("Leitura assíncrona não usada aqui.");
            }

            @Override
            public int read() {
                return fonte.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
