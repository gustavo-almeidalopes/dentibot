package br.com.dentibot.plataforma.config;

import br.com.dentibot.plataforma.tenant.GerenciadorTransacaoComTenant;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Troca o gerenciador de transação padrão do Boot pelo que injeta o contexto de
 * tenant. Este bean é a diferença entre "temos RLS configurado" e "o RLS está
 * valendo": sem ele nenhuma política encontra tenant e todo o sistema devolve
 * zero linhas.
 */
@Configuration(proxyBeanMethods = false)
public class BancoConfig {

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new GerenciadorTransacaoComTenant(dataSource);
    }
}
