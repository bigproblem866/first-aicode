package com.erp.framework.security;

import com.erp.common.core.domain.R;
import com.erp.common.core.enums.ResultCode;
import com.erp.framework.web.JsonResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未登录 / Token 失效时返回 401。
 * <p>
 * 不实现这个类的话，Spring Security 默认会重定向到登录页（返回 302 + HTML），
 * 前端拿到的就不是 JSON，axios 的错误处理会走到奇怪的分支。
 * <p>
 * HTTP 状态码用 <b>401</b>（而不是 HTTP 200 + body.code=401）：
 * 这样前端 axios 会走 error 分支，能统一处理「登录过期 → 清 Token → 跳登录页」，
 * 不用在每个业务请求里判断 code。
 */
@Component
@RequiredArgsConstructor
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final JsonResponseWriter jsonResponseWriter;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        jsonResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                R.fail(ResultCode.UNAUTHORIZED));
    }
}
