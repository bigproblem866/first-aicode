package com.erp.system.controller;

import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.domain.R;
import com.erp.framework.security.SecurityUtils;
import com.erp.system.domain.dto.LoginBody;
import com.erp.system.domain.vo.LoginVo;
import com.erp.system.domain.vo.RouterVo;
import com.erp.system.domain.vo.UserInfoVo;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.SysLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口。
 * <p>
 * 路径前缀 /api/auth 是免登录白名单里唯一放开的一组（见 application.yml 的 erp.security.permit-urls），
 * 其中只有 login 不需要 Token，logout / getInfo / routers 虽然路径在白名单里……
 * <b>注意</b>：本工程的白名单是按路径精确匹配的，下面三个接口路径不在白名单内，
 * 因此依然需要 Token，未带 Token 会返回 401。
 */
@Tag(name = "认证管理", description = "登录、登出、获取用户信息与动态路由")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysLoginService sysLoginService;
    private final ISysMenuService sysMenuService;

    @Operation(summary = "登录", description = "成功返回 JWT，后续请求放在 Authorization: Bearer {token}")
    @PostMapping("/login")
    public R<LoginVo> login(@Valid @RequestBody LoginBody loginBody) {
        String token = sysLoginService.login(loginBody);
        return R.ok(new LoginVo(token));
    }

    @Operation(summary = "登出", description = "删除服务端登录态，Token 立即失效")
    @PostMapping("/logout")
    public R<Void> logout(
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String authorizationHeader) {
        sysLoginService.logout(authorizationHeader);
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息", description = "返回用户基本信息、角色标识集合、权限串集合")
    @GetMapping("/getInfo")
    public R<UserInfoVo> getInfo() {
        return R.ok(sysLoginService.getInfo());
    }

    @Operation(summary = "获取动态路由", description = "按当前用户的角色返回菜单树，前端据此 addRoute")
    @GetMapping("/routers")
    public R<List<RouterVo>> routers() {
        return R.ok(sysMenuService.buildRouters(SecurityUtils.getLoginUser()));
    }
}
