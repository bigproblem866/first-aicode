package com.erp.framework.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 进程内缓存。替代 Redis：本机没装 Redis，单体单进程部署也用不上。
 * <p>
 * 与 {@code TokenService} 里那个 Caffeine Cache 的分工：
 * <ul>
 *   <li>这里的是「Spring 缓存抽象」的 CacheManager，给 {@code @Cacheable} / {@code @CacheEvict} 用，
 *       放的是字典、参数配置这类可重复计算的只读数据。</li>
 *   <li>TokenService 里的是强类型 Cache，存登录用户，不能随便被 @Cacheable 语义影响。</li>
 * </ul>
 * <p>
 * 缓存名的常量集中在这里，避免各处硬编码字符串导致 {@code @CacheEvict} 清不掉。
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    /** 字典数据缓存，如 sys_dict_data 按 type 分组 */
    public static final String CACHE_DICT = "dict";
    /** 系统参数缓存 */
    public static final String CACHE_CONFIG = "sysConfig";

    /** 全局统一策略：写后 30 分钟过期，最多 1000 条。MVP 阶段够用，不必按缓存名分别调参 */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1_000));
        return cacheManager;
    }
}
