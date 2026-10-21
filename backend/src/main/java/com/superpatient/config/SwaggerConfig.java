package com.superpatient.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme tokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("token")
                .description("JWT Token，登录后获取");

        return new OpenAPI()
                .info(new Info()
                        .title("SuperPatient API")
                        .version("1.0.0")
                        .description("检查指标记录系统 - 后端接口文档")
                        .contact(new Contact()
                                .name("SuperPatient Team")))
                .addSecurityItem(new SecurityRequirement().addList("tokenAuth"))
                .schemaRequirement("tokenAuth", tokenScheme);
    }
}
