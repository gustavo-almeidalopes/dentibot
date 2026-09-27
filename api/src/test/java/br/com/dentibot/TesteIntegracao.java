package br.com.dentibot;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes que tocam banco de verdade.
 *
 * <p>Postgres real, não H2: metade do que este sistema garante — RLS, FORCE ROW
 * LEVEL SECURITY, EXCLUDE com gist, coluna gerada, partição — simplesmente não
 * existe num banco em memória. Um teste verde contra H2 aqui não significaria
 * nada.
 *
 * <p>O container sobe uma vez por JVM (static) e é reaproveitado entre as
 * classes de teste.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integracao")
public abstract class TesteIntegracao {

    protected static final String SENHA_MIGRADOR = "migrador_teste";
    protected static final String SENHA_APP = "app_teste";

    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16")
                    .withDatabaseName("dentibot")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withInitScript("db/test-init.sql")
                    .withReuse(false);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        // A aplicação conecta como dentibot_app: sem posse de tabela, sem
        // superusuário. É a única forma de o RLS realmente valer.
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", () -> "dentibot_app");
        registro.add("spring.datasource.password", () -> SENHA_APP);

        // O Flyway conecta como migrador, que é dono do schema. Papéis separados
        // (camada 10).
        registro.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registro.add("spring.flyway.user", () -> "dentibot_migrador");
        registro.add("spring.flyway.password", () -> SENHA_MIGRADOR);
    }
}
