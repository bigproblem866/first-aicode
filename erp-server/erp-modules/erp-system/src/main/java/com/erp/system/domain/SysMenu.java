package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单实体，对应表 sys_menu。
 * <p>
 * 字段组合规则见 R__seed_system_menu.sql 头部注释：
 * M 目录 / C 菜单 / F 按钮。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    private Long parentId;

    private Integer orderNum;

    /** 路由地址。目录是一级地址（system），菜单是相对子地址（user） */
    private String path;

    /** 前端组件路径，相对 views/，不带 .vue（system/user/index） */
    private String component;

    /** M目录 C菜单 F按钮 */
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    /** 是否显示：0显示 1隐藏 */
    private String visible;

    /** 状态：0正常 1停用 */
    private String status;

    /** 权限串，如 system:user:add */
    private String perms;

    private String icon;

    /** 功能分组（仅功能导航页分区展示用，不参与路由与权限） */
    private String groupName;

    /** 是否外链：0否 1是 */
    private Integer isFrame;

    /** 是否缓存页面：0缓存 1不缓存 */
    private Integer isCache;

    private String remark;

    // ---------------- 非数据库字段 ----------------

    /** 子菜单，构造菜单树时使用 */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
