package com.erp.system.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户列表查询条件。继承 PageQuery 即自动获得 pageNum / pageSize / 排序参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQuery extends PageQuery {

    /** 登录账号，模糊匹配 */
    private String username;

    /** 昵称，模糊匹配 */
    private String nickName;

    /** 手机号，模糊匹配 */
    private String phone;

    /** 状态：0正常 1停用，精确匹配 */
    private String status;

    /** 所属部门，按部门树查询时传 */
    private Long deptId;
}
