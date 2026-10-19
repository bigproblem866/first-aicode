package com.erp.framework.web;

import com.erp.common.core.domain.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 在过滤器 / 异常处理器里直接往响应写 JSON。
 * <p>
 * 这些场景发生在 Spring MVC 的返回值处理链之外（过滤器、Security 的 EntryPoint），
 * 所以必须自己写响应体，不能靠 {@code @RestControllerAdvice} 的自动序列化。
 */
@Component
@RequiredArgsConstructor
public class JsonResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int httpStatus, R<?> body) throws IOException {
        response.setStatus(httpStatus);
        // 必须显式指定 charset，否则中文提示在 curl / 浏览器里会乱码
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
