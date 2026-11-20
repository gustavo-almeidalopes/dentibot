package br.com.dentibot.plataforma.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O contrato OpenAPI (ST-20) — só o que o gerador não deduz sozinho.
 *
 * <p>O {@code operationId} vira {@code Controller.metodo}. O padrão do
 * springdoc é o nome do método com sufixo para os repetidos ({@code listar_3}),
 * e o sufixo depende da ordem em que os controllers são varridos: o mesmo
 * código gerava contratos diferentes, e o teste de contrato piscava.
 */
@Configuration
public class ContratoOpenApi {

    @Bean
    public OpenAPI contrato() {
        return new OpenAPI()
                .info(new Info().title("DentiBot API").version("v1")
                        .description("Contrato gerado do código. Autenticação: Bearer do Clerk."))
                .components(new Components().addSecuritySchemes("clerk", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("clerk"));
    }

    @Bean
    public OperationCustomizer operacaoPorController() {
        return (operacao, metodo) -> operacao.operationId(
                metodo.getBeanType().getSimpleName().replace("Controller", "")
                        + "." + metodo.getMethod().getName());
    }
}
