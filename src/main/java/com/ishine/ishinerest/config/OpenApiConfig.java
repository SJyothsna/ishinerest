package com.ishine.ishinerest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("iShine REST API")
                        .version("1.0")
                        .description("API documentation for iShine Learning Platform - Multi-role user management system with support for Students, Parents, Teachers, and Admins")
                        .contact(new Contact()
                                .name("iShine Support")
                                .email("support@ishine.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ));
    }
}

// Made with Bob
