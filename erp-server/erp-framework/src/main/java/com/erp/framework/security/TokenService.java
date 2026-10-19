package com.erp.framework.security;

import com.erp.common.core.constant.CommonConstants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录态管理：签发 Token、按请求还原登录用户、登出。
 * <p>
 * <b>为什么不需要 Redis</b>：单体单进程部署，登录态放进程内的 Caffeine 就够
 * （重启应用会导致所有人重新登录，开发阶段完全可接受）。
 * 将来要多实例部署时，把这个类里的 Cache 换成 RedisTemplate 即可，
 * 对外接口不变——这是当初把登录态集中在这一个类里的原因。
 */
@Slf4j
@Component
public class TokenService {

    /** key = tokenId，value = 登录用户 */
    private final Cache<String, LoginUser> loginUserCache;
    private final JwtUtils jwtUtils;
    private final JwtProperties properties;

    public TokenService(JwtUtils jwtUtils, JwtProperties properties) {
        this.jwtUtils = jwtUtils;
        this.properties = properties;
        this.loginUserCache = Caffeine.newBuilder()
                // 上限 2000 个在线用户，超出按 LRU 淘汰（单体应用足够；真到上限说明该上 Redis 了）
                .maximumSize(2_000)
                // 写入后 expireMinutes 过期。活跃用户在到期前会被 getLoginUser 续期
                .expireAfterWrite(properties.getExpireMinutes(), TimeUnit.MINUTES)
                .build();
    }

    /**
     * 登录成功后调用：缓存登录用户并返回 Token 字符串。
     * <p>
     * 调用前请确保 {@code loginUser.password} 已被清空——密码哈希不允许进入缓存和响应体。
     */
    public String createToken(LoginUser loginUser) {
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        loginUser.setTokenId(tokenId);
        long now = System.currentTimeMillis();
        loginUser.setLoginTime(now);
        loginUser.setExpireTime(now + properties.getExpireMinutes() * 60_000L);

        loginUserCache.put(tokenId, loginUser);
        return jwtUtils.createToken(tokenId, loginUser.getUserId(), loginUser.getUsername());
    }

    /**
     * 从请求头还原登录用户。
     *
     * @param authorizationHeader 完整的 Authorization 头，形如 {@code Bearer eyJhbGci...}
     * @return 未登录 / Token 失效时返回 null
     */
    public LoginUser getLoginUser(String authorizationHeader) {
        String token = resolveToken(authorizationHeader);
        Claims claims = jwtUtils.parseToken(token);
        if (claims == null) {
            return null;
        }
        String tokenId = claims.getSubject();
        LoginUser loginUser = loginUserCache.getIfPresent(tokenId);
        if (loginUser == null) {
            // Token 本身有效，但服务端缓存没了：已登出，或应用重启过
            return null;
        }
        refreshIfNeeded(loginUser);
        return loginUser;
    }

    /** 登出 / 踢人：删掉服务端缓存，Token 立刻失效 */
    public void deleteLoginUser(String authorizationHeader) {
        String token = resolveToken(authorizationHeader);
        Claims claims = jwtUtils.parseToken(token);
        if (claims != null) {
            loginUserCache.invalidate(claims.getSubject());
        }
    }

    public void deleteLoginUserByTokenId(String tokenId) {
        if (tokenId != null) {
            loginUserCache.invalidate(tokenId);
        }
    }

    /** 当前在线人数，排查问题用 */
    public long onlineCount() {
        loginUserCache.cleanUp();
        return loginUserCache.estimatedSize();
    }

    /**
     * 滑动续期：快到期时延长。
     * <p>
     * 注意 Caffeine 的 expireAfterWrite 不会因为"读取"而续期，必须重新 put 才会重置计时，
     * 所以这里不能只改 expireTime 字段。
     */
    private void refreshIfNeeded(LoginUser loginUser) {
        long remaining = loginUser.getExpireTime() - System.currentTimeMillis();
        if (remaining > properties.getRefreshThresholdMinutes() * 60_000L) {
            return;
        }
        loginUser.setExpireTime(System.currentTimeMillis() + properties.getExpireMinutes() * 60_000L);
        loginUserCache.put(loginUser.getTokenId(), loginUser);
    }

    /** 去掉 "Bearer " 前缀 */
    private String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return authorizationHeader.substring(CommonConstants.TOKEN_PREFIX.length()).trim();
        }
        // 容错：有些调试工具（curl/Postman）会直接把 token 贴进 header
        return authorizationHeader.trim();
    }
}
