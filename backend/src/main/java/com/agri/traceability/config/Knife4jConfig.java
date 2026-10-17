package com.agri.traceability.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

/**
 * Knife4j(Swagger) API文档配置
 * 文档地址: http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.agri.traceability.controller"))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts())
                .groupName("全部接口");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("农产品溯源销售系统 API文档")
                .description("基于Spring Boot + MyBatis-Plus的农产品溯源销售平台\n\n"
                        + "角色说明:\n"
                        + "- ADMIN  系统管理员\n"
                        + "- MERCHANT 商家\n"
                        + "- FARMER  农户\n"
                        + "- CONSUMER 消费者\n\n"
                        + "认证方式: 在请求头中携带 Authorization: Bearer {token}")
                .version("1.0.0")
                .build();
    }

    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> list = new ArrayList<>();
        list.add(new ApiKey("Authorization", "Authorization", "header"));
        return list;
    }

    private List<SecurityContext> securityContexts() {
        List<SecurityContext> list = new ArrayList<>();
        list.add(SecurityContext.builder()
                .securityReferences(securityReferences())
                .forPaths(PathSelectors.regex("^(?!/api/user/(login|register)).*"))
                .build());
        return list;
    }

    private List<SecurityReference> securityReferences() {
        List<SecurityReference> list = new ArrayList<>();
        list.add(new SecurityReference("Authorization",
                new AuthorizationScope[]{new AuthorizationScope("global", "全局授权")}));
        return list;
    }
}
