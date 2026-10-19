package com.erp.system.domain.vo;

import com.erp.system.domain.SysUser;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 登录后获取当前用户信息的返回体，对应 {@code GET /api/auth/getInfo}。
 * <p>
 * 前端登录后立刻调这个接口，把 roles / permissions 存进 Pinia：
 * roles 用于 {@code v-hasRole} 与部分页面的显隐判断；
 * permissions 用于 {@code v-hasPermi} 控制按钮。
 */
@Data
public class UserInfoVo {

    private SysUser user;

    /** 角色标识集合，如 ["admin"] */
    private Set<String> roles;

    /** 权限串集合，如 ["mdm:material:add", ...]；超级管理员为 ["*:*:*"] */
    private Set<String> permissions;
}
