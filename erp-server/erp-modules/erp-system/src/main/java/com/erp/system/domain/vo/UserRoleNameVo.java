package com.erp.system.domain.vo;

import lombok.Data;

/**
 * 用户与角色名的扁平结果。
 * <p>
 * 用户列表要把「角色」列展示出来，但 {@code SysUser.roleNames} 不是表字段，
 * 单条查询又会产生 N+1。这里用一次 {@code IN (userIds)} 查出所有行，
 * 再在服务层按 userId 分组回填。
 */
@Data
public class UserRoleNameVo {

    private Long userId;

    private String roleName;
}
