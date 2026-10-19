package com.erp.system.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色列表查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleQuery extends PageQuery {

    /** 角色名称，模糊匹配 */
    private String roleName;

    /** 角色标识，模糊匹配 */
    private String roleKey;

    /** 状态：0正常 1停用 */
    private String status;
}
