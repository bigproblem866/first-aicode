package com.erp.system.service;

import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.exception.ServiceException;
import com.erp.framework.security.LoginUser;
import com.erp.framework.security.SecurityUtils;
import com.erp.framework.security.TokenService;
import com.erp.framework.web.ServletUtils;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.dto.LoginBody;
import com.erp.system.domain.vo.UserInfoVo;
import com.erp.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录 / 登出 / 获取当前用户信息。
 * <p>
 * 认证过程：把用户名密码交给 {@code AuthenticationManager}，
 * 它会用 {@link UserDetailsServiceImpl} 加载用户、用 {@code BCryptPasswordEncoder}
 * 比对密码哈希。密码比对**绝不能**自己写 equals，BCrypt 每次加密的盐不同，
 * 只能通过 matches 校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final SysUserMapper sysUserMapper;

    /**
     * 登录，成功返回 Token。
     */
    public String login(LoginBody loginBody) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginBody.getUsername(), loginBody.getPassword()));
        } catch (BadCredentialsException e) {
            // 用户不存在与密码错误返回同一句提示，避免被用来探测账号是否存在
            throw new ServiceException("用户名或密码错误");
        } catch (AuthenticationException e) {
            log.warn("登录失败，用户名={}，原因={}", loginBody.getUsername(), e.getMessage());
            throw new ServiceException("登录失败：" + e.getMessage());
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 账号状态校验放在认证之后：DaoAuthenticationProvider 会包装非 UsernameNotFound 的异常，
        // 在 loadUserByUsername 里抛"已停用"会丢失原始提示（见 UserDetailsServiceImpl 注释）
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        if (CommonConstants.STATUS_DISABLE.equals(user.getStatus())) {
            throw new ServiceException("账号已停用，请联系管理员");
        }

        recordLoginInfo(user.getId());

        // 密码哈希绝不进入缓存与响应体
        loginUser.setPassword(null);
        return tokenService.createToken(loginUser);
    }

    /** 登出：删掉服务端登录态，Token 立即失效 */
    public void logout(String authorizationHeader) {
        tokenService.deleteLoginUser(authorizationHeader);
    }

    /**
     * 取当前登录用户信息 + 角色 + 权限。
     * <p>
     * 前端登录成功后立刻调它，把 roles / permissions 放进 Pinia。
     * 注意 user 里的密码字段虽然查出来了，但实体上标注了 WRITE_ONLY，不会序列化出去。
     */
    public UserInfoVo getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new ServiceException("用户不存在或已被删除，请重新登录");
        }
        user.setRoleNames(sysUserMapper.selectRoleNamesByUserId(user.getId()));

        UserInfoVo vo = new UserInfoVo();
        vo.setUser(user);
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        return vo;
    }

    /** 记录最后登录 IP 与时间。失败不影响登录流程本身 */
    private void recordLoginInfo(Long userId) {
        try {
            SysUser update = new SysUser();
            update.setId(userId);
            update.setLoginIp(ServletUtils.getClientIp());
            update.setLoginDate(LocalDateTime.now());
            sysUserMapper.updateById(update);
        } catch (Exception e) {
            log.warn("记录登录信息失败，userId={}", userId, e);
        }
    }
}
