package com.epproject.ShortBrew.config;

import com.epproject.ShortBrew.security.RequireAuth;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static {
        // Ignore the @CurrentUser parameter type (User record) globally in Swagger documentation
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(com.epproject.ShortBrew.model.User.class);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("ShortBrew API")
                        .version("1.0.0")
                        .description("API documentation for the ShortBrew URL shortening and redirection service.\n\n" +
                                     "### Rate Limiting\n" +
                                     "* **Create URL** (`POST /api/v1/urls`): Rate limited by User ID to **10 requests per 60 seconds**.\n" +
                                     "* **Redirection** (`GET /{code}`): Rate limited by IP Address to **30 requests per 60 seconds**.\n" +
                                     "* Returns `429 Too Many Requests` on breach.")
                )
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                );
    }

    @Bean
    public OperationCustomizer customizeRequireAuth() {
        return (operation, handlerMethod) -> {
            boolean hasRequireAuth = handlerMethod.hasMethodAnnotation(RequireAuth.class) ||
                                   handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class);
            if (hasRequireAuth) {
                operation.addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
            }
            return operation;
        };
    }
}
