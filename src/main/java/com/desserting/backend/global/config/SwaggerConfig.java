package com.desserting.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Access Token을 입력해주세요 (접두사 'Bearer ' 제외)"));

        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server");

        return new OpenAPI()
                .info(new Info()
                        .title("🍰 Desserting (디저팅) REST API 명세서")
                        .description("2026 단디톤 디저팅 백엔드 API 명세서입니다.\n" +
                                "- 프론트엔드 openapi-typescript 타입 연동\n" +
                                "- RESTful URI v1 지원\n" +
                                "- STOMP 실시간 웹소켓 엔드포인트: /ws")
                        .version("v0.1.0")
                        .contact(new Contact().name("Desserting Team").email("team@desserting.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .servers(List.of(localServer))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
