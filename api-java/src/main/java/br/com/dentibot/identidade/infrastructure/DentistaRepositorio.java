package br.com.dentibot.identidade.infrastructure;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DentistaRepositorio {

    private final JdbcClient jdbc;

    public DentistaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> idPorUsuario(long idUsuario) {
        return jdbc.sql("""
                        SELECT id_dentista FROM identidade.dentistas
                        WHERE id_usuario = :usuario AND status = 'ativo'
                        """)
                .param("usuario", idUsuario)
                .query(Long.class)
                .optional();
    }
}
