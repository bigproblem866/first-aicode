package com.erp.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析。基于 jjwt 0.12.x 的新 API
 * （{@code Jwts.SIG.HS256} / {@code parser().verifyWith()}，0.11 的老写法已废弃）。
 * <p>
 * Token 里只放一个 tokenId（JWT 的 subject），真正的用户信息与权限存在服务端
 * 缓存里（见 {@link TokenService}）。这样做的理由：
 * <ul>
 *   <li>权限变更能立刻生效，不用等 Token 过期</li>
 *   <li>支持登出 / 踢人（删缓存即可，纯无状态 JWT 做不到）</li>
 *   <li>Token 体积小，不会因为权限多而超长</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtUtils {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";

    /** HS256 要求密钥至少 256 位 = 32 字节 */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtUtils(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "erp.jwt.secret 未配置或长度不足 " + MIN_SECRET_BYTES + " 字节，"
                            + "请在 application-local.yml 配置或通过环境变量 ERP_JWT_SECRET 注入");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 Token */
    public String createToken(String tokenId, Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + properties.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .subject(tokenId)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析并验签。
     * <p>
     * 解析失败（过期、签名不对、被篡改、格式错误）一律返回 null，<b>不抛异常</b>：
     * 过滤器里抛异常会导致 500，而正确行为是让请求以"未认证"身份继续，
     * 最终由 AuthenticationEntryPoint 返回标准的 401。
     */
    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token 校验失败：{}", e.getMessage());
            return null;
        }
    }
}
