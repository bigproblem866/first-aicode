package com.erp.framework.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求上下文工具。
 * <p>
 * 通过 {@link RequestContextHolder} 拿到当前请求，避免为了取一个 IP 或请求头
 * 就把 HttpServletRequest 一层层往 Service 里传。
 */
public final class ServletUtils {

    private ServletUtils() {
    }

    /** 取当前请求；不在请求线程中（如定时任务）返回 null */
    public static HttpServletRequest getRequestOrNull() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 取客户端真实 IP。
     * <p>
     * 必须按顺序检查代理头：经过 Nginx / 网关后 {@code getRemoteAddr()} 拿到的是代理的 IP，
     * 直接用它记录登录 IP 就全是 "127.0.0.1"，等于没记。
     */
    public static String getClientIp() {
        HttpServletRequest request = getRequestOrNull();
        if (request == null) {
            return null;
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能是 "客户端IP, 代理1, 代理2"，取第一个
                int comma = ip.indexOf(',');
                return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
