package com.erp.framework.security;

import com.erp.common.core.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 配置（无状态 + JWT）。
 * <p>
 * 关键点：
 * <ul>
 *   <li>写成 {@code SecurityFilterChain} Bean 的 lambda 风格——Spring Security 6 里
 *       {@code WebSecurityConfigurerAdapter} 已被移除，网上 Boot 2.x 的老教程不能照抄</li>
 *   <li>{@code @EnableMethodSecurity} 让 {@code @PreAuthorize} 生效</li>
 *   <li>显式关掉 formLogin / httpBasic / logout：不关的话未认证请求会被重定向到
 *       Spring 自带的登录页（HTML），而不是走我们的 401 JSON</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final SecurityProperties securityProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] permitUrls = securityProperties.getPermitUrls().toArray(String[]::new);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // 纯 Token 鉴权，服务端不创建 HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(registry -> {
                    if (permitUrls.length > 0) {
                        registry.requestMatchers(permitUrls).permitAll();
                    }
                    // 其余一律要求已认证；细粒度权限由 @PreAuthorize 控制
                    registry.anyRequest().authenticated();
                })
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码加密器。
     * <p>
     * <b>必须用 BCrypt</b>：数据库里存的是哈希，登录校验用
     * {@code passwordEncoder.matches(明文, 哈希)}，绝不能用 equals 比对。
     * 种子数据里的 admin 密码就是用它算出来的（见 erp-system 的 SysDataInitializer）。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 暴露 AuthenticationManager 供登录接口调用（erp-system 的 SysLoginService）。
     * <p>
     * 它会自动使用容器里的 {@code UserDetailsService} 实现（在 erp-system 中提供）
     * 和上面的 PasswordEncoder，不需要手动 new DaoAuthenticationProvider。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 跨域配置。
     * <p>
     * 开发时前端走 Vite 代理（同源），其实用不到 CORS；保留是为了能用
     * Swagger/Postman、或前端直连 8080 调试。
     * <p>
     * 注意 {@code allowCredentials(true)} 与 {@code allowedOrigins("*")} 不能同时用
     * （Spring 会直接抛异常），必须用 {@code allowedOriginPatterns}。
     * 生产环境请把来源收窄到真实域名。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addExposedHeader(CommonConstants.TOKEN_HEADER);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
