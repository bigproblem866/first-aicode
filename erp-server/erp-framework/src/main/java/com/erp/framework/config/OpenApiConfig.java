package com.erp.framework.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档。访问 <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a>。
 * <p>
 * 原设计文档完全没提接口文档这件事。对 ERP 这种接口多、前后端分离的项目，
 * 没有文档就只能靠翻代码对接口，所以第一天就接上。
 * <p>
 * 这里只做两件事：声明带 Bearer 鉴权的 OpenAPI 元信息 + 按业务模块分组，
 * 其余配置在 application.yml 的 {@code springdoc.*} 下。
 * <p>
 * 调试方式：先调 {@code /api/auth/login} 拿 token，点右上角 Authorize 填入后即可调其它接口。
 * <p>
 * 为什么是 springdoc 原生 UI 而不是 knife4j：见 erp-framework/pom.xml 里的说明，
 * 简单说就是 knife4j 与 Spring Boot 3.5 不兼容且没有可用的新版本。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI erpOpenAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("填登录接口返回的 token，不需要手写 Bearer 前缀");

        return new OpenAPI()
                .info(new Info()
                        .title("ERP 系统接口文档")
                        .version("1.0.0")
                        .description("模块化单体架构，所有接口统一前缀 /api，统一返回 {code,msg,data}。")
                        .contact(new Contact().name("ERP 开发组")))
                .components(new Components().addSecuritySchemes("Bearer", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"));
    }

    // 按模块分组，避免一个页面里几百个接口混在一起。
    // group 名会出现在 URL 上（/v3/api-docs/system），所以用英文；中文名在 UI 下拉里也能看，交给前端分组即可。

    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("system")
                .displayName("系统管理")
                .packagesToScan("com.erp.system")
                .build();
    }

    @Bean
    public GroupedOpenApi mdmApi() {
        return GroupedOpenApi.builder()
                .group("mdm")
                .displayName("主数据")
                .packagesToScan("com.erp.mdm")
                .build();
    }

    @Bean
    public GroupedOpenApi inventoryApi() {
        return GroupedOpenApi.builder()
                .group("inventory")
                .displayName("库存管理")
                .packagesToScan("com.erp.inventory")
                .build();
    }

    @Bean
    public GroupedOpenApi purchaseApi() {
        return GroupedOpenApi.builder()
                .group("purchase")
                .displayName("采购管理")
                .packagesToScan("com.erp.purchase")
                .build();
    }
}
