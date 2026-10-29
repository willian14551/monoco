package br.com.monoco

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication(
    exclude =
        [org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration::class]
)
class MonocoBackend {
    @Bean
    fun documentacao() =
        OpenAPI()
            .info(
                Info()
                    .title("Monoco API")
                    .version("0.3.0")
                    .description(
                        "Cadastre-se, faça login e use o token em Authorize. Datas são milissegundos Unix."
                    )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearer",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"),
                    )
            )
            .addSecurityItem(SecurityRequirement().addList("bearer"))
}

fun main(args: Array<String>) {
    runApplication<MonocoBackend>(*args)
}
