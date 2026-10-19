package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色实体，对应表 sys_role。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称不能超过 50 个字符")
    private String roleName;

    /** 角色标识，如 admin。权限判断里用的就是它 */
    @NotBlank(message = "角色标识不能为空")
    @Size(max = 100, message = "角色标识不能超过 100 个字符")
    private String roleKey;

    private Integer roleSort;

    /** 数据范围：1全部 2本部门及以下 3本部门 4仅本人（本期只实现 1） */
    private String dataScope;

    /** 状态：0正常 1停用 */
    private String status;

    private String remark;

    /** 该角色拥有的菜单 ID 列表（新增/修改角色时前端传回的勾选结果） */
    @TableField(exist = false)
    private List<Long> menuIds;
}
