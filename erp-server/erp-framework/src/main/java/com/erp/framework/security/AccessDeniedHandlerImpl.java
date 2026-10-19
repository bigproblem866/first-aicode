package com.erp.framework.security;

import com.erp.common.core.domain.R;
import com.erp.common.core.enums.ResultCode;
import com.erp.framework.web.JsonResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 已登录但权限不足时返回 403。
 * <p>
 * 触发场景：{@code @PreAuthorize("@ss.hasPermi('mdm:material:add')")} 校验不通过。
 * 与 401 区分开很重要——401 表示"你是谁不知道"，403 表示"知道你是谁，但你没这个权限"，
 * 前端对两者的处理不同（401 跳登录页，403 只弹提示，不能把人踢下线）。
 */
@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final JsonResponseWriter jsonResponseWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        jsonResponseWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                R.fail(ResultCode.FORBIDDEN));
    }
}
