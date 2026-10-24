package com.opina.comun;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Añade a Swagger UI el botón "Authorize" para pegar el token de /api/auth/login y probar el panel. */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(title = "opInA API", version = "fase 3",
                description = "Opiniones de clientes para negocios locales. "
                        + "Cuenta de demostración (perfil demo): dueno@barlaplaza.demo / demo-opina-2026"),
        security = @SecurityRequirement(name = "token"))
@SecurityScheme(name = "token", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
