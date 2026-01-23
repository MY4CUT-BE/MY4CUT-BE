package com.my4cut.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정 클래스.
 * JWT 토큰 인증을 위한 SecurityScheme을 정의하고 전역적으로 적용합니다.
 * 
 * @author koohyunmo
 * @since 2026-01-23
 */
@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI 빈을 등록합니다.
     * JWT 인증을 위한 보안 스키마를 설정합니다.
     * 
     * @return 설정된 OpenAPI 객체
     */
    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "jwtAuth";

        // API 요청 시 SecurityRequirement 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        // SecurityScheme 설정
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(apiInfo())
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    private Info apiInfo() {
        return new Info()
                .title("MY4CUT API Document")
                .description("MY4CUT 프로젝트의 API 문서입니다.")
                .version("1.0.0");
    }
}
