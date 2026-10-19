package com.erp.controller;

import com.erp.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查。放在启动模块里，属于"部署与运维"性质而非业务功能。
 * <p>
 * 它的实际用途：新同事拉下代码后，先访问这个接口确认「应用起来了、能对外服务」，
 * 再去排查登录问题 —— 把"服务没起来"和"鉴权不对"两类问题分开，能省很多来回。
 * <p>
 * 已在白名单里（erp.security.permit-urls），不需要 Token。
 */
@Tag(name = "健康检查", description = "服务可用性探测")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "健康检查", description = "无需登录，返回服务状态与服务器时间")
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("application", "erp-server");
        data.put("serverTime", LocalDateTime.now().toString());
        return R.ok(data);
    }
}
