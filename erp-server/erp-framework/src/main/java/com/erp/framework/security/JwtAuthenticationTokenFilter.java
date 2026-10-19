package com.erp.framework.security;

import com.erp.common.core.constant.CommonConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从请求头解析 Token 并把登录用户放进 SecurityContext。
 * <p>
 * 设计要点：<b>解析失败不抛异常、也不直接返回 401</b>，而是"什么都不做"地放行。
 * 因为放行之后，Spring Security 的授权规则（{@code anyRequest().authenticated()}）
 * 会判定当前请求未认证，交给 AuthenticationEntryPointImpl 返回标准的 401 JSON。
 * 如果在这里直接返回 401，就把「白名单接口」也一起挡掉了——白名单接口
 * （登录、健康检查、接口文档）本来就没有 Token。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 已有认证信息就不重复解析（同一个请求里过滤器可能被多次触发）
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            LoginUser loginUser = tokenService.getLoginUser(request.getHeader(CommonConstants.TOKEN_HEADER));
            if (loginUser != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
