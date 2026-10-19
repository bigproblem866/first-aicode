package com.erp.framework.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置，对应 application.yml 的 {@code erp.jwt.*}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "erp.jwt")
public class JwtProperties {

    /**
     * 签名密钥。<b>长度必须 ≥ 32 字节</b>（256 位），否则 jjwt 会抛 WeakKeyException。
     * <p>
     * 开发环境写在 application-local.yml，生产环境必须用环境变量覆盖：
     * {@code ERP_JWT_SECRET=xxx}。这个值泄露等于任何人都能伪造 Token。
     */
    private String secret;

    /** Token 有效期（分钟）。默认 12 小时，覆盖一个工作日 */
    private long expireMinutes = 720L;

    /** 剩余有效期低于该分钟数时自动续期，避免用户操作到一半被踢出 */
    private long refreshThresholdMinutes = 20L;
}
