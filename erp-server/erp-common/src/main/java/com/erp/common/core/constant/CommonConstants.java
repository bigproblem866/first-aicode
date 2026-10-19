package com.erp.common.core.constant;

/**
 * 全局常量。
 * <p>
 * 约定：常量类只放「多个模块都要用到」的值；只在一个模块内用的常量写在那个模块里。
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    // ---------------- 认证相关 ----------------

    /** 请求头名 */
    public static final String TOKEN_HEADER = "Authorization";
    /** Token 前缀，注意有个空格 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 超级管理员账号名。
     * <p>
     * 用作权限判断的兜底：即使有人误删了 admin 的角色关联，系统也不会被锁死。
     * <p>
     * 刻意<b>不</b>用「用户 ID == 1」来判断：主键由数据库自增生成，
     * 谁也不能保证 admin 就是 1 号（重建库、导数据、调整自增起点都会变），
     * 一旦判断失效，症状是「管理员突然没权限了」，极难联想到原因。
     */
    public static final String SUPER_ADMIN_USERNAME = "admin";
    /** 超级管理员角色标识 */
    public static final String SUPER_ADMIN_ROLE_KEY = "admin";
    /** 超级管理员权限通配符：拥有它即代表拥有全部权限，权限校验时直接放行 */
    public static final String ALL_PERMISSION = "*:*:*";

    // ---------------- 通用状态 ----------------

    /** 正常 */
    public static final String STATUS_NORMAL = "0";
    /** 停用 */
    public static final String STATUS_DISABLE = "1";

    /** 是 */
    public static final String YES = "Y";
    /** 否 */
    public static final String NO = "N";

    /** 通用成功/失败标识 */
    public static final String SUCCESS = "0";
    public static final String FAIL = "1";

    /**
     * 无登录态场景的操作人标识（应用启动初始化、定时任务等）。
     * create_by / update_by 永远不应该为 NULL，取不到当前用户时统一写这个值。
     */
    public static final String SYSTEM_OPERATOR = "system";

    // ---------------- 菜单类型（sys_menu.menu_type）----------------

    /** 目录 */
    public static final String MENU_TYPE_DIR = "M";
    /** 菜单 */
    public static final String MENU_TYPE_MENU = "C";
    /** 按钮（只做权限点，不出现在左侧导航） */
    public static final String MENU_TYPE_BUTTON = "F";

    /** 前端 Layout 组件标识，后端菜单树里用这个字符串告诉前端挂到主框架下 */
    public static final String LAYOUT = "Layout";
    /** 前端 ParentView 组件标识（多级目录的中间层） */
    public static final String PARENT_VIEW = "ParentView";
}
