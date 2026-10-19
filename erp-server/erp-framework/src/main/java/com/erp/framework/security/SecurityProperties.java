package com.erp.framework.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关配置，对应 application.yml 的 {@code erp.security.*}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "erp.security")
public class SecurityProperties {

    /**
     * 免登录路径（Ant 风格）。放在 yml 里而不是硬编码在 SecurityConfig，
     * 是为了新增免登录接口（比如将来的回调地址、大屏接口）不用改代码重新打包。
     * <p>
     * 注意：路径是「应用内真实路径」，即包含 /api 前缀，
     * 例如 {@code /api/auth/login}（本工程 Controller 直接声明 /api 开头，见设计文档）。
     */
    private List<String> permitUrls = new ArrayList<>();
}
