package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门实体，对应表 sys_dept。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /** 父部门 ID，顶级为 0。@NotNull 而非 @NotBlank：前端传 0 也是合法的（0 会被判为 blank 相关的坑在字符串上） */
    @NotNull(message = "上级部门不能为空")
    private Long parentId;

    /**
     * 祖级列表，如 "0,100"。
     * <p>
     * 由 Service 在新增/修改时自动维护，前端不需要传、也不允许传
     * （传错了会导致"查下级部门"整棵树都查错）。
     */
    private String ancestors;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Integer orderNum;

    private String leader;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 状态：0正常 1停用 */
    private String status;

    private String remark;

    /** 子部门，构造部门树时使用 */
    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}
